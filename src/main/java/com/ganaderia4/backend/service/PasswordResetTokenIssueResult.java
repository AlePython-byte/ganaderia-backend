package com.ganaderia4.backend.service;

import java.time.Instant;

public record PasswordResetTokenIssueResult(
        Long userId,
        String rawToken,
        Instant expiresAt
) {
}
