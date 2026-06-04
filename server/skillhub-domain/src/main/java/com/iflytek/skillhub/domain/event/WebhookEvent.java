package com.iflytek.skillhub.domain.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standardized webhook event payload (CloudEvents-compatible).
 * Sent to all registered webhooks for a namespace when a lifecycle event occurs.
 */
public class WebhookEvent {

    // CloudEvents fields
    private String id = UUID.randomUUID().toString();
    private String source;
    private String type;
    private Instant time = Instant.now();
    private String dataContentType = "application/json";

    // SkillHub-specific
    private String namespace;
    private String skillName;
    private String skillVersion;
    private Map<String, Object> data;  // event-specific payload

    public WebhookEvent() {}

    public WebhookEvent(String type, String namespace, String skillName, String skillVersion) {
        this.type = type;
        this.namespace = namespace;
        this.skillName = skillName;
        this.skillVersion = skillVersion;
        this.source = "skillhub/" + namespace;
    }

    // Event type constants
    public static final String SKILL_PUBLISHED = "skill.published";
    public static final String SKILL_DEPRECATED = "skill.deprecated";
    public static final String SKILL_HARD_DEPRECATED = "skill.hard_deprecated";
    public static final String REVIEW_APPROVED = "review.approved";
    public static final String REVIEW_REJECTED = "review.rejected";
    public static final String PIPELINE_COMPLETED = "pipeline.completed";
    public static final String PIPELINE_FAILED = "pipeline.failed";

    // Getters/setters
    public String getId() { return id; }
    public String getSource() { return source; }
    public void setSource(String s) { this.source = s; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public Instant getTime() { return time; }
    public String getDataContentType() { return dataContentType; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String n) { this.namespace = n; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String s) { this.skillName = s; }
    public String getSkillVersion() { return skillVersion; }
    public void setSkillVersion(String v) { this.skillVersion = v; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> d) { this.data = d; }
}
