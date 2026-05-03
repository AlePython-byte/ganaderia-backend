package com.ganaderia4.backend.service;

import com.ganaderia4.backend.config.PasswordResetProperties;
import com.ganaderia4.backend.model.PasswordResetToken;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetTokenService {

    private static final int TOKEN_BYTES = 32;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository,
                                     PasswordResetProperties properties,
                                     Clock clock) {
        this(passwordResetTokenRepository, properties, clock, new SecureRandom());
    }

    PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository,
                              PasswordResetProperties properties,
                              Clock clock,
                              SecureRandom secureRandom) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public PasswordResetTokenIssueResult generateToken(User user, String requestIp, String userAgent) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User with persisted id is required");
        }

        validateConfiguration();

        Instant now = Instant.now(clock);
        passwordResetTokenRepository.invalidateActiveTokensForUser(user.getId(), now, now);

        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(properties.getTokenTtl()));
        token.setRequestIpHash(hashOptionalValue(requestIp));
        token.setUserAgentHash(hashOptionalValue(userAgent));

        passwordResetTokenRepository.save(token);
        return new PasswordResetTokenIssueResult(user.getId(), rawToken, token.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String rawToken) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Password reset token is invalid"));

        if (token.getUsedAt() != null) {
            throw new InvalidPasswordResetTokenException("Password reset token has already been used");
        }

        if (!token.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new InvalidPasswordResetTokenException("Password reset token has expired");
        }

        return token;
    }

    @Transactional
    public PasswordResetToken consumeToken(String rawToken) {
        PasswordResetToken token = validateToken(rawToken);
        token.setUsedAt(Instant.now(clock));
        return passwordResetTokenRepository.save(token);
    }

    private void validateConfiguration() {
        if (properties.getTokenTtl() == null || properties.getTokenTtl().isZero() || properties.getTokenTtl().isNegative()) {
            throw new IllegalStateException("app.auth.password-reset.token-ttl must be greater than zero");
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new InvalidPasswordResetTokenException("Password reset token is invalid");
        }

        return sha256Hex(rawToken.trim());
    }

    private String hashOptionalValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return sha256Hex(value.trim());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
