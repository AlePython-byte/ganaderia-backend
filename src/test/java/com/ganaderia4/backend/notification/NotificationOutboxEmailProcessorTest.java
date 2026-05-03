package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.NotificationOutboxEmailProcessorProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class NotificationOutboxEmailProcessorTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-03T21:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void shouldDoNothingWhenProcessorIsDisabled() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(false, 20, Duration.ofMinutes(1)),
                emailProperties(),
                List.of(providerClient()),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        verifyNoInteractions(repository);
    }

    @Test
    void shouldSendDuePendingMessageAndMarkItSent() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        EmailProviderClient providerClient = providerClient();
        when(repository.findEligibleForProcessing(eq(NotificationChannel.EMAIL), any(), eq(FIXED_NOW), any()))
                .thenReturn(List.of(message(NotificationOutboxStatus.PENDING, FIXED_NOW.minusSeconds(5), 0, 3, validPayload("admin@test.com"))));

        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(true, 20, Duration.ofMinutes(1)),
                emailProperties(),
                List.of(providerClient),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        ArgumentCaptor<EmailNotificationRequest> requestCaptor = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(providerClient).send(requestCaptor.capture());
        assertEquals(List.of("admin@test.com"), requestCaptor.getValue().to());

        ArgumentCaptor<NotificationOutboxMessage> messageCaptor = ArgumentCaptor.forClass(NotificationOutboxMessage.class);
        verify(repository, atLeastOnce()).save(messageCaptor.capture());
        NotificationOutboxMessage finalState = messageCaptor.getAllValues().get(messageCaptor.getAllValues().size() - 1);
        assertEquals(NotificationOutboxStatus.SENT, finalState.getStatus());
        assertEquals(1, finalState.getAttempts());
        assertEquals(FIXED_NOW, finalState.getSentAt());
    }

    @Test
    void shouldNotProcessFuturePendingMessage() {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        EmailProviderClient providerClient = providerClient();
        when(repository.findEligibleForProcessing(eq(NotificationChannel.EMAIL), any(), eq(FIXED_NOW), any()))
                .thenReturn(List.of());

        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(true, 20, Duration.ofMinutes(1)),
                emailProperties(),
                List.of(providerClient),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        verify(providerClient, never()).send(any());
        verify(repository, never()).save(any(NotificationOutboxMessage.class));
    }

    @Test
    void shouldMarkMessageFailedWithNextAttemptWhenProviderFails(CapturedOutput output) {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        EmailProviderClient providerClient = providerClient();
        doThrow(new EmailNotificationException("http_500")).when(providerClient).send(any());
        when(repository.findEligibleForProcessing(eq(NotificationChannel.EMAIL), any(), eq(FIXED_NOW), any()))
                .thenReturn(List.of(message(NotificationOutboxStatus.PENDING, FIXED_NOW.minusSeconds(5), 0, 3, validPayload("admin@test.com"))));

        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(true, 20, Duration.ofMinutes(2)),
                emailProperties(),
                List.of(providerClient),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        ArgumentCaptor<NotificationOutboxMessage> messageCaptor = ArgumentCaptor.forClass(NotificationOutboxMessage.class);
        verify(repository, atLeastOnce()).save(messageCaptor.capture());
        NotificationOutboxMessage finalState = messageCaptor.getAllValues().get(messageCaptor.getAllValues().size() - 1);
        assertEquals(NotificationOutboxStatus.FAILED, finalState.getStatus());
        assertEquals(1, finalState.getAttempts());
        assertEquals(FIXED_NOW.plus(Duration.ofMinutes(2)), finalState.getNextAttemptAt());
        assertEquals("http_500", finalState.getLastError());
        assertTrue((output.getOut() + output.getErr()).contains("event=notification_outbox_email_send_failed"));
    }

    @Test
    void shouldMarkMessageDeadWhenMaxAttemptsIsReached(CapturedOutput output) {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        EmailProviderClient providerClient = providerClient();
        doThrow(new EmailNotificationException("io_error")).when(providerClient).send(any());
        when(repository.findEligibleForProcessing(eq(NotificationChannel.EMAIL), any(), eq(FIXED_NOW), any()))
                .thenReturn(List.of(message(NotificationOutboxStatus.FAILED, FIXED_NOW.minusSeconds(5), 2, 3, validPayload("admin@test.com"))));

        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(true, 20, Duration.ofMinutes(1)),
                emailProperties(),
                List.of(providerClient),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        ArgumentCaptor<NotificationOutboxMessage> messageCaptor = ArgumentCaptor.forClass(NotificationOutboxMessage.class);
        verify(repository, atLeastOnce()).save(messageCaptor.capture());
        NotificationOutboxMessage finalState = messageCaptor.getAllValues().get(messageCaptor.getAllValues().size() - 1);
        assertEquals(NotificationOutboxStatus.DEAD, finalState.getStatus());
        assertEquals(3, finalState.getAttempts());
        assertEquals(FIXED_NOW, finalState.getFailedAt());
        assertTrue((output.getOut() + output.getErr()).contains("event=notification_outbox_email_dead"));
    }

    @Test
    void shouldMarkCorruptPayloadDeadAndContinueBatch(CapturedOutput output) {
        NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
        EmailProviderClient providerClient = providerClient();
        NotificationOutboxMessage corrupt = message(NotificationOutboxStatus.PENDING, FIXED_NOW.minusSeconds(5), 0, 3, "{\"provider\":\"resend\"");
        NotificationOutboxMessage valid = message(NotificationOutboxStatus.PENDING, FIXED_NOW.minusSeconds(5), 0, 3, validPayload("admin@test.com"));
        when(repository.findEligibleForProcessing(eq(NotificationChannel.EMAIL), any(), eq(FIXED_NOW), any()))
                .thenReturn(List.of(corrupt, valid));

        NotificationOutboxEmailProcessor processor = new NotificationOutboxEmailProcessor(
                repository,
                processorProperties(true, 20, Duration.ofMinutes(1)),
                emailProperties(),
                List.of(providerClient),
                new DomainMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper(),
                FIXED_CLOCK
        );

        processor.processPendingEmailMessages();

        verify(providerClient, times(1)).send(any());
        assertEquals(NotificationOutboxStatus.DEAD, corrupt.getStatus());
        assertEquals(NotificationOutboxStatus.SENT, valid.getStatus());
        assertTrue((output.getOut() + output.getErr()).contains("event=notification_outbox_email_dead"));
    }

    @Test
    void shouldNotExposeSecretsInPayloadOrRequireRealProvider() {
        String payload = validPayload("admin@test.com");
        assertFalse(payload.contains("api-key"));
        assertNotNull(payload);
    }

    private NotificationOutboxEmailProcessorProperties processorProperties(boolean enabled, int batchSize, Duration retryBackoff) {
        NotificationOutboxEmailProcessorProperties properties = new NotificationOutboxEmailProcessorProperties();
        properties.setProcessorEnabled(enabled);
        properties.setProcessorBatchSize(batchSize);
        properties.setRetryBackoff(retryBackoff);
        properties.setProcessorFixedDelay(Duration.ofSeconds(30));
        return properties;
    }

    private EmailNotificationProperties emailProperties() {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(true);
        properties.setProvider("resend");
        properties.setApiKey("api-key");
        properties.setFrom("alerts@ganaderia.test");
        return properties;
    }

    private EmailProviderClient providerClient() {
        EmailProviderClient client = mock(EmailProviderClient.class);
        when(client.getProviderName()).thenReturn("resend");
        return client;
    }

    private NotificationOutboxMessage message(NotificationOutboxStatus status,
                                              Instant nextAttemptAt,
                                              int attempts,
                                              int maxAttempts,
                                              String payload) {
        NotificationOutboxMessage message = new NotificationOutboxMessage();
        message.setId(10L + attempts);
        message.setChannel(NotificationChannel.EMAIL);
        message.setStatus(status);
        message.setEventType("CRITICAL_ALERT_CREATED");
        message.setRecipient("admin@test.com");
        message.setSubject("Asunto");
        message.setPayload(payload);
        message.setAttempts(attempts);
        message.setMaxAttempts(maxAttempts);
        message.setNextAttemptAt(nextAttemptAt);
        message.setCreatedAt(FIXED_NOW.minus(Duration.ofHours(1)));
        message.setUpdatedAt(FIXED_NOW.minus(Duration.ofHours(1)));
        return message;
    }

    private String validPayload(String to) {
        return """
                {
                  "provider":"resend",
                  "to":"%s",
                  "subject":"Asunto",
                  "textBody":"Texto",
                  "htmlBody":"<p>Html</p>"
                }
                """.formatted(to);
    }
}
