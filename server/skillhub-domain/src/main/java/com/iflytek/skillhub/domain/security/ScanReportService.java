package com.iflytek.skillhub.domain.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanReportService {

    private static final Logger log = LoggerFactory.getLogger(ScanReportService.class);

    private final ScanReportRepository scanReportRepository;

    public ScanReportService(ScanReportRepository scanReportRepository) {
        this.scanReportRepository = scanReportRepository;
    }

    @Transactional
    public ScanReport ingest(ScanReport report) {
        return scanReportRepository.save(report);
    }

    public boolean canPublish(String skillName, String skillVersion) {
        return scanReportRepository
                .findTopBySkillNameAndSkillVersionOrderByScannedAtDesc(skillName, skillVersion)
                .map(ScanReport::isGatePassed)
                .orElse(false);
    }

    public void deleteBySkillName(String skillName) {
        scanReportRepository.deleteBySkillName(skillName);
    }
}
