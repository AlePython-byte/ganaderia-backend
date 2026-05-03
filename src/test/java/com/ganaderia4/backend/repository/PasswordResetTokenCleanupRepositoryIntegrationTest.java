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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetTokenCleanupRepositoryIntegrationTest extends AbstractIntegrationTest {

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
    void shouldDeleteOnlyOldExpiredOrUsedTokens() {
        User user = userRepository.save(user("admin@ganaderia.com"));

        PasswordResetToken expiredOld = passwordResetTokenRepository.save(token(
                user,
                "hash-expired-old",
                Instant.parse("2026-05-01T11:00:00Z"),
                null,
                Instant.parse("2026-05-01T10:30:00Z")
        ));
        PasswordResetToken usedOld = passwordResetTokenRepository.save(token(
                user,
                "hash-used-old",
                Instant.parse("2026-05-03T12:00:00Z"),
                Instant.parse("2026-05-01T11:00:00Z"),
                Instant.parse("2026-05-01T10:45:00Z")
        ));
        PasswordResetToken active = passwordResetTokenRepository.save(token(
                user,
                "hash-active",
                Instant.parse("2026-05-04T12:00:00Z"),
                null,
                Instant.parse("2026-05-03T11:00:00Z")
        ));
        PasswordResetToken usedRecent = passwordResetTokenRepository.save(token(
                user,
                "hash-used-recent",
                Instant.parse("2026-05-03T13:00:00Z"),
                Instant.parse("2026-05-03T11:30:00Z"),
                Instant.parse("2026-05-03T11:00:00Z")
        ));

        int deleted = passwordResetTokenRepository.deleteStaleTokens(Instant.parse("2026-05-02T12:00:00Z"));

        assertEquals(2, deleted);
        assertFalse(passwordResetTokenRepository.findById(expiredOld.getId()).isPresent());
        assertFalse(passwordResetTokenRepository.findById(usedOld.getId()).isPresent());
        assertTrue(passwordResetTokenRepository.findById(active.getId()).isPresent());
        assertTrue(passwordResetTokenRepository.findById(usedRecent.getId()).isPresent());
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
