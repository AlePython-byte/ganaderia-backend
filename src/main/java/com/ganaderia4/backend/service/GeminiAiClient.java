package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ganaderia4.backend.config.AiAnalysisProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class GeminiAiClient {

    private final AiAnalysisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAiClient(AiAnalysisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getGeminiConnectTimeout())
                .build();
    }

    public AiGeneratedSummary generateOperationalSummary(String prompt) {
        try {
            HttpRequest request = HttpRequest.newBuilder(buildUri())
                    .timeout(properties.getGeminiReadTimeout())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", properties.getGeminiApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GeminiAiClientException("http_" + response.statusCode());
            }

            return parseResponse(response.body());
        } catch (IOException ex) {
            throw new GeminiAiClientException("io_error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiAiClientException("interrupted", ex);
        }
    }

    private URI buildUri() {
        String baseUrl = properties.getGeminiBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalizedBaseUrl + "/" + properties.getGeminiModel() + ":generateContent");
    }

    private String buildRequestBody(String prompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("contents", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode()
                        .set("parts", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("text", prompt)))));
        root.set("generationConfig", objectMapper.createObjectNode()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 200)
                .put("responseMimeType", "application/json"));

        return objectMapper.writeValueAsString(root);
    }

    private AiGeneratedSummary parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new GeminiAiClientException("missing_text");
            }

            String text = stripCodeFence(textNode.asText());
            JsonNode summaryJson = objectMapper.readTree(text);
            String summary = summaryJson.path("summary").asText("").trim();
            String recommendation = summaryJson.path("recommendation").asText("").trim();

            if (summary.isBlank() || recommendation.isBlank()) {
                throw new GeminiAiClientException("invalid_payload");
            }

            return new AiGeneratedSummary(summary, recommendation);
        } catch (IOException ex) {
            throw new GeminiAiClientException("parse_error", ex);
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }

        return trimmed;
    }

    public record AiGeneratedSummary(String summary, String recommendation) {
    }

    public static class GeminiAiClientException extends RuntimeException {
        public GeminiAiClientException(String message) {
            super(message);
        }

        public GeminiAiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
