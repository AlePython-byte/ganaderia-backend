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
    private LocalDateTime firstIncidentAt;
    private LocalDateTime lastIncidentAt;
    private String cowStatus;
    private String lastIncidentType;

    public CowIncidentReportDTO(Long cowId,
                                String cowToken,
                                String cowName,
                                long totalIncidents,
                                long pendingIncidents,
                                long resolvedIncidents,
                                long discardedIncidents,
                                LocalDateTime lastIncidentAt) {
        this(
                cowId,
                cowToken,
                cowName,
                totalIncidents,
                pendingIncidents,
                resolvedIncidents,
                discardedIncidents,
                null,
                lastIncidentAt,
                null,
                null
        );
    }

    public CowIncidentReportDTO(Long cowId,
                                String cowToken,
                                String cowName,
                                long totalIncidents,
                                long pendingIncidents,
                                long resolvedIncidents,
                                long discardedIncidents,
                                LocalDateTime firstIncidentAt,
                                LocalDateTime lastIncidentAt,
                                String cowStatus,
                                String lastIncidentType) {
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
        this.totalIncidents = totalIncidents;
        this.pendingIncidents = pendingIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.discardedIncidents = discardedIncidents;
        this.firstIncidentAt = firstIncidentAt;
        this.lastIncidentAt = lastIncidentAt;
        this.cowStatus = cowStatus;
        this.lastIncidentType = lastIncidentType;
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

    public LocalDateTime getFirstIncidentAt() {
        return firstIncidentAt;
    }

    public LocalDateTime getLastIncidentAt() {
        return lastIncidentAt;
    }

    public String getCowStatus() {
        return cowStatus;
    }

    public String getLastIncidentType() {
        return lastIncidentType;
    }
}
