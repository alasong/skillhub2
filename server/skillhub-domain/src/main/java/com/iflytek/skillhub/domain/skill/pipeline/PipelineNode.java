package com.iflytek.skillhub.domain.skill.pipeline;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pipeline_nodes")
public class PipelineNode {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private SkillPipeline pipeline;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;  // unique within pipeline, e.g. "lint", "test", "deploy"

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Column(name = "skill_version", length = 64)
    private String skillVersion;  // null = latest

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "condition_expr", length = 512)
    private String conditionExpr;  // "prev.exitCode == 0" | "inputs.mode == 'prod'"

    @Column(name = "on_failure", length = 16)
    @Enumerated(EnumType.STRING)
    private FailureStrategy onFailure = FailureStrategy.STOP;

    @Column(name = "on_failure_fallback_skill", length = 128)
    private String onFailureFallbackSkill;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "timeout_seconds")
    private int timeoutSeconds = 300;

    @Column(name = "parameter_mapping", columnDefinition = "jsonb")
    private String parameterMapping;  // JSON: {"input_key": "$.prev.output.field"}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineNode that = (PipelineNode) o;
        return Objects.equals(pipeline, that.pipeline) && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipeline, nodeId);
    }

    // Getters/setters
    public UUID getId() { return id; }
    public SkillPipeline getPipeline() { return pipeline; }
    public void setPipeline(SkillPipeline p) { this.pipeline = p; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String n) { this.nodeId = n; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String s) { this.skillName = s; }
    public String getSkillVersion() { return skillVersion; }
    public void setSkillVersion(String v) { this.skillVersion = v; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int o) { this.orderIndex = o; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String c) { this.conditionExpr = c; }
    public FailureStrategy getOnFailure() { return onFailure; }
    public void setOnFailure(FailureStrategy o) { this.onFailure = o; }
    public String getOnFailureFallbackSkill() { return onFailureFallbackSkill; }
    public void setOnFailureFallbackSkill(String f) { this.onFailureFallbackSkill = f; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int r) { this.retryCount = r; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int t) { this.timeoutSeconds = t; }
    public String getParameterMapping() { return parameterMapping; }
    public void setParameterMapping(String p) { this.parameterMapping = p; }

    public enum FailureStrategy { STOP, RETRY, CONTINUE, FALLBACK }
    public enum Visibility { PUBLIC, NAMESPACE_ONLY, PRIVATE }
}
