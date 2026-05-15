package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionProperties;
import com.ganaderia4.backend.notification.NotificationMessage;
import com.ganaderia4.backend.observability.DomainMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationThrottleServiceTest {

    @Test
    void shouldAllowRecipientsAndStoreOnlyHashedKeys() {
        AbuseProtectionService abuseProtectionService = mock(AbuseProtectionService.class);
        when(abuseProtectionService.recordAttempt(any(), any(), any()))
                .thenReturn(AbuseProtectionDecision.allow());

        EmailNotificationThrottleService service = new EmailNotificationThrottleService(
                new AbuseProtectionProperties(),
                abuseProtectionService,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        List<String> allowed = service.filterAllowedRecipients(
                sampleMessage(),
                List.of("Admin@Test.com")
        );

        assertEquals(List.of("Admin@Test.com"), allowed);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(abuseProtectionService, times(3)).recordAttempt(any(), keyCaptor.capture(), any());
        for (String key : keyCaptor.getAllValues()) {
            assertEquals(64, key.length());
            assertTrue(key.matches("[0-9a-f]+"));
            assertFalse(key.contains("Admin@Test.com"));
            assertFalse(key.contains("admin@test.com"));
        }
    }

    @Test
    void shouldFilterRecipientWhenRecipientEventTypeLimitIsExceeded() {
        AbuseProtectionService abuseProtectionService = mock(AbuseProtectionService.class);
        when(abuseProtectionService.recordAttempt(eq("EMAIL_CHANNEL"), any(), any()))
                .thenReturn(AbuseProtectionDecision.allow());
        when(abuseProtectionService.recordAttempt(eq("EMAIL_RECIPIENT"), any(), any()))
                .thenReturn(AbuseProtectionDecision.allow());
        when(abuseProtectionService.recordAttempt(eq("EMAIL_RECIPIENT_EVENT_TYPE"), any(), any()))
                .thenReturn(AbuseProtectionDecision.blocked(120));

        EmailNotificationThrottleService service = new EmailNotificationThrottleService(
                new AbuseProtectionProperties(),
                abuseProtectionService,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        List<String> allowed = service.filterAllowedRecipients(
                sampleMessage(),
                List.of("admin@test.com")
        );

        assertTrue(allowed.isEmpty());
    }

    @Test
    void shouldNotRecordAttemptsWhenEmailThrottleIsDisabled() {
        AbuseProtectionProperties properties = new AbuseProtectionProperties();
        properties.getEmail().setEnabled(false);
        AbuseProtectionService abuseProtectionService = mock(AbuseProtectionService.class);

        EmailNotificationThrottleService service = new EmailNotificationThrottleService(
                properties,
                abuseProtectionService,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        List<String> allowed = service.filterAllowedRecipients(
                sampleMessage(),
                List.of("admin@test.com")
        );

        assertEquals(List.of("admin@test.com"), allowed);
        verify(abuseProtectionService, times(0)).recordAttempt(any(), any(), any());
    }

    private NotificationMessage sampleMessage() {
        return NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .createdAt(LocalDateTime.of(2026, 5, 1, 9, 30))
                .metadata("alertType", "COLLAR_OFFLINE")
                .build();
    }
}
