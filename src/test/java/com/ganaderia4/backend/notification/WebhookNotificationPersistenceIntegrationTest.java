package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.WebhookNotificationDeliveryRepository;
import com.ganaderia4.backend.service.AlertService;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        "app.notifications.webhook.enabled=true",
        "app.notifications.webhook.url=https://example.com/notifications"
})
class WebhookNotificationPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private WebhookNotificationDeliveryRepository webhookNotificationDeliveryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        webhookNotificationDeliveryRepository.deleteAll();
        alertRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
    }

    @Test
    void shouldPersistWebhookDeliveryWhenCreatingOfflineAlert() throws Exception {
        Cow cow = createCow("VACA-WEBHOOK-001", "Luna");
        Collar collar = createCollar(
                "COLLAR-WEBHOOK-001",
                cow,
                LocalDateTime.now().minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true
        );

        var alert = alertService.createCollarOfflineAlert(collar);

        assertNotNull(alert);

        List<WebhookNotificationDelivery> deliveries = webhookNotificationDeliveryRepository.findAll();
        assertEquals(1, deliveries.size());

        WebhookNotificationDelivery delivery = deliveries.get(0);
        assertNotNull(delivery.getId());
        assertNotNull(delivery.getNotificationId());
        assertEquals("CRITICAL_ALERT_CREATED", delivery.getEventType());
        assertEquals("https://example.com/notifications", delivery.getDestination());
        assertEquals(WebhookNotificationDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals(0, delivery.getAttempts());
        assertNull(delivery.getLastError());
        assertNotNull(delivery.getNextAttemptAt());
        assertNotNull(delivery.getCreatedAt());
        assertNotNull(delivery.getUpdatedAt());

        JsonNode payload = objectMapper.readTree(delivery.getPayload());
        assertEquals("CRITICAL_ALERT_CREATED", payload.get("eventType").asText());
        assertEquals("HIGH", payload.get("severity").asText());
        assertEquals("COLLAR_OFFLINE", payload.get("metadata").get("alertType").asText());
        assertEquals("VACA-WEBHOOK-001", payload.get("metadata").get("cowToken").asText());
        assertEquals(String.valueOf(alert.getId()), payload.get("metadata").get("alertId").asText());
    }

    private Cow createCow(String token, String name) {
        Cow cow = new Cow();
        cow.setToken(token);
        cow.setInternalCode("INT-" + token);
        cow.setName(name);
        cow.setStatus(CowStatus.DENTRO);
        return cowRepository.save(cow);
    }

    private Collar createCollar(String token,
                                Cow cow,
                                LocalDateTime lastSeenAt,
                                DeviceSignalStatus signalStatus,
                                boolean enabled) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setBatteryLevel(80);
        collar.setLastSeenAt(lastSeenAt);
        collar.setSignalStatus(signalStatus);
        collar.setEnabled(enabled);
        return collarRepository.save(collar);
    }
}
