package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class CollarResponseDTO {

    private Long id;
    private String token;
    private String status;
    private Long cowId;
    private String cowToken;
    private String cowName;

    private Integer batteryLevel;
    private LocalDateTime lastSeenAt;
    private String signalStatus;
    private String firmwareVersion;
    private Double gpsAccuracy;
    private Boolean enabled;
    private String notes;

    public CollarResponseDTO() {
    }

    public CollarResponseDTO(Long id,
                             String token,
                             String status,
                             Long cowId,
                             String cowToken,
                             String cowName,
                             Integer batteryLevel,
                             LocalDateTime lastSeenAt,
                             String signalStatus,
                             String firmwareVersion,
                             Double gpsAccuracy,
                             Boolean enabled,
                             String notes) {
        this.id = id;
        this.token = token;
        this.status = status;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
        this.batteryLevel = batteryLevel;
        this.lastSeenAt = lastSeenAt;
        this.signalStatus = signalStatus;
        this.firmwareVersion = firmwareVersion;
        this.gpsAccuracy = gpsAccuracy;
        this.enabled = enabled;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCowName() {
        return cowName;
    }

    public void setCowName(String cowName) {
        this.cowName = cowName;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getSignalStatus() {
        return signalStatus;
    }

    public void setSignalStatus(String signalStatus) {
        this.signalStatus = signalStatus;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public Double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Double gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}