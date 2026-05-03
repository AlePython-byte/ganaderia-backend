package com.ganaderia4.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Datos de entrada para crear o actualizar un collar")
public class CollarRequestDTO {

    @Schema(
            description = "Identificador publico del collar. En creacion es opcional y el backend genera un valor como COLLAR-001; si se envia, se ignora por compatibilidad. En actualizacion, si no se informa, se conserva el valor actual.",
            example = "COLLAR-001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 50, message = "El token del collar no puede superar 50 caracteres")
    private String token;

    @Schema(description = "Estado operativo del collar", example = "ACTIVE")
    @NotNull(message = "El estado del collar es obligatorio")
    private CollarStatus status;

    @Schema(description = "Id de la vaca asociada. Es opcional en creacion y actualizacion.", example = "1")
    @Positive(message = "El id de la vaca debe ser mayor que cero")
    private Long cowId;

    @Schema(description = "Nivel de bateria en porcentaje", example = "78")
    @Min(value = 0, message = "La bateria no puede ser menor a 0")
    @Max(value = 100, message = "La bateria no puede ser mayor a 100")
    private Integer batteryLevel;

    @Schema(description = "Fecha y hora de ultima senal observada", example = "2026-05-02T10:15:30")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeenAt;

    @Schema(description = "Estado de senal del dispositivo", example = "ONLINE")
    private DeviceSignalStatus signalStatus;

    @Schema(description = "Version de firmware reportada por el collar", example = "v1.2.3")
    @Size(max = 100, message = "La version de firmware no puede superar 100 caracteres")
    private String firmwareVersion;

    @Schema(description = "Precision GPS reportada en metros", example = "5.4")
    @DecimalMin(value = "0.0", inclusive = true, message = "La precision GPS no puede ser negativa")
    private Double gpsAccuracy;

    @Schema(description = "Indica si el collar queda habilitado operativamente", example = "true")
    private Boolean enabled;

    @Schema(description = "Notas operativas opcionales", example = "Collar reasignado recientemente")
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
