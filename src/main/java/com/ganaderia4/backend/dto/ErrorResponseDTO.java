package com.ganaderia4.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Lista opcional de errores por campo. Solo se informa en validaciones de DTO.")
    private List<FieldErrorDTO> fieldErrors;

    public ErrorResponseDTO() {
    }

    public ErrorResponseDTO(int status,
                            String error,
                            String code,
                            String message,
                            String path,
                            String requestId,
                            LocalDateTime timestamp) {
        this(status, error, code, message, path, requestId, timestamp, null);
    }

    public ErrorResponseDTO(int status,
                            String error,
                            String code,
                            String message,
                            String path,
                            String requestId,
                            LocalDateTime timestamp,
                            List<FieldErrorDTO> fieldErrors) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.fieldErrors = fieldErrors;
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

    public List<FieldErrorDTO> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorDTO> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    @Schema(description = "Error de validacion asociado a un campo especifico")
    public static class FieldErrorDTO {

        @Schema(description = "Nombre del campo del DTO que fallo validacion", example = "email")
        private String field;

        @Schema(description = "Mensaje de validacion asociado al campo", example = "El correo es obligatorio")
        private String message;

        public FieldErrorDTO() {
        }

        public FieldErrorDTO(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
