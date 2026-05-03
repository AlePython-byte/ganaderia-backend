package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticacion con JWT y datos basicos del usuario")
public class LoginResponseDTO {

    @Schema(description = "Identificador numerico del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre del usuario autenticado", example = "Administrador")
    private String name;

    @Schema(description = "Correo del usuario autenticado", example = "admin@ganaderia.com")
    private String email;

    @Schema(description = "Rol operativo del usuario", example = "ADMINISTRADOR")
    private String role;

    @Schema(description = "JWT Bearer para endpoints protegidos", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Tipo de token devuelto", example = "Bearer")
    private String tokenType;

    @Schema(description = "Duracion del token en segundos", example = "86400")
    private Long expiresIn;

    @Schema(description = "Mensaje operativo de autenticacion", example = "Inicio de sesion exitoso")
    private String message;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(Long id, String name, String email, String role, String token, String tokenType, Long expiresIn, String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
