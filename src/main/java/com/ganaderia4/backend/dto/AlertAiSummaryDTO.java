package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen operativo asistido por IA o fallback heurístico")
public class AlertAiSummaryDTO {

    @Schema(description = "Nivel de riesgo consolidado para la operación")
    private AlertAnalysisRiskLevel riskLevel;

    @Schema(description = "Resumen narrativo de la situación operativa")
    private String summary;

    @Schema(description = "Recomendación principal generada para el operador")
    private String recommendation;

    @Schema(description = "Fuente de la respuesta, por ejemplo GEMINI o RULE_BASED_FALLBACK", example = "RULE_BASED_FALLBACK")
    private String source;

    @Schema(description = "Indica si se usó fallback heurístico en lugar de IA externa", example = "true")
    private boolean fallbackUsed;

    public AlertAiSummaryDTO() {
    }

    public AlertAiSummaryDTO(AlertAnalysisRiskLevel riskLevel,
                             String summary,
                             String recommendation,
                             String source,
                             boolean fallbackUsed) {
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.recommendation = recommendation;
        this.source = source;
        this.fallbackUsed = fallbackUsed;
    }

    public AlertAnalysisRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(AlertAnalysisRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
}
