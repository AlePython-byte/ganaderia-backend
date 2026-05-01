package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;

public class AlertAiSummaryDTO {

    private AlertAnalysisRiskLevel riskLevel;
    private String summary;
    private String recommendation;
    private String source;
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
