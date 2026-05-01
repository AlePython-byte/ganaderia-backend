package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.AlertAnalysisRiskLevel;

import java.util.ArrayList;
import java.util.List;

public class AlertAnalysisSummaryDTO {

    private AlertAnalysisRiskLevel riskLevel;
    private long totalPendingAlerts;
    private List<String> criticalSignals = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private String confidence;

    public AlertAnalysisSummaryDTO() {
    }

    public AlertAnalysisSummaryDTO(AlertAnalysisRiskLevel riskLevel,
                                   long totalPendingAlerts,
                                   List<String> criticalSignals,
                                   List<String> recommendedActions,
                                   String confidence) {
        this.riskLevel = riskLevel;
        this.totalPendingAlerts = totalPendingAlerts;
        this.criticalSignals = criticalSignals;
        this.recommendedActions = recommendedActions;
        this.confidence = confidence;
    }

    public AlertAnalysisRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(AlertAnalysisRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public long getTotalPendingAlerts() {
        return totalPendingAlerts;
    }

    public void setTotalPendingAlerts(long totalPendingAlerts) {
        this.totalPendingAlerts = totalPendingAlerts;
    }

    public List<String> getCriticalSignals() {
        return criticalSignals;
    }

    public void setCriticalSignals(List<String> criticalSignals) {
        this.criticalSignals = criticalSignals;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
}
