package skillhub.gate

import future.keywords.if

default allow := false

# Gate passes if all hard-block conditions are met
allow if {
    trivy_critical == 0
    trivy_high <= 0
    trufflehog_verified_secrets == 0
    semgrep_error_findings <= 5
}

# --- Count extractors from aggregated report ---

trivy_critical := count([r |
    r := input.trivy_findings[_]
    r.Severity == "CRITICAL"
])

trivy_high := count([r |
    r := input.trivy_findings[_]
    r.Severity == "HIGH"
])

trufflehog_verified_secrets := count([r |
    r := input.trufflehog_findings[_]
    r.Verified == true
])

semgrep_error_findings := count([r |
    r := input.semgrep_findings[_]
    r.level == "error"
])
