package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.skill.pipeline.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
interface JpaPipelineRepository extends JpaRepository<SkillPipeline, UUID>, PipelineRepository {
    List<SkillPipeline> findByNamespace(String namespace);
    Optional<SkillPipeline> findByNamespaceAndName(String namespace, String name);
}
