package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.DeviceReplayNonceCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.DeviceReplayNonceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DeviceReplayNonceCleanupJobTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-02T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private DeviceReplayNonceRepository deviceReplayNonceRepository;

    private DeviceReplayNonceCleanupProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private DomainMetricsService domainMetricsService;

    @BeforeEach
    void setUp() {
        properties = new DeviceReplayNonceCleanupProperties();
        properties.setEnabled(true);
        properties.setFixedDelay(Duration.ofMinutes(10));
        properties.setRetention(Duration.ofMinutes(30));
        meterRegistry = new SimpleMeterRegistry();
        domainMetricsService = new DomainMetricsService(meterRegistry);
    }

    @Test
    void shouldDeleteNoncesOlderThanRetentionAndRecordMetric(CapturedOutput output) {
        when(deviceReplayNonceRepository.deleteByCreatedAtLessThanEqual(Instant.parse("2026-05-02T17:30:00Z")))
                .thenReturn(3);

        DeviceReplayNonceCleanupJob job = new DeviceReplayNonceCleanupJob(
                deviceReplayNonceRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK,
                300
        );

        job.cleanupExpiredNonces();

        verify(deviceReplayNonceRepository).deleteByCreatedAtLessThanEqual(Instant.parse("2026-05-02T17:30:00Z"));
        assertEquals(
                3.0,
                meterRegistry.counter("ganaderia.device.replay_nonce.cleanup.deleted.count").count()
        );
        assertTrue(output.getOut().contains("event=device_replay_nonce_cleanup_completed"));
        assertTrue(output.getOut().contains("deleted=3"));
    }

    @Test
    void shouldNotDeleteWhenCleanupIsDisabled() {
        properties.setEnabled(false);

        DeviceReplayNonceCleanupJob job = new DeviceReplayNonceCleanupJob(
                deviceReplayNonceRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK,
                300
        );

        job.cleanupExpiredNonces();

        verify(deviceReplayNonceRepository, never()).deleteByCreatedAtLessThanEqual(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldUseSafeRetentionWhenConfiguredRetentionIsTooShort(CapturedOutput output) {
        properties.setRetention(Duration.ofMinutes(5));
        when(deviceReplayNonceRepository.deleteByCreatedAtLessThanEqual(Instant.parse("2026-05-02T17:54:00Z")))
                .thenReturn(1);

        DeviceReplayNonceCleanupJob job = new DeviceReplayNonceCleanupJob(
                deviceReplayNonceRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK,
                300
        );

        job.cleanupExpiredNonces();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(deviceReplayNonceRepository).deleteByCreatedAtLessThanEqual(cutoffCaptor.capture());
        assertEquals(Instant.parse("2026-05-02T17:54:00Z"), cutoffCaptor.getValue());
        assertTrue(output.getOut().contains("event=device_replay_nonce_cleanup_retention_adjusted"));
        assertTrue(output.getOut().contains("event=device_replay_nonce_cleanup_completed"));
    }
}
