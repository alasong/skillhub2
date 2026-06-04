package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.domain.security.HardDeprecation;
import com.iflytek.skillhub.domain.security.KillSwitchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/skills/{name}/versions/{version}")
public class AdminKillSwitchController {

    private final KillSwitchService killSwitchService;

    public AdminKillSwitchController(KillSwitchService killSwitchService) {
        this.killSwitchService = killSwitchService;
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> hardDeprecate(
            @PathVariable String name,
            @PathVariable String version,
            @RequestParam(defaultValue = "false") boolean hard,
            @RequestBody Map<@NotBlank String, @NotBlank String> body) {

        if (!hard) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Use ?hard=true for emergency kill-switch deprecation"
            ));
        }

        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "MISSING_REASON",
                    "message", "reason field is required for hard deprecation"
            ));
        }

        // TODO: extract admin user from SecurityContext
        String adminUser = "system";

        HardDeprecation dep = killSwitchService.hardDeprecate(name, version, reason, adminUser);

        return ResponseEntity.ok(Map.of(
                "status", "HARD_DEPRECATED",
                "skillName", dep.getSkillName(),
                "skillVersion", dep.getSkillVersion(),
                "deprecatedAt", dep.getDeprecatedAt().toString(),
                "reason", dep.getReason()
        ));
    }

    @GetMapping("/deprecated")
    public ResponseEntity<Map<String, Object>> checkDeprecated(
            @PathVariable String name,
            @PathVariable String version) {

        boolean deprecated = killSwitchService.isHardDeprecated(name, version);

        return ResponseEntity.ok(Map.of(
                "skillName", name,
                "skillVersion", version,
                "hardDeprecated", deprecated
        ));
    }
}
