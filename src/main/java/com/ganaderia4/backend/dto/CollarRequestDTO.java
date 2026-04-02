package com.ganaderia4.backend.dto;

import com.ganaderia4.backend.model.CollarStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CollarRequestDTO {

    @NotBlank(message = "El token del collar es obligatorio")
    @Size(max = 50, message = "El token del collar no puede superar 50 caracteres")
    private String token;

    @NotNull(message = "El estado del collar es obligatorio")
    private CollarStatus status;

    @Positive(message = "El id de la vaca debe ser mayor que cero")
    private Long cowId;

    public CollarRequestDTO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CollarStatus getStatus() {
        return status;
    }

    public void setStatus(CollarStatus status) {
        this.status = status;
    }

    public Long getCowId() {
        return cowId;
    }

    public void setCowId(Long cowId) {
        this.cowId = cowId;
    }
}