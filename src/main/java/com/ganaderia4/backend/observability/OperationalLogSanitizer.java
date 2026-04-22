package com.ganaderia4.backend.observability;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class OperationalLogSanitizer {

    private static final Pattern UNSAFE_LOG_CHARS = Pattern.compile("[\\r\\n\\t ]+");

    private OperationalLogSanitizer() {
    }

    public static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return UNSAFE_LOG_CHARS.matcher(value.trim()).replaceAll("_");
    }

    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "UNKNOWN";
        }

        String sanitized = safe(token);
        if (sanitized.length() <= 4) {
            return "****";
        }

        return "****" + sanitized.substring(sanitized.length() - 4);
    }

    public static String destination(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getHost().isBlank()) {
            return "UNKNOWN";
        }

        StringBuilder destination = new StringBuilder()
                .append(safe(uri.getScheme()))
                .append("://")
                .append(safe(uri.getHost()));

        if (uri.getPort() >= 0) {
            destination.append(":").append(uri.getPort());
        }

        return destination.toString();
    }

    public static String metadataKeys(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "-";
        }

        return metadata.keySet().stream()
                .map(OperationalLogSanitizer::safe)
                .sorted()
                .collect(Collectors.joining(","));
    }
}
