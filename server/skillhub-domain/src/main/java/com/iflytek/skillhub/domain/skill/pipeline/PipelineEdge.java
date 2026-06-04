package com.iflytek.skillhub.domain.skill.pipeline;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pipeline_edges")
public class PipelineEdge {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private SkillPipeline pipeline;

    @Column(name = "source_node_id", nullable = false, length = 64)
    private String sourceNodeId;

    @Column(name = "target_node_id", nullable = false, length = 64)
    private String targetNodeId;

    @Convert(converter = DataMapping.JpaConverter.class)
    @Column(name = "data_mapping", columnDefinition = "jsonb")
    private DataMapping dataMapping = new DataMapping();

    @Column(name = "condition_expr", length = 512)
    private String conditionExpr;  // edge is only traversed if condition is met

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineEdge that = (PipelineEdge) o;
        return Objects.equals(pipeline, that.pipeline)
                && Objects.equals(sourceNodeId, that.sourceNodeId)
                && Objects.equals(targetNodeId, that.targetNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipeline, sourceNodeId, targetNodeId);
    }

    // Getters/setters
    public UUID getId() { return id; }
    public SkillPipeline getPipeline() { return pipeline; }
    public void setPipeline(SkillPipeline p) { this.pipeline = p; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String s) { this.sourceNodeId = s; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String t) { this.targetNodeId = t; }
    public DataMapping getDataMapping() { return dataMapping; }
    public void setDataMapping(DataMapping d) { this.dataMapping = d; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String c) { this.conditionExpr = c; }
}
