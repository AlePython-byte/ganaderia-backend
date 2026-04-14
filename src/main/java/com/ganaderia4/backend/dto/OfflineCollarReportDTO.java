package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class OfflineCollarReportDTO {

    private Long collarId;
    private String collarToken;
    private String collarStatus;
    private Boolean enabled;
    private Integer batteryLevel;
    private String signalStatus;
    private LocalDateTime lastSeenAt;
    private Long cowId;
    private String cowToken;
    private String cowName;

    public OfflineCollarReportDTO(Long collarId,
                                  String collarToken,
                                  String collarStatus,
                                  Boolean enabled,
                                  Integer batteryLevel,
                                  String signalStatus,
                                  LocalDateTime lastSeenAt,
                                  Long cowId,
                                  String cowToken,
                                  String cowName) {
        this.collarId = collarId;
        this.collarToken = collarToken;
        this.collarStatus = collarStatus;
        this.enabled = enabled;
        this.batteryLevel = batteryLevel;
        this.signalStatus = signalStatus;
        this.lastSeenAt = lastSeenAt;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
    }

    public Long getCollarId() {
        return collarId;
    }

    public String getCollarToken() {
        return collarToken;
    }

    public String getCollarStatus() {
        return collarStatus;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public String getSignalStatus() {
        return signalStatus;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
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
}