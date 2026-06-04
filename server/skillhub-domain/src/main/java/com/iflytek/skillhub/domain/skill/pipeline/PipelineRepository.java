package com.iflytek.skillhub.domain.skill.pipeline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRepository {
    SkillPipeline save(SkillPipeline pipeline);
    Optional<SkillPipeline> findById(UUID id);
    List<SkillPipeline> findByNamespace(String namespace);
    Optional<SkillPipeline> findByNamespaceAndName(String namespace, String name);
    void delete(SkillPipeline pipeline);
}
