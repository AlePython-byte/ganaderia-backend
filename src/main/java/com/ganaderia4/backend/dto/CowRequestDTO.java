package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.CowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CowRequestDTO {

    @NotBlank(message = "El token es obligatorio")
    @Size(max = 50, message = "El token no puede superar 50 caracteres")
    private String token;

    @Size(max = 50, message = "El código interno no puede superar 50 caracteres")
    private String internalCode;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    @NotNull(message = "El estado es obligatorio")
    private CowStatus status;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observations;

    public CowRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public CowStatus getStatus() {
        return status;
    }

    public void setStatus(CowStatus status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}