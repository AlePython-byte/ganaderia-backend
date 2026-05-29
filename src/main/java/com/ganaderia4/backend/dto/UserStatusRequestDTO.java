package com.ganaderia4.backend.dto;

import jakarta.validation.constraints.NotNull;

public class UserStatusRequestDTO {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean active;

    public UserStatusRequestDTO() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
