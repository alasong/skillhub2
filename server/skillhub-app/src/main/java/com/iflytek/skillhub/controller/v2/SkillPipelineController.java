package com.iflytek.skillhub.controller.v2;

import com.iflytek.skillhub.domain.skill.pipeline.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * /api/v2/pipelines — Skill orchestration (DAG pipeline) endpoints.
 * v2 API: versioned, RESTful, consistent error format, HATEOAS links.
 *
 * @deprecated Prototype — uses in-memory store, migrate to PipelineService before production
 */
@Deprecated
@RestController
@RequestMapping("/api/v2/pipelines")
public class SkillPipelineController {

    // TODO: inject PipelineService when implemented
    private final Map<UUID, SkillPipeline> store = new LinkedHashMap<>();

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPipeline(@RequestBody Map<String, Object> body) {
        // TODO: Replace with CreatePipelineRequest DTO when PipelineService is implemented
        SkillPipeline pipeline = new SkillPipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName((String) body.get("name"));
        pipeline.setVersion((String) body.get("version"));
        pipeline.setNamespace((String) body.get("namespace"));
        pipeline.setOwnerId((String) body.get("ownerId"));
        pipeline.setDescription((String) body.get("description"));
        if (body.containsKey("visibility")) {
            pipeline.setVisibility((String) body.get("visibility"));
        }
        store.put(pipeline.getId(), pipeline);
        return ResponseEntity.status(201).body(Map.of(
                "status", "CREATED",
                "id", pipeline.getId().toString(),
                "links", Map.of("self", "/api/v2/pipelines/" + pipeline.getId())
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPipeline(@PathVariable UUID id) {
        SkillPipeline p = store.get(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(@PathVariable UUID id) {
        SkillPipeline p = store.get(id);
        if (p == null) return ResponseEntity.notFound().build();
        // TODO: actual execution engine
        return ResponseEntity.accepted()
                .header("X-Mock", "true")
                .body(Map.of(
                "status", "ACCEPTED",
                "pipelineId", id.toString(),
                "executionId", UUID.randomUUID().toString()
        ));
    }

    @GetMapping("/{id}/executions/{execId}")
    public ResponseEntity<Map<String, Object>> getExecution(
            @PathVariable UUID id, @PathVariable String execId) {
        return ResponseEntity.ok()
                .header("X-Mock", "true")
                .body(Map.of(
                "executionId", execId,
                "pipelineId", id.toString(),
                "status", "RUNNING",
                "nodes", Map.of(
                        "lint", Map.of("status", "PASSED", "durationMs", 1200),
                        "test", Map.of("status", "RUNNING", "durationMs", 3400)
                )
        ));
    }
}
