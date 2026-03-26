package com.ganaderia4.backend.dto;

public class CollarResponseDTO {

    private Long id;
    private String identifier;
    private String status;
    private Long cowId;
    private String cowIdentifier;
    private String cowName;

    public CollarResponseDTO() {
    }

    public CollarResponseDTO(Long id, String identifier, String status, Long cowId, String cowIdentifier, String cowName) {
        this.id = id;
        this.identifier = identifier;
        this.status = status;
        this.cowId = cowId;
        this.cowIdentifier = cowIdentifier;
        this.cowName = cowName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}