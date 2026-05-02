package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ResendEmailClient implements EmailProviderClient {

    private static final String PROVIDER_NAME = "resend";

    private final EmailNotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailClient(EmailNotificationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(validatedTimeout(properties.getConnectTimeoutMs(), "connect-timeout-ms")))
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public void send(EmailNotificationRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(buildUri())
                    .timeout(Duration.ofMillis(validatedTimeout(properties.getReadTimeoutMs(), "read-timeout-ms")))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "ganaderia4backend-notifications")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmailNotificationException("http_" + response.statusCode());
            }
        } catch (JsonProcessingException ex) {
            throw new EmailNotificationException("serialization_error", ex);
        } catch (IOException ex) {
            throw new EmailNotificationException("io_error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EmailNotificationException("interrupted", ex);
        }
    }

    private URI buildUri() {
        String baseUrl = properties.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return URI.create(normalizedBaseUrl + "/emails");
    }

    private String buildRequestBody(EmailNotificationRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("from", request.from());
        root.put("subject", request.subject());
        root.put("text", request.textBody());
        if (request.to() != null && !request.to().isEmpty()) {
            root.set("to", objectMapper.valueToTree(request.to()));
        }
        if (request.htmlBody() != null && !request.htmlBody().isBlank()) {
            root.put("html", request.htmlBody());
        }
        return objectMapper.writeValueAsString(root);
    }

    private long validatedTimeout(long timeoutMs, String propertyName) {
        if (timeoutMs <= 0) {
            throw new IllegalStateException("Email notification " + propertyName + " must be greater than zero");
        }

        return timeoutMs;
    }
}
