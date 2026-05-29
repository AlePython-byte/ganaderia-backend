package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ganaderia4.backend.config.ClaudeProperties;
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
public class ClaudeAiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiClient.class);
    private static final String PROVIDER = "CLAUDE";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ClaudeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ClaudeAiClient(ClaudeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    public String complete(String prompt) {
        try {
            URI uri = URI.create(properties.getApiUrl());
            String body = buildRequestBody(prompt);

            log.info(
                    "event=claude_request_start provider={} url={} api_key_prefix={}",
                    PROVIDER, uri, resolveApiKeyPreview()
            );
            log.debug("event=claude_request_body provider={} body={}", PROVIDER, body);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(properties.getReadTimeout())
                    .header("Content-Type", "application/json")
                    .header("x-api-key", properties.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            log.info(
                    "event=claude_response_received provider={} status={}",
                    PROVIDER, response.statusCode()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "event=claude_provider_http_error requestId={} provider={} model={} status={} response_body={}",
                        OperationalLogSanitizer.requestId(),
                        PROVIDER,
                        OperationalLogSanitizer.safe(properties.getModel()),
                        response.statusCode(),
                        response.body()
                );
                throw new ClaudeAiClientException("http_" + response.statusCode());
            }

            return parseContent(response.body());

        } catch (IOException ex) {
            log.warn(
                    "event=claude_io_error provider={} error={}",
                    PROVIDER, ex.getMessage()
            );
            throw new ClaudeAiClientException("io_error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("event=claude_interrupted provider={}", PROVIDER);
            throw new ClaudeAiClientException("interrupted", ex);
        }
    }

    private String resolveApiKeyPreview() {
        String key = properties.getApiKey();
        if (key == null || key.isBlank()) return "MISSING";
        return key.substring(0, Math.min(8, key.length())) + "...";
    }

    private String buildRequestBody(String prompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("max_tokens", properties.getMaxTokens());

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", prompt));
        root.set("messages", messages);

        return objectMapper.writeValueAsString(root);
    }

    String parseContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new ClaudeAiClientException("missing_content");
            }

            JsonNode text = content.path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new ClaudeAiClientException("missing_text");
            }

            return text.asText().trim();

        } catch (IOException ex) {
            throw new ClaudeAiClientException("parse_error", ex);
        }
    }

    public static class ClaudeAiClientException extends RuntimeException {
        public ClaudeAiClientException(String message) {
            super(message);
        }

        public ClaudeAiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
