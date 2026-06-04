package com.iflytek.skillhub.domain.skill.metadata;

/**
 * Lifecycle status of an evaluation run.
 * PENDING: created but not yet started
 * RUNNING: evaluation in progress
 * PASSED: completed and met the minimum pass rate threshold
 * FAILED: completed but did not meet the threshold
 * ERROR: infrastructure failure (timeout, system error, etc.)
 */
public enum EvalRunStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    ERROR
}
