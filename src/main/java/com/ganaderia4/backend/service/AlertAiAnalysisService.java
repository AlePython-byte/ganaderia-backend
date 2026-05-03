package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.AiAnalysisProperties;
import com.ganaderia4.backend.dto.AlertAiSummaryDTO;
import com.ganaderia4.backend.dto.AlertAnalysisSummaryDTO;
import com.ganaderia4.backend.dto.AlertPriorityRecommendationDTO;
import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AlertAiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AlertAiAnalysisService.class);
    private static final String SOURCE_AI = "AI";
    private static final String SOURCE_RULE_BASED_FALLBACK = "RULE_BASED_FALLBACK";

    private final AlertAnalysisService alertAnalysisService;
    private final GeminiAiClient geminiAiClient;
    private final AiAnalysisProperties properties;
    private final DomainMetricsService domainMetricsService;

    public AlertAiAnalysisService(AlertAnalysisService alertAnalysisService,
                                  GeminiAiClient geminiAiClient,
                                  AiAnalysisProperties properties,
                                  DomainMetricsService domainMetricsService) {
        this.alertAnalysisService = alertAnalysisService;
        this.geminiAiClient = geminiAiClient;
        this.properties = properties;
        this.domainMetricsService = domainMetricsService;
    }

    public AlertAiSummaryDTO getAiSummary() {
        AlertAnalysisSummaryDTO heuristicSummary = alertAnalysisService.getSummary();
        List<AlertPriorityRecommendationDTO> topPriorities =
                alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT);

        if (!properties.isEnabled()) {
            return fallback("disabled", heuristicSummary, topPriorities);
        }

        if (!"gemini".equalsIgnoreCase(properties.getProvider())) {
            return fallback("unknown", heuristicSummary, topPriorities);
        }

        if (properties.getGeminiApiKey() == null || properties.getGeminiApiKey().isBlank()) {
            return fallback("missing_api_key", heuristicSummary, topPriorities);
        }

        try {
            GeminiAiClient.AiGeneratedSummary aiGeneratedSummary =
                    geminiAiClient.generateOperationalSummary(buildPrompt(heuristicSummary, topPriorities));

            AlertAiSummaryDTO response = new AlertAiSummaryDTO(
                    heuristicSummary.getRiskLevel(),
                    aiGeneratedSummary.summary(),
                    aiGeneratedSummary.recommendation() != null && !aiGeneratedSummary.recommendation().isBlank()
                            ? aiGeneratedSummary.recommendation()
                            : fallbackRecommendationFor(heuristicSummary, topPriorities),
                    SOURCE_AI,
                    false
            );

            domainMetricsService.incrementAiProviderRequest("gemini", "success");
            domainMetricsService.incrementAiSummaryGenerated(SOURCE_AI, heuristicSummary.getRiskLevel().name());

            log.info(
                    "event=alert_ai_summary_generated requestId={} source={} fallbackUsed={} riskLevel={}",
                    OperationalLogSanitizer.requestId(),
                    SOURCE_AI,
                    false,
                    heuristicSummary.getRiskLevel()
            );

            return response;
        } catch (GeminiAiClient.GeminiAiClientException ex) {
            domainMetricsService.incrementAiProviderRequest("gemini", "failure");
            return fallback(normalizeFallbackReason(ex.getMessage()), heuristicSummary, topPriorities);
        }
    }

    private AlertAiSummaryDTO fallback(String reason,
                                       AlertAnalysisSummaryDTO heuristicSummary,
                                       List<AlertPriorityRecommendationDTO> topPriorities) {
        String normalizedReason = normalizeFallbackReason(reason);

        domainMetricsService.incrementAiSummaryFallback(normalizedReason);

        log.warn(
                "event=alert_ai_summary_fallback requestId={} reason={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(normalizedReason)
        );

        AlertAiSummaryDTO response = new AlertAiSummaryDTO(
                heuristicSummary.getRiskLevel(),
                fallbackSummaryFor(heuristicSummary),
                fallbackRecommendationFor(heuristicSummary, topPriorities),
                SOURCE_RULE_BASED_FALLBACK,
                true
        );

        domainMetricsService.incrementAiSummaryGenerated(
                SOURCE_RULE_BASED_FALLBACK,
                heuristicSummary.getRiskLevel().name()
        );

        log.info(
                "event=alert_ai_summary_generated requestId={} source={} fallbackUsed={} riskLevel={}",
                OperationalLogSanitizer.requestId(),
                SOURCE_RULE_BASED_FALLBACK,
                true,
                heuristicSummary.getRiskLevel()
        );

        return response;
    }

    private String normalizeFallbackReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }

        return switch (reason) {
            case "disabled", "ai_disabled" -> "disabled";
            case "missing_api_key" -> "missing_api_key";
            case "provider_error" -> "provider_error";
            case "io_error", "interrupted" -> "provider_error";
            case "unusable_response" -> "unusable_response";
            case "parse_error", "missing_candidates", "missing_text" -> "parse_error";
            case "unusable_plain_text" -> "unusable_response";
            case "unknown", "unsupported_provider" -> "unknown";
            default -> {
                if (reason.startsWith("http_")) {
                    yield "provider_error";
                }
                yield "unknown";
            }
        };
    }

    private String buildPrompt(AlertAnalysisSummaryDTO heuristicSummary,
                               List<AlertPriorityRecommendationDTO> topPriorities) {
        StringBuilder prompt = new StringBuilder()
                .append("Eres un asistente operativo para monitoreo ganadero.\n")
                .append("Debes responder en espanol claro, breve y operativo.\n")
                .append("Responde unicamente con JSON valido.\n")
                .append("No uses markdown.\n")
                .append("No uses bloques ```.\n")
                .append("No escribas texto antes ni despues del JSON.\n")
                .append("No escribas frases introductorias como 'Here is the JSON requested'.\n")
                .append("No inventes alertas, vacas, collares, cantidades ni hechos que no aparezcan en los datos.\n")
                .append("Usa solo la informacion entregada por el backend. Si faltan datos, responde con cautela.\n")
                .append("No recomiendes acciones destructivas ni cambios automaticos en el sistema.\n")
                .append("El campo summary debe estar en espanol y describir el estado operativo actual.\n")
                .append("El campo recommendation debe estar en espanol y dar una accion operativa concreta.\n")
                .append("Devuelve solo JSON valido con este formato exacto y sin campos adicionales: ")
                .append("{\"summary\":\"...\",\"recommendation\":\"...\"}\n")
                .append("Datos del backend:\n")
                .append("- riskLevel: ").append(heuristicSummary.getRiskLevel()).append('\n')
                .append("- totalPendingAlerts: ").append(heuristicSummary.getTotalPendingAlerts()).append('\n')
                .append("- criticalSignals: ").append(String.join(" | ", heuristicSummary.getCriticalSignals())).append('\n')
                .append("- recommendedActions: ").append(String.join(" | ", heuristicSummary.getRecommendedActions())).append('\n')
                .append("- topPriorities:\n");

        if (topPriorities.isEmpty()) {
            prompt.append("  - sin casos pendientes\n");
        } else {
            for (AlertPriorityRecommendationDTO recommendation : topPriorities) {
                prompt.append("  - alertType=").append(recommendation.getAlertType())
                        .append(", priorityLabel=").append(recommendation.getPriorityLabel())
                        .append(", priorityScore=").append(recommendation.getPriorityScore())
                        .append(", reason=").append(recommendation.getReason())
                        .append(", recommendedAction=").append(recommendation.getRecommendedAction())
                        .append('\n');
            }
        }

        return prompt.toString();
    }

    private String fallbackSummaryFor(AlertAnalysisSummaryDTO heuristicSummary) {
        if (heuristicSummary.getRiskLevel() == AlertAnalysisRiskLevel.LOW) {
            return "No hay alertas pendientes relevantes en este momento.";
        }

        return "Resumen generado con reglas internas porque la IA no esta configurada o no respondio correctamente.";
    }

    private String fallbackRecommendationFor(AlertAnalysisSummaryDTO heuristicSummary,
                                             List<AlertPriorityRecommendationDTO> topPriorities) {
        if (heuristicSummary.getRiskLevel() == AlertAnalysisRiskLevel.LOW || topPriorities.isEmpty()) {
            return "No hay acciones criticas pendientes en este momento.";
        }

        AlertPriorityRecommendationDTO firstPriority = topPriorities.get(0);
        String alertType = firstPriority.getAlertType() != null
                ? firstPriority.getAlertType().toLowerCase(Locale.ROOT)
                : "alertas";

        return "Revise primero las alertas pendientes de mayor prioridad, comenzando por " + alertType + ".";
    }
}
