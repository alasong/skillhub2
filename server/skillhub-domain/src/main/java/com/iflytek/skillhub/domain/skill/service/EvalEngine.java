package com.iflytek.skillhub.domain.skill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResult;
import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResultRepository;
import com.iflytek.skillhub.domain.skill.metadata.EvalRun;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunRepository;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunStatus;
import com.iflytek.skillhub.domain.skill.metadata.SkillManifest;
import com.iflytek.skillhub.domain.skill.pipeline.ParameterMapping;
import com.iflytek.skillhub.domain.skill.pipeline.PipelineEdge;
import com.iflytek.skillhub.domain.skill.pipeline.PipelineNode;
import com.iflytek.skillhub.domain.skill.pipeline.PipelineService;
import com.iflytek.skillhub.domain.skill.pipeline.SkillPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core evaluation engine that orchestrates skill evaluation runs.
 *
 * <p>For each eval run, the engine:
 * <ol>
 *   <li>Loads the SkillManifest for the target skill version</li>
 *   <li>Converts each EvalCase into a PipelineNode within a temporary DAG</li>
 *   <li>Executes each case (with pass@k retry logic)</li>
 *   <li>Applies the configured validator to determine pass/fail</li>
 *   <li>Computes aggregate pass rate and overall score</li>
 *   <li>Persists all results and returns the completed EvalRun</li>
 * </ol>
 */
@Service
public class EvalEngine {

    private static final Logger log = LoggerFactory.getLogger(EvalEngine.class);

    private static final String EVAL_ROOT_NODE_ID = "eval-root";

    private static final int MAX_TIMEOUT_SECONDS = 600;

    private final PipelineService pipelineService;
    private final EvalRunRepository evalRunRepository;
    private final EvalCaseResultRepository caseResultRepository;
    private final SkillManifestLoader manifestLoader;
    private final SkillExecutionService executionService;
    private final JsonValidator jsonValidator;
    private final LlmJudgeService llmJudgeService;
    private final ObjectMapper objectMapper;

    public EvalEngine(PipelineService pipelineService,
                      EvalRunRepository evalRunRepository,
                      EvalCaseResultRepository caseResultRepository,
                      SkillManifestLoader manifestLoader,
                      SkillExecutionService executionService,
                      JsonValidator jsonValidator,
                      LlmJudgeService llmJudgeService,
                      ObjectMapper objectMapper) {
        this.pipelineService = pipelineService;
        this.evalRunRepository = evalRunRepository;
        this.caseResultRepository = caseResultRepository;
        this.manifestLoader = manifestLoader;
        this.executionService = executionService;
        this.jsonValidator = jsonValidator;
        this.llmJudgeService = llmJudgeService;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Executes a full evaluation run for the given skill version.
     *
     * @param skillName    the skill name
     * @param skillVersion the skill version
     * @return the completed or errored EvalRun
     */
    @Transactional
    public EvalRun execute(String skillName, String skillVersion) {
        return execute(skillName, skillVersion, "system");
    }

    /**
     * Executes a full evaluation run, recording who triggered it.
     *
     * @param skillName    the skill name
     * @param skillVersion the skill version
     * @param triggeredBy  user ID or "system"
     * @return the completed or errored EvalRun
     */
    @Transactional
    public EvalRun execute(String skillName, String skillVersion, String triggeredBy) {
        // 1. Load the SkillManifest
        SkillManifest manifest;
        try {
            manifest = manifestLoader.load(skillName, skillVersion);
        } catch (Exception e) {
            log.error("Failed to load manifest for {}:{}: {}", skillName, skillVersion, e.getMessage());
            EvalRun run = createErrorRun(skillName, skillVersion, triggeredBy);
            return evalRunRepository.save(run);
        }

        // 2. Validate manifest has eval section with cases
        SkillManifest.EvalSpec eval = manifest.getEval();
        if (eval == null || eval.getCases() == null || eval.getCases().isEmpty()) {
            log.warn("No eval cases found for {}:{}", skillName, skillVersion);
            EvalRun run = createErrorRun(skillName, skillVersion, triggeredBy);
            run.setTotalCases(0);
            return evalRunRepository.save(run);
        }

        List<SkillManifest.EvalCase> cases = eval.getCases();
        int k = Math.max(1, eval.getK());
        int minPassRate = eval.getMinPassRate();
        int timeout = resolveTimeout(manifest);

        // 3. Create initial EvalRun record
        EvalRun run = new EvalRun(skillName, skillVersion);
        run.setStatus(EvalRunStatus.PENDING);
        run.setTriggeredBy(triggeredBy);
        run.setTotalCases(cases.size());
        run = evalRunRepository.save(run);

        // 4. Build and save the pipeline DAG
        SkillPipeline pipeline;
        try {
            pipeline = buildPipeline(skillName, skillVersion, cases, timeout, k);
            pipeline = pipelineService.save(pipeline);
        } catch (Exception e) {
            log.error("Failed to create eval pipeline for {}:{}: {}", skillName, skillVersion, e.getMessage());
            run.setStatus(EvalRunStatus.ERROR);
            return evalRunRepository.save(run);
        }

        // 5. Transition to RUNNING
        run.setPipelineId(pipeline.getId());
        run.setStatus(EvalRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = evalRunRepository.save(run);

        // 6. Execute each case
        int passedCount = 0;
        UUID runId = run.getId();

        for (SkillManifest.EvalCase evalCase : cases) {
            EvalCaseResult result = executeCase(runId, evalCase, skillName, skillVersion, k, timeout);
            caseResultRepository.save(result);
            if (result.isPassed()) {
                passedCount++;
                log.debug("Case '{}' PASSED (attempt {})", evalCase.getName(), result.getRetryAttempts() + 1);
            } else {
                log.warn("Case '{}' FAILED after {} attempts: {}",
                        evalCase.getName(), result.getRetryAttempts() + 1, result.getErrorMessage());
            }
        }

        // 7. Compute aggregate metrics
        int totalCases = cases.size();
        double passRate = totalCases > 0 ? (double) passedCount / totalCases : 0.0;
        double overallScore = passRate * 100.0;

        run.setPassedCases(passedCount);
        run.setPassRate(passRate);
        run.setOverallScore(overallScore);

        if (passRate * 100.0 >= minPassRate) {
            run.setStatus(EvalRunStatus.PASSED);
        } else {
            run.setStatus(EvalRunStatus.FAILED);
        }
        run.setCompletedAt(Instant.now());

        log.info("Eval run {} for {}:{} complete: {} (score={}, passRate={}, {}/{})",
                run.getId(), skillName, skillVersion, run.getStatus(),
                overallScore, passRate, passedCount, totalCases);

        return evalRunRepository.save(run);
    }

    /**
     * Retrieves an eval run by its ID.
     */
    public EvalRun getRun(UUID runId) {
        return evalRunRepository.findById(runId).orElse(null);
    }

    /**
     * Retrieves all eval runs for a given skill version, newest first.
     */
    public List<EvalRun> getRuns(String skillName, String skillVersion) {
        return evalRunRepository.findBySkillNameAndSkillVersionOrderByCreatedAtDesc(skillName, skillVersion);
    }

    /**
     * Retrieves the most recent eval run for a given skill version.
     */
    public Optional<EvalRun> getLatest(String skillName, String skillVersion) {
        return evalRunRepository.findTopBySkillNameAndSkillVersionOrderByCreatedAtDesc(skillName, skillVersion);
    }

    // ---------------------------------------------------------------
    // Pipeline construction
    // ---------------------------------------------------------------

    private SkillPipeline buildPipeline(String skillName, String skillVersion,
                                        List<SkillManifest.EvalCase> cases,
                                        int timeout, int k) {
        SkillPipeline pipeline = new SkillPipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName("__eval__" + skillName + "-" + skillVersion);
        pipeline.setVersion("1.0.0");
        pipeline.setDescription("Temporary eval pipeline for " + skillName + ":" + skillVersion);
        pipeline.setNamespace("__eval__");
        pipeline.setOwnerId("system");
        pipeline.setVisibility(PipelineNode.Visibility.PRIVATE);

        // Sentinel root node
        PipelineNode rootNode = new PipelineNode();
        rootNode.setNodeId(EVAL_ROOT_NODE_ID);
        rootNode.setSkillName("__eval_root__");
        rootNode.setOrderIndex(0);
        rootNode.setTimeoutSeconds(1);
        rootNode.setPipeline(pipeline);

        List<PipelineNode> nodes = new ArrayList<>();
        nodes.add(rootNode);

        List<PipelineEdge> edges = new ArrayList<>();

        // Case nodes
        for (int i = 0; i < cases.size(); i++) {
            SkillManifest.EvalCase evalCase = cases.get(i);
            String nodeId = sanitizeNodeId(evalCase.getName());

            PipelineNode caseNode = new PipelineNode();
            caseNode.setNodeId(nodeId);
            caseNode.setSkillName(skillName);
            caseNode.setSkillVersion(skillVersion);
            caseNode.setOrderIndex(i + 1);
            caseNode.setTimeoutSeconds(timeout);
            caseNode.setRetryCount(k - 1);
            caseNode.setOnFailure(k > 1 ? PipelineNode.FailureStrategy.RETRY : PipelineNode.FailureStrategy.STOP);
            caseNode.setPipeline(pipeline);
            caseNode.setParameterMapping(buildParameterMapping(evalCase));
            nodes.add(caseNode);

            // Edge: root -> case node
            PipelineEdge edge = new PipelineEdge();
            edge.setSourceNodeId(EVAL_ROOT_NODE_ID);
            edge.setTargetNodeId(nodeId);
            edge.setPipeline(pipeline);
            edges.add(edge);
        }

        pipeline.setNodes(nodes);
        pipeline.setEdges(edges);

        return pipeline;
    }

    private ParameterMapping buildParameterMapping(SkillManifest.EvalCase evalCase) {
        if (evalCase.getInput() == null || evalCase.getInput().isEmpty()) {
            return new ParameterMapping();
        }
        ParameterMapping mapping = new ParameterMapping();
        for (String key : evalCase.getInput().keySet()) {
            mapping.put(key, "$.inputs." + key);
        }
        return mapping;
    }

    // ---------------------------------------------------------------
    // Case execution (pass@k)
    // ---------------------------------------------------------------

    private EvalCaseResult executeCase(UUID runId, SkillManifest.EvalCase evalCase,
                                       String skillName, String skillVersion, int k, int timeout) {
        EvalCaseResult result = new EvalCaseResult();
        result.setEvalRunId(runId);
        result.setCaseName(evalCase.getName());
        result.setExpectedOutput(serializeExpected(evalCase.getExpectedOutput()));
        result.setValidatorType(
                evalCase.getValidator() != null ? evalCase.getValidator() : SkillManifest.ValidatorType.EXACT);

        long startTime = System.currentTimeMillis();
        boolean passed = false;
        int attempts = 0;
        String lastError = null;
        String actualOutput = null;

        for (int attempt = 0; attempt < k; attempt++) {
            attempts = attempt + 1;
            try {
                actualOutput = executionService.execute(skillName, skillVersion,
                        evalCase.getInput(), timeout);

                if (validate(evalCase, actualOutput, result.getExpectedOutput())) {
                    passed = true;
                    lastError = null;
                    break;
                } else if (attempt < k - 1) {
                    log.debug("Case '{}' attempt {}/{} failed, retrying...",
                            evalCase.getName(), attempt + 1, k);
                } else {
                    lastError = "All " + k + " attempts failed";
                }
            } catch (Exception e) {
                log.warn("Case '{}' attempt {}/{} error: {}",
                        evalCase.getName(), attempt + 1, k, e.getMessage());
                lastError = "Execution error: " + e.getMessage();
                if (attempt >= k - 1) {
                    lastError = "All " + k + " attempts failed. Last error: " + e.getMessage();
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        result.setPassed(passed);
        result.setActualOutput(actualOutput);
        result.setDurationMs(duration);
        result.setErrorMessage(lastError);
        result.setRetryAttempts(attempts - 1);
        result.setEvaluatedAt(Instant.now());

        return result;
    }

    // ---------------------------------------------------------------
    // Validators
    // ---------------------------------------------------------------

    private boolean validate(SkillManifest.EvalCase evalCase, String actualOutput, String expectedOutput) {
        SkillManifest.ValidatorType validator = evalCase.getValidator();
        if (validator == null) {
            validator = SkillManifest.ValidatorType.EXACT;
        }

        return switch (validator) {
            case EXACT -> exactMatch(actualOutput, expectedOutput);
            case CONTAINS -> containsMatch(actualOutput, expectedOutput);
            case JSON_SCHEMA -> jsonSchemaMatch(actualOutput, expectedOutput);
            case LLM_JUDGE -> llmJudgeMatch(evalCase.getName(), expectedOutput, actualOutput);
        };
    }

    /**
     * EXACT match: normalized string equality with deep JSON comparison for JSON strings.
     */
    private boolean exactMatch(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;

        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);

        // Try deep JSON equality if both are valid JSON
        try {
            JsonNode actualNode = objectMapper.readTree(normalizedActual);
            JsonNode expectedNode = objectMapper.readTree(normalizedExpected);
            return actualNode.equals(expectedNode);
        } catch (Exception e) {
            // Not valid JSON, fall back to string comparison
            return normalizedActual.equals(normalizedExpected);
        }
    }

    /**
     * CONTAINS: substring check after normalization.
     * For JSON strings, checks both stringified containment and structural deep-contains.
     */
    private boolean containsMatch(String actual, String expected) {
        if (actual == null || expected == null) return false;

        String normalizedActual = normalize(actual);
        String normalizedExpected = normalize(expected);

        // Substring check
        if (normalizedActual.contains(normalizedExpected)) {
            return true;
        }

        // For JSON, try structural deep-contains (expected keys exist in actual)
        try {
            JsonNode actualNode = objectMapper.readTree(normalizedActual);
            JsonNode expectedNode = objectMapper.readTree(normalizedExpected);
            return jsonContains(actualNode, expectedNode);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JSON_SCHEMA validation against the expected output as JSON Schema.
     */
    private boolean jsonSchemaMatch(String actual, String schemaJson) {
        if (actual == null) return false;
        if (schemaJson == null) return false;

        JsonValidator.ValidationResult result = jsonValidator.validate(actual, schemaJson);
        return result.passed();
    }

    /**
     * LLM_JUDGE: prompt-based PASS/FAIL evaluation.
     */
    private boolean llmJudgeMatch(String caseName, String expected, String actual) {
        return llmJudgeService.evaluate(caseName,
                expected != null ? expected : "",
                actual != null ? actual : "");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Normalize a string: trim whitespace and normalize line endings.
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.strip().replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * Checks if an actual JSON node structurally contains all fields of an expected node.
     * For objects: every key in expected must exist in actual with matching value.
     * For arrays: every element in expected must exist in actual.
     * For primitives: direct equality.
     */
    private boolean jsonContains(JsonNode actual, JsonNode expected) {
        if (expected.isObject() && actual.isObject()) {
            for (java.util.Iterator<java.util.Map.Entry<String, JsonNode>> it = expected.fields(); it.hasNext(); ) {
                java.util.Map.Entry<String, JsonNode> field = it.next();
                JsonNode actualField = actual.get(field.getKey());
                if (actualField == null || !jsonContains(actualField, field.getValue())) {
                    return false;
                }
            }
            return true;
        }
        if (expected.isArray() && actual.isArray()) {
            for (JsonNode expectedElement : expected) {
                boolean found = false;
                for (JsonNode actualElement : actual) {
                    if (jsonContains(actualElement, expectedElement)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        return actual.equals(expected);
    }

    /**
     * Resolves the timeout for each case node, capped at MAX_TIMEOUT_SECONDS.
     */
    private int resolveTimeout(SkillManifest manifest) {
        if (manifest.getResources() == null) {
            return 30;
        }
        return Math.min(manifest.getResources().getTimeoutSeconds(), MAX_TIMEOUT_SECONDS);
    }

    /**
     * Sanitizes a case name to be a valid pipeline nodeId (alphanumeric + hyphens).
     */
    private String sanitizeNodeId(String caseName) {
        if (caseName == null || caseName.isBlank()) {
            return "eval-case-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String sanitized = caseName.replaceAll("[^a-zA-Z0-9-]", "-");
        // Collapse multiple hyphens
        sanitized = sanitized.replaceAll("-+", "-");
        // Remove leading/trailing hyphens
        sanitized = sanitized.replaceAll("^-|-$", "");
        if (sanitized.isEmpty()) {
            return "eval-case-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return "eval-" + sanitized;
    }

    /**
     * Serializes the expected output object to JSON string.
     */
    private String serializeExpected(Object expectedOutput) {
        if (expectedOutput == null) return null;
        if (expectedOutput instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(expectedOutput);
        } catch (Exception e) {
            return expectedOutput.toString();
        }
    }

    /**
     * Creates an EvalRun in ERROR status.
     */
    private EvalRun createErrorRun(String skillName, String skillVersion, String triggeredBy) {
        EvalRun run = new EvalRun(skillName, skillVersion);
        run.setStatus(EvalRunStatus.ERROR);
        run.setTriggeredBy(triggeredBy);
        run.setStartedAt(Instant.now());
        run.setCompletedAt(Instant.now());
        return run;
    }
}
