package com.ganaderia4.backend.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldLogHandledValidationErrorWithoutStacktrace(CapturedOutput output) {
        MDC.put("requestId", "req-validation-001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cows");
        request.setQueryString("token=secret");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "cowRequest");
        bindingResult.addError(new FieldError(
                "cowRequest",
                "name",
                "secret-cow-name",
                false,
                null,
                null,
                "name is required"
        ));
        bindingResult.addError(new FieldError(
                "cowRequest",
                "status",
                "INVALID_STATUS",
                false,
                null,
                null,
                "status is required"
        ));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = handler.handleValidationException(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("name is required", response.getBody().getMessage());
        assertEquals("req-validation-001", response.getBody().getRequestId());
        assertEquals(2, response.getBody().getFieldErrors().size());
        assertEquals("name", response.getBody().getFieldErrors().get(0).getField());
        assertEquals("name is required", response.getBody().getFieldErrors().get(0).getMessage());
        assertEquals("status", response.getBody().getFieldErrors().get(1).getField());
        assertEquals("status is required", response.getBody().getFieldErrors().get(1).getMessage());
        assertFalse(writeJson(response.getBody()).contains("secret-cow-name"));
        assertFalse(writeJson(response.getBody()).contains("INVALID_STATUS"));
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("event=http_error_handled"));
        assertTrue(logs.contains("requestId=req-validation-001"));
        assertTrue(logs.contains("category=validation"));
        assertTrue(logs.contains("status=400"));
        assertTrue(logs.contains("method=POST"));
        assertTrue(logs.contains("path=/api/cows"));
        assertTrue(logs.contains("queryPresent=true"));
        assertFalse(logs.contains("token=secret"));
        assertFalse(logs.contains("MethodArgumentNotValidException"));
    }

    @Test
    void shouldLogHandledNotFoundErrorWithoutStacktrace(CapturedOutput output) {
        MDC.put("requestId", "req-not-found-001");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cows/99");

        var response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Vaca no encontrada"),
                request
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("req-not-found-001", response.getBody().getRequestId());
        assertNull(response.getBody().getFieldErrors());
        assertFalse(writeJson(response.getBody()).contains("fieldErrors"));
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("event=http_error_handled"));
        assertTrue(logs.contains("requestId=req-not-found-001"));
        assertTrue(logs.contains("category=not_found"));
        assertTrue(logs.contains("status=404"));
        assertTrue(logs.contains("method=GET"));
        assertTrue(logs.contains("path=/api/cows/99"));
        assertTrue(logs.contains("queryPresent=false"));
        assertFalse(logs.contains("ResourceNotFoundException"));
    }

    @Test
    void shouldLogUnhandledErrorWithStacktrace(CapturedOutput output) {
        MDC.put("requestId", "req-unhandled-001");
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/collars/7");
        request.setQueryString("secret=hidden");

        var response = handler.handleGenericException(new IllegalStateException("database unavailable"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("req-unhandled-001", response.getBody().getRequestId());
        assertEquals("Ocurrió un error interno del servidor", response.getBody().getMessage());
        assertNull(response.getBody().getFieldErrors());
        String responseJson = writeJson(response.getBody());
        assertFalse(responseJson.contains("fieldErrors"));
        assertFalse(responseJson.contains("database unavailable"));
        assertFalse(responseJson.contains("IllegalStateException"));
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("event=http_error_unhandled"));
        assertTrue(logs.contains("requestId=req-unhandled-001"));
        assertTrue(logs.contains("category=internal_error"));
        assertTrue(logs.contains("status=500"));
        assertTrue(logs.contains("method=PATCH"));
        assertTrue(logs.contains("path=/api/collars/7"));
        assertTrue(logs.contains("queryPresent=true"));
        assertTrue(logs.contains("exceptionClass=java.lang.IllegalStateException"));
        assertTrue(logs.contains("exceptionMessage=database_unavailable"));
        assertTrue(logs.contains("java.lang.IllegalStateException"));
        assertFalse(logs.contains("secret=hidden"));
    }

    @Test
    void shouldReturnBadRequestForMalformedJsonWithRequestId() {
        MDC.put("requestId", "req-json-001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        var response = handler.handleHttpMessageNotReadableException(
                new HttpMessageNotReadableException("invalid json", mock(HttpInputMessage.class)),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("El cuerpo de la solicitud no es válido o no tiene formato JSON correcto.",
                response.getBody().getMessage());
        assertEquals("req-json-001", response.getBody().getRequestId());
        assertNull(response.getBody().getFieldErrors());
        assertFalse(writeJson(response.getBody()).contains("fieldErrors"));
    }

    @Test
    void shouldReturnBadRequestForArgumentTypeMismatchWithRequestId() {
        MDC.put("requestId", "req-argument-001");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/alerts");

        var response = handler.handleMethodArgumentTypeMismatchException(
                new MethodArgumentTypeMismatchException(
                        "invalid",
                        Long.class,
                        "id",
                        mock(MethodParameter.class),
                        new IllegalArgumentException("invalid")
                ),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("Uno o más parámetros tienen un formato inválido.", response.getBody().getMessage());
        assertEquals("req-argument-001", response.getBody().getRequestId());
    }

    @Test
    void shouldReturnBadRequestForMissingRequestParameterWithRequestId() {
        MDC.put("requestId", "req-parameter-001");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reports/alerts");

        var response = handler.handleMissingServletRequestParameterException(
                new MissingServletRequestParameterException("from", "String"),
                request
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("Falta un parámetro requerido en la solicitud.", response.getBody().getMessage());
        assertEquals("req-parameter-001", response.getBody().getRequestId());
    }

    @Test
    void shouldKeepMethodNotAllowedAs405WithRequestId() {
        MDC.put("requestId", "req-method-001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/health");

        var response = handler.handleMethodNotSupportedException(
                new HttpRequestMethodNotSupportedException("POST"),
                request
        );

        assertEquals(405, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("Método HTTP no permitido para este endpoint", response.getBody().getMessage());
        assertEquals("req-method-001", response.getBody().getRequestId());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
