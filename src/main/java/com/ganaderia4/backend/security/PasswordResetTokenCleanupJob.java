package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.PasswordResetCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenCleanupJob.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetCleanupProperties properties;
    private final DomainMetricsService domainMetricsService;
    private final Clock clock;

    public PasswordResetTokenCleanupJob(PasswordResetTokenRepository passwordResetTokenRepository,
                                        PasswordResetCleanupProperties properties,
                                        DomainMetricsService domainMetricsService,
                                        Clock clock) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.properties = properties;
        this.domainMetricsService = domainMetricsService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "#{@passwordResetCleanupProperties.fixedDelay.toMillis()}")
    @Transactional
    public void cleanupStaleTokens() {
        if (!properties.isEnabled()) {
            return;
        }

        long startedAt = System.nanoTime();
        String requestId = OperationalLogSanitizer.requestIdOr("scheduled");

        try {
            Instant now = Instant.now(clock);
            Instant cutoff = now.minus(effectiveRetention());
            int deleted = passwordResetTokenRepository.deleteStaleTokens(cutoff);
            if (deleted > 0) {
                domainMetricsService.incrementPasswordResetCleanupDeleted(deleted);
            }

            log.info(
                    "event=password_reset_token_cleanup_completed requestId={} deleted={} durationMs={}",
                    requestId,
                    deleted,
                    elapsedMs(startedAt)
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=password_reset_token_cleanup_failed requestId={} durationMs={} errorType={} error={}",
                    requestId,
                    elapsedMs(startedAt),
                    ex.getClass().getSimpleName(),
                    OperationalLogSanitizer.safe(ex.getMessage()),
                    ex
            );
            throw ex;
        }
    }

    Duration effectiveRetention() {
        Duration configuredRetention = properties.getRetention() != null
                ? properties.getRetention()
                : Duration.ofHours(24);

        if (configuredRetention.isZero() || configuredRetention.isNegative()) {
            return Duration.ofHours(24);
        }

        return configuredRetention;
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
