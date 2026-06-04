package com.iflytek.skillhub.domain.skill.metadata;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * An aggregate root representing a single evaluation run for a skill version.
 * Each run contains one or more case results and tracks overall pass rate and score.
 */
@Entity
@Table(name = "eval_runs")
public class EvalRun {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Column(name = "skill_version", nullable = false, length = 64)
    private String skillVersion;

    @Column(name = "pipeline_id")
    private UUID pipelineId;

    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private EvalRunStatus status = EvalRunStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "pass_rate")
    private Double passRate;

    @Column(name = "total_cases")
    private int totalCases;

    @Column(name = "passed_cases")
    private int passedCases;

    @Column(name = "triggered_by", length = 128)
    private String triggeredBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public EvalRun() {}

    public EvalRun(String skillName, String skillVersion) {
        this.skillName = skillName;
        this.skillVersion = skillVersion;
        this.status = EvalRunStatus.PENDING;
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public String getSkillName() { return skillName; }
    public String getSkillVersion() { return skillVersion; }
    public UUID getPipelineId() { return pipelineId; }
    public EvalRunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Double getOverallScore() { return overallScore; }
    public Double getPassRate() { return passRate; }
    public int getTotalCases() { return totalCases; }
    public int getPassedCases() { return passedCases; }
    public String getTriggeredBy() { return triggeredBy; }
    public Instant getCreatedAt() { return createdAt; }

    // --- Setters ---

    public void setId(UUID id) { this.id = id; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public void setSkillVersion(String skillVersion) { this.skillVersion = skillVersion; }
    public void setPipelineId(UUID pipelineId) { this.pipelineId = pipelineId; }
    public void setStatus(EvalRunStatus status) { this.status = status; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    public void setPassRate(Double passRate) { this.passRate = passRate; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }
    public void setPassedCases(int passedCases) { this.passedCases = passedCases; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
