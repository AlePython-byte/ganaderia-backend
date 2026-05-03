package com.ganaderia4.backend.service;

public record PasswordResetEmailContent(
        String subject,
        String textBody,
        String htmlBody
) {
}
