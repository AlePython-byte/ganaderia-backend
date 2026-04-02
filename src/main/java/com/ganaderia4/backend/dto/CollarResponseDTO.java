package com.ganaderia4.backend.dto;

public class CollarResponseDTO {

    private Long id;
    private String token;
    private String status;
    private Long cowId;
    private String cowToken;
    private String cowName;

    public CollarResponseDTO() {
    }

    public CollarResponseDTO(Long id, String token, String status, Long cowId, String cowToken, String cowName) {
        this.id = id;
        this.token = token;
        this.status = status;
        this.cowId = cowId;
        this.cowToken = cowToken;
        this.cowName = cowName;
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

    public String getCowToken() {
        return cowToken;
    }

    public void setCowToken(String cowToken) {
        this.cowToken = cowToken;
    }

    public String getCowName() {
        return cowName;
    }

    public void setCowName(String cowName) {
        this.cowName = cowName;
    }
}