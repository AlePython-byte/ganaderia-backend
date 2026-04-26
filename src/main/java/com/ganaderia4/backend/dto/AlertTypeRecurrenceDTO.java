package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class AlertTypeRecurrenceDTO {

    private String type;
    private long totalAlerts;
    private long pendingAlerts;
    private long resolvedAlerts;
    private long discardedAlerts;
    private LocalDateTime lastAlertAt;

    public AlertTypeRecurrenceDTO(String type,
                                  long totalAlerts,
                                  long pendingAlerts,
                                  long resolvedAlerts,
                                  long discardedAlerts,
                                  LocalDateTime lastAlertAt) {
        this.type = type;
        this.totalAlerts = totalAlerts;
        this.pendingAlerts = pendingAlerts;
        this.resolvedAlerts = resolvedAlerts;
        this.discardedAlerts = discardedAlerts;
        this.lastAlertAt = lastAlertAt;
    }

    public String getType() {
        return type;
    }

    public long getTotalAlerts() {
        return totalAlerts;
    }

    public long getPendingAlerts() {
        return pendingAlerts;
    }

    public long getResolvedAlerts() {
        return resolvedAlerts;
    }

    public long getDiscardedAlerts() {
        return discardedAlerts;
    }

    public LocalDateTime getLastAlertAt() {
        return lastAlertAt;
    }
}
