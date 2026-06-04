package com.iflytek.skillhub.domain.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scan_reports", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"skill_name", "skill_version", "scan_run_id"})
})
public class ScanReport {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Column(name = "skill_version", nullable = false, length = 64)
    private String skillVersion;

    @Column(name = "scan_run_id", nullable = false, length = 64)
    private String scanRunId;

    @Column(name = "gate_passed", nullable = false)
    private boolean gatePassed;

    @Column(name = "gate_reason", length = 512)
    private String gateReason;

    @Column(name = "semgrep_findings", columnDefinition = "jsonb")
    private String semgrepFindings;

    @Column(name = "trivy_findings", columnDefinition = "jsonb")
    private String trivyFindings;

    @Column(name = "trufflehog_findings", columnDefinition = "jsonb")
    private String trufflehogFindings;

    @Column(name = "publisher_claim", length = 256)
    private String publisherClaim;

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    public ScanReport() {}

    public ScanReport(String skillName, String skillVersion, String scanRunId) {
        this.skillName = skillName;
        this.skillVersion = skillVersion;
        this.scanRunId = scanRunId;
        this.scannedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSkillName() { return skillName; }
    public String getSkillVersion() { return skillVersion; }
    public String getScanRunId() { return scanRunId; }
    public boolean isGatePassed() { return gatePassed; }
    public String getGateReason() { return gateReason; }
    public String getSemgrepFindings() { return semgrepFindings; }
    public String getTrivyFindings() { return trivyFindings; }
    public String getTrufflehogFindings() { return trufflehogFindings; }
    public String getPublisherClaim() { return publisherClaim; }
    public boolean isSignatureVerified() { return signatureVerified; }
    public Instant getScannedAt() { return scannedAt; }

    public void setGatePassed(boolean gatePassed) { this.gatePassed = gatePassed; }
    public void setGateReason(String gateReason) { this.gateReason = gateReason; }
    public void setSemgrepFindings(String semgrepFindings) { this.semgrepFindings = semgrepFindings; }
    public void setTrivyFindings(String trivyFindings) { this.trivyFindings = trivyFindings; }
    public void setTrufflehogFindings(String trufflehogFindings) { this.trufflehogFindings = trufflehogFindings; }
    public void setPublisherClaim(String publisherClaim) { this.publisherClaim = publisherClaim; }
    public void setSignatureVerified(boolean signatureVerified) { this.signatureVerified = signatureVerified; }
}
