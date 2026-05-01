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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiAiClient {

    private static final Pattern JSON_CODE_FENCE = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

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

            return parseResponseBody(response.body());
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

    AiGeneratedSummary parseResponseBody(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode candidatesNode = root.path("candidates");
            if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
                throw new GeminiAiClientException("missing_candidates");
            }

            JsonNode textNode = candidatesNode.path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new GeminiAiClientException("missing_text");
            }

            return parseGeneratedText(textNode.asText());
        } catch (IOException ex) {
            throw new GeminiAiClientException("parse_error", ex);
        }
    }

    AiGeneratedSummary parseGeneratedText(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new GeminiAiClientException("missing_text");
        }

        String normalizedText = extractCodeFenceJson(trimmed);
        String jsonCandidate = extractJsonObject(normalizedText);
        if (jsonCandidate != null) {
            try {
                JsonNode summaryJson = objectMapper.readTree(jsonCandidate);
                String summary = summaryJson.path("summary").asText("").trim();
                String recommendation = summaryJson.path("recommendation").asText("").trim();

                if (!summary.isBlank()) {
                    return new AiGeneratedSummary(summary, recommendation);
                }
            } catch (IOException ignored) {
                // Fall through to plain-text handling.
            }
        }

        return new AiGeneratedSummary(trimmed, "");
    }

    private String extractCodeFenceJson(String value) {
        Matcher matcher = JSON_CODE_FENCE.matcher(value);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return value;
    }

    private String extractJsonObject(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1).trim();
        }

        return null;
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
