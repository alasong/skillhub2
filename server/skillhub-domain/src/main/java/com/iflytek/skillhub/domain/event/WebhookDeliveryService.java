package com.iflytek.skillhub.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class WebhookDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async
    public void deliver(WebhookConfig config, WebhookEvent event) {
        if (!config.isEnabled()) return;
        if (!matchesEvent(config, event)) return;

        try {
            String body = toJson(event);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-SkillHub-Event", event.getType())
                    .header("X-SkillHub-Delivery", event.getId())
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (config.getSecretHash() != null) {
                String signature = hmacSha256(config.getSecretHash(), body);
                builder.header("X-SkillHub-Signature", "sha256=" + signature);
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                config.setLastDeliveredAt(Instant.now());
                config.setDeliveryFailures(0);
            } else {
                config.setDeliveryFailures(config.getDeliveryFailures() + 1);
                log.warn("Webhook delivery failed: {} {} — {}", config.getUrl(),
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            config.setDeliveryFailures(config.getDeliveryFailures() + 1);
            log.error("Webhook delivery error: {} — {}", config.getUrl(), e.getMessage());
        }
    }

    private boolean matchesEvent(WebhookConfig config, WebhookEvent event) {
        if (config.getEvents() == null || config.getEvents().isBlank()) return false;
        String eventType = event.getType();
        for (String registered : config.getEvents().split(",")) {
            if (registered.trim().equals(eventType)) return true;
        }
        return false;
    }

    private String hmacSha256(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String toJson(WebhookEvent event) {
        // Simple manual serialization to avoid Jackson dependency in domain layer
        return String.format(
            "{\"specversion\":\"%s\",\"id\":\"%s\",\"type\":\"%s\",\"source\":\"%s\"," +
            "\"time\":\"%s\",\"namespace\":\"%s\",\"skillName\":\"%s\",\"skillVersion\":\"%s\"}",
            event.getSpecversion(), event.getId(), event.getType(), event.getSource(),
            event.getTime(), event.getNamespace(), event.getSkillName(), event.getSkillVersion()
        );
    }
}
