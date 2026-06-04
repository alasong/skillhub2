package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.security.ScanReport;
import com.iflytek.skillhub.domain.security.ScanReportRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface JpaScanReportRepository extends JpaRepository<ScanReport, java.util.UUID>, ScanReportRepository {

    Optional<ScanReport> findTopBySkillNameAndSkillVersionOrderByScannedAtDesc(
            String skillName, String skillVersion);

    default ScanReport save(ScanReport report) {
        return JpaRepository.super.save(report);
    }
}
