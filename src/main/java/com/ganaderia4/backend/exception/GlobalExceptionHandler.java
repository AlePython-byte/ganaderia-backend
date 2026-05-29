package com.ganaderia4.backend.exception;

import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.ApiErrorCode;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.observability.RequestCorrelationFilter;
import com.ganaderia4.backend.service.ClaudeAiClient;
import com.ganaderia4.backend.service.DeepSeekAiClient;
import com.ganaderia4.backend.service.InvalidPasswordResetTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex,
                                                                            HttpServletRequest request) {
        logHandled(HttpStatus.NOT_FOUND, "not_found", request);
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequestException(BadRequestException ex,
                                                                      HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "bad_request", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflictException(ConflictException ex,
                                                                    HttpServletRequest request) {
        logHandled(HttpStatus.CONFLICT, "conflict", request);
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONFLICT,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(DeviceUnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeviceUnauthorizedException(DeviceUnauthorizedException ex,
                                                                              HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.DEVICE_UNAUTHORIZED,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex,
                                                                        HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                "Acceso denegado",
                request
        );
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponseDTO> handleTooManyRequestsException(TooManyRequestsException ex,
                                                                           HttpServletRequest request) {
        logHandled(HttpStatus.TOO_MANY_REQUESTS, "rate_limited", request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(buildError(
                        HttpStatus.TOO_MANY_REQUESTS,
                        ApiErrorCode.TOO_MANY_REQUESTS,
                        ex.getMessage(),
                        request
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Error de validación";

        logHandled(HttpStatus.BAD_REQUEST, "validation", request);
        ErrorResponseDTO error = buildError(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                message,
                request
        );
        error.setFieldErrors(buildFieldErrors(ex));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "malformed_request_body", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido o no tiene formato JSON correcto.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "argument_type_mismatch", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                "Uno o más parámetros tienen un formato inválido.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "missing_request_parameter", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                "Falta un parámetro requerido en la solicitud.",
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "constraint_violation", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "La solicitud contiene valores inválidos.",
                request
        );
    }

    @ExceptionHandler({
            ClaudeAiClient.ClaudeAiClientException.class,
            DeepSeekAiClient.DeepSeekAiClientException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleAiClientException(
            RuntimeException ex,
            HttpServletRequest request) {
        log.warn(
                "event=http_error_handled requestId={} category=ai_provider_unavailable status={} method={} path={} queryPresent={} reason={}",
                OperationalLogSanitizer.requestId(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                method(request),
                path(request),
                queryPresent(request),
                OperationalLogSanitizer.safe(ex.getMessage())
        );
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ApiErrorCode.AI_PROVIDER_UNAVAILABLE,
                "El servicio de análisis IA no está disponible en este momento. Intente nuevamente más tarde.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error(
                "event=http_error_unhandled requestId={} category=internal_error status={} method={} path={} queryPresent={} exceptionClass={} exceptionMessage={}",
                OperationalLogSanitizer.requestId(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                method(request),
                path(request),
                queryPresent(request),
                exceptionClass(ex),
                exceptionMessage(ex),
                ex
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Ocurrió un error interno del servidor",
                request
        );
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", request);
        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                ApiErrorCode.BAD_REQUEST,
                "Método HTTP no permitido para este endpoint",
                request
        );
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPasswordResetTokenException(
            InvalidPasswordResetTokenException ex,
            HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "bad_request", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BAD_REQUEST,
                "El token de recuperación es inválido o expiró.",
                request
        );
    }

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status,
                                                                ApiErrorCode code,
                                                                String message,
                                                                HttpServletRequest request) {
        return new ResponseEntity<>(buildError(status, code, message, request), status);
    }

    private ErrorResponseDTO buildError(HttpStatus status,
                                        ApiErrorCode code,
                                        String message,
                                        HttpServletRequest request) {
        return new ErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                path(request),
                requestId(request),
                LocalDateTime.now()
        );
    }

    private List<ErrorResponseDTO.FieldErrorDTO> buildFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();
    }

    private ErrorResponseDTO.FieldErrorDTO mapFieldError(FieldError fieldError) {
        return new ErrorResponseDTO.FieldErrorDTO(
                fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valor invalido"
        );
    }

    private void logHandled(HttpStatus status, String category, HttpServletRequest request) {
        log.warn(
                "event=http_error_handled requestId={} category={} status={} method={} path={} queryPresent={}",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.safe(category),
                status.value(),
                method(request),
                path(request),
                queryPresent(request)
        );
    }

    private String method(HttpServletRequest request) {
        return request == null ? "-" : OperationalLogSanitizer.safe(request.getMethod());
    }

    private String path(HttpServletRequest request) {
        return request == null ? "-" : OperationalLogSanitizer.safe(request.getRequestURI());
    }

    private boolean queryPresent(HttpServletRequest request) {
        return request != null && request.getQueryString() != null && !request.getQueryString().isBlank();
    }

    private String exceptionClass(Exception ex) {
        return ex == null ? "-" : OperationalLogSanitizer.safe(ex.getClass().getName());
    }

    private String exceptionMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "-";
        }

        return OperationalLogSanitizer.safe(ex.getMessage());
    }

    private String requestId(HttpServletRequest request) {
        if (request != null) {
            Object attribute = request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE);
            if (attribute instanceof String requestId && !requestId.isBlank()) {
                return requestId;
            }

            String header = request.getHeader(RequestCorrelationFilter.HEADER_NAME);
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }

        String requestId = MDC.get(RequestCorrelationFilter.MDC_KEY);
        return requestId == null || requestId.isBlank() ? "-" : requestId;
    }
}
