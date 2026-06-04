package com.iflytek.skillhub.domain.skill.pipeline;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A declarative DAG that chains multiple skills into a pipeline.
 * Each node is a skill invocation; edges represent data flow.
 * Analogous to GitHub Actions workflow or Argo WorkflowTemplate.
 */
@Entity
@Table(name = "skill_pipelines")
public class SkillPipeline {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "version", nullable = false, length = 16)
    private String version;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "namespace", nullable = false, length = 64)
    private String namespace;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Column(name = "visibility", nullable = false, length = 16)
    private String visibility = "NAMESPACE_ONLY";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<PipelineNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PipelineEdge> edges = new ArrayList<>();

    // Getters/setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String n) { this.namespace = n; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String o) { this.ownerId = o; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String v) { this.visibility = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
    public List<PipelineNode> getNodes() { return nodes; }
    public void setNodes(List<PipelineNode> n) { this.nodes = n; }
    public List<PipelineEdge> getEdges() { return edges; }
    public void setEdges(List<PipelineEdge> e) { this.edges = e; }
}
