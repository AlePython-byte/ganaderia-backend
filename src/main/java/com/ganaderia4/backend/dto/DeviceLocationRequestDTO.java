package com.ganaderia4.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Payload de telemetria enviado por un collar autenticado mediante HMAC")
public class DeviceLocationRequestDTO {

    @Schema(description = "Latitud reportada por el dispositivo", example = "1.214")
    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud no puede ser menor a -90")
    @DecimalMax(value = "90.0", message = "La latitud no puede ser mayor a 90")
    private Double latitude;

    @Schema(description = "Longitud reportada por el dispositivo", example = "-77.281")
    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud no puede ser menor a -180")
    @DecimalMax(value = "180.0", message = "La longitud no puede ser mayor a 180")
    private Double longitude;

    @Schema(description = "Timestamp UTC reportado por el dispositivo", example = "2026-05-02T10:15:30")
    @NotNull(message = "El timestamp es obligatorio")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Nivel de bateria en porcentaje", example = "78")
    @Min(value = 0, message = "El batteryLevel no puede ser menor a 0")
    @Max(value = 100, message = "El batteryLevel no puede ser mayor a 100")
    private Integer batteryLevel;

    @Schema(description = "Precision GPS en metros", example = "5.4")
    @DecimalMin(value = "0.0", message = "El gpsAccuracy no puede ser menor a 0")
    private Double gpsAccuracy;

    public DeviceLocationRequestDTO() {
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Double gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }
}
