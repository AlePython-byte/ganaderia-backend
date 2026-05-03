package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class PasswordResetEmailTemplateBuilder {

    public PasswordResetEmailContent build(User user, String resetLink, Duration tokenTtl) {
        String displayName = escapeHtml(resolveDisplayName(user));
        String escapedLink = escapeHtml(resetLink);
        String expirationText = formatDuration(tokenTtl);
        String subject = "[Ganadería 4.0] Recuperación de contraseña";

        String textBody = String.join("\n",
                "Hola " + resolveDisplayName(user) + ",",
                "",
                "Recibimos una solicitud para restablecer tu contraseña en Ganadería 4.0.",
                "Usa el siguiente enlace para continuar:",
                resetLink,
                "",
                "Este enlace expira en " + expirationText + ".",
                "Si no solicitaste este cambio, puedes ignorar este correo."
        );

        String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1f2937; line-height: 1.5;">
                  <h2>Recuperación de contraseña</h2>
                  <p>Hola %s,</p>
                  <p>Recibimos una solicitud para restablecer tu contraseña en Ganadería 4.0.</p>
                  <p>
                    <a href="%s">Restablecer contraseña</a>
                  </p>
                  <p>Este enlace expira en %s.</p>
                  <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>
                </body>
                </html>
                """.formatted(displayName, escapedLink, escapeHtml(expirationText));

        return new PasswordResetEmailContent(subject, textBody, htmlBody);
    }

    private String resolveDisplayName(User user) {
        if (user == null || !StringUtils.hasText(user.getName())) {
            return "usuario";
        }

        return user.getName().trim();
    }

    private String formatDuration(Duration tokenTtl) {
        long minutes = Math.max(1, tokenTtl.toMinutes());
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours == 1 ? "1 hora" : hours + " horas";
        }

        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
