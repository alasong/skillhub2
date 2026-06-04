package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResult;
import com.iflytek.skillhub.domain.skill.metadata.EvalCaseResultRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA-backed repository for EvalCaseResult entities.
 * Bridges the domain repository interface with Spring Data JPA.
 */
@Repository
interface JpaEvalCaseResultRepository extends JpaRepository<EvalCaseResult, UUID>, EvalCaseResultRepository {

    List<EvalCaseResult> findByEvalRunId(UUID evalRunId);
}
