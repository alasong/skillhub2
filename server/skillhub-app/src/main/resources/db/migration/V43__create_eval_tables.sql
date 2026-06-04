CREATE TABLE IF NOT EXISTS eval_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_name      VARCHAR(128) NOT NULL,
    skill_version   VARCHAR(64) NOT NULL,
    pipeline_id     UUID,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    overall_score   DOUBLE PRECISION,
    pass_rate       DOUBLE PRECISION,
    total_cases     INTEGER NOT NULL DEFAULT 0,
    passed_cases    INTEGER NOT NULL DEFAULT 0,
    triggered_by    VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS eval_case_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eval_run_id     UUID NOT NULL REFERENCES eval_runs(id) ON DELETE CASCADE,
    case_name       VARCHAR(128) NOT NULL,
    passed          BOOLEAN NOT NULL DEFAULT FALSE,
    actual_output   JSONB,
    expected_output JSONB,
    validator_type  VARCHAR(20) NOT NULL,
    duration_ms     BIGINT,
    error_message   VARCHAR(2048),
    retry_attempts  INTEGER NOT NULL DEFAULT 0,
    evaluated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_eval_runs_skill ON eval_runs (skill_name, skill_version);
CREATE INDEX IF NOT EXISTS idx_eval_runs_status ON eval_runs (status);
CREATE INDEX IF NOT EXISTS idx_eval_case_results_run ON eval_case_results (eval_run_id);
