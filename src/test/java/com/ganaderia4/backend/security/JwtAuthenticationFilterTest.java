package com.ganaderia4.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class JwtAuthenticationFilterTest {

    @Test
    void shouldLogInvalidJwtWithoutExposingToken(CapturedOutput output) throws Exception {
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "invalid.jwt.token";

        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new JwtException("invalid token"));

        filter.doFilter(request, response, filterChain);

        String logs = output.getOut();

        assertTrue(logs.contains("event=security_auth_failed"));
        assertTrue(logs.contains("reason=invalid_jwt"));
        assertTrue(logs.contains("path=/api/auth/me"));
        assertTrue(logs.contains("user=UNKNOWN"));
        assertTrue(logs.contains("status=401"));
        assertFalse(logs.contains(token));
        assertTrue(Boolean.TRUE.equals(request.getAttribute(
                JwtAuthenticationFilter.SECURITY_FAILURE_LOGGED_ATTRIBUTE)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldLogExpiredJwtWithoutExposingToken(CapturedOutput output) throws Exception {
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reports/alerts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "expired.jwt.token";

        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new ExpiredJwtException(null, null, "expired token"));

        filter.doFilter(request, response, filterChain);

        String logs = output.getOut();

        assertTrue(logs.contains("event=security_auth_failed"));
        assertTrue(logs.contains("reason=expired_jwt"));
        assertTrue(logs.contains("path=/api/reports/alerts"));
        assertTrue(logs.contains("user=UNKNOWN"));
        assertTrue(logs.contains("status=401"));
        assertFalse(logs.contains(token));
        assertTrue(Boolean.TRUE.equals(request.getAttribute(
                JwtAuthenticationFilter.SECURITY_FAILURE_LOGGED_ATTRIBUTE)));
        verify(filterChain).doFilter(request, response);
    }
}
