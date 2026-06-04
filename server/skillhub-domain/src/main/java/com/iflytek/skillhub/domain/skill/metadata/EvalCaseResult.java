package com.iflytek.skillhub.domain.skill.metadata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Result of a single eval case within an evaluation run.
 * Each case is validated against an expected output using the declared validator type.
 */
@Entity
@Table(name = "eval_case_results")
public class EvalCaseResult {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "eval_run_id", nullable = false)
    private UUID evalRunId;

    @Column(name = "case_name", nullable = false, length = 128)
    private String caseName;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(columnDefinition = "jsonb", name = "actual_output")
    private String actualOutput;

    @Column(columnDefinition = "jsonb", name = "expected_output")
    private String expectedOutput;

    @Column(name = "validator_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SkillManifest.ValidatorType validatorType;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "retry_attempts")
    private int retryAttempts;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    public EvalCaseResult() {}

    // --- Getters ---

    public UUID getId() { return id; }
    public UUID getEvalRunId() { return evalRunId; }
    public String getCaseName() { return caseName; }
    public boolean isPassed() { return passed; }
    public String getActualOutput() { return actualOutput; }
    public String getExpectedOutput() { return expectedOutput; }
    public SkillManifest.ValidatorType getValidatorType() { return validatorType; }
    public Long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryAttempts() { return retryAttempts; }
    public Instant getEvaluatedAt() { return evaluatedAt; }

    // --- Setters ---

    public void setId(UUID id) { this.id = id; }
    public void setEvalRunId(UUID evalRunId) { this.evalRunId = evalRunId; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public void setValidatorType(SkillManifest.ValidatorType validatorType) { this.validatorType = validatorType; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
