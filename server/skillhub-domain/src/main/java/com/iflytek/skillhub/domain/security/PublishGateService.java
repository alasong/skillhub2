package com.iflytek.skillhub.domain.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublishGateService {

    private static final Logger log = LoggerFactory.getLogger(PublishGateService.class);

    private final KillSwitchService killSwitchService;
    private final ScanReportService scanReportService;

    public PublishGateService(KillSwitchService killSwitchService,
                              ScanReportService scanReportService) {
        this.killSwitchService = killSwitchService;
        this.scanReportService = scanReportService;
    }

    public GateResult check(String skillName, String skillVersion) {
        List<String> blockers = new ArrayList<>();

        if (killSwitchService.isHardDeprecated(skillName, skillVersion)) {
            blockers.add("HARD_DEPRECATED: " + skillName + ":" + skillVersion + " has been killed");
        }

        if (!scanReportService.canPublish(skillName, skillVersion)) {
            blockers.add("SCAN_GATE_BLOCKED: no passing scan report for " + skillName + ":" + skillVersion);
        }

        if (blockers.isEmpty()) {
            return GateResult.PASSED;
        }

        log.warn("Publish gate blocked for {}:{} — {}", skillName, skillVersion, blockers);
        return GateResult.blocked(blockers);
    }

    public record GateResult(boolean passed, List<String> blockers) {
        public static final GateResult PASSED = new GateResult(true, List.of());

        public static GateResult blocked(List<String> blockers) {
            return new GateResult(false, List.copyOf(blockers));
        }
    }
}
