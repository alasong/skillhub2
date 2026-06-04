CREATE TABLE IF NOT EXISTS skill_pipelines (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL UNIQUE,
    version     VARCHAR(16) NOT NULL DEFAULT '0.1.0',
    description VARCHAR(1024),
    namespace   VARCHAR(64) NOT NULL,
    owner_id    VARCHAR(128) NOT NULL,
    visibility  VARCHAR(16) NOT NULL DEFAULT 'NAMESPACE_ONLY',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pipeline_nodes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id             UUID NOT NULL REFERENCES skill_pipelines(id) ON DELETE CASCADE,
    node_id                 VARCHAR(64) NOT NULL,
    skill_name              VARCHAR(128) NOT NULL,
    skill_version           VARCHAR(64),
    order_index             INTEGER NOT NULL DEFAULT 0,
    condition_expr          VARCHAR(512),
    on_failure              VARCHAR(16) NOT NULL DEFAULT 'STOP',
    on_failure_fallback_skill VARCHAR(128),
    retry_count             INTEGER NOT NULL DEFAULT 0,
    timeout_seconds         INTEGER NOT NULL DEFAULT 300,
    parameter_mapping       JSONB,
    UNIQUE (pipeline_id, node_id)
);

CREATE TABLE IF NOT EXISTS pipeline_edges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id     UUID NOT NULL REFERENCES skill_pipelines(id) ON DELETE CASCADE,
    source_node_id  VARCHAR(64) NOT NULL,
    target_node_id  VARCHAR(64) NOT NULL,
    data_mapping    JSONB,
    condition_expr  VARCHAR(512),
    UNIQUE (pipeline_id, source_node_id, target_node_id)
);

CREATE TABLE IF NOT EXISTS webhook_configs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    namespace         VARCHAR(64) NOT NULL,
    url               VARCHAR(2048) NOT NULL,
    secret_hash       VARCHAR(128),
    events            TEXT[] NOT NULL,
    enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_delivered_at TIMESTAMPTZ,
    delivery_failures INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_webhook_namespace ON webhook_configs (namespace, enabled);
CREATE INDEX IF NOT EXISTS idx_pipeline_namespace ON skill_pipelines (namespace);
