package com.ganaderia4.backend.dto;

import java.time.LocalDate;

public class AlertTrendPointDTO {

    private LocalDate date;
    private long totalAlerts;
    private long pendingAlerts;
    private long resolvedAlerts;
    private long discardedAlerts;

    public AlertTrendPointDTO(LocalDate date,
                              long totalAlerts,
                              long pendingAlerts,
                              long resolvedAlerts,
                              long discardedAlerts) {
        this.date = date;
        this.totalAlerts = totalAlerts;
        this.pendingAlerts = pendingAlerts;
        this.resolvedAlerts = resolvedAlerts;
        this.discardedAlerts = discardedAlerts;
    }

    public LocalDate getDate() {
        return date;
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
}
