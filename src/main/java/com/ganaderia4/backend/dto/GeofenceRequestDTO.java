package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GeofenceRequestDTO {

    @NotBlank(message = "El nombre de la geocerca es obligatorio")
    private String name;

    @NotNull(message = "La latitud central es obligatoria")
    private Double centerLatitude;

    @NotNull(message = "La longitud central es obligatoria")
    private Double centerLongitude;

    @NotNull(message = "El radio es obligatorio")
    private Double radiusMeters;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean active;

    private Long cowId;

    public GeofenceRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public Double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
    }
}