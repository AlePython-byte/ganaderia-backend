package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ganaderia4.backend.config.WebhookNotificationProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.WebhookNotificationDeliveryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class WebhookNotificationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPersistPendingWebhookDelivery(CapturedOutput output) throws Exception {
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        when(repository.save(any(WebhookNotificationDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        WebhookNotificationService service = new WebhookNotificationService(
                webhookProperties(),
                objectMapper,
                repository,
                new DomainMetricsService(meterRegistry)
        );
        MDC.put("requestId", "req-webhook-001");

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .createdAt(LocalDateTime.of(2026, 4, 22, 10, 30))
                .metadata("alertType", "COLLAR_OFFLINE")
                .metadata("cowToken", "VACA-001")
                .build();

        service.send(message);

        ArgumentCaptor<WebhookNotificationDelivery> captor = ArgumentCaptor.forClass(WebhookNotificationDelivery.class);
        verify(repository).save(captor.capture());

        WebhookNotificationDelivery delivery = captor.getValue();
        assertNotNull(delivery.getNotificationId());
        assertEquals("CRITICAL_ALERT_CREATED", delivery.getEventType());
        assertEquals("https://example.com/notifications?access_token=secret-token", delivery.getDestination());
        assertEquals(WebhookNotificationDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(0, delivery.getAttempts());
        assertNull(delivery.getLastError());
        assertNotNull(delivery.getNextAttemptAt());
        assertNotNull(delivery.getCreatedAt());
        assertNotNull(delivery.getUpdatedAt());

        JsonNode payload = objectMapper.readTree(delivery.getPayload());
        assertEquals("CRITICAL_ALERT_CREATED", payload.get("eventType").asText());
        assertEquals("Nueva alerta critica", payload.get("title").asText());
        assertEquals("HIGH", payload.get("severity").asText());
        assertEquals("COLLAR_OFFLINE", payload.get("metadata").get("alertType").asText());

        assertEquals(
                1.0,
                meterRegistry.counter(
                        "ganaderia.notifications.queued",
                        "channel", "WEBHOOK",
                        "eventType", "CRITICAL_ALERT_CREATED"
                ).count()
        );

        String logs = output.getOut();
        assertTrue(logs.contains("event=webhook_delivery_enqueued"));
        assertTrue(logs.contains("requestId=req-webhook-001"));
        assertTrue(logs.contains("host=https://example.com"));
        assertTrue(logs.contains("status=PENDING"));
        assertTrue(logs.contains("notificationType=CRITICAL_ALERT_CREATED"));
        assertFalse(logs.contains(delivery.getPayload()));
        assertFalse(logs.contains("/notifications"));
        assertFalse(logs.contains("access_token=secret-token"));
    }

    @Test
    void shouldThrowWhenPayloadCannotBeSerialized() throws Exception {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("payload invalid") { });

        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookNotificationService service = new WebhookNotificationService(
                webhookProperties(),
                failingObjectMapper,
                repository,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        NotificationMessage message = NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .build();

        NotificationPersistenceException ex =
                assertThrows(NotificationPersistenceException.class, () -> service.send(message));
        assertEquals("Webhook notification payload could not be serialized", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldIgnoreNullMessage() {
        WebhookNotificationDeliveryRepository repository = mock(WebhookNotificationDeliveryRepository.class);
        WebhookNotificationService service = new WebhookNotificationService(
                webhookProperties(),
                objectMapper,
                repository,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        service.send(null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectEnabledWebhookWithoutUrl() {
        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setUrl("");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new WebhookNotificationService(
                        properties,
                        objectMapper,
                        mock(WebhookNotificationDeliveryRepository.class),
                        new DomainMetricsService(new SimpleMeterRegistry())
                )
        );

        assertEquals("Webhook notification URL must be configured when webhook channel is enabled", ex.getMessage());
    }

    private WebhookNotificationProperties webhookProperties() {
        WebhookNotificationProperties properties = new WebhookNotificationProperties();
        properties.setEnabled(true);
        properties.setUrl("https://example.com/notifications?access_token=secret-token");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setSecret("webhook-secret");
        return properties;
    }
}
