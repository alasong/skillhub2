package com.iflytek.skillhub.controller.v2;

import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.SkillVersionStatus;
import com.iflytek.skillhub.domain.skill.service.VersionResolutionService;
import com.iflytek.skillhub.domain.skill.service.VersionResolutionService.Semver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * /api/v2/skills/{name}/versions — Semantic versioning endpoints.
 * <p>
 * Provides paginated version listing with deprecation metadata and semver constraint
 * resolution for the skill hub registry.
 */
@RestController
@RequestMapping("/api/v2/skills")
public class VersionController {

    private final SkillRepository skillRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final VersionResolutionService versionResolutionService;

    public VersionController(SkillRepository skillRepository,
                             SkillVersionRepository skillVersionRepository,
                             VersionResolutionService versionResolutionService) {
        this.skillRepository = skillRepository;
        this.skillVersionRepository = skillVersionRepository;
        this.versionResolutionService = versionResolutionService;
    }

    // ------------------------------------------------------------------
    //  Response DTOs
    // ------------------------------------------------------------------

    /** Paginated wrapper matching the existing v2 API response shape. */
    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {}

    /** A single version row in the versions list. */
    public record VersionRow(
            String version,
            Instant publishedAt,
            boolean deprecated,
            String deprecationMessage,
            long downloadCount,
            SemverInfo semver
    ) {}

    /** Pre-parsed semver components for client-side sorting/filtering. */
    public record SemverInfo(
            int major,
            int minor,
            int patch,
            List<String> prerelease
    ) {}

    // ------------------------------------------------------------------
    //  GET /api/v2/skills/{name}/versions
    // ------------------------------------------------------------------

    /**
     * Paginated version list for a skill.
     *
     * @param name               skill slug name
     * @param namespace          skill namespace (required query param)
     * @param page               zero-based page number (default 0)
     * @param size               page size (default 20)
     * @param includePrerelease  if true, include prerelease versions (default false)
     * @return paginated version rows ordered by semver descending
     */
    @GetMapping("/{name}/versions")
    public ResponseEntity<PageResponse<VersionRow>> listVersions(
            @PathVariable String name,
            @RequestParam String namespace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includePrerelease) {

        List<Skill> skills = skillRepository.findByNamespaceSlugAndSlug(namespace, name);
        if (skills.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Skill skill = skills.get(0);

        List<SkillVersion> allVersions = skillVersionRepository
                .findBySkillIdAndStatus(skill.getId(), SkillVersionStatus.PUBLISHED);

        // Parse, filter prerelease, sort descending by semver
        List<VersionRow> rows = allVersions.stream()
                .map(v -> {
                    try {
                        Semver sv = Semver.parse(v.getVersion());
                        return Map.entry(v, sv);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(e -> includePrerelease || !e.getValue().isPrerelease())
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> toVersionRow(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Manual pagination
        int start = Math.min(page * size, rows.size());
        int end = Math.min(start + size, rows.size());
        List<VersionRow> pageContent = rows.subList(start, end);

        PageResponse<VersionRow> response = new PageResponse<>(
                pageContent,
                rows.size(),
                (int) Math.ceil((double) rows.size() / size),
                page,
                size
        );
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------
    //  GET /api/v2/skills/{name}/versions/resolve
    // ------------------------------------------------------------------

    /**
     * Resolve a semver constraint to the best matching version.
     *
     * @param name       skill slug name
     * @param namespace  skill namespace
     * @param constraint semver range expression, e.g. "^1.2.3"
     * @return the resolved version metadata
     */
    @GetMapping("/{name}/versions/resolve")
    public ResponseEntity<?> resolveVersion(
            @PathVariable String name,
            @RequestParam String namespace,
            @RequestParam String constraint) {

        VersionResolutionService.VersionResolution resolution =
                versionResolutionService.resolve(namespace, name, constraint);

        return ResponseEntity.ok(Map.of(
                "namespace", resolution.namespace(),
                "name", resolution.name(),
                "constraint", resolution.constraint(),
                "resolvedVersion", resolution.resolvedVersion(),
                "rangeDescription", resolution.rangeDescription()
        ));
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private static VersionRow toVersionRow(SkillVersion v, Semver sv) {
        return new VersionRow(
                v.getVersion(),
                v.getPublishedAt(),
                v.isDeprecated(),
                v.getDeprecationMessage(),
                0L, // download count lives in skill_version_stats — omitted for brevity
                new SemverInfo(sv.major, sv.minor, sv.patch, sv.prerelease)
        );
    }
}
