package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AlertUpdateRequestDTO {

    @NotNull(message = "El estado de la alerta es obligatorio")
    private AlertStatus status;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observations;

    public AlertUpdateRequestDTO() {
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}