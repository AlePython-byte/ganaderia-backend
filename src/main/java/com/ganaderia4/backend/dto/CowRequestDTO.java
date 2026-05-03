package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.CowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de entrada para crear o actualizar una vaca")
public class CowRequestDTO {

    @Schema(
            description = "Identificador publico de la vaca. En creacion es opcional y el backend genera un valor como COW-001; si se envia, se ignora por compatibilidad. En actualizacion, si no se informa, se conserva el valor actual.",
            example = "COW-001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 50, message = "El token no puede superar 50 caracteres")
    private String token;

    @Schema(description = "Codigo interno opcional de la vaca", example = "INT-025")
    @Size(max = 50, message = "El codigo interno no puede superar 50 caracteres")
    private String internalCode;

    @Schema(description = "Nombre visible de la vaca", example = "Luna")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    @Schema(description = "Estado operativo de la vaca", example = "ACTIVA")
    @NotNull(message = "El estado es obligatorio")
    private CowStatus status;

    @Schema(description = "Observaciones operativas opcionales", example = "Vaca en monitoreo diario")
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
