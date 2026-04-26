package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.CowStatus;

import java.time.LocalDateTime;

public class CowIncidentAggregateView {

    private final Long cowId;
    private final String cowToken;
    private final String cowName;
    private final CowStatus cowStatus;
    private final long totalIncidents;
    private final long pendingIncidents;
    private final long resolvedIncidents;
    private final long discardedIncidents;
    private final LocalDateTime firstIncidentAt;
    private final LocalDateTime lastIncidentAt;

    public CowIncidentAggregateView(Long cowId,
                                    String cowToken,
                                    String cowName,
                                    CowStatus cowStatus,
                                    long totalIncidents,
                                    long pendingIncidents,
                                    long resolvedIncidents,
                                    long discardedIncidents,
                                    LocalDateTime firstIncidentAt,
                                    LocalDateTime lastIncidentAt) {
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
        this.cowStatus = cowStatus;
        this.totalIncidents = totalIncidents;
        this.pendingIncidents = pendingIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.discardedIncidents = discardedIncidents;
        this.firstIncidentAt = firstIncidentAt;
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

    public CowStatus getCowStatus() {
        return cowStatus;
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
}
