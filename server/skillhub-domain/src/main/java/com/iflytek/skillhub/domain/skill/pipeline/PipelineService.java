package com.iflytek.skillhub.domain.skill.pipeline;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PipelineService {
    private final PipelineRepository repository;

    public PipelineService(PipelineRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SkillPipeline save(SkillPipeline pipeline) {
        validateEdges(pipeline);
        validateNoCycles(pipeline);
        if (pipeline.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Pipeline must have at least one node");
        }
        return repository.save(pipeline);
    }

    private void validateEdges(SkillPipeline pipeline) {
        Set<String> nodeIds = new HashSet<>();
        for (PipelineNode node : pipeline.getNodes()) {
            nodeIds.add(node.getNodeId());
        }
        for (PipelineEdge edge : pipeline.getEdges()) {
            if (!nodeIds.contains(edge.getSourceNodeId())) {
                throw new IllegalArgumentException(
                    "Edge references unknown source node: " + edge.getSourceNodeId());
            }
            if (!nodeIds.contains(edge.getTargetNodeId())) {
                throw new IllegalArgumentException(
                    "Edge references unknown target node: " + edge.getTargetNodeId());
            }
        }
    }

    private void validateNoCycles(SkillPipeline pipeline) {
        Map<String, List<String>> adj = new HashMap<>();
        for (PipelineNode node : pipeline.getNodes()) {
            adj.put(node.getNodeId(), new ArrayList<>());
        }
        for (PipelineEdge edge : pipeline.getEdges()) {
            adj.get(edge.getSourceNodeId()).add(edge.getTargetNodeId());
        }
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String nodeId : adj.keySet()) {
            if (dfs(nodeId, adj, visited, inStack)) {
                throw new IllegalArgumentException("Pipeline contains a cycle at node: " + nodeId);
            }
        }
    }

    private boolean dfs(String node, Map<String, List<String>> adj,
                        Set<String> visited, Set<String> inStack) {
        if (inStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        for (String neighbor : adj.getOrDefault(node, List.of())) {
            if (dfs(neighbor, adj, visited, inStack)) return true;
        }
        inStack.remove(node);
        return false;
    }

    public Optional<SkillPipeline> findById(UUID id) { return repository.findById(id); }
    public List<SkillPipeline> findByNamespace(String ns) { return repository.findByNamespace(ns); }
    public Optional<SkillPipeline> findByNamespaceAndName(String ns, String name) {
        return repository.findByNamespaceAndName(ns, name);
    }
}
