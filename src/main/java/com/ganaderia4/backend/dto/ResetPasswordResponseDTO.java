package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta generica de ejecucion de cambio de contrasena")
public class ResetPasswordResponseDTO {

    @Schema(description = "Mensaje operativo del cambio de contrasena", example = "La contraseña fue actualizada correctamente.")
    private String message;

    public ResetPasswordResponseDTO() {
    }

    public ResetPasswordResponseDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
