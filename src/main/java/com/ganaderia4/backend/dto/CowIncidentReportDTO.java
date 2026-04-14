package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class CowIncidentReportDTO {

    private Long cowId;
    private String cowToken;
    private String cowName;
    private long totalIncidents;
    private long pendingIncidents;
    private long resolvedIncidents;
    private long discardedIncidents;
    private LocalDateTime lastIncidentAt;

    public CowIncidentReportDTO(Long cowId,
                                String cowToken,
                                String cowName,
                                long totalIncidents,
                                long pendingIncidents,
                                long resolvedIncidents,
                                long discardedIncidents,
                                LocalDateTime lastIncidentAt) {
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
        this.totalIncidents = totalIncidents;
        this.pendingIncidents = pendingIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.discardedIncidents = discardedIncidents;
        this.lastIncidentAt = lastIncidentAt;
    }

    public Long getCowId() {
        return cowId;
    }

    public String getCowToken() {
        return cowToken;
    }

    public String getCowName() {
        return cowName;
    }

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public long getPendingIncidents() {
        return pendingIncidents;
    }

    public long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public long getDiscardedIncidents() {
        return discardedIncidents;
    }

    public LocalDateTime getLastIncidentAt() {
        return lastIncidentAt;
    }
}