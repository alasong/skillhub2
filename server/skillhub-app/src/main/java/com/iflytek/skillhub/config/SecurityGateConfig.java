package com.iflytek.skillhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "skillhub.security")
public class SecurityGateConfig {

    private ScanGate scanGate = new ScanGate();
    private Cosign cosign = new Cosign();
    private KillSwitch killSwitch = new KillSwitch();
    private Dependency dependency = new Dependency();

    public ScanGate getScanGate() { return scanGate; }
    public Cosign getCosign() { return cosign; }
    public KillSwitch getKillSwitch() { return killSwitch; }
    public Dependency getDependency() { return dependency; }

    public static class ScanGate {
        private boolean enabled = true;
        private boolean requirePassingReport = true;
        private int maxCveCritical = 0;
        private int maxCveHigh = 0;
        private int maxSastFindings = 5;
        private int maxSecrets = 0;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isRequirePassingReport() { return requirePassingReport; }
        public void setRequirePassingReport(boolean v) { this.requirePassingReport = v; }
        public int getMaxCveCritical() { return maxCveCritical; }
        public void setMaxCveCritical(int v) { this.maxCveCritical = v; }
        public int getMaxCveHigh() { return maxCveHigh; }
        public void setMaxCveHigh(int v) { this.maxCveHigh = v; }
        public int getMaxSastFindings() { return maxSastFindings; }
        public void setMaxSastFindings(int v) { this.maxSastFindings = v; }
        public int getMaxSecrets() { return maxSecrets; }
        public void setMaxSecrets(int v) { this.maxSecrets = v; }
    }

    public static class Cosign {
        private String fulcioUrl = "https://fulcio.sigstore.dev";
        private String rekorUrl = "https://rekor.sigstore.dev";
        private String allowedIssuers = "https://token.actions.githubusercontent.com";

        public String getFulcioUrl() { return fulcioUrl; }
        public void setFulcioUrl(String v) { this.fulcioUrl = v; }
        public String getRekorUrl() { return rekorUrl; }
        public void setRekorUrl(String v) { this.rekorUrl = v; }
        public String getAllowedIssuers() { return allowedIssuers; }
        public void setAllowedIssuers(String v) { this.allowedIssuers = v; }
    }

    public static class KillSwitch {
        private boolean cacheInvalidation = true;
        private boolean notifyNamespaceOwners = true;
        private int webhookTimeoutMs = 5000;

        public boolean isCacheInvalidation() { return cacheInvalidation; }
        public void setCacheInvalidation(boolean v) { this.cacheInvalidation = v; }
        public boolean isNotifyNamespaceOwners() { return notifyNamespaceOwners; }
        public void setNotifyNamespaceOwners(boolean v) { this.notifyNamespaceOwners = v; }
        public int getWebhookTimeoutMs() { return webhookTimeoutMs; }
        public void setWebhookTimeoutMs(int v) { this.webhookTimeoutMs = v; }
    }

    public static class Dependency {
        private String conflictResolution = "strict";
        private int maxTransitiveDepth = 3;

        public String getConflictResolution() { return conflictResolution; }
        public void setConflictResolution(String v) { this.conflictResolution = v; }
        public int getMaxTransitiveDepth() { return maxTransitiveDepth; }
        public void setMaxTransitiveDepth(int v) { this.maxTransitiveDepth = v; }
    }
}
