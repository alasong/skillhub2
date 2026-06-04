CREATE TABLE IF NOT EXISTS skill_dependencies (
    id                  BIGSERIAL PRIMARY KEY,
    source_skill_name   VARCHAR(128) NOT NULL,
    source_version      VARCHAR(64)  NOT NULL,
    dep_skill_name      VARCHAR(128) NOT NULL,
    version_constraint  VARCHAR(64)  NOT NULL,
    UNIQUE (source_skill_name, source_version, dep_skill_name)
);

CREATE INDEX IF NOT EXISTS idx_dep_source
    ON skill_dependencies (source_skill_name, source_version);

CREATE INDEX IF NOT EXISTS idx_dep_target
    ON skill_dependencies (dep_skill_name);
