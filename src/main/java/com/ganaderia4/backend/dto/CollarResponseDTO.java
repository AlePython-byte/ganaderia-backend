package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Respuesta con los datos de un collar")
public class CollarResponseDTO {

    @Schema(description = "Identificador numerico interno", example = "1")
    private Long id;

    @Schema(description = "Identificador publico generado por el backend y usado como X-Device-Token", example = "COLLAR-001")
    private String token;

    @Schema(description = "Estado operativo actual del collar", example = "ACTIVE")
    private String status;

    @Schema(description = "Id de la vaca asociada, si aplica", example = "1")
    private Long cowId;

    @Schema(description = "Token publico de la vaca asociada, si aplica", example = "COW-001")
    private String cowToken;

    @Schema(description = "Nombre de la vaca asociada, si aplica", example = "Luna")
    private String cowName;

    @Schema(description = "Nivel de bateria en porcentaje", example = "78")
    private Integer batteryLevel;

    @Schema(description = "Fecha y hora de ultima senal observada", example = "2026-05-02T10:15:30")
    private LocalDateTime lastSeenAt;

    @Schema(description = "Estado de senal reportado", example = "ONLINE")
    private String signalStatus;

    @Schema(description = "Version de firmware reportada", example = "v1.2.3")
    private String firmwareVersion;

    @Schema(description = "Precision GPS reportada en metros", example = "5.4")
    private Double gpsAccuracy;

    @Schema(description = "Indica si el collar esta habilitado operativamente", example = "true")
    private Boolean enabled;

    @Schema(description = "Notas operativas", example = "Collar reasignado recientemente")
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
