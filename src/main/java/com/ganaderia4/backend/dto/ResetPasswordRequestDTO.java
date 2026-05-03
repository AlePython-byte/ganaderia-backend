package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud publica para ejecutar el cambio de contrasena con token de recuperacion")
public class ResetPasswordRequestDTO {

    @Schema(description = "Token de recuperacion recibido por el usuario", example = "eyJ1c2VyIjoiLi4uIn0")
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @Schema(description = "Nueva contrasena del usuario", example = "123ganadero456*")
    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contrasena debe tener entre 8 y 100 caracteres")
    private String newPassword;

    @Schema(description = "Confirmacion de la nueva contrasena", example = "123ganadero456*")
    @NotBlank(message = "La confirmacion de contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La confirmacion de contrasena debe tener entre 8 y 100 caracteres")
    private String confirmPassword;

    public ResetPasswordRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
