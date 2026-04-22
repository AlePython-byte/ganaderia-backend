package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@ConditionalOnProperty(name = "app.notifications.webhook.enabled", havingValue = "true")
public class WebhookNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationService.class);

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String UNKNOWN_STATUS = "N/A";

    private final WebhookNotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI webhookUri;

    public WebhookNotificationService(WebhookNotificationProperties properties,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webhookUri = validateWebhookUri(properties.getUrl());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(validateTimeout(properties.getConnectTimeout(), "connect-timeout"))
                .build();
        validateTimeout(properties.getReadTimeout(), "read-timeout");
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WEBHOOK;
    }

    @Override
    public void send(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return;
        }

        String requestBody = serialize(notificationMessage);
        HttpRequest request = buildRequest(notificationMessage, requestBody);
        long startedAt = System.nanoTime();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long durationMs = elapsedMs(startedAt);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logResult("failure", response.statusCode(), durationMs, notificationMessage);
                throw new IllegalStateException("Webhook notification failed with status " + response.statusCode());
            }

            logResult("success", response.statusCode(), durationMs, notificationMessage);
        } catch (IOException ex) {
            logException("failure", UNKNOWN_STATUS, elapsedMs(startedAt), notificationMessage, ex);
            throw new IllegalStateException("Webhook notification request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logException("failure", UNKNOWN_STATUS, elapsedMs(startedAt), notificationMessage, ex);
            throw new IllegalStateException("Webhook notification request was interrupted", ex);
        }
    }

    private HttpRequest buildRequest(NotificationMessage notificationMessage, String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(webhookUri)
                .timeout(properties.getReadTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "ganaderia4backend-notifications")
                .header("X-Ganaderia4-Event-Type", nullSafe(notificationMessage.getEventType()))
                .header("X-Ganaderia4-Timestamp", notificationMessage.getCreatedAt().toString())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

        String secret = properties.getSecret();
        if (secret != null && !secret.isBlank()) {
            builder.header("X-Ganaderia4-Signature", SIGNATURE_PREFIX + sign(requestBody, secret));
        }

        return builder.build();
    }

    private String serialize(NotificationMessage notificationMessage) {
        try {
            return objectMapper.writeValueAsString(WebhookNotificationPayload.from(notificationMessage));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Webhook notification payload could not be serialized", ex);
        }
    }

    private String sign(String requestBody, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Webhook notification signature could not be created", ex);
        }
    }

    private URI validateWebhookUri(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Webhook notification URL must be configured when webhook channel is enabled");
        }

        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("Webhook notification URL must use http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("Webhook notification URL must include a host");
        }

        return uri;
    }

    private Duration validateTimeout(Duration timeout, String propertyName) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("Webhook notification " + propertyName + " must be greater than zero");
        }

        return timeout;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private void logResult(String result,
                           int status,
                           long durationMs,
                           NotificationMessage notificationMessage) {
        String message = "event=notification_webhook_result channel={} result={} requestId={} destination={} "
                + "status={} durationMs={} eventType={}";

        if ("success".equals(result)) {
            logger.info(
                    message,
                    getChannel().name(),
                    result,
                    requestId(),
                    OperationalLogSanitizer.destination(webhookUri),
                    status,
                    durationMs,
                    OperationalLogSanitizer.safe(notificationMessage.getEventType())
            );
            return;
        }

        logger.warn(
                message,
                getChannel().name(),
                result,
                requestId(),
                OperationalLogSanitizer.destination(webhookUri),
                status,
                durationMs,
                OperationalLogSanitizer.safe(notificationMessage.getEventType())
        );
    }

    private void logException(String result,
                              String status,
                              long durationMs,
                              NotificationMessage notificationMessage,
                              Exception exception) {
        logger.warn(
                "event=notification_webhook_result channel={} result={} requestId={} destination={} status={} durationMs={} eventType={} errorType={}",
                getChannel().name(),
                result,
                requestId(),
                OperationalLogSanitizer.destination(webhookUri),
                status,
                durationMs,
                OperationalLogSanitizer.safe(notificationMessage.getEventType()),
                exception.getClass().getSimpleName()
        );
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String requestId() {
        return OperationalLogSanitizer.safe(MDC.get("requestId"));
    }
}
