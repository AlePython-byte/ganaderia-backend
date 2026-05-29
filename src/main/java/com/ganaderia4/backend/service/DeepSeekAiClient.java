package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ganaderia4.backend.config.DeepSeekProperties;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class DeepSeekAiClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiClient.class);
    private static final String PROVIDER = "DEEPSEEK";

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekAiClient(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    public String complete(String systemPrompt, String userMessage) {
        try {
            HttpRequest request = HttpRequest.newBuilder(buildUri())
                    .timeout(properties.getReadTimeout())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            buildRequestBody(systemPrompt, userMessage), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "event=deepseek_provider_http_error requestId={} provider={} model={} status={}",
                        OperationalLogSanitizer.requestId(),
                        PROVIDER,
                        OperationalLogSanitizer.safe(properties.getModel()),
                        response.statusCode()
                );
                throw new DeepSeekAiClientException("http_" + response.statusCode());
            }

            return parseContent(response.body());

        } catch (IOException ex) {
            throw new DeepSeekAiClientException("io_error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DeepSeekAiClientException("interrupted", ex);
        }
    }

    private URI buildUri() {
        String baseUrl = properties.getBaseUrl();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + "/chat/completions");
    }

    private String buildRequestBody(String systemPrompt, String userMessage) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("max_tokens", properties.getMaxTokens());
        root.put("temperature", 0.2);
        root.put("stream", false);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", systemPrompt));
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", userMessage));
        root.set("messages", messages);

        return objectMapper.writeValueAsString(root);
    }

    String parseContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new DeepSeekAiClientException("missing_choices");
            }

            JsonNode content = choices.path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new DeepSeekAiClientException("missing_content");
            }

            return content.asText().trim();

        } catch (IOException ex) {
            throw new DeepSeekAiClientException("parse_error", ex);
        }
    }

    public static class DeepSeekAiClientException extends RuntimeException {
        public DeepSeekAiClientException(String message) {
            super(message);
        }

        public DeepSeekAiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
