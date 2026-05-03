package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.EmailNotificationProperties;
import com.ganaderia4.backend.config.FrontendProperties;
import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.EmailNotificationException;
import com.ganaderia4.backend.notification.EmailNotificationRequest;
import com.ganaderia4.backend.notification.EmailProviderClient;
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
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");

        PasswordResetEmailService service = new PasswordResetEmailService(
                emailProperties(false, "api-key", "alerts@test.com"),
                frontendProperties("http://localhost:5173/reset-password"),
                passwordResetProperties(Duration.ofMinutes(15)),
                List.of(providerClient),
                new PasswordResetEmailTemplateBuilder()
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw-token"));

        verify(providerClient, never()).send(any());
        assertTrue(output.getOut().contains("event=password_reset_email_skipped"));
        assertTrue(output.getOut().contains("reason=email_disabled"));
    }

    @Test
    void shouldCallProviderAndBuildEncodedResetLink() {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");

        PasswordResetEmailService service = new PasswordResetEmailService(
                emailProperties(true, "api-key", "alerts@test.com"),
                frontendProperties("http://localhost:5173/reset-password?source=forgot"),
                passwordResetProperties(Duration.ofMinutes(15)),
                List.of(providerClient),
                new PasswordResetEmailTemplateBuilder()
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw token/+"));

        ArgumentCaptor<EmailNotificationRequest> captor = ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(providerClient).send(captor.capture());

        EmailNotificationRequest request = captor.getValue();
        assertEquals(List.of("admin@test.com"), request.to());
        assertEquals("[Ganadería 4.0] Recuperación de contraseña", request.subject());
        assertTrue(request.textBody().contains("source=forgot&token=raw+token%2F%2B"));
        assertTrue(request.htmlBody().contains("source=forgot&amp;token=raw+token%2F%2B"));
    }

    @Test
    void shouldSwallowProviderFailureAndLogSafely(CapturedOutput output) {
        EmailProviderClient providerClient = mock(EmailProviderClient.class);
        when(providerClient.getProviderName()).thenReturn("resend");
        doThrow(new EmailNotificationException("http_500")).when(providerClient).send(any());

        PasswordResetEmailService service = new PasswordResetEmailService(
                emailProperties(true, "api-key", "alerts@test.com"),
                frontendProperties("http://localhost:5173/reset-password"),
                passwordResetProperties(Duration.ofMinutes(15)),
                List.of(providerClient),
                new PasswordResetEmailTemplateBuilder()
        );

        service.sendPasswordResetEmail(user("admin@test.com", true), issuedToken("raw-token"));

        verify(providerClient).send(any());
        assertTrue(output.getOut().contains("event=password_reset_email_failed"));
        assertTrue(output.getOut().contains("reason=provider_error"));
    }

    private EmailNotificationProperties emailProperties(boolean enabled, String apiKey, String from) {
        EmailNotificationProperties properties = new EmailNotificationProperties();
        properties.setEnabled(enabled);
        properties.setProvider("resend");
        properties.setApiKey(apiKey);
        properties.setFrom(from);
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
