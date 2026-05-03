package com.ganaderia4.backend.notification;

import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class NotificationOutboxServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-03T20:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldEnqueuePendingMessageWithDefaultAttemptsAndClock(CapturedOutput output) {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxService service = new NotificationOutboxService(repository, FIXED_CLOCK);

        when(repository.save(any(NotificationOutboxMessage.class))).thenAnswer(invocation -> {
            NotificationOutboxMessage message = invocation.getArgument(0);
            message.setId(15L);
            return message;
        });

        NotificationOutboxMessage response = service.enqueue(
                NotificationChannel.EMAIL,
                "PASSWORD_RESET_REQUESTED",
                "alerts@ganaderia.test",
                "Asunto",
                "{\"ok\":true}"
        );

        assertNotNull(response.getId());
        assertEquals(NotificationOutboxStatus.PENDING, response.getStatus());
        assertEquals(0, response.getAttempts());
        assertEquals(NotificationOutboxMessage.DEFAULT_MAX_ATTEMPTS, response.getMaxAttempts());
        assertEquals(FIXED_INSTANT, response.getNextAttemptAt());

        ArgumentCaptor<NotificationOutboxMessage> captor = ArgumentCaptor.forClass(NotificationOutboxMessage.class);
        verify(repository).save(captor.capture());
        NotificationOutboxMessage saved = captor.getValue();
        assertEquals(NotificationChannel.EMAIL, saved.getChannel());
        assertEquals("PASSWORD_RESET_REQUESTED", saved.getEventType());
        assertEquals(FIXED_INSTANT, saved.getCreatedAt());
        assertEquals(FIXED_INSTANT, saved.getUpdatedAt());
        assertEquals(FIXED_INSTANT, saved.getNextAttemptAt());

        assertTrue((output.getOut() + output.getErr()).contains("event=notification_outbox_enqueued"));
    }

    @Test
    void shouldRejectBlankPayload() {
        NotificationOutboxService service = new NotificationOutboxService(mock(NotificationOutboxRepository.class), FIXED_CLOCK);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(NotificationChannel.WEBHOOK, "EVENT", null, null, "   ")
        );

        assertEquals("Notification payload is required", ex.getMessage());
    }

    @Test
    void shouldRejectMissingChannel() {
        NotificationOutboxService service = new NotificationOutboxService(mock(NotificationOutboxRepository.class), FIXED_CLOCK);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(null, "EVENT", null, null, "{\"ok\":true}")
        );

        assertEquals("Notification channel is required", ex.getMessage());
    }

    @Test
    void shouldRejectBlankEventType() {
        NotificationOutboxService service = new NotificationOutboxService(mock(NotificationOutboxRepository.class), FIXED_CLOCK);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(NotificationChannel.LOG, "   ", null, null, "{\"ok\":true}")
        );

        assertEquals("Notification event type is required", ex.getMessage());
    }
}
