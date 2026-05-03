package com.ganaderia4.backend.repository;

import com.ganaderia4.backend.model.PasswordResetToken;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordResetTokenRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void shouldInvalidateOnlyActiveTokensForUser() {
        User user = userRepository.save(user("admin@ganaderia.com"));

        PasswordResetToken active = passwordResetTokenRepository.save(token(
                user,
                "hash-active",
                Instant.parse("2026-05-03T12:15:00Z"),
                null,
                Instant.parse("2026-05-03T12:00:00Z")
        ));
        PasswordResetToken expired = passwordResetTokenRepository.save(token(
                user,
                "hash-expired",
                Instant.parse("2026-05-03T11:50:00Z"),
                null,
                Instant.parse("2026-05-03T11:30:00Z")
        ));
        PasswordResetToken alreadyUsed = passwordResetTokenRepository.save(token(
                user,
                "hash-used",
                Instant.parse("2026-05-03T12:20:00Z"),
                Instant.parse("2026-05-03T12:01:00Z"),
                Instant.parse("2026-05-03T11:55:00Z")
        ));

        int invalidated = passwordResetTokenRepository.invalidateActiveTokensForUser(
                user.getId(),
                Instant.parse("2026-05-03T12:05:00Z"),
                Instant.parse("2026-05-03T12:05:00Z")
        );

        assertEquals(1, invalidated);

        PasswordResetToken refreshedActive = passwordResetTokenRepository.findById(active.getId()).orElseThrow();
        PasswordResetToken refreshedExpired = passwordResetTokenRepository.findById(expired.getId()).orElseThrow();
        PasswordResetToken refreshedUsed = passwordResetTokenRepository.findById(alreadyUsed.getId()).orElseThrow();

        assertNotNull(refreshedActive.getUsedAt());
        assertNull(refreshedExpired.getUsedAt());
        assertEquals(Instant.parse("2026-05-03T12:01:00Z"), refreshedUsed.getUsedAt());
    }

    private User user(String email) {
        User user = new User();
        user.setName("Admin");
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        return user;
    }

    private PasswordResetToken token(User user,
                                     String tokenHash,
                                     Instant expiresAt,
                                     Instant usedAt,
                                     Instant createdAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        token.setCreatedAt(createdAt);
        return token;
    }
}
