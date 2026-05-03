package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.PasswordResetToken;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetTokenServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-03T12:00:00Z");

    @Test
    void shouldGenerateRawTokenAndPersistOnlyHash() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();

        when(repository.invalidateActiveTokensForUser(anyLong(), any(), any())).thenReturn(1);
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            savedToken.set(token);
            return token;
        }).when(repository).save(any(PasswordResetToken.class));

        PasswordResetTokenService service = new PasswordResetTokenService(
                repository,
                properties,
                fixedClock(),
                fixedRandom()
        );

        PasswordResetTokenIssueResult result = service.generateToken(user(7L), "127.0.0.1", "JUnit-Agent");

        assertNotNull(result.rawToken());
        assertEquals(7L, result.userId());
        assertEquals(FIXED_NOW.plus(Duration.ofMinutes(15)), result.expiresAt());
        assertNotNull(savedToken.get());
        assertNotEquals(result.rawToken(), savedToken.get().getTokenHash());
        assertEquals(64, savedToken.get().getTokenHash().length());
        assertEquals(FIXED_NOW, savedToken.get().getCreatedAt());
        assertEquals(FIXED_NOW.plus(Duration.ofMinutes(15)), savedToken.get().getExpiresAt());
        assertEquals(64, savedToken.get().getRequestIpHash().length());
        assertEquals(64, savedToken.get().getUserAgentHash().length());
        verify(repository).invalidateActiveTokensForUser(7L, FIXED_NOW, FIXED_NOW);
        verify(repository).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldValidateActiveToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();

        when(repository.invalidateActiveTokensForUser(anyLong(), any(), any())).thenReturn(0);
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            savedToken.set(token);
            return token;
        }).when(repository).save(any(PasswordResetToken.class));

        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());
        PasswordResetTokenIssueResult result = service.generateToken(user(9L), null, null);

        when(repository.findByTokenHash(savedToken.get().getTokenHash())).thenReturn(Optional.of(savedToken.get()));

        PasswordResetToken validated = service.validateToken(result.rawToken());

        assertEquals(savedToken.get().getTokenHash(), validated.getTokenHash());
        assertEquals(9L, validated.getUser().getId());
    }

    @Test
    void shouldRejectExpiredToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();

        when(repository.invalidateActiveTokensForUser(anyLong(), any(), any())).thenReturn(0);
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            savedToken.set(token);
            return token;
        }).when(repository).save(any(PasswordResetToken.class));

        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());
        PasswordResetTokenIssueResult result = service.generateToken(user(11L), null, null);
        savedToken.get().setExpiresAt(FIXED_NOW.minusSeconds(1));

        when(repository.findByTokenHash(savedToken.get().getTokenHash())).thenReturn(Optional.of(savedToken.get()));

        InvalidPasswordResetTokenException exception = assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> service.validateToken(result.rawToken())
        );

        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectUsedToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();

        when(repository.invalidateActiveTokensForUser(anyLong(), any(), any())).thenReturn(0);
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            savedToken.set(token);
            return token;
        }).when(repository).save(any(PasswordResetToken.class));

        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());
        PasswordResetTokenIssueResult result = service.generateToken(user(13L), null, null);
        savedToken.get().setUsedAt(FIXED_NOW.minusSeconds(5));

        when(repository.findByTokenHash(savedToken.get().getTokenHash())).thenReturn(Optional.of(savedToken.get()));

        InvalidPasswordResetTokenException exception = assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> service.validateToken(result.rawToken())
        );

        assertTrue(exception.getMessage().contains("already been used"));
    }

    @Test
    void shouldConsumeTokenAndMarkUsedAt() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        AtomicReference<PasswordResetToken> savedToken = new AtomicReference<>();

        when(repository.invalidateActiveTokensForUser(anyLong(), any(), any())).thenReturn(0);
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            savedToken.set(token);
            return token;
        }).when(repository).save(any(PasswordResetToken.class));

        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());
        PasswordResetTokenIssueResult result = service.generateToken(user(15L), null, null);

        when(repository.findByTokenHash(savedToken.get().getTokenHash())).thenReturn(Optional.of(savedToken.get()));
        when(repository.save(savedToken.get())).thenReturn(savedToken.get());

        PasswordResetToken consumed = service.consumeToken(result.rawToken());

        assertEquals(FIXED_NOW, consumed.getUsedAt());
        verify(repository, times(2)).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldRejectInvalidToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());

        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        InvalidPasswordResetTokenException exception = assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> service.validateToken("invalid-token")
        );

        assertTrue(exception.getMessage().contains("invalid"));
        verify(repository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void shouldRejectBlankToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ofMinutes(15));
        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());

        InvalidPasswordResetTokenException exception = assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> service.validateToken("   ")
        );

        assertTrue(exception.getMessage().contains("invalid"));
        verify(repository, never()).findByTokenHash(anyString());
    }

    @Test
    void shouldRejectNonPositiveTtlConfiguration() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetProperties properties = properties(Duration.ZERO);
        PasswordResetTokenService service = new PasswordResetTokenService(repository, properties, fixedClock(), fixedRandom());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateToken(user(17L), null, null)
        );

        assertTrue(exception.getMessage().contains("token-ttl"));
    }

    private PasswordResetProperties properties(Duration ttl) {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTokenTtl(ttl);
        return properties;
    }

    private Clock fixedClock() {
        return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }

    private SecureRandom fixedRandom() {
        return new SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                for (int index = 0; index < bytes.length; index++) {
                    bytes[index] = (byte) (index + 1);
                }
            }
        };
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Admin");
        user.setEmail("admin@ganaderia.com");
        user.setPassword("hash");
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(true);
        return user;
    }
}
