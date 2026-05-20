package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con los datos de una vaca")
public class CowResponseDTO {

    @Schema(description = "Identificador numerico interno", example = "1")
    private Long id;

    @Schema(description = "Identificador publico generado por el backend", example = "COW-001")
    private String token;

    @Schema(description = "Codigo interno secuencial generado por el backend", example = "INT-001")
    private String internalCode;

    @Schema(description = "Nombre visible de la vaca", example = "Luna")
    private String name;

    @Schema(description = "Estado de ubicacion actual", example = "SIN_UBICACION")
    private String status;

    @Schema(description = "Indica si la vaca esta activa operativamente", example = "true")
    private Boolean active;

    @Schema(description = "Observaciones operativas", example = "Vaca en monitoreo diario")
    private String observations;

    public CowResponseDTO() {
    }

    public CowResponseDTO(Long id, String token, String internalCode, String name, String status, String observations) {
        this(id, token, internalCode, name, status, true, observations);
    }

    public CowResponseDTO(Long id, String token, String internalCode, String name, String status, Boolean active, String observations) {
        this.id = id;
        this.token = token;
        this.internalCode = internalCode;
        this.name = name;
        this.status = status;
        this.active = active;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
