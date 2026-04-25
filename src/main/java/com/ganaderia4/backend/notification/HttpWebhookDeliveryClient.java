package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.config.WebhookNotificationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

@Component
@ConditionalOnProperty(prefix = "app.notifications.webhook", name = "enabled", havingValue = "true")
public class HttpWebhookDeliveryClient implements WebhookDeliveryClient {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final WebhookNotificationProperties properties;
    private final HttpClient httpClient;

    public HttpWebhookDeliveryClient(WebhookNotificationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    @Override
    public WebhookDeliveryResponse send(WebhookNotificationDelivery delivery, int attemptNumber)
            throws IOException, InterruptedException {
        HttpRequest request = buildRequest(delivery, attemptNumber);
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return new WebhookDeliveryResponse(response.statusCode());
    }

    private HttpRequest buildRequest(WebhookNotificationDelivery delivery, int attemptNumber) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(delivery.getDestination()))
                .timeout(properties.getReadTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "ganaderia4backend-notifications")
                .header("Idempotency-Key", delivery.getNotificationId())
                .header("X-Ganaderia4-Notification-Id", delivery.getNotificationId())
                .header("X-Ganaderia4-Event-Type", delivery.getEventType())
                .header("X-Ganaderia4-Delivery-Attempt", String.valueOf(Math.max(1, attemptNumber)))
                .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayload(), StandardCharsets.UTF_8));

        String secret = properties.getSecret();
        if (secret != null && !secret.isBlank()) {
            builder.header("X-Ganaderia4-Signature", SIGNATURE_PREFIX + sign(delivery.getPayload(), secret));
        }

        return builder.build();
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
}
