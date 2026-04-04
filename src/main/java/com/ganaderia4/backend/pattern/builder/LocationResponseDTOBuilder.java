package com.ganaderia4.backend.pattern.builder;

import com.ganaderia4.backend.dto.LocationResponseDTO;

import java.time.LocalDateTime;

public class LocationResponseDTOBuilder {

    private Long id;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private Long cowId;
    private String cowToken;
    private String cowName;
    private String collarToken;

    public LocationResponseDTOBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public LocationResponseDTOBuilder latitude(Double latitude) {
        this.latitude = latitude;
        return this;
    }

    public LocationResponseDTOBuilder longitude(Double longitude) {
        this.longitude = longitude;
        return this;
    }

    public LocationResponseDTOBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LocationResponseDTOBuilder cowId(Long cowId) {
        this.cowId = cowId;
        return this;
    }

    public LocationResponseDTOBuilder cowToken(String cowToken) {
        this.cowToken = cowToken;
        return this;
    }

    public LocationResponseDTOBuilder cowName(String cowName) {
        this.cowName = cowName;
        return this;
    }

    public LocationResponseDTOBuilder collarToken(String collarToken) {
        this.collarToken = collarToken;
        return this;
    }

    public LocationResponseDTO build() {
        LocationResponseDTO dto = new LocationResponseDTO();
        dto.setId(id);
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setTimestamp(timestamp);
        dto.setCowId(cowId);
        dto.setCowToken(cowToken);
        dto.setCowName(cowName);
        dto.setCollarToken(collarToken);
        return dto;
    }
}