package com.iflytek.skillhub.domain.skill.metadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository for evaluation runs.
 */
public interface EvalRunRepository {

    EvalRun save(EvalRun run);

    Optional<EvalRun> findById(UUID id);

    Optional<EvalRun> findTopBySkillNameAndSkillVersionOrderByCreatedAtDesc(
            String skillName, String skillVersion);

    List<EvalRun> findBySkillNameAndSkillVersionOrderByCreatedAtDesc(
            String skillName, String skillVersion);

    List<EvalRun> findByStatus(EvalRunStatus status);
}
