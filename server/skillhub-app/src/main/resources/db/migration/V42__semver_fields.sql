-- V42__semver_fields.sql
-- Add semantic versioning fields to skill_version table.

ALTER TABLE skill_version
    ADD COLUMN IF NOT EXISTS compatible_with      VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deprecated           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deprecation_message  VARCHAR(512),
    ADD COLUMN IF NOT EXISTS replaces             TEXT[];
