package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class GeofenceRequestDTO {

    @NotBlank(message = "El nombre de la geocerca es obligatorio")
    @Size(max = 100, message = "El nombre de la geocerca no puede superar 100 caracteres")
    private String name;

    @NotNull(message = "La latitud central es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima permitida es -90")
    @DecimalMax(value = "90.0", message = "La latitud máxima permitida es 90")
    private Double centerLatitude;

    @NotNull(message = "La longitud central es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima permitida es -180")
    @DecimalMax(value = "180.0", message = "La longitud máxima permitida es 180")
    private Double centerLongitude;

    @NotNull(message = "El radio es obligatorio")
    @Positive(message = "El radio debe ser mayor que cero")
    private Double radiusMeters;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean active;

    @Positive(message = "El id de la vaca debe ser mayor que cero")
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