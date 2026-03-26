package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CowRequestDTO {

    @NotBlank(message = "El identificador es obligatorio")
    private String identifier;

    private String internalCode;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El estado es obligatorio")
    private String status;

    private String observations;

    public CowRequestDTO() {
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