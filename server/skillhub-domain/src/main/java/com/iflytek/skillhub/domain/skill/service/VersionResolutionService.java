package com.iflytek.skillhub.domain.skill.service;

import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionStatus;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolves semver version constraints against available skill versions.
 *
 * <p>Provides constraint parsing (^, ~, ranges, x-ranges, exact pins)
 * and best-match resolution using simple major.minor.patch comparison.
 * Prerelease versions are excluded unless {@code includePrerelease} is set.
 */
@Service
public class VersionResolutionService {

    // -----------------------------------------------------------------------
    //  Simple semver value object — parses "1.2.3" / "1.2.3-alpha.1"
    // -----------------------------------------------------------------------

    public static class Semver implements Comparable<Semver> {
        public final int major;
        public final int minor;
        public final int patch;
        public final List<String> prerelease;
        private final String original;

        public Semver(int major, int minor, int patch, List<String> prerelease, String original) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.prerelease = prerelease;
            this.original = original;
        }

        /**
         * Parse a version string into its major.minor.patch components.
         * Prerelease suffix (after '-') is captured but not compared numerically.
         */
        public static Semver parse(String v) {
            Objects.requireNonNull(v, "version must not be null");
            String original = v;
            String pre = null;
            int dash = v.indexOf('-');
            if (dash >= 0) {
                pre = v.substring(dash + 1);
                v = v.substring(0, dash);
            }
            String[] parts = v.split("\\.");
            int major = 0, minor = 0, patch = 0;
            try {
                major = Integer.parseInt(parts[0]);
                if (parts.length > 1) {
                    minor = Integer.parseInt(parts[1]);
                }
                if (parts.length > 2) {
                    patch = Integer.parseInt(parts[2]);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid semver: " + original, e);
            }
            List<String> prerelease = (pre == null || pre.isEmpty())
                    ? List.of()
                    : List.of(pre.split("\\."));
            return new Semver(major, minor, patch, prerelease, original);
        }

        public boolean isPrerelease() {
            return !prerelease.isEmpty();
        }

        /**
         * Compare major → minor → patch numerically.
         * Prerelease versions sort before their release counterpart (e.g. 1.0.0-alpha &lt; 1.0.0).
         * Identical prerelease identifier lists are equal.
         */
        @Override
        public int compareTo(Semver o) {
            int cmp = Integer.compare(this.major, o.major);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.minor, o.minor);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(this.patch, o.patch);
            if (cmp != 0) return cmp;
            // Prerelease: 1.0.0-alpha < 1.0.0
            if (this.isPrerelease() && !o.isPrerelease()) return -1;
            if (!this.isPrerelease() && o.isPrerelease()) return 1;
            // Compare prerelease identifiers lexicographically
            for (int i = 0; i < Math.min(this.prerelease.size(), o.prerelease.size()); i++) {
                cmp = this.prerelease.get(i).compareTo(o.prerelease.get(i));
                if (cmp != 0) return cmp;
            }
            return Integer.compare(this.prerelease.size(), o.prerelease.size());
        }

        @Override
        public String toString() {
            return original;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Semver semver)) return false;
            return major == semver.major && minor == semver.minor && patch == semver.patch
                    && prerelease.equals(semver.prerelease);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, prerelease);
        }
    }

    // -----------------------------------------------------------------------
    //  Semver range — parsed constraint expression
    // -----------------------------------------------------------------------

    /**
     * A version range parsed from a constraint string. Supports:
     * <ul>
     *   <li>{@code ^1.2.3} → {@code [1.2.3, 2.0.0)}</li>
     *   <li>{@code ~1.2.3} → {@code [1.2.3, 1.3.0)}</li>
     *   <li>{@code >=1.0} → {@code [1.0.0, ∞)}</li>
     *   <li>{@code >=1.0 <2.0} → {@code [1.0.0, 2.0.0)}</li>
     *   <li>{@code 1.2.3} → exact pin {@code =1.2.3}</li>
     *   <li>{@code 1.x} → {@code [1.0.0, 2.0.0)}</li>
     *   <li>{@code 1.2.x} → {@code [1.2.0, 1.3.0)}</li>
     * </ul>
     */
    public static class SemverRange {
        private final Semver lower;
        private final boolean lowerInclusive;
        private final Semver upper;
        private final boolean upperInclusive;
        private final String description;

        public SemverRange(Semver lower, boolean lowerInclusive,
                           Semver upper, boolean upperInclusive,
                           String description) {
            this.lower = lower;
            this.lowerInclusive = lowerInclusive;
            this.upper = upper;
            this.upperInclusive = upperInclusive;
            this.description = description;
        }

        /**
         * Returns true if the given version falls within this range.
         * Prerelease versions are always excluded from range matching.
         */
        public boolean contains(Semver v) {
            if (v.isPrerelease()) return false;
            if (lower != null) {
                int cmp = v.compareTo(lower);
                if (cmp < 0 || (cmp == 0 && !lowerInclusive)) return false;
            }
            if (upper != null) {
                int cmp = v.compareTo(upper);
                if (cmp > 0 || (cmp == 0 && !upperInclusive)) return false;
            }
            return true;
        }

        /** Human-readable description of this range, e.g. ">=1.2.3 <2.0.0". */
        public String describe() {
            return description;
        }

        // ----- parsing -----

        public static SemverRange parse(String constraint) {
            if (constraint == null || constraint.isBlank()) {
                return new SemverRange(null, true, null, true, "latest");
            }
            constraint = constraint.trim();

            // Compound ranges: ">=1.0 <2.0"
            if (constraint.contains(" ")) {
                String[] parts = constraint.split("\\s+");
                if (parts.length >= 2) {
                    SemverRange lowerRange = parseSingle(parts[0]);
                    SemverRange upperRange = parseSingle(parts[parts.length - 1]);
                    return new SemverRange(
                            lowerRange.lower, lowerRange.lowerInclusive,
                            upperRange.upper, upperRange.upperInclusive,
                            constraint
                    );
                }
            }
            return parseSingle(constraint);
        }

        private static SemverRange parseSingle(String c) {
            if (c.startsWith("^")) return parseCaret(c.substring(1));
            if (c.startsWith("~")) return parseTilde(c.substring(1));
            if (c.startsWith(">=")) return parseGte(c.substring(2));
            if (c.startsWith(">")) return parseGt(c.substring(1));
            if (c.startsWith("<=")) return parseLte(c.substring(2));
            if (c.startsWith("<")) return parseLt(c.substring(1));
            if (c.endsWith(".x") || c.endsWith(".X") || c.endsWith(".*")) {
                return parseXRange(c);
            }
            // Exact pin
            Semver v = Semver.parse(c);
            return new SemverRange(v, true, v, true, "=" + c);
        }

        /**
         * Caret range: {@code ^1.2.3} → {@code >=1.2.3 <2.0.0}
         * (always bumps the leftmost non-zero segment).
         */
        private static SemverRange parseCaret(String v) {
            Semver base = Semver.parse(v);
            Semver upper;
            String upperStr;
            if (base.major != 0) {
                upperStr = (base.major + 1) + ".0.0";
                upper = new Semver(base.major + 1, 0, 0, List.of(), upperStr);
            } else if (base.minor != 0) {
                upperStr = "0." + (base.minor + 1) + ".0";
                upper = new Semver(0, base.minor + 1, 0, List.of(), upperStr);
            } else {
                upperStr = "0.0." + (base.patch + 1);
                upper = new Semver(0, 0, base.patch + 1, List.of(), upperStr);
            }
            return new SemverRange(base, true, upper, false,
                    ">=" + base.original + " <" + upperStr);
        }

        /**
         * Tilde range: {@code ~1.2.3} → {@code >=1.2.3 <1.3.0}
         * (allows patch-level changes only).
         */
        private static SemverRange parseTilde(String v) {
            Semver base = Semver.parse(v);
            String upperStr = base.major + "." + (base.minor + 1) + ".0";
            Semver upper = new Semver(base.major, base.minor + 1, 0, List.of(), upperStr);
            return new SemverRange(base, true, upper, false,
                    ">=" + base.original + " <" + upperStr);
        }

        private static SemverRange parseGte(String v) {
            Semver base = Semver.parse(v);
            return new SemverRange(base, true, null, true, ">=" + v);
        }

        private static SemverRange parseGt(String v) {
            Semver base = Semver.parse(v);
            return new SemverRange(base, false, null, true, ">" + v);
        }

        private static SemverRange parseLte(String v) {
            Semver base = Semver.parse(v);
            return new SemverRange(null, true, base, true, "<=" + v);
        }

        private static SemverRange parseLt(String v) {
            Semver base = Semver.parse(v);
            return new SemverRange(null, true, base, false, "<" + v);
        }

        /**
         * X-range: {@code 1.x} → {@code [1.0.0, 2.0.0)},
         * {@code 1.2.x} → {@code [1.2.0, 1.3.0)}.
         */
        private static SemverRange parseXRange(String c) {
            String normalized = c.toLowerCase(Locale.ROOT).replace('*', 'x');
            String[] parts = normalized.split("\\.");
            int major = Integer.parseInt(parts[0]);
            if (parts.length == 1 || "x".equals(parts[1])) {
                // "1.x" -> >=1.0.0 <2.0.0
                Semver lower = new Semver(major, 0, 0, List.of(), major + ".0.0");
                Semver upper = new Semver(major + 1, 0, 0, List.of(), (major + 1) + ".0.0");
                return new SemverRange(lower, true, upper, false,
                        ">=" + lower + " <" + upper);
            }
            int minor = Integer.parseInt(parts[1]);
            // "1.2.x" -> >=1.2.0 <1.3.0
            Semver lo = new Semver(major, minor, 0, List.of(), major + "." + minor + ".0");
            Semver up = new Semver(major, minor + 1, 0, List.of(), major + "." + (minor + 1) + ".0");
            return new SemverRange(lo, true, up, false,
                    ">=" + lo + " <" + up);
        }
    }

    // -----------------------------------------------------------------------
    //  Resolution result value object
    // -----------------------------------------------------------------------

    /**
     * The result of resolving a semver constraint against a skill's published versions.
     *
     * @param namespace        skill namespace
     * @param name             skill name
     * @param constraint       original constraint expression
     * @param resolvedVersion  the best-matching version string
     * @param rangeDescription human-readable expansion of the constraint
     */
    public record VersionResolution(
            String namespace,
            String name,
            String constraint,
            String resolvedVersion,
            String rangeDescription
    ) {}

    // -----------------------------------------------------------------------
    //  Exceptions
    // -----------------------------------------------------------------------

    /**
     * Thrown when no published version satisfies the given constraint.
     * Translated to HTTP 404 by the exception handler.
     */
    public static class VersionNotFoundException extends DomainNotFoundException {
        public VersionNotFoundException(String namespace, String name, String constraint) {
            super("version.notFound", namespace + "/" + name, constraint);
        }
    }

    /**
     * Thrown when a dependency graph contains a cycle.
     * Translated to HTTP 422 by the exception handler.
     */
    public static class CircularDependencyException extends DomainBadRequestException {
        public CircularDependencyException(String qualifiedName) {
            super("dependency.circular", qualifiedName);
        }
    }

    // -----------------------------------------------------------------------
    //  Service fields & methods
    // -----------------------------------------------------------------------

    private final SkillRepository skillRepository;
    private final SkillVersionRepository skillVersionRepository;

    public VersionResolutionService(SkillRepository skillRepository,
                                    SkillVersionRepository skillVersionRepository) {
        this.skillRepository = skillRepository;
        this.skillVersionRepository = skillVersionRepository;
    }

    /**
     * Resolve a semver constraint to the best matching published version of a skill.
     * Prerelease versions are excluded by default.
     *
     * @param namespace  skill namespace
     * @param name       skill name
     * @param constraint semver range expression (or null/blank for latest)
     * @return the highest non-prerelease version satisfying the constraint
     * @throws VersionNotFoundException if no version matches
     */
    public VersionResolution resolve(String namespace, String name, String constraint) {
        return resolve(namespace, name, constraint, false);
    }

    /**
     * Resolve a semver constraint, optionally including prerelease versions.
     *
     * @param namespace          skill namespace
     * @param name               skill name
     * @param constraint         semver range expression (or null/blank for latest)
     * @param includePrerelease  if true, prerelease versions are also considered
     * @return the highest version satisfying the constraint
     * @throws VersionNotFoundException if no version matches
     */
    public VersionResolution resolve(String namespace, String name,
                                     String constraint, boolean includePrerelease) {
        List<Skill> skills = skillRepository.findByNamespaceSlugAndSlug(namespace, name);
        if (skills.isEmpty()) {
            throw new VersionNotFoundException(namespace, name, constraint);
        }
        Skill skill = skills.get(0);

        List<SkillVersion> versions = skillVersionRepository.findBySkillIdAndStatus(
                skill.getId(), SkillVersionStatus.PUBLISHED);
        if (versions.isEmpty()) {
            throw new VersionNotFoundException(namespace, name, constraint);
        }

        SemverRange range = SemverRange.parse(constraint);

        SkillVersion best = versions.stream()
                .map(v -> {
                    try {
                        return Map.entry(v, Semver.parse(v.getVersion()));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(e -> includePrerelease || !e.getValue().isPrerelease())
                .filter(e -> range.contains(e.getValue()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new VersionNotFoundException(namespace, name, constraint));

        return new VersionResolution(namespace, name, constraint,
                best.getVersion(), range.describe());
    }

    /**
     * Batch-resolve all entries in the dependency map, detecting circular references.
     *
     * <p>Each entry in {@code dependencies} is a skill name → version constraint pair.
     * This method resolves each dependency once and throws
     * {@link CircularDependencyException} if a cycle is detected.
     *
     * @param namespace    the shared namespace under which all dependencies are resolved
     * @param dependencies skill-name → version constraint map
     * @return resolved name → VersionResolution map in insertion order
     */
    public Map<String, VersionResolution> resolveDependencies(
            String namespace, Map<String, String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return Map.of();
        }
        Map<String, VersionResolution> resolved = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        for (Map.Entry<String, String> entry : dependencies.entrySet()) {
            resolveRecursive(namespace, entry.getKey(), entry.getValue(),
                    resolved, visiting);
        }
        return resolved;
    }

    private void resolveRecursive(String namespace, String name, String constraint,
                                  Map<String, VersionResolution> resolved,
                                  Set<String> visiting) {
        if (resolved.containsKey(name)) {
            return;
        }
        if (!visiting.add(name)) {
            throw new CircularDependencyException(namespace + "/" + name);
        }
        VersionResolution resolution = resolve(namespace, name, constraint);
        resolved.put(name, resolution);
        visiting.remove(name);
    }
}
