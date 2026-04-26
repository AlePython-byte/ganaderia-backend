package com.ganaderia4.backend.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReuseSafeIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String requestId = "req-safe_001:test.trace";

        request.addHeader(RequestCorrelationFilter.HEADER_NAME, requestId);

        filter.doFilter(request, response, emptyChain());

        assertEquals(requestId, response.getHeader(RequestCorrelationFilter.HEADER_NAME));
        assertEquals(requestId, request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE));
    }

    @Test
    void shouldGenerateUuidWhenIncomingRequestIdContainsUnsafeCharacters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "req invalid");

        filter.doFilter(request, response, emptyChain());

        String generatedRequestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);

        assertNotEquals("req invalid", generatedRequestId);
        assertFalse(generatedRequestId == null || generatedRequestId.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
    }

    @Test
    void shouldGenerateUuidWhenIncomingRequestIdIsTooLong() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String tooLongRequestId = "a".repeat(RequestCorrelationFilter.MAX_REQUEST_ID_LENGTH + 1);

        request.addHeader(RequestCorrelationFilter.HEADER_NAME, tooLongRequestId);

        filter.doFilter(request, response, emptyChain());

        String generatedRequestId = response.getHeader(RequestCorrelationFilter.HEADER_NAME);

        assertNotEquals(tooLongRequestId, generatedRequestId);
        assertFalse(generatedRequestId == null || generatedRequestId.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
    }

    @Test
    void shouldWriteStructuredAccessLog(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cows");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String requestId = "req-access-001";
        String username = "admin@test.com";

        request.addHeader(RequestCorrelationFilter.HEADER_NAME, requestId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(201);
        });

        String logs = output.getOut();

        assertTrue(logs.contains("event=http_request"));
        assertTrue(logs.contains("requestId=" + requestId));
        assertTrue(logs.contains("method=POST"));
        assertTrue(logs.contains("path=/api/cows"));
        assertTrue(logs.contains("status=201"));
        assertTrue(logs.contains("durationMs="));
        assertTrue(logs.contains("user=" + username));
    }

    @Test
    void shouldNotWriteInfoAccessLogForHealthEndpoint(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, emptyChain());

        assertFalse(output.getOut().contains("event=http_request"));
    }

    @Test
    void shouldNotWriteInfoAccessLogForHealthzEndpoint(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/healthz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, emptyChain());

        assertFalse(output.getOut().contains("event=http_request"));
    }

    @Test
    void shouldNotWriteInfoAccessLogForActuatorHealthSubpath(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, emptyChain());

        assertFalse(output.getOut().contains("event=http_request"));
    }

    private FilterChain emptyChain() {
        return (request, response) -> {
        };
    }
}
