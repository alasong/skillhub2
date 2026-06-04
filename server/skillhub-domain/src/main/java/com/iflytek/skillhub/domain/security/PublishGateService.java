package com.iflytek.skillhub.domain.security;

import com.iflytek.skillhub.domain.skill.metadata.EvalRun;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunRepository;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunStatus;
import com.iflytek.skillhub.domain.skill.metadata.SkillManifest;
import com.iflytek.skillhub.domain.skill.service.SkillManifestLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quality gate that must pass before a skill version can be published.
 *
 * <p>Checks three things in order:
 * <ol>
 *   <li>Kill switch — hard-deprecated skills cannot be published</li>
 *   <li>Scan report — a passing security scan is required</li>
 *   <li>Eval result — a passing evaluation run is required (unless skipped)</li>
 * </ol>
 */
@Service
public class PublishGateService {

    private static final Logger log = LoggerFactory.getLogger(PublishGateService.class);

    private final KillSwitchService killSwitchService;
    private final ScanReportService scanReportService;
    private final EvalRunRepository evalRunRepository;
    private final SkillManifestLoader manifestLoader;

    public PublishGateService(KillSwitchService killSwitchService,
                              ScanReportService scanReportService,
                              EvalRunRepository evalRunRepository,
                              SkillManifestLoader manifestLoader) {
        this.killSwitchService = killSwitchService;
        this.scanReportService = scanReportService;
        this.evalRunRepository = evalRunRepository;
        this.manifestLoader = manifestLoader;
    }

    /**
     * Original check method — uses default EvalGateOptions (eval check enabled).
     */
    public GateResult check(String skillName, String skillVersion) {
        return check(skillName, skillVersion, EvalGateOptions.DEFAULT);
    }

    /**
     * Runs all three gate checks. If skipEval is true the eval gate is bypassed.
     */
    public GateResult check(String skillName, String skillVersion, EvalGateOptions options) {
        List<String> blockers = new ArrayList<>();

        // 1. Kill switch check
        if (killSwitchService.isHardDeprecated(skillName, skillVersion)) {
            blockers.add("HARD_DEPRECATED: " + skillName + ":" + skillVersion + " has been killed");
        }

        // 2. Scan report check
        if (!scanReportService.canPublish(skillName, skillVersion)) {
            blockers.add("SCAN_GATE_BLOCKED: no passing scan report for " + skillName + ":" + skillVersion);
        }

        // 3. Eval gate check (NEW)
        if (!options.skipEval()) {
            GateResult evalGate = checkEvalGate(skillName, skillVersion);
            if (!evalGate.passed()) {
                blockers.addAll(evalGate.blockers());
            }
        }

        if (blockers.isEmpty()) {
            return GateResult.PASSED;
        }

        log.warn("Publish gate blocked for {}:{} — {}", skillName, skillVersion, blockers);
        return GateResult.blocked(blockers);
    }

    /**
     * Evaluates whether the latest eval run for this skill version meets the minimum pass rate.
     */
    private GateResult checkEvalGate(String skillName, String skillVersion) {
        Optional<EvalRun> latest = evalRunRepository
                .findTopBySkillNameAndSkillVersionOrderByCreatedAtDesc(skillName, skillVersion);

        if (latest.isEmpty()) {
            return GateResult.blocked(List.of(
                    "EVAL_FAILED: no eval run found for " + skillName + ":" + skillVersion));
        }

        EvalRun run = latest.get();
        if (run.getStatus() != EvalRunStatus.PASSED) {
            return GateResult.blocked(List.of(
                    "EVAL_FAILED: latest eval run " + run.getId() + " status is " + run.getStatus()
                            + " (score=" + run.getOverallScore() + ")"));
        }

        // Load the manifest to get minPassRate
        try {
            SkillManifest manifest = manifestLoader.load(skillName, skillVersion);
            SkillManifest.EvalSpec evalSpec = manifest.getEval();
            if (evalSpec == null) {
                // No eval spec means no requirement — the existence of a PASSED run is enough
                return GateResult.PASSED;
            }

            double minPassRate = evalSpec.getMinPassRate() / 100.0;

            if (run.getPassRate() != null && run.getPassRate() < minPassRate) {
                return GateResult.blocked(List.of(
                        String.format("EVAL_FAILED: pass rate %.2f < min required %.2f",
                                run.getPassRate(), minPassRate)));
            }
        } catch (Exception e) {
            log.warn("Could not load manifest for eval gate check on {}:{}: {}",
                    skillName, skillVersion, e.getMessage());
            // If we can't load the manifest but a PASSED run exists, allow it
            return GateResult.PASSED;
        }

        return GateResult.PASSED;
    }

    public record GateResult(boolean passed, List<String> blockers) {
        public static final GateResult PASSED = new GateResult(true, List.of());

        public static GateResult blocked(List<String> blockers) {
            return new GateResult(false, List.copyOf(blockers));
        }
    }
}
