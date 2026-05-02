package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AbuseRateLimitCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AbuseRateLimitCleanupJob.class);

    private final AbuseRateLimitRepository abuseRateLimitRepository;
    private final AbuseProtectionCleanupProperties properties;
    private final DomainMetricsService domainMetricsService;
    private final Clock clock;

    public AbuseRateLimitCleanupJob(AbuseRateLimitRepository abuseRateLimitRepository,
                                    AbuseProtectionCleanupProperties properties,
                                    DomainMetricsService domainMetricsService,
                                    Clock clock) {
        this.abuseRateLimitRepository = abuseRateLimitRepository;
        this.properties = properties;
        this.domainMetricsService = domainMetricsService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "#{@abuseProtectionCleanupProperties.fixedDelay.toMillis()}")
    @Transactional
    public void cleanupStaleRateLimits() {
        if (!properties.isEnabled()) {
            return;
        }

        long startedAt = System.nanoTime();
        String requestId = OperationalLogSanitizer.requestIdOr("scheduled");

        try {
            Instant now = Instant.now(clock);
            Instant cutoff = now.minus(effectiveRetention());
            int deleted = abuseRateLimitRepository.deleteInactiveEntries(cutoff, now);
            if (deleted > 0) {
                domainMetricsService.incrementAbuseRateLimitCleanupDeleted(deleted);
            }

            log.info(
                    "event=abuse_rate_limit_cleanup_completed requestId={} deleted={} durationMs={}",
                    requestId,
                    deleted,
                    elapsedMs(startedAt)
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=abuse_rate_limit_cleanup_failed requestId={} durationMs={} errorType={} error={}",
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

        if (configuredRetention.isNegative() || configuredRetention.isZero()) {
            return Duration.ofHours(24);
        }

        return configuredRetention;
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
