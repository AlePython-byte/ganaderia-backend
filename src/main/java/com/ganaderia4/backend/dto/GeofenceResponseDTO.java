package com.ganaderia4.backend.dto;

public class GeofenceResponseDTO {

    private Long id;
    private String name;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMeters;
    private Boolean active;
    private Long cowId;
    private String cowToken;
    private String cowName;

    public GeofenceResponseDTO() {
    }

    public GeofenceResponseDTO(Long id, String name, Double centerLatitude, Double centerLongitude,
                               Double radiusMeters, Boolean active, Long cowId,
                               String cowToken, String cowName) {
        this.id = id;
        this.name = name;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radiusMeters = radiusMeters;
        this.active = active;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}