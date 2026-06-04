package com.iflytek.skillhub.domain.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_configs")
public class WebhookConfig {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "namespace", nullable = false, length = 64)
    private String namespace;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "secret_hash", length = 128)
    private String secretHash;  // HMAC-SHA256 shared secret for signature verification

    @Column(name = "events", nullable = false, columnDefinition = "TEXT")
    private String events;  // Comma-separated event types: publish,deprecate,review,etc.

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    @Column(name = "delivery_failures")
    private int deliveryFailures;

    // Getters/setters
    public UUID getId() { return id; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String n) { this.namespace = n; }
    public String getUrl() { return url; }
    public void setUrl(String u) { this.url = u; }
    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String s) { this.secretHash = s; }
    public String getEvents() { return events; }
    public void setEvents(String e) { this.events = e; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastDeliveredAt() { return lastDeliveredAt; }
    public void setLastDeliveredAt(Instant l) { this.lastDeliveredAt = l; }
    public int getDeliveryFailures() { return deliveryFailures; }
    public void setDeliveryFailures(int d) { this.deliveryFailures = d; }
}
