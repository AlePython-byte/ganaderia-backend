package com.ganaderia4.backend.pattern.adapter.location;

import java.time.LocalDateTime;

public class LocationCommand {

    private String collarToken;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;

    public LocationCommand() {
    }

    public LocationCommand(String collarToken, Double latitude, Double longitude, LocalDateTime timestamp) {
        this.collarToken = collarToken;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getCollarToken() {
        return collarToken;
    }

    public void setCollarToken(String collarToken) {
        this.collarToken = collarToken;
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