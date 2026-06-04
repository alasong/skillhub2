package com.iflytek.skillhub.domain.security;

import java.util.List;
import java.util.Optional;

public interface ScanReportRepository {

    ScanReport save(ScanReport report);

    Optional<ScanReport> findTopBySkillNameAndSkillVersionOrderByScannedAtDesc(
            String skillName, String skillVersion);

    List<ScanReport> findBySkillNameAndSkillVersion(String skillName, String skillVersion);

    void deleteBySkillName(String skillName);
}
