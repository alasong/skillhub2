package com.iflytek.skillhub.domain.skill.pipeline;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A declarative DAG that chains multiple skills into a pipeline.
 * Each node is a skill invocation; edges represent data flow.
 * Analogous to GitHub Actions workflow or Argo WorkflowTemplate.
 */
@Entity
@Table(name = "skill_pipelines", uniqueConstraints = @UniqueConstraint(columnNames = {"namespace", "name"}))
public class SkillPipeline {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
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
    @Enumerated(EnumType.STRING)
    private PipelineNode.Visibility visibility = PipelineNode.Visibility.NAMESPACE_ONLY;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<PipelineNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PipelineEdge> edges = new ArrayList<>();

    @Column(name = "input_definitions", columnDefinition = "jsonb")
    private String inputDefinitions;  // JSON-serialized SkillManifest.InputSpec for pipeline params

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillPipeline that = (SkillPipeline) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    // Getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public PipelineNode.Visibility getVisibility() { return visibility; }
    public void setVisibility(PipelineNode.Visibility v) { this.visibility = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
    public List<PipelineNode> getNodes() { return nodes; }
    public void setNodes(List<PipelineNode> n) { this.nodes = n; }
    public List<PipelineEdge> getEdges() { return edges; }
    public void setEdges(List<PipelineEdge> e) { this.edges = e; }
    public String getInputDefinitions() { return inputDefinitions; }
    public void setInputDefinitions(String v) { this.inputDefinitions = v; }
}
