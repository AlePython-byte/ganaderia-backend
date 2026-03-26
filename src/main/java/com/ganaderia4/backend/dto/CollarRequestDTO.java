package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CollarRequestDTO {

    @NotBlank(message = "El identificador del collar es obligatorio")
    private String identifier;

    @NotBlank(message = "El estado del collar es obligatorio")
    private String status;

    private Long cowId;

    public CollarRequestDTO() {
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
}