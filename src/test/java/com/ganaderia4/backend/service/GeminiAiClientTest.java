package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.AiAnalysisProperties;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiAiClientTest {

    private GeminiAiClient geminiAiClient;

    @BeforeEach
    void setUp() {
        geminiAiClient = new GeminiAiClient(new AiAnalysisProperties(), new ObjectMapper());
    }

    @Test
    void shouldParsePureJsonResponseText() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"riskLevel\\":\\"HIGH\\",\\"summary\\":\\"Hay varias alertas pendientes.\\",\\"recommendation\\":\\"Revise primero los collares offline.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals(AlertAnalysisRiskLevel.HIGH, response.riskLevel());
        assertEquals("Hay varias alertas pendientes.", response.summary());
        assertEquals("Revise primero los collares offline.", response.recommendation());
    }

    @Test
    void shouldParseJsonInsideMarkdownCodeFence() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\"riskLevel\\":\\"CRITICAL\\",\\"summary\\":\\"Se recomienda atender los casos criticos primero.\\",\\"recommendation\\":\\"Priorice las alertas de geocerca.\\"}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals(AlertAnalysisRiskLevel.CRITICAL, response.riskLevel());
        assertEquals("Se recomienda atender los casos criticos primero.", response.summary());
        assertEquals("Priorice las alertas de geocerca.", response.recommendation());
    }

    @Test
    void shouldParseJsonSurroundedByAdditionalText() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Analisis: {\\"riskLevel\\":\\"HIGH\\",\\"summary\\":\\"Hay riesgo operativo alto.\\" , \\"recommendation\\":\\"Revise primero la cola priorizada.\\"} Fin."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals(AlertAnalysisRiskLevel.HIGH, response.riskLevel());
        assertEquals("Hay riesgo operativo alto.", response.summary());
        assertEquals("Revise primero la cola priorizada.", response.recommendation());
    }

    @Test
    void shouldNormalizeLowercaseRiskLevel() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"riskLevel\\":\\"critical\\",\\"summary\\":\\"Hay alertas criticas pendientes.\\",\\"recommendation\\":\\"Atienda primero los casos de mayor prioridad.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals(AlertAnalysisRiskLevel.CRITICAL, response.riskLevel());
        assertEquals("Hay alertas criticas pendientes.", response.summary());
        assertEquals("Atienda primero los casos de mayor prioridad.", response.recommendation());
    }

    @Test
    void shouldNormalizeSpanishRiskLevel() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"riskLevel\\":\\"CRÍTICO\\",\\"summary\\":\\"Hay riesgo critico en la operacion.\\",\\"recommendation\\":\\"Revise de inmediato las alertas prioritarias.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals(AlertAnalysisRiskLevel.CRITICAL, response.riskLevel());
        assertEquals("Hay riesgo critico en la operacion.", response.summary());
        assertEquals("Revise de inmediato las alertas prioritarias.", response.recommendation());
    }

    @Test
    void shouldRejectPlainTextWithoutUsableJson() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Here is the JSON requested"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.GeminiAiClientException exception = assertThrows(
                GeminiAiClient.GeminiAiClientException.class,
                () -> geminiAiClient.parseResponseBody(rawResponse)
        );

        assertEquals("unusable_response", exception.getMessage());
    }

    @Test
    void shouldRejectJsonWithoutRecommendation() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"riskLevel\\":\\"HIGH\\",\\"summary\\":\\"Hay alertas pendientes.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.GeminiAiClientException exception = assertThrows(
                GeminiAiClient.GeminiAiClientException.class,
                () -> geminiAiClient.parseResponseBody(rawResponse)
        );

        assertEquals("unusable_response", exception.getMessage());
    }

    @Test
    void shouldRejectResponseWhenCandidatesAreMissing() {
        String rawResponse = "{\"candidates\":[]}";

        GeminiAiClient.GeminiAiClientException exception = assertThrows(
                GeminiAiClient.GeminiAiClientException.class,
                () -> geminiAiClient.parseResponseBody(rawResponse)
        );

        assertEquals("missing_candidates", exception.getMessage());
    }

    @Test
    void shouldRejectResponseWhenTextIsEmpty() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": ""
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.GeminiAiClientException exception = assertThrows(
                GeminiAiClient.GeminiAiClientException.class,
                () -> geminiAiClient.parseResponseBody(rawResponse)
        );

        assertEquals("missing_text", exception.getMessage());
    }
}
