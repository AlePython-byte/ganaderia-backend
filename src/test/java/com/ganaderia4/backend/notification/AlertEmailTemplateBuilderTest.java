package com.ganaderia4.backend.notification;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertEmailTemplateBuilderTest {

    private final AlertEmailTemplateBuilder builder = new AlertEmailTemplateBuilder();

    @Test
    void shouldBuildSubjectForCollarOffline() {
        EmailNotificationContent content = builder.build(message("COLLAR_OFFLINE", "HIGH", "Collar sin senal"));

        assertEquals("[Ganaderia 4.0] HIGH - Collar offline", content.subject());
    }

    @Test
    void shouldBuildSubjectForExitGeofence() {
        EmailNotificationContent content = builder.build(message("EXIT_GEOFENCE", "HIGH", "Animal fuera"));

        assertEquals("[Ganaderia 4.0] HIGH - Salida de geocerca", content.subject());
    }

    @Test
    void shouldBuildSubjectForLowBattery() {
        EmailNotificationContent content = builder.build(message("LOW_BATTERY", "MEDIUM", "Bateria baja"));

        assertEquals("[Ganaderia 4.0] MEDIUM - Bateria baja", content.subject());
    }

    @Test
    void shouldBuildFallbackSubjectForUnknownAlertType() {
        EmailNotificationContent content = builder.build(message("SOMETHING_ELSE", "LOW", "Caso generico"));

        assertEquals("[Ganaderia 4.0] Alerta operativa", content.subject());
    }

    @Test
    void shouldEscapeDangerousHtmlAndMaskKnownTokens() {
        NotificationMessage notificationMessage = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta <script>alert(1)</script>")
                .message("El collar COLLAR-ABC-001 reporta <b>riesgo</b> para VACA-001")
                .severity("HIGH")
                .createdAt(LocalDateTime.of(2026, 5, 2, 8, 15))
                .metadata("alertType", "COLLAR_OFFLINE")
                .metadata("cowToken", "VACA-001")
                .build();

        EmailNotificationContent content = builder.build(notificationMessage);

        assertTrue(content.htmlBody().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(content.htmlBody().contains("&lt;b&gt;riesgo&lt;/b&gt;"));
        assertFalse(content.htmlBody().contains("<script>"));
        assertFalse(content.htmlBody().contains("VACA-001"));
        assertFalse(content.htmlBody().contains("COLLAR-ABC-001"));
        assertTrue(content.htmlBody().contains("****-001"));
        assertTrue(content.textBody().contains("Recomendacion: Revisar conectividad, bateria y estado fisico del collar."));
    }

    @Test
    void shouldIncludeEssentialInformationInTextBody() {
        EmailNotificationContent content = builder.build(message("EXIT_GEOFENCE", "HIGH", "Animal fuera de la geocerca"));

        assertTrue(content.textBody().contains("Tipo: Salida de geocerca"));
        assertTrue(content.textBody().contains("Severidad: HIGH"));
        assertTrue(content.textBody().contains("Mensaje: Animal fuera de la geocerca"));
        assertTrue(content.textBody().contains("Recomendacion: Verificar ubicacion fisica del animal y configuracion de geocerca."));
        assertTrue(content.textBody().contains("Fecha: 2026-05-02 08:15:00"));
    }

    private NotificationMessage message(String alertType, String severity, String message) {
        return NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message(message)
                .severity(severity)
                .createdAt(LocalDateTime.of(2026, 5, 2, 8, 15))
                .metadata("alertType", alertType)
                .metadata("cowToken", "VACA-001")
                .build();
    }
}
