package com.iflytek.skillhub.controller.portal;

import com.iflytek.skillhub.domain.security.ScanReport;
import com.iflytek.skillhub.domain.security.ScanReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/skills/{name}/versions/{version}")
public class ScanReportController {

    private final ScanReportService scanReportService;

    public ScanReportController(ScanReportService scanReportService) {
        this.scanReportService = scanReportService;
    }

    @PostMapping("/scan-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SKILL_ADMIN') or hasAuthority('SCOPE_scan:write')")
    public ResponseEntity<Map<String, Object>> uploadScanReport(
            @PathVariable String name,
            @PathVariable String version,
            @RequestPart("report") ScanReport report,
            @RequestPart(value = "signature", required = false) MultipartFile signature) {

        if (signature != null && !signature.isEmpty()) {
            report.setSignatureVerified(true);
        }

        ScanReport saved = scanReportService.ingest(report);

        if (!saved.isGatePassed()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", "GATE_FAILED",
                    "gateReason", saved.getGateReason() != null ? saved.getGateReason() : "unknown",
                    "scanRunId", saved.getScanRunId()
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "STORED",
                "scanRunId", saved.getScanRunId(),
                "gatePassed", true
        ));
    }

    @GetMapping("/scan-report")
    public ResponseEntity<Map<String, Object>> getScanReport(
            @PathVariable String name,
            @PathVariable String version) {

        boolean canPublish = scanReportService.canPublish(name, version);

        return ResponseEntity.ok(Map.of(
                "skillName", name,
                "skillVersion", version,
                "canPublish", canPublish
        ));
    }
}
