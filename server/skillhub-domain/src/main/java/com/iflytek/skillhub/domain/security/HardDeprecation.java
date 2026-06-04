package com.iflytek.skillhub.domain.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hard_deprecations")
public class HardDeprecation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Column(name = "skill_version", nullable = false, length = 64)
    private String skillVersion;

    @Column(name = "reason", nullable = false, length = 1024)
    private String reason;

    @Column(name = "deprecated_by", nullable = false, length = 128)
    private String deprecatedBy;

    @Column(name = "deprecated_at", nullable = false)
    private Instant deprecatedAt;

    @Column(name = "notified_owners", nullable = false)
    private boolean notifiedOwners;

    public HardDeprecation() {}

    public HardDeprecation(String skillName, String skillVersion, String reason, String deprecatedBy) {
        this.skillName = skillName;
        this.skillVersion = skillVersion;
        this.reason = reason;
        this.deprecatedBy = deprecatedBy;
        this.deprecatedAt = Instant.now();
        this.notifiedOwners = false;
    }

    public UUID getId() { return id; }
    public String getSkillName() { return skillName; }
    public String getSkillVersion() { return skillVersion; }
    public String getReason() { return reason; }
    public String getDeprecatedBy() { return deprecatedBy; }
    public Instant getDeprecatedAt() { return deprecatedAt; }
    public boolean isNotifiedOwners() { return notifiedOwners; }
    public void setNotifiedOwners(boolean notified) { this.notifiedOwners = notified; }
}
