package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.AiAnalysisProperties;
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
                            "text": "{\\"summary\\":\\"Hay varias alertas pendientes.\\",\\"recommendation\\":\\"Revise primero los collares offline.\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

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
                            "text": "```json\\n{\\"summary\\":\\"Se recomienda atender los casos criticos primero.\\",\\"recommendation\\":\\"Priorice las alertas de geocerca.\\"}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

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
                            "text": "Analisis: {\\"summary\\":\\"Hay riesgo operativo alto.\\" , \\"recommendation\\":\\"Revise primero la cola priorizada.\\"} Fin."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals("Hay riesgo operativo alto.", response.summary());
        assertEquals("Revise primero la cola priorizada.", response.recommendation());
    }

    @Test
    void shouldAcceptPlainTextWhenJsonCannotBeParsed() {
        String rawResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "El sistema presenta alertas pendientes que requieren atencion operativa inmediata."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiAiClient.AiGeneratedSummary response = geminiAiClient.parseResponseBody(rawResponse);

        assertEquals("El sistema presenta alertas pendientes que requieren atencion operativa inmediata.", response.summary());
        assertEquals("", response.recommendation());
    }

    @Test
    void shouldRejectGenericIntroductoryPlainText() {
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

        assertEquals("unusable_plain_text", exception.getMessage());
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
