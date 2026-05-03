package com.ganaderia4.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class EmailNotificationServiceTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldSkipWhenEmailNotificationsAreDisabled(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(false, "api-key", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SKIPPED, result);
        verify(providerClient, never()).send(any());
        verifyNoInteractions(outboxService);
        verifyNoInteractions(recipientResolver);
        assertTrue(output.getOut().contains("event=email_notification_skipped"));
        assertTrue(output.getOut().contains("reason=disabled"));
    }

    @Test
    void shouldSkipWhenApiKeyIsMissing(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SKIPPED, result);
        verify(providerClient, never()).send(any());
        verifyNoInteractions(outboxService);
        verifyNoInteractions(recipientResolver);
        assertTrue(output.getOut().contains("reason=missing_api_key"));
    }

    @Test
    void shouldSkipWhenFromIsMissing(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SKIPPED, result);
        verify(providerClient, never()).send(any());
        verifyNoInteractions(outboxService);
        verifyNoInteractions(recipientResolver);
        assertTrue(output.getOut().contains("reason=missing_from"));
    }

    @Test
    void shouldSkipWhenNoRecipientsCanBeResolved(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(List.of(), com.ganaderia4.backend.model.NotificationSeverity.MEDIUM, false)
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", ""),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SKIPPED, result);
        verify(providerClient, never()).send(any());
        verifyNoInteractions(outboxService);
        verify(recipientResolver).resolveRecipients(any(NotificationMessage.class));
        assertTrue(output.getOut().contains("reason=no_recipients"));
    }

    @Test
    void shouldCallProviderWhenEmailIsEnabledAndConfigured(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(
                        List.of("admin@test.com", "supervisor@test.com"),
                        com.ganaderia4.backend.model.NotificationSeverity.HIGH,
                        false
                )
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        ArgumentCaptor<EmailNotificationRequest> captor = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(providerClient).send(captor.capture());

        EmailNotificationRequest request = captor.getValue();
        assertEquals(NotificationSendResult.SENT, result);
        assertEquals("alerts@ganaderia.test", request.from());
        assertEquals(List.of("admin@test.com", "supervisor@test.com"), request.to());
        assertEquals("[Ganaderia 4.0] HIGH - Collar offline", request.subject());
        assertTrue(request.textBody().contains("Tipo: Collar offline"));
        assertTrue(request.htmlBody().contains("<html"));
        verify(outboxService, times(2)).enqueue(eq(NotificationChannel.EMAIL), eq("CRITICAL_ALERT_CREATED"), any(), any(), any());
        assertTrue(output.getOut().contains("event=email_notification_recipients_resolved"));
        assertTrue(output.getOut().contains("event=email_notification_outbox_enqueued"));
        assertTrue(output.getOut().contains("event=email_notification_send_requested"));
        assertTrue(output.getOut().contains("event=email_notification_sent"));
    }

    @Test
    void shouldUseGlobalFallbackWhenResolverReportsIt(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(
                        List.of("fallback@test.com"),
                        com.ganaderia4.backend.model.NotificationSeverity.HIGH,
                        true
                )
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", "fallback@test.com"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SENT, result);
        verify(providerClient).send(argThat(request -> request.to().equals(List.of("fallback@test.com"))));
        verify(outboxService).enqueue(eq(NotificationChannel.EMAIL), eq("CRITICAL_ALERT_CREATED"), eq("fallback@test.com"), any(), any());
        assertTrue(output.getOut().contains("event=email_notification_global_fallback_used"));
    }

    @Test
    void shouldUseTemplateBuilderOutput() {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(List.of("admin@test.com"), com.ganaderia4.backend.model.NotificationSeverity.HIGH, false)
        );
        AlertEmailTemplateBuilder templateBuilder = mock(AlertEmailTemplateBuilder.class);
        when(templateBuilder.build(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationContent("subject-x", "text-x", "html-x")
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                templateBuilder,
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        service.send(sampleMessage());

        verify(templateBuilder).build(any(NotificationMessage.class));
        verify(providerClient).send(argThat(request ->
                request.to().equals(List.of("admin@test.com"))
                        &&
                "subject-x".equals(request.subject())
                        && "text-x".equals(request.textBody())
                        && "html-x".equals(request.htmlBody())
        ));
        verify(outboxService).enqueue(eq(NotificationChannel.EMAIL), eq("CRITICAL_ALERT_CREATED"), eq("admin@test.com"), eq("subject-x"), any());
    }

    @Test
    void shouldLogFailureAndRethrowWhenProviderFails(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        doThrow(new EmailNotificationException("http_500")).when(providerClient).send(any());
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(List.of("admin@test.com"), com.ganaderia4.backend.model.NotificationSeverity.HIGH, false)
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        EmailNotificationException ex = assertThrows(
                EmailNotificationException.class,
                () -> service.send(sampleMessage())
        );

        assertEquals("http_500", ex.getMessage());
        verify(outboxService).enqueue(eq(NotificationChannel.EMAIL), eq("CRITICAL_ALERT_CREATED"), eq("admin@test.com"), any(), any());
        assertTrue(output.getOut().contains("event=email_notification_failed"));
        assertTrue(output.getOut().contains("reason=http_500"));
    }

    @Test
    void shouldContinueDirectSendWhenOutboxEnqueueFails(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(List.of("admin@test.com"), com.ganaderia4.backend.model.NotificationSeverity.HIGH, false)
        );
        doThrow(new IllegalStateException("enqueue_failed")).when(outboxService)
                .enqueue(eq(NotificationChannel.EMAIL), eq("CRITICAL_ALERT_CREATED"), eq("admin@test.com"), any(), any());

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        NotificationSendResult result = service.send(sampleMessage());

        assertEquals(NotificationSendResult.SENT, result);
        verify(providerClient).send(any());
        assertTrue(output.getOut().contains("event=email_notification_outbox_enqueue_failed"));
    }

    @Test
    void shouldPersistOneOutboxPerRecipientWithoutApiKey() throws Exception {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        EmailNotificationRecipientResolver recipientResolver = mock(EmailNotificationRecipientResolver.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        when(recipientResolver.resolveRecipients(any(NotificationMessage.class))).thenReturn(
                new EmailNotificationRecipientsResolution(
                        List.of("admin@test.com", "supervisor@test.com"),
                        com.ganaderia4.backend.model.NotificationSeverity.HIGH,
                        false
                )
        );

        EmailNotificationService service = new EmailNotificationService(
                emailProperties(true, "api-key-secret", "alerts@ganaderia.test", "ops@ganaderia.test"),
                List.of(providerClient),
                new AlertEmailTemplateBuilder(),
                recipientResolver,
                outboxService,
                new ObjectMapper()
        );

        service.send(sampleMessage());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(2)).enqueue(
                eq(NotificationChannel.EMAIL),
                eq("CRITICAL_ALERT_CREATED"),
                any(),
                any(),
                payloadCaptor.capture()
        );

        List<String> payloads = payloadCaptor.getAllValues();
        assertEquals(2, payloads.size());
        for (String payload : payloads) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new ObjectMapper().readValue(payload, Map.class);
            assertEquals("resend", parsed.get("provider"));
            assertTrue(parsed.containsKey("to"));
            assertTrue(parsed.containsKey("subject"));
            assertTrue(parsed.containsKey("textBody"));
            assertTrue(parsed.containsKey("htmlBody"));
            assertFalse(payload.contains("api-key-secret"));
        }
    }

    @Test
    void dispatcherShouldContinueWhenEmailProviderFails() {
        NotificationService emailService = mock(NotificationService.class);
        NotificationService loggingService = mock(NotificationService.class);

        when(emailService.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(loggingService.getChannel()).thenReturn(NotificationChannel.LOG);
        when(loggingService.send(any(NotificationMessage.class))).thenReturn(NotificationSendResult.SENT);
        doThrow(new EmailNotificationException("io_error"))
                .when(emailService)
                .send(any(NotificationMessage.class));

        DefaultNotificationDispatcher dispatcher = new DefaultNotificationDispatcher(
                List.of(emailService, loggingService),
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        dispatcher.dispatch(sampleMessage());

        verify(emailService).send(any(NotificationMessage.class));
        verify(loggingService).send(any(NotificationMessage.class));
    }

    private EmailNotificationProperties emailProperties(boolean enabled,
                                                        String apiKey,
                                                        String from,
                                                        String to) {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(enabled);
        properties.setProvider("resend");
        properties.setApiKey(apiKey);
        properties.setFrom(from);
        properties.setTo(to);
        properties.setConnectTimeoutMs(3000);
        properties.setReadTimeoutMs(5000);
        return properties;
    }

    private NotificationMessage sampleMessage() {
        return NotificationMessage.builder()
                .eventType("CRITICAL_ALERT_CREATED")
                .title("Nueva alerta critica")
                .message("Collar sin senal")
                .severity("HIGH")
                .createdAt(LocalDateTime.of(2026, 5, 1, 9, 30))
                .metadata("alertType", "COLLAR_OFFLINE")
                .metadata("cowToken", "VACA-001")
                .build();
    }
}
