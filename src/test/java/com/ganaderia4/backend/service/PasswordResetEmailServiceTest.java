package com.ganaderia4.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.FrontendProperties;
import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.EmailDeliveryMode;
import com.ganaderia4.backend.notification.EmailNotificationException;
import com.ganaderia4.backend.notification.EmailNotificationRequest;
import com.ganaderia4.backend.notification.EmailProviderClient;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class PasswordResetEmailServiceTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldSkipWhenEmailIsDisabled(CapturedOutput output) {
        EmailProviderClient providerClient = providerClient();
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        PasswordResetEmailService service = service(
                emailProperties(false, "api-key", "alerts@test.com"),
                List.of(providerClient),
                outboxService
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw-token"));

        verify(providerClient, never()).send(any());
        verifyNoInteractions(outboxService);
        assertTrue(output.getOut().contains("event=password_reset_email_skipped"));
        assertTrue(output.getOut().contains("reason=email_disabled"));
    }

    @Test
    void shouldCallProviderAndBuildEncodedResetLinkInDirectMode() {
        EmailProviderClient providerClient = providerClient();
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        PasswordResetEmailService service = service(
                emailProperties(true, "api-key", "alerts@test.com", EmailDeliveryMode.DIRECT),
                List.of(providerClient),
                outboxService,
                "http://localhost:5173/reset-password?source=forgot"
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw token/+"));

        ArgumentCaptor<EmailNotificationRequest> captor = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(providerClient).send(captor.capture());
        verifyNoInteractions(outboxService);

        EmailNotificationRequest request = captor.getValue();
        assertEquals(List.of("admin@test.com"), request.to());
        assertEquals("[Ganadería 4.0] Recuperación de contraseña", request.subject());
        assertTrue(request.textBody().contains("source=forgot&token=raw+token%2F%2B"));
        assertTrue(request.htmlBody().contains("source=forgot&amp;token=raw+token%2F%2B"));
    }

    @Test
    void shouldEnqueueOutboxAndSkipProviderInOutboxMode() throws Exception {
        EmailProviderClient providerClient = providerClient();
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PasswordResetEmailService service = service(
                emailProperties(true, "api-key", "alerts@test.com", EmailDeliveryMode.OUTBOX),
                List.of(providerClient),
                outboxService,
                objectMapper,
                "http://localhost:5173/reset-password?source=forgot"
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw token/+"));

        verify(providerClient, never()).send(any());
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).enqueue(
                eq(NotificationChannel.EMAIL),
                eq("PASSWORD_RESET_REQUESTED"),
                eq("admin@test.com"),
                eq("[Ganadería 4.0] Recuperación de contraseña"),
                payloadCaptor.capture()
        );

        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("resend", payload.get("provider").asText());
        assertEquals("admin@test.com", payload.get("to").asText());
        assertEquals("[Ganadería 4.0] Recuperación de contraseña", payload.get("subject").asText());
        assertTrue(payload.get("textBody").asText().contains("source=forgot&token=raw+token%2F%2B"));
        assertTrue(payload.get("htmlBody").asText().contains("source=forgot&amp;token=raw+token%2F%2B"));
        assertFalse(payloadCaptor.getValue().contains("api-key"));
    }

    @Test
    void shouldSwallowProviderFailureAndLogSafely(CapturedOutput output) {
        EmailProviderClient providerClient = providerClient();
        doThrow(new EmailNotificationException("http_500")).when(providerClient).send(any());
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);

        PasswordResetEmailService service = service(
                emailProperties(true, "api-key", "alerts@test.com", EmailDeliveryMode.DIRECT),
                List.of(providerClient),
                outboxService
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw-token"));

        verify(providerClient).send(any());
        verifyNoInteractions(outboxService);
        assertTrue(output.getOut().contains("event=password_reset_email_failed"));
        assertTrue(output.getOut().contains("reason=provider_error"));
    }

    private PasswordResetEmailService service(EmailNotificationProperties emailProperties,
                                              List<EmailProviderClient> providerClients,
                                              NotificationOutboxService outboxService) {
        return service(emailProperties, providerClients, outboxService, new ObjectMapper(),
                "http://localhost:5173/reset-password");
    }

    private PasswordResetEmailService service(EmailNotificationProperties emailProperties,
                                              List<EmailProviderClient> providerClients,
                                              NotificationOutboxService outboxService,
                                              String passwordResetUrl) {
        return service(emailProperties, providerClients, outboxService, new ObjectMapper(), passwordResetUrl);
    }

    private PasswordResetEmailService service(EmailNotificationProperties emailProperties,
                                              List<EmailProviderClient> providerClients,
                                              NotificationOutboxService outboxService,
                                              ObjectMapper objectMapper,
                                              String passwordResetUrl) {
        return new PasswordResetEmailService(
                emailProperties,
                frontendProperties(passwordResetUrl),
                passwordResetProperties(Duration.ofMinutes(15)),
                providerClients,
                new PasswordResetEmailTemplateBuilder(),
                outboxService,
                objectMapper
        );
    }

    private EmailProviderClient providerClient() {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        return providerClient;
    }

    private EmailNotificationProperties emailProperties(boolean enabled, String apiKey, String from) {
        return emailProperties(enabled, apiKey, from, EmailDeliveryMode.DIRECT);
    }

    private EmailNotificationProperties emailProperties(boolean enabled,
                                                        String apiKey,
                                                        String from,
                                                        EmailDeliveryMode deliveryMode) {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(enabled);
        properties.setProvider("resend");
        properties.setApiKey(apiKey);
        properties.setFrom(from);
        properties.setDeliveryMode(deliveryMode.name().toLowerCase());
        return properties;
    }

    private FrontendProperties frontendProperties(String passwordResetUrl) {
        FrontendProperties properties = new FrontendProperties();
        properties.setPasswordResetUrl(passwordResetUrl);
        return properties;
    }

    private PasswordResetProperties passwordResetProperties(Duration ttl) {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTokenTtl(ttl);
        return properties;
    }

    private PasswordResetTokenIssueResult issuedToken(String rawToken) {
        return new PasswordResetTokenIssueResult(1L, rawToken, Instant.parse("2026-05-03T12:15:00Z"));
    }

    private User user(String email, boolean active) {
        User user = new User();
        user.setId(1L);
        user.setName("Admin");
        user.setEmail(email);
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(active);
        return user;
    }
}
