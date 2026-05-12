package com.ganaderia4.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Contrato estandar de error REST del backend")
public class ErrorResponseDTO {

    @Schema(description = "Codigo HTTP devuelto por el backend", example = "400")
    private int status;

    @Schema(description = "Razon HTTP asociada al estado", example = "Bad Request")
    private String error;

    @Schema(description = "Codigo de error interno estable para integraciones", example = "BAD_REQUEST")
    private String code;

    @Schema(description = "Mensaje legible del error", example = "El timestamp reportado no puede estar demasiado en el futuro")
    private String message;

    @Schema(description = "Ruta del request que produjo el error", example = "/api/device/locations")
    private String path;

    @Schema(description = "Identificador de correlacion del request asociado al error", example = "req-demo-001")
    private String requestId;

    @Schema(description = "Fecha y hora del error con el contrato temporal actual", example = "2026-04-28T20:52:08")
    private LocalDateTime timestamp;

    public ErrorResponseDTO() {
    }

    public ErrorResponseDTO(int status,
                            String error,
                            String code,
                            String message,
                            String path,
                            String requestId,
                            LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
