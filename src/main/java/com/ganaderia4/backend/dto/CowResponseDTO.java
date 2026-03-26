package com.ganaderia4.backend.dto;

public class CowResponseDTO {

    private Long id;
    private String identifier;
    private String internalCode;
    private String name;
    private String status;
    private String observations;

    public CowResponseDTO() {
    }

    public CowResponseDTO(Long id, String identifier, String internalCode, String name, String status, String observations) {
        this.id = id;
        this.identifier = identifier;
        this.internalCode = internalCode;
        this.name = name;
        this.status = status;
        this.observations = observations;
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

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}