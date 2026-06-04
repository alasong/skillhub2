package com.iflytek.skillhub.domain.skill.metadata;

import java.util.List;
import java.util.UUID;

/**
 * Domain repository for per-case evaluation results.
 */
public interface EvalCaseResultRepository {

    EvalCaseResult save(EvalCaseResult result);

    List<EvalCaseResult> findByEvalRunId(UUID evalRunId);
}
