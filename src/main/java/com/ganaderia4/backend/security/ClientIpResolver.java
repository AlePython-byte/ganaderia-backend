package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String UNKNOWN_IP = "UNKNOWN";

    private final AbuseProtectionProperties properties;

    public ClientIpResolver(AbuseProtectionProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        if (properties.getClientIp().isTrustForwardedHeaders()) {
            String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
            if (!forwardedFor.isBlank()) {
                return normalize(forwardedFor);
            }

            String realIp = normalize(request.getHeader("X-Real-IP"));
            if (!UNKNOWN_IP.equals(realIp)) {
                return realIp;
            }
        }

        return normalize(request.getRemoteAddr());
    }

    private String firstForwardedFor(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return "";
        }

        return headerValue.split(",")[0].trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_IP;
        }

        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.contains("]")) {
            normalized = normalized.substring(1, normalized.indexOf(']'));
        } else if (isIpv4WithPort(normalized)) {
            normalized = normalized.substring(0, normalized.lastIndexOf(':'));
        }

        normalized = normalized.replaceAll("[\\r\\n\\t ]+", "_");
        return normalized.isBlank() ? UNKNOWN_IP : normalized;
    }

    private boolean isIpv4WithPort(String value) {
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex < 0 || value.indexOf(':') != colonIndex) {
            return false;
        }

        String host = value.substring(0, colonIndex);
        String port = value.substring(colonIndex + 1);
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}") && port.matches("\\d+");
    }
}
