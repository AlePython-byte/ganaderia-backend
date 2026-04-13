package com.ganaderia4.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CollarRequestDTO {

    @NotBlank(message = "El token del collar es obligatorio")
    @Size(max = 50, message = "El token del collar no puede superar 50 caracteres")
    private String token;

    @NotNull(message = "El estado del collar es obligatorio")
    private CollarStatus status;

    @Positive(message = "El id de la vaca debe ser mayor que cero")
    private Long cowId;

    @Min(value = 0, message = "La batería no puede ser menor a 0")
    @Max(value = 100, message = "La batería no puede ser mayor a 100")
    private Integer batteryLevel;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeenAt;

    private DeviceSignalStatus signalStatus;

    @Size(max = 100, message = "La versión de firmware no puede superar 100 caracteres")
    private String firmwareVersion;

    @DecimalMin(value = "0.0", inclusive = true, message = "La precisión GPS no puede ser negativa")
    private Double gpsAccuracy;

    private Boolean enabled;

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notes;

    public CollarRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CollarStatus getStatus() {
        return status;
    }

    public void setStatus(CollarStatus status) {
        this.status = status;
    }

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
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

    public DeviceSignalStatus getSignalStatus() {
        return signalStatus;
    }

    public void setSignalStatus(DeviceSignalStatus signalStatus) {
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