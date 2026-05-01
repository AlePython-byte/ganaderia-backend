package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class AlertPriorityRecommendationDTO {

    private Long alertId;
    private String alertType;
    private String alertStatus;
    private Integer priorityScore;
    private String priorityLabel;
    private String reason;
    private String recommendedAction;
    private Long cowId;
    private String cowToken;
    private LocalDateTime createdAt;

    public AlertPriorityRecommendationDTO() {
    }

    public AlertPriorityRecommendationDTO(Long alertId,
                                          String alertType,
                                          String alertStatus,
                                          Integer priorityScore,
                                          String priorityLabel,
                                          String reason,
                                          String recommendedAction,
                                          Long cowId,
                                          String cowToken,
                                          LocalDateTime createdAt) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.alertStatus = alertStatus;
        this.priorityScore = priorityScore;
        this.priorityLabel = priorityLabel;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.createdAt = createdAt;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(String alertStatus) {
        this.alertStatus = alertStatus;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
    }

    public String getCowToken() {
        return cowToken;
    }

    public void setCowToken(String cowToken) {
        this.cowToken = cowToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
