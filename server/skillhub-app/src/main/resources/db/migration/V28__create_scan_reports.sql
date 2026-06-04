CREATE TABLE IF NOT EXISTS scan_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_name      VARCHAR(128) NOT NULL,
    skill_version   VARCHAR(64)  NOT NULL,
    scan_run_id     VARCHAR(64)  NOT NULL,
    gate_passed     BOOLEAN NOT NULL DEFAULT FALSE,
    gate_reason     VARCHAR(512),
    semgrep_findings   JSONB,
    trivy_findings     JSONB,
    trufflehog_findings JSONB,
    publisher_claim VARCHAR(256),
    signature_verified BOOLEAN NOT NULL DEFAULT FALSE,
    scanned_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (skill_name, skill_version, scan_run_id)
);

CREATE INDEX IF NOT EXISTS idx_scan_reports_latest
    ON scan_reports (skill_name, skill_version, scanned_at DESC);
