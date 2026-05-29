package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.ClaudeProperties;
import com.ganaderia4.backend.dto.AlertAnalysisRequest;
import com.ganaderia4.backend.dto.AlertAnalysisResponse;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);
    private static final String PROVIDER = "CLAUDE";

    private static final String SYSTEM_PROMPT =
            "Eres un asistente experto en monitoreo ganadero. " +
            "Analiza los datos operativos del hato y genera un diagnóstico claro y conciso en español. " +
            "Describe el estado actual, identifica los riesgos principales y proporciona recomendaciones concretas. " +
            "No inventes datos ni hagas suposiciones fuera de la información entregada. " +
            "Responde en texto plano, sin markdown ni JSON. Máximo 300 palabras.";

    private final ClaudeAiClient claudeAiClient;
    private final ClaudeProperties claudeProperties;

    public DeepSeekService(ClaudeAiClient claudeAiClient, ClaudeProperties claudeProperties) {
        this.claudeAiClient = claudeAiClient;
        this.claudeProperties = claudeProperties;
    }

    public AlertAnalysisResponse analyzeAlerts(AlertAnalysisRequest request) {
        if (claudeProperties.getApiKey() == null || claudeProperties.getApiKey().isBlank()) {
            throw new BadRequestException("El servicio de análisis IA no está configurado.");
        }

        String prompt = SYSTEM_PROMPT + "\n\n" + buildUserMessage(request);

        try {
            String analysis = claudeAiClient.complete(prompt);

            log.info(
                    "event=claude_analysis_generated requestId={} provider={} riskLevel={} activeAlerts={}",
                    OperationalLogSanitizer.requestId(),
                    PROVIDER,
                    OperationalLogSanitizer.safe(request.getRiskLevel()),
                    request.getActiveAlerts()
            );

            return new AlertAnalysisResponse(analysis, PROVIDER, Instant.now());

        } catch (ClaudeAiClient.ClaudeAiClientException ex) {
            log.error(
                    "event=claude_analysis_failed requestId={} provider={} reason={}",
                    OperationalLogSanitizer.requestId(),
                    PROVIDER,
                    OperationalLogSanitizer.safe(ex.getMessage())
            );
            throw ex;
        }
    }

    private String buildUserMessage(AlertAnalysisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Datos operativos del hato:\n");
        sb.append("- Alertas activas: ").append(request.getActiveAlerts()).append('\n');
        sb.append("- Nivel de riesgo: ").append(request.getRiskLevel()).append('\n');

        sb.append("- Eventos críticos: ");
        appendList(sb, request.getCriticalEvents());

        sb.append("- Problemas identificados: ");
        appendList(sb, request.getProblems());

        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<String> items) {
        if (items == null || items.isEmpty()) {
            sb.append("ninguno\n");
        } else {
            sb.append('\n');
            for (String item : items) {
                sb.append("  * ").append(item).append('\n');
            }
        }
    }
}
