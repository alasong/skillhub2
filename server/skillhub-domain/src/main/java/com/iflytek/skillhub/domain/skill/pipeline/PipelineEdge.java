package com.iflytek.skillhub.domain.skill.pipeline;

import jakarta.persistence.*;
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

    @Column(name = "data_mapping", columnDefinition = "jsonb")
    private String dataMapping;  // JSON path mapping: {"source.output.field": "target.input.field"}

    @Column(name = "condition_expr", length = 512)
    private String conditionExpr;  // edge is only traversed if condition is met

    // Getters/setters
    public UUID getId() { return id; }
    public SkillPipeline getPipeline() { return pipeline; }
    public void setPipeline(SkillPipeline p) { this.pipeline = p; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String s) { this.sourceNodeId = s; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String t) { this.targetNodeId = t; }
    public String getDataMapping() { return dataMapping; }
    public void setDataMapping(String d) { this.dataMapping = d; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String c) { this.conditionExpr = c; }
}
