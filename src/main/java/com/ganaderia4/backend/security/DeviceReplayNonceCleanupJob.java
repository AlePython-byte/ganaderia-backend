package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.DeviceReplayNonceCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DeviceReplayNonceCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DeviceReplayNonceCleanupJob.class);
    private static final Duration MINIMUM_RETENTION_MARGIN = Duration.ofMinutes(1);

    private final DeviceReplayNonceRepository deviceReplayNonceRepository;
    private final DeviceReplayNonceCleanupProperties properties;
    private final DomainMetricsService domainMetricsService;
    private final Clock clock;
    private final Duration validityWindow;
    private final AtomicBoolean retentionAdjustedLogged = new AtomicBoolean(false);

    public DeviceReplayNonceCleanupJob(DeviceReplayNonceRepository deviceReplayNonceRepository,
                                       DeviceReplayNonceCleanupProperties properties,
                                       DomainMetricsService domainMetricsService,
                                       Clock clock,
                                       @Value("${device.auth.window-seconds:300}") long windowSeconds) {
        this.deviceReplayNonceRepository = deviceReplayNonceRepository;
        this.properties = properties;
        this.domainMetricsService = domainMetricsService;
        this.clock = clock;
        this.validityWindow = Duration.ofSeconds(windowSeconds);
    }

    @Scheduled(fixedDelayString = "#{@deviceReplayNonceCleanupProperties.fixedDelay.toMillis()}")
    @Transactional
    public void cleanupExpiredNonces() {
        if (!properties.isEnabled()) {
            return;
        }

        long startedAt = System.nanoTime();
        String requestId = OperationalLogSanitizer.requestIdOr("scheduled");

        try {
            Instant cutoff = Instant.now(clock).minus(effectiveRetention());
            int deleted = deviceReplayNonceRepository.deleteByCreatedAtLessThanEqual(cutoff);
            if (deleted > 0) {
                domainMetricsService.incrementDeviceReplayNonceCleanupDeleted(deleted);
            }

            log.info(
                    "event=device_replay_nonce_cleanup_completed requestId={} deleted={} durationMs={}",
                    requestId,
                    deleted,
                    elapsedMs(startedAt)
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=device_replay_nonce_cleanup_failed requestId={} durationMs={} errorType={} error={}",
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
                : Duration.ofMinutes(30);
        Duration minimumSafeRetention = validityWindow.plus(MINIMUM_RETENTION_MARGIN);

        if (configuredRetention.compareTo(minimumSafeRetention) <= 0) {
            if (retentionAdjustedLogged.compareAndSet(false, true)) {
                log.warn(
                        "event=device_replay_nonce_cleanup_retention_adjusted requestId={} configuredRetention={} effectiveRetention={} windowSeconds={}",
                        OperationalLogSanitizer.requestIdOr("scheduled"),
                        configuredRetention,
                        minimumSafeRetention,
                        validityWindow.toSeconds()
                );
            }
            return minimumSafeRetention;
        }

        return configuredRetention;
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
