CREATE TABLE IF NOT EXISTS hard_deprecations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_name      VARCHAR(128) NOT NULL,
    skill_version   VARCHAR(64)  NOT NULL,
    reason          VARCHAR(1024) NOT NULL,
    deprecated_by   VARCHAR(128) NOT NULL,
    deprecated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    notified_owners BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_hard_deprecations_lookup
    ON hard_deprecations (skill_name, skill_version);

ALTER TABLE skill_versions
    ADD COLUMN IF NOT EXISTS deprecated          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deprecation_reason VARCHAR(1024);
