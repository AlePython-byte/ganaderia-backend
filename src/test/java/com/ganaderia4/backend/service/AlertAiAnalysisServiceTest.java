package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.AiAnalysisProperties;
import com.ganaderia4.backend.dto.AlertAiSummaryDTO;
import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.AlertPriorityRecommendationDTO;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertAiAnalysisServiceTest {

    @Mock
    private AlertAnalysisService alertAnalysisService;

    @Mock
    private GeminiAiClient geminiAiClient;

    private AiAnalysisProperties properties;
    private AlertAiAnalysisService alertAiAnalysisService;

    @BeforeEach
    void setUp() {
        properties = new AiAnalysisProperties();
        alertAiAnalysisService = new AlertAiAnalysisService(alertAnalysisService, geminiAiClient, properties);
    }

    @Test
    void shouldUseFallbackWhenAiIsDisabled() {
        when(alertAnalysisService.getSummary()).thenReturn(summary(AlertAnalysisRiskLevel.HIGH, 4));
        when(alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT))
                .thenReturn(List.of(priority("COLLAR_OFFLINE")));

        AlertAiSummaryDTO response = alertAiAnalysisService.getAiSummary();

        assertEquals("RULE_BASED_FALLBACK", response.getSource());
        assertTrue(response.isFallbackUsed());
        verify(geminiAiClient, never()).generateOperationalSummary(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldUseAiResponseWhenEnabledAndClientSucceeds() {
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGeminiApiKey("test-key");

        when(alertAnalysisService.getSummary()).thenReturn(summary(AlertAnalysisRiskLevel.CRITICAL, 7));
        when(alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT))
                .thenReturn(List.of(priority("COLLAR_OFFLINE")));
        when(geminiAiClient.generateOperationalSummary(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new GeminiAiClient.AiGeneratedSummary(
                        "El sistema presenta alertas pendientes que requieren atencion inmediata.",
                        "Revise primero los collares offline y luego las alertas de bateria baja."
                ));

        AlertAiSummaryDTO response = alertAiAnalysisService.getAiSummary();

        assertEquals("AI", response.getSource());
        assertEquals(false, response.isFallbackUsed());
        assertEquals(AlertAnalysisRiskLevel.CRITICAL, response.getRiskLevel());
    }

    @Test
    void shouldFallbackWhenAiClientFails() {
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGeminiApiKey("test-key");

        when(alertAnalysisService.getSummary()).thenReturn(summary(AlertAnalysisRiskLevel.HIGH, 2));
        when(alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT))
                .thenReturn(List.of(priority("LOW_BATTERY")));
        when(geminiAiClient.generateOperationalSummary(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new GeminiAiClient.GeminiAiClientException("http_500"));

        AlertAiSummaryDTO response = alertAiAnalysisService.getAiSummary();

        assertEquals("RULE_BASED_FALLBACK", response.getSource());
        assertTrue(response.isFallbackUsed());
        assertEquals(AlertAnalysisRiskLevel.HIGH, response.getRiskLevel());
    }

    @Test
    void shouldUseAiSourceWhenClientReturnsPlainTextWithoutRecommendation() {
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGeminiApiKey("test-key");

        when(alertAnalysisService.getSummary()).thenReturn(summary(AlertAnalysisRiskLevel.HIGH, 2));
        when(alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT))
                .thenReturn(List.of(priority("LOW_BATTERY")));
        when(geminiAiClient.generateOperationalSummary(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new GeminiAiClient.AiGeneratedSummary(
                        "Hay varias alertas pendientes que requieren atencion operativa.",
                        ""
                ));

        AlertAiSummaryDTO response = alertAiAnalysisService.getAiSummary();

        assertEquals("AI", response.getSource());
        assertEquals(false, response.isFallbackUsed());
        assertEquals("Hay varias alertas pendientes que requieren atencion operativa.", response.getSummary());
        assertTrue(response.getRecommendation().contains("mayor prioridad"));
    }

    private AlertAnalysisSummaryDTO summary(AlertAnalysisRiskLevel riskLevel, long totalPendingAlerts) {
        return new AlertAnalysisSummaryDTO(
                riskLevel,
                totalPendingAlerts,
                List.of("Hay collares offline pendientes."),
                List.of("Revisar conectividad y estado fisico de los collares offline."),
                AlertAnalysisService.CONFIDENCE_RULE_BASED
        );
    }

    private AlertPriorityRecommendationDTO priority(String alertType) {
        return new AlertPriorityRecommendationDTO(
                1L,
                alertType,
                "PENDIENTE",
                80,
                "HIGH",
                "La alerta requiere revision operativa.",
                "Revisar el detalle de la alerta en la cola de prioridades.",
                1L,
                "****0001",
                LocalDateTime.now()
        );
    }
}
