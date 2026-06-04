package com.iflytek.skillhub.domain.skill;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "skill_dependencies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_skill_name", "source_version", "dep_skill_name"})
})
public class SkillDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_skill_name", nullable = false, length = 128)
    private String sourceSkillName;

    @Column(name = "source_version", nullable = false, length = 64)
    private String sourceVersion;

    @Column(name = "dep_skill_name", nullable = false, length = 128)
    private String depSkillName;

    @Column(name = "version_constraint", nullable = false, length = 64)
    private String versionConstraint;

    public SkillDependency() {}

    public SkillDependency(String sourceSkillName, String sourceVersion,
                           String depSkillName, String versionConstraint) {
        this.sourceSkillName = sourceSkillName;
        this.sourceVersion = sourceVersion;
        this.depSkillName = depSkillName;
        this.versionConstraint = versionConstraint;
    }

    public Long getId() { return id; }
    public String getSourceSkillName() { return sourceSkillName; }
    public String getSourceVersion() { return sourceVersion; }
    public String getDepSkillName() { return depSkillName; }
    public String getVersionConstraint() { return versionConstraint; }
}
