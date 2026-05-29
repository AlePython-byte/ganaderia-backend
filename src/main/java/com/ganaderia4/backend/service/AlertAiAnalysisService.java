package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.AiAnalysisProperties;
import com.ganaderia4.backend.config.ClaudeProperties;
import com.ganaderia4.backend.config.DeepSeekProperties;
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
    private static final String SOURCE_GEMINI = "GEMINI";
    private static final String SOURCE_DEEPSEEK = "DEEPSEEK";
    private static final String SOURCE_CLAUDE = "CLAUDE";
    private static final String SOURCE_RULE_BASED_FALLBACK = "RULE_BASED_FALLBACK";

    private final AlertAnalysisService alertAnalysisService;
    private final GeminiAiClient geminiAiClient;
    private final AiAnalysisProperties properties;
    private final DeepSeekAiClient deepSeekAiClient;
    private final DeepSeekProperties deepSeekProperties;
    private final ClaudeAiClient claudeAiClient;
    private final ClaudeProperties claudeProperties;
    private final DomainMetricsService domainMetricsService;

    public AlertAiAnalysisService(AlertAnalysisService alertAnalysisService,
                                  GeminiAiClient geminiAiClient,
                                  AiAnalysisProperties properties,
                                  DeepSeekAiClient deepSeekAiClient,
                                  DeepSeekProperties deepSeekProperties,
                                  ClaudeAiClient claudeAiClient,
                                  ClaudeProperties claudeProperties,
                                  DomainMetricsService domainMetricsService) {
        this.alertAnalysisService = alertAnalysisService;
        this.geminiAiClient = geminiAiClient;
        this.properties = properties;
        this.deepSeekAiClient = deepSeekAiClient;
        this.deepSeekProperties = deepSeekProperties;
        this.claudeAiClient = claudeAiClient;
        this.claudeProperties = claudeProperties;
        this.domainMetricsService = domainMetricsService;
    }

    public AlertAiSummaryDTO getAiSummary() {
        AlertAnalysisSummaryDTO heuristicSummary = alertAnalysisService.getSummary();
        List<AlertPriorityRecommendationDTO> topPriorities =
                alertAnalysisService.getTopPriorities(AlertAnalysisService.DEFAULT_TOP_PRIORITIES_LIMIT);

        if (!properties.isEnabled()) {
            return fallback("disabled", heuristicSummary, topPriorities);
        }

        String provider = properties.getProvider();

        if ("deepseek".equalsIgnoreCase(provider)) {
            return getDeepSeekSummary(heuristicSummary, topPriorities);
        }

        if ("gemini".equalsIgnoreCase(provider)) {
            return getGeminiSummary(heuristicSummary, topPriorities);
        }

        if ("claude".equalsIgnoreCase(provider)) {
            return getClaudeSummary(heuristicSummary, topPriorities);
        }

        return fallback("unknown", heuristicSummary, topPriorities);
    }

    private AlertAiSummaryDTO getGeminiSummary(AlertAnalysisSummaryDTO heuristicSummary,
                                               List<AlertPriorityRecommendationDTO> topPriorities) {
        if (properties.getGeminiApiKey() == null || properties.getGeminiApiKey().isBlank()) {
            return fallback("missing_api_key", heuristicSummary, topPriorities);
        }

        try {
            GeminiAiClient.AiGeneratedSummary aiGeneratedSummary =
                    geminiAiClient.generateOperationalSummary(buildGeminiPrompt(heuristicSummary, topPriorities));

            domainMetricsService.incrementAiProviderRequest("gemini", "success");
            domainMetricsService.incrementAiSummaryGenerated(SOURCE_GEMINI, aiGeneratedSummary.riskLevel().name());

            log.info(
                    "event=alert_ai_summary_generated requestId={} source={} fallbackUsed={} riskLevel={}",
                    OperationalLogSanitizer.requestId(),
                    SOURCE_GEMINI,
                    false,
                    aiGeneratedSummary.riskLevel()
            );

            return new AlertAiSummaryDTO(
                    aiGeneratedSummary.riskLevel(),
                    aiGeneratedSummary.summary(),
                    aiGeneratedSummary.recommendation(),
                    SOURCE_GEMINI,
                    false
            );
        } catch (GeminiAiClient.GeminiAiClientException ex) {
            domainMetricsService.incrementAiProviderRequest("gemini", "failure");
            return fallback(normalizeFallbackReason(ex.getMessage()), heuristicSummary, topPriorities);
        }
    }

    private AlertAiSummaryDTO getDeepSeekSummary(AlertAnalysisSummaryDTO heuristicSummary,
                                                  List<AlertPriorityRecommendationDTO> topPriorities) {
        if (deepSeekProperties.getApiKey() == null || deepSeekProperties.getApiKey().isBlank()) {
            return fallback("missing_api_key", heuristicSummary, topPriorities);
        }

        try {
            String rawText = deepSeekAiClient.complete(
                    buildDeepSeekSystemPrompt(),
                    buildDeepSeekUserMessage(heuristicSummary, topPriorities)
            );

            // Reuse Gemini's package-private JSON parser: same expected format {riskLevel, summary, recommendation}
            GeminiAiClient.AiGeneratedSummary aiGeneratedSummary = geminiAiClient.parseGeneratedText(rawText);

            domainMetricsService.incrementAiProviderRequest("deepseek", "success");
            domainMetricsService.incrementAiSummaryGenerated(SOURCE_DEEPSEEK, aiGeneratedSummary.riskLevel().name());

            log.info(
                    "event=alert_ai_summary_generated requestId={} source={} fallbackUsed={} riskLevel={}",
                    OperationalLogSanitizer.requestId(),
                    SOURCE_DEEPSEEK,
                    false,
                    aiGeneratedSummary.riskLevel()
            );

            return new AlertAiSummaryDTO(
                    aiGeneratedSummary.riskLevel(),
                    aiGeneratedSummary.summary(),
                    aiGeneratedSummary.recommendation(),
                    SOURCE_DEEPSEEK,
                    false
            );
        } catch (DeepSeekAiClient.DeepSeekAiClientException | GeminiAiClient.GeminiAiClientException ex) {
            domainMetricsService.incrementAiProviderRequest("deepseek", "failure");
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
            case "io_error", "interrupted", "prompt_too_long" -> "provider_error";
            case "unusable_response" -> "unusable_response";
            case "parse_error", "missing_candidates", "missing_text",
                 "missing_choices", "missing_content" -> "parse_error";
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

    private AlertAiSummaryDTO getClaudeSummary(AlertAnalysisSummaryDTO heuristicSummary,
                                               List<AlertPriorityRecommendationDTO> topPriorities) {
        if (claudeProperties.getApiKey() == null || claudeProperties.getApiKey().isBlank()) {
            return fallback("missing_api_key", heuristicSummary, topPriorities);
        }

        try {
            String rawText = claudeAiClient.complete(buildClaudePrompt(heuristicSummary, topPriorities));

            GeminiAiClient.AiGeneratedSummary aiGeneratedSummary = geminiAiClient.parseGeneratedText(rawText);

            domainMetricsService.incrementAiProviderRequest("claude", "success");
            domainMetricsService.incrementAiSummaryGenerated(SOURCE_CLAUDE, aiGeneratedSummary.riskLevel().name());

            log.info(
                    "event=alert_ai_summary_generated requestId={} source={} fallbackUsed={} riskLevel={}",
                    OperationalLogSanitizer.requestId(),
                    SOURCE_CLAUDE,
                    false,
                    aiGeneratedSummary.riskLevel()
            );

            return new AlertAiSummaryDTO(
                    aiGeneratedSummary.riskLevel(),
                    aiGeneratedSummary.summary(),
                    aiGeneratedSummary.recommendation(),
                    SOURCE_CLAUDE,
                    false
            );
        } catch (ClaudeAiClient.ClaudeAiClientException | GeminiAiClient.GeminiAiClientException ex) {
            domainMetricsService.incrementAiProviderRequest("claude", "failure");
            return fallback(normalizeFallbackReason(ex.getMessage()), heuristicSummary, topPriorities);
        }
    }

    // --- Gemini prompt (single string, unchanged behavior) ---

    private String buildGeminiPrompt(AlertAnalysisSummaryDTO heuristicSummary,
                                     List<AlertPriorityRecommendationDTO> topPriorities) {
        StringBuilder prompt = new StringBuilder()
                .append("Eres un asistente operativo para monitoreo ganadero.\n")
                .append("Debes responder en espanol claro, breve y operativo.\n")
                .append("Responde unicamente con JSON puro y valido.\n")
                .append("No uses markdown.\n")
                .append("No uses bloques ```.\n")
                .append("No escribas texto antes ni despues del JSON.\n")
                .append("No escribas frases introductorias como 'Here is the JSON requested'.\n")
                .append("No inventes alertas, vacas, collares, cantidades ni hechos que no aparezcan en los datos.\n")
                .append("Usa solo la informacion entregada por el backend. Si faltan datos, responde con cautela.\n")
                .append("No recomiendes acciones destructivas ni cambios automaticos en el sistema.\n")
                .append("Usa exactamente los campos riskLevel, summary y recommendation.\n")
                .append("riskLevel debe ser exactamente uno de estos valores: LOW, MEDIUM, HIGH, CRITICAL.\n")
                .append("El campo summary debe estar en espanol y describir el estado operativo actual.\n")
                .append("El campo recommendation debe estar en espanol y dar una accion operativa concreta.\n")
                .append("Devuelve solo JSON valido con este formato exacto y sin campos adicionales: ")
                .append("{\"riskLevel\":\"LOW|MEDIUM|HIGH|CRITICAL\",\"summary\":\"...\",\"recommendation\":\"...\"}\n");

        appendBackendData(prompt, heuristicSummary, topPriorities);
        return prompt.toString();
    }

    // --- Claude prompt (system instructions + data combined in single user message) ---

    private String buildClaudePrompt(AlertAnalysisSummaryDTO heuristicSummary,
                                     List<AlertPriorityRecommendationDTO> topPriorities) {
        StringBuilder prompt = new StringBuilder(buildDeepSeekSystemPrompt())
                .append('\n')
                .append("Datos del backend:\n");
        appendBackendData(prompt, heuristicSummary, topPriorities);
        return prompt.toString();
    }

    // --- DeepSeek prompt (system + user split for chat format) ---

    private String buildDeepSeekSystemPrompt() {
        return "Eres un asistente operativo para monitoreo ganadero.\n" +
               "Debes responder en espanol claro, breve y operativo.\n" +
               "Responde unicamente con JSON puro y valido.\n" +
               "No uses markdown. No uses bloques ```.\n" +
               "No escribas texto antes ni despues del JSON.\n" +
               "No inventes alertas, vacas, collares, cantidades ni hechos que no aparezcan en los datos.\n" +
               "Usa solo la informacion entregada. Si faltan datos, responde con cautela.\n" +
               "No recomiendes acciones destructivas ni cambios automaticos en el sistema.\n" +
               "Usa exactamente los campos riskLevel, summary y recommendation.\n" +
               "riskLevel debe ser exactamente uno de estos valores: LOW, MEDIUM, HIGH, CRITICAL.\n" +
               "El campo summary debe estar en espanol y describir el estado operativo actual.\n" +
               "El campo recommendation debe estar en espanol y dar una accion operativa concreta.\n" +
               "Devuelve solo JSON valido con este formato exacto y sin campos adicionales: " +
               "{\"riskLevel\":\"LOW|MEDIUM|HIGH|CRITICAL\",\"summary\":\"...\",\"recommendation\":\"...\"}";
    }

    private String buildDeepSeekUserMessage(AlertAnalysisSummaryDTO heuristicSummary,
                                            List<AlertPriorityRecommendationDTO> topPriorities) {
        StringBuilder data = new StringBuilder("Datos del backend:\n");
        appendBackendData(data, heuristicSummary, topPriorities);
        return data.toString();
    }

    private void appendBackendData(StringBuilder sb,
                                   AlertAnalysisSummaryDTO heuristicSummary,
                                   List<AlertPriorityRecommendationDTO> topPriorities) {
        sb.append("- riskLevel: ").append(heuristicSummary.getRiskLevel()).append('\n')
          .append("- totalPendingAlerts: ").append(heuristicSummary.getTotalPendingAlerts()).append('\n')
          .append("- criticalSignals: ").append(String.join(" | ", heuristicSummary.getCriticalSignals())).append('\n')
          .append("- recommendedActions: ").append(String.join(" | ", heuristicSummary.getRecommendedActions())).append('\n')
          .append("- topPriorities:\n");

        if (topPriorities.isEmpty()) {
            sb.append("  - sin casos pendientes\n");
        } else {
            for (AlertPriorityRecommendationDTO recommendation : topPriorities) {
                sb.append("  - alertType=").append(recommendation.getAlertType())
                  .append(", priorityLabel=").append(recommendation.getPriorityLabel())
                  .append(", priorityScore=").append(recommendation.getPriorityScore())
                  .append(", reason=").append(recommendation.getReason())
                  .append(", recommendedAction=").append(recommendation.getRecommendedAction())
                  .append('\n');
            }
        }
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
