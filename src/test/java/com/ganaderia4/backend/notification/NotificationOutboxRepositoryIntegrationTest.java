package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationOutboxRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @BeforeEach
    void setUp() {
        notificationOutboxRepository.deleteAll();
    }

    @Test
    void shouldPersistAndRecoverOutboxMessage() {
        NotificationOutboxMessage message = new NotificationOutboxMessage();
        message.setChannel(NotificationChannel.EMAIL);
        message.setStatus(NotificationOutboxStatus.PENDING);
        message.setEventType("ALERT_CREATED");
        message.setRecipient("alerts@ganaderia.test");
        message.setSubject("Asunto");
        message.setPayload("{\"message\":\"hola\"}");
        message.setAttempts(0);
        message.setMaxAttempts(3);
        message.setNextAttemptAt(Instant.parse("2026-05-03T20:30:00Z"));
        message.setCreatedAt(Instant.parse("2026-05-03T20:30:00Z"));
        message.setUpdatedAt(Instant.parse("2026-05-03T20:30:00Z"));

        NotificationOutboxMessage saved = notificationOutboxRepository.save(message);

        assertNotNull(saved.getId());
        Optional<NotificationOutboxMessage> reloaded = notificationOutboxRepository.findById(saved.getId());

        assertTrue(reloaded.isPresent());
        assertEquals(NotificationChannel.EMAIL, reloaded.get().getChannel());
        assertEquals(NotificationOutboxStatus.PENDING, reloaded.get().getStatus());
        assertEquals("ALERT_CREATED", reloaded.get().getEventType());
        assertEquals("{\"message\":\"hola\"}", reloaded.get().getPayload());
        assertEquals(1L, notificationOutboxRepository.countByStatus(NotificationOutboxStatus.PENDING));
    }
}
