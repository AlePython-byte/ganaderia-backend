package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta generica de solicitud de recuperacion de contrasena")
public class ForgotPasswordResponseDTO {

    @Schema(
            description = "Mensaje generico de aceptacion que no revela si el correo existe",
            example = "Si el correo existe, recibirás instrucciones para recuperar tu contraseña."
    )
    private String message;

    public ForgotPasswordResponseDTO() {
    }

    public ForgotPasswordResponseDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
