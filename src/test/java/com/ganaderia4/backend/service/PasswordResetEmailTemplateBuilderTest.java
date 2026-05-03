package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetEmailTemplateBuilderTest {

    private final PasswordResetEmailTemplateBuilder builder = new PasswordResetEmailTemplateBuilder();

    @Test
    void shouldBuildFixedSubjectAndIncludeExpirationNotice() {
        PasswordResetEmailContent content = builder.build(
                user("Admin"),
                "http://localhost:5173/reset-password?token=abc123",
                Duration.ofMinutes(15)
        );

        assertEquals("[Ganadería 4.0] Recuperación de contraseña", content.subject());
        assertTrue(content.textBody().contains("Este enlace expira en 15 minutos."));
        assertTrue(content.textBody().contains("Si no solicitaste este cambio, puedes ignorar este correo."));
        assertTrue(content.htmlBody().contains("Restablecer contraseña"));
    }

    @Test
    void shouldEscapeDangerousHtmlInDynamicFields() {
        PasswordResetEmailContent content = builder.build(
                user("Admin <script>alert(1)</script>"),
                "http://localhost:5173/reset-password?token=abc&next=<x>",
                Duration.ofMinutes(15)
        );

        assertTrue(content.htmlBody().contains("Admin &lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(content.htmlBody().contains("token=abc&amp;next=&lt;x&gt;"));
        assertFalse(content.htmlBody().contains("<script>"));
    }

    private User user(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail("admin@test.com");
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        return user;
    }
}
