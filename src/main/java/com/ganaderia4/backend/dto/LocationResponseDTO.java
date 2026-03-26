package com.ganaderia4.backend.dto;

import java.time.LocalDateTime;

public class LocationResponseDTO {

    private Long id;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private Long cowId;
    private String cowIdentifier;
    private String cowName;
    private String collarIdentifier;

    public LocationResponseDTO() {
    }

    public LocationResponseDTO(Long id, Double latitude, Double longitude, LocalDateTime timestamp,
                               Long cowId, String cowIdentifier, String cowName, String collarIdentifier) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.cowId = cowId;
        this.cowIdentifier = cowIdentifier;
        this.cowName = cowName;
        this.collarIdentifier = collarIdentifier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
    }

    public String getCowIdentifier() {
        return cowIdentifier;
    }

    public void setCowIdentifier(String cowIdentifier) {
        this.cowIdentifier = cowIdentifier;
    }

    public String getCowName() {
        return cowName;
    }

    public void setCowName(String cowName) {
        this.cowName = cowName;
    }

    public String getCollarIdentifier() {
        return collarIdentifier;
    }

    public void setCollarIdentifier(String collarIdentifier) {
        this.collarIdentifier = collarIdentifier;
    }
}