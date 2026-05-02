package com.ganaderia4.backend.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class LoggingNotificationServiceTest {

    @Test
    void shouldLogNotificationSummaryWithoutMetadataValues(CapturedOutput output) {
        LoggingNotificationService service = new LoggingNotificationService();
        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("El collar COLLAR-OFF-001 no reporta")
                .severity("HIGH")
                .metadata("cowToken", "VACA-OFF-001")
                .metadata("collarToken", "COLLAR-OFF-001")
                .metadata("alertType", "COLLAR_OFFLINE")
                .build();

        NotificationSendResult result = service.send(message);

        String logs = output.getOut();

        assertEquals(NotificationSendResult.SENT, result);
        assertTrue(logs.contains("event=notification_logged"));
        assertTrue(logs.contains("requestId=-"));
        assertTrue(logs.contains("channel=LOG"));
        assertTrue(logs.contains("notificationType=CRITICAL_ALERT_CREATED"));
        assertTrue(logs.contains("severity=HIGH"));
        assertTrue(logs.contains("metadataKeys=alertType,collarToken,cowToken"));
        assertTrue(logs.contains("metadataSize=3"));
        assertFalse(logs.contains("VACA-OFF-001"));
        assertFalse(logs.contains("COLLAR-OFF-001"));
        assertFalse(logs.contains("El collar"));
    }
}
