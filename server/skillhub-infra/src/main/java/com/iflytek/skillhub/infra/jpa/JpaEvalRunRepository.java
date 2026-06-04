package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.skill.metadata.EvalRun;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunRepository;
import com.iflytek.skillhub.domain.skill.metadata.EvalRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed repository for EvalRun entities.
 * Bridges the domain repository interface with Spring Data JPA.
 */
@Repository
interface JpaEvalRunRepository extends JpaRepository<EvalRun, UUID>, EvalRunRepository {

    Optional<EvalRun> findTopBySkillNameAndSkillVersionOrderByCreatedAtDesc(
            String skillName, String skillVersion);

    List<EvalRun> findBySkillNameAndSkillVersionOrderByCreatedAtDesc(
            String skillName, String skillVersion);

    List<EvalRun> findByStatus(EvalRunStatus status);
}
