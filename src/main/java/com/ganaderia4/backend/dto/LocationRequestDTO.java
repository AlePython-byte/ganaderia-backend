package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class LocationRequestDTO {

    @NotBlank(message = "El identificador del collar es obligatorio")
    @Size(max = 50, message = "El identificador del collar no puede superar 50 caracteres")
    private String collarIdentifier;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima permitida es -90")
    @DecimalMax(value = "90.0", message = "La latitud máxima permitida es 90")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima permitida es -180")
    @DecimalMax(value = "180.0", message = "La longitud máxima permitida es 180")
    private Double longitude;

    @NotNull(message = "La fecha y hora son obligatorias")
    @PastOrPresent(message = "La fecha y hora no pueden estar en el futuro")
    private LocalDateTime timestamp;

    public LocationRequestDTO() {
    }

    public String getCollarIdentifier() {
        return collarIdentifier;
    }

    public void setCollarIdentifier(String collarIdentifier) {
        this.collarIdentifier = collarIdentifier;
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
}