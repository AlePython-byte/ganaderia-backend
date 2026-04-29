package com.ganaderia4.backend.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
        bindingResult.addError(new FieldError("cowRequest", "name", "name is required"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = handler.handleValidationException(exception, request);

        assertEquals(400, response.getStatusCode().value());
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
        String logs = output.getOut() + output.getErr();
        assertTrue(logs.contains("event=http_error_unhandled"));
        assertTrue(logs.contains("requestId=req-unhandled-001"));
        assertTrue(logs.contains("category=internal_error"));
        assertTrue(logs.contains("status=500"));
        assertTrue(logs.contains("method=PATCH"));
        assertTrue(logs.contains("path=/api/collars/7"));
        assertTrue(logs.contains("queryPresent=true"));
        assertTrue(logs.contains("java.lang.IllegalStateException"));
        assertFalse(logs.contains("secret=hidden"));
    }
}
