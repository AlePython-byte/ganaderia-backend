package com.ganaderia4.backend.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String REQUEST_ATTRIBUTE = "requestId";
    public static final int MAX_REQUEST_ID_LENGTH = 64;

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]+$");
    private static final String ANONYMOUS_USER = "ANONYMOUS";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();

        MDC.put(MDC_KEY, requestId);
        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
        response.setHeader(HEADER_NAME, requestId);

        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException ex) {
            failure = ex;
            throw ex;
        } catch (ServletException ex) {
            failure = ex;
            throw ex;
        } catch (RuntimeException ex) {
            failure = ex;
            throw ex;
        } catch (Error ex) {
            failure = ex;
            throw ex;
        } finally {
            logRequest(request, response, requestId, startedAt, failure);
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_NAME);

        if (incoming == null || incoming.isBlank()) {
            return UUID.randomUUID().toString();
        }

        String candidate = incoming.trim();
        if (candidate.length() > MAX_REQUEST_ID_LENGTH
                || !SAFE_REQUEST_ID_PATTERN.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }

        return candidate;
    }

    private void logRequest(HttpServletRequest request,
                            HttpServletResponse response,
                            String requestId,
                            long startedAt,
                            Throwable failure) {
        long durationMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        String method = safe(request.getMethod());
        String path = safe(request.getRequestURI());
        int status = resolveStatus(response, failure);
        String user = resolveAuthenticatedUser();

        String message = "event=http_request requestId={} method={} path={} status={} durationMs={} user={}";
        if (isHealthEndpoint(path)) {
            log.debug(message, requestId, method, path, status, durationMs, user);
            return;
        }

        log.info(message, requestId, method, path, status, durationMs, user);
    }

    private int resolveStatus(HttpServletResponse response, Throwable failure) {
        int status = response.getStatus();
        if (failure != null && status < HttpServletResponse.SC_BAD_REQUEST) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        return status;
    }

    private String resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ANONYMOUS_USER;
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return ANONYMOUS_USER;
        }

        return sanitizeLogValue(name);
    }

    private boolean isHealthEndpoint(String path) {
        return "/healthz".equals(path)
                || "/actuator/health".equals(path)
                || path.startsWith("/actuator/health/");
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return sanitizeLogValue(value);
    }

    private String sanitizeLogValue(String value) {
        return value.replaceAll("[\\r\\n\\t ]+", "_");
    }
}
