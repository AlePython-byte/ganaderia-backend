package com.ganaderia4.backend.exception;

import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.ApiErrorCode;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

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
                request.getRequestURI()
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
                request.getRequestURI()
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
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DeviceUnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeviceUnauthorizedException(DeviceUnauthorizedException ex,
                                                                              HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.DEVICE_UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException ex,
                                                                        HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                "Acceso denegado",
                request.getRequestURI()
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
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Error de validación";

        logHandled(HttpStatus.BAD_REQUEST, "validation", request);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error(
                "event=http_error_unhandled requestId={} category=internal_error status={} method={} path={} queryPresent={}",
                OperationalLogSanitizer.requestId(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                method(request),
                path(request),
                queryPresent(request),
                ex
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR,
                "Ocurrió un error interno del servidor",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status,
                                                                ApiErrorCode code,
                                                                String message,
                                                                String path) {
        return new ResponseEntity<>(buildError(status, code, message, path), status);
    }

    private ErrorResponseDTO buildError(HttpStatus status,
                                        ApiErrorCode code,
                                        String message,
                                        String path) {
        return new ErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                path,
                LocalDateTime.now()
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
                request.getRequestURI()
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
}
