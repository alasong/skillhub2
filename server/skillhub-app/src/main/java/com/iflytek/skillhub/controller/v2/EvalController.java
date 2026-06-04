package com.iflytek.skillhub.controller.v2;

import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResult;
import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResultRepository;
import com.iflytek.skillhub.domain.skill.metadata.EvalRun;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunStatus;
import com.iflytek.skillhub.domain.skill.service.EvalEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for the evaluation framework (v2 API).
 *
 * <p>Provides endpoints to trigger evaluation runs, query run status,
 * and retrieve the latest evaluation results for a skill version.
 */
@RestController
@RequestMapping("/api/v2")
public class EvalController {

    private final EvalEngine evalEngine;
    private final EvalCaseResultRepository caseResultRepository;

    public EvalController(EvalEngine evalEngine,
                          EvalCaseResultRepository caseResultRepository) {
        this.evalEngine = evalEngine;
        this.caseResultRepository = caseResultRepository;
    }

    /**
     * POST /api/v2/skills/{name}/versions/{version}/eval
     *
     * Triggers a new evaluation run for the specified skill version.
     * Returns 202 Accepted with the eval run ID and resource links.
     */
    @PostMapping("/skills/{name}/versions/{version}/eval")
    public ResponseEntity<Map<String, Object>> triggerEval(
            @PathVariable String name,
            @PathVariable String version) {

        EvalRun run = evalEngine.execute(name, version);

        if (run.getStatus() == EvalRunStatus.ERROR) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to start evaluation run",
                    "evalRunId", run.getId().toString()
            ));
        }

        return ResponseEntity.accepted().body(Map.of(
                "status", "ACCEPTED",
                "evalRunId", run.getId().toString(),
                "skillName", name,
                "skillVersion", version,
                "links", Map.of(
                        "self", "/api/v2/eval/runs/" + run.getId(),
                        "latest", "/api/v2/skills/" + name + "/versions/" + version + "/eval/latest"
                )
        ));
    }

    /**
     * GET /api/v2/eval/runs/{id}
     *
     * Retrieves the full evaluation run result including per-case results.
     */
    @GetMapping("/eval/runs/{id}")
    public ResponseEntity<?> getEvalRun(@PathVariable UUID id) {
        EvalRun run = evalEngine.getRun(id);
        if (run == null) {
            return ResponseEntity.notFound().build();
        }
        List<EvalCaseResult> cases = caseResultRepository.findByEvalRunId(id);
        return ResponseEntity.ok(buildResponse(run, cases));
    }

    /**
     * GET /api/v2/skills/{name}/versions/{version}/eval/latest
     *
     * Retrieves the most recent evaluation run for the given skill version.
     */
    @GetMapping("/skills/{name}/versions/{version}/eval/latest")
    public ResponseEntity<?> getLatestEval(
            @PathVariable String name,
            @PathVariable String version) {

        return evalEngine.getLatest(name, version)
                .map(run -> {
                    List<EvalCaseResult> cases = caseResultRepository.findByEvalRunId(run.getId());
                    return ResponseEntity.ok(buildResponse(run, cases));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------
    // Response builder
    // ---------------------------------------------------------------

    private Map<String, Object> buildResponse(EvalRun run, List<EvalCaseResult> cases) {
        long totalDurationMs = 0;
        if (run.getStartedAt() != null && run.getCompletedAt() != null) {
            totalDurationMs = run.getCompletedAt().toEpochMilli() - run.getStartedAt().toEpochMilli();
        }

        List<Map<String, Object>> caseList = cases.stream()
                .map(c -> {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("caseName", c.getCaseName());
                    cm.put("passed", c.isPassed());
                    cm.put("validatorType", c.getValidatorType() != null ? c.getValidatorType().name() : null);
                    cm.put("durationMs", c.getDurationMs());
                    cm.put("errorMessage", c.getErrorMessage());
                    cm.put("retryAttempts", c.getRetryAttempts());
                    cm.put("actualOutput", c.getActualOutput());
                    cm.put("expectedOutput", c.getExpectedOutput());
                    return cm;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("evalRunId", run.getId().toString());
        response.put("skillName", run.getSkillName());
        response.put("skillVersion", run.getSkillVersion());
        response.put("status", run.getStatus().name());
        response.put("overallScore", run.getOverallScore());
        response.put("passRate", run.getPassRate());
        response.put("totalCases", run.getTotalCases());
        response.put("passedCases", run.getPassedCases());
        response.put("startedAt", run.getStartedAt());
        response.put("completedAt", run.getCompletedAt());
        response.put("durationMs", totalDurationMs);
        response.put("cases", caseList);

        return response;
    }
}
