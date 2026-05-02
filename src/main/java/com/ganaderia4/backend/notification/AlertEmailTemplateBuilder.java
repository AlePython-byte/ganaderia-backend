package com.ganaderia4.backend.notification;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AlertEmailTemplateBuilder {

    private static final String DEFAULT_SUBJECT = "[Ganaderia 4.0] Alerta operativa";
    private static final String FOOTER = "Este correo fue generado automaticamente por Ganaderia 4.0.";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b([A-Z]{3,}(?:-[A-Z0-9]{2,})+)\\b");

    public EmailNotificationContent build(NotificationMessage notificationMessage) {
        if (notificationMessage == null) {
            return new EmailNotificationContent(DEFAULT_SUBJECT, "", "");
        }

        String alertType = resolveAlertType(notificationMessage.getMetadata());
        String subject = buildSubject(notificationMessage.getSeverity(), alertType);
        String recommendation = recommendationFor(alertType);
        String alertLabel = labelFor(alertType);
        String severityLabel = safePlain(notificationMessage.getSeverity());
        String title = maskSensitiveValues(safePlain(notificationMessage.getTitle()), notificationMessage.getMetadata());
        String message = maskSensitiveValues(safePlain(notificationMessage.getMessage()), notificationMessage.getMetadata());
        String createdAt = formatDate(notificationMessage.getCreatedAt());

        String textBody = buildTextBody(alertLabel, severityLabel, title, message, recommendation, createdAt);
        String htmlBody = buildHtmlBody(alertLabel, severityLabel, title, message, recommendation, createdAt);

        return new EmailNotificationContent(subject, textBody, htmlBody);
    }

    private String buildSubject(String severity, String alertType) {
        String label = labelFor(alertType);
        String normalizedSeverity = safePlain(severity);
        if ("Alerta operativa".equals(label)) {
            return DEFAULT_SUBJECT;
        }

        if ("-".equals(normalizedSeverity)) {
            return "[Ganaderia 4.0] " + label;
        }

        return "[Ganaderia 4.0] " + normalizedSeverity + " - " + label;
    }

    private String buildTextBody(String alertLabel,
                                 String severity,
                                 String title,
                                 String message,
                                 String recommendation,
                                 String createdAt) {
        return new StringBuilder()
                .append("Ganaderia 4.0 - Alerta operativa").append(System.lineSeparator()).append(System.lineSeparator())
                .append("Titulo: ").append(title).append(System.lineSeparator())
                .append("Tipo: ").append(alertLabel).append(System.lineSeparator())
                .append("Severidad: ").append(severity).append(System.lineSeparator())
                .append("Mensaje: ").append(message).append(System.lineSeparator())
                .append("Recomendacion: ").append(recommendation).append(System.lineSeparator())
                .append("Fecha: ").append(createdAt).append(System.lineSeparator()).append(System.lineSeparator())
                .append(FOOTER)
                .toString();
    }

    private String buildHtmlBody(String alertLabel,
                                 String severity,
                                 String title,
                                 String message,
                                 String recommendation,
                                 String createdAt) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#f4f6f8;font-family:Arial,sans-serif;color:#1f2937;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #dbe3ea;border-radius:8px;padding:24px;">
                    <h1 style="margin:0 0 16px;font-size:22px;color:#111827;">%s</h1>
                    <p style="margin:0 0 16px;color:#4b5563;">Se detecto una alerta operativa que requiere revision.</p>
                    <table style="width:100%%;border-collapse:collapse;margin-bottom:20px;">
                      <tr><td style="padding:8px 0;font-weight:bold;width:160px;">Tipo de alerta</td><td style="padding:8px 0;">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:bold;">Severidad</td><td style="padding:8px 0;">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:bold;">Mensaje</td><td style="padding:8px 0;">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:bold;">Fecha</td><td style="padding:8px 0;">%s</td></tr>
                      <tr><td style="padding:8px 0;font-weight:bold;">Recomendacion</td><td style="padding:8px 0;">%s</td></tr>
                    </table>
                    <p style="margin:0;color:#6b7280;font-size:13px;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(alertLabel),
                escapeHtml(severity),
                escapeHtml(message),
                escapeHtml(createdAt),
                escapeHtml(recommendation),
                escapeHtml(FOOTER)
        );
    }

    private String resolveAlertType(Map<String, String> metadata) {
        if (metadata == null) {
            return "";
        }

        String alertType = metadata.get("alertType");
        return alertType == null ? "" : alertType.trim().toUpperCase(Locale.ROOT);
    }

    private String labelFor(String alertType) {
        return switch (alertType) {
            case "COLLAR_OFFLINE" -> "Collar offline";
            case "EXIT_GEOFENCE" -> "Salida de geocerca";
            case "LOW_BATTERY" -> "Bateria baja";
            default -> "Alerta operativa";
        };
    }

    private String recommendationFor(String alertType) {
        return switch (alertType) {
            case "COLLAR_OFFLINE" -> "Revisar conectividad, bateria y estado fisico del collar.";
            case "EXIT_GEOFENCE" -> "Verificar ubicacion fisica del animal y configuracion de geocerca.";
            case "LOW_BATTERY" -> "Programar recarga o reemplazo de bateria del collar.";
            default -> "Revisar la alerta en el sistema.";
        };
    }

    private String formatDate(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "-";
        }

        return createdAt.format(DATE_TIME_FORMATTER);
    }

    private String safePlain(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private String maskSensitiveValues(String value, Map<String, String> metadata) {
        String sanitized = value;
        if (metadata != null) {
            sanitized = replaceIfPresent(sanitized, metadata.get("cowToken"));
            sanitized = replaceIfPresent(sanitized, metadata.get("collarToken"));
        }

        return maskIdentifiers(sanitized);
    }

    private String replaceIfPresent(String source, String rawValue) {
        if (source == null || rawValue == null || rawValue.isBlank()) {
            return source;
        }

        return source.replace(rawValue, maskValue(rawValue));
    }

    private String maskIdentifiers(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = IDENTIFIER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(maskValue(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String maskValue(String value) {
        String collapsed = safePlain(value);
        if (collapsed.length() <= 4) {
            return "****";
        }

        return "****" + collapsed.substring(collapsed.length() - 4);
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
