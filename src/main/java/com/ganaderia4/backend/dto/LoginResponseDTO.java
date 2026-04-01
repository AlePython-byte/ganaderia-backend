package com.ganaderia4.backend.dto;

public class LoginResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String token;
    private String tokenType;
    private Long expiresIn;
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