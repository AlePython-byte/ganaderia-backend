package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class LocationRequestDTO {

    @NotBlank(message = "El identificador del collar es obligatorio")
    private String collarIdentifier;

    @NotNull(message = "La latitud es obligatoria")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    private Double longitude;

    @NotNull(message = "La fecha y hora son obligatorias")
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