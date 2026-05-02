package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.AbuseProtectionCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AbuseRateLimitCleanupJobTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-02T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AbuseRateLimitRepository abuseRateLimitRepository;

    private AbuseProtectionCleanupProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private DomainMetricsService domainMetricsService;

    @BeforeEach
    void setUp() {
        properties = new AbuseProtectionCleanupProperties();
        properties.setEnabled(true);
        properties.setFixedDelay(Duration.ofMinutes(30));
        properties.setRetention(Duration.ofHours(24));
        meterRegistry = new SimpleMeterRegistry();
        domainMetricsService = new DomainMetricsService(meterRegistry);
    }

    @Test
    void shouldDeleteOldInactiveEntriesAndRecordMetric(CapturedOutput output) {
        when(abuseRateLimitRepository.deleteInactiveEntries(
                Instant.parse("2026-05-01T18:00:00Z"),
                Instant.parse("2026-05-02T18:00:00Z")
        )).thenReturn(4);

        AbuseRateLimitCleanupJob job = new AbuseRateLimitCleanupJob(
                abuseRateLimitRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleRateLimits();

        verify(abuseRateLimitRepository).deleteInactiveEntries(
                Instant.parse("2026-05-01T18:00:00Z"),
                Instant.parse("2026-05-02T18:00:00Z")
        );
        assertEquals(
                4.0,
                meterRegistry.counter("ganaderia.abuse.rate_limit.cleanup.deleted.count").count()
        );
        assertTrue(output.getOut().contains("event=abuse_rate_limit_cleanup_completed"));
        assertTrue(output.getOut().contains("deleted=4"));
    }

    @Test
    void shouldNotDeleteWhenCleanupIsDisabled() {
        properties.setEnabled(false);

        AbuseRateLimitCleanupJob job = new AbuseRateLimitCleanupJob(
                abuseRateLimitRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleRateLimits();

        verify(abuseRateLimitRepository, never()).deleteInactiveEntries(any(), any());
    }

    @Test
    void shouldUseDefaultRetentionWhenConfiguredRetentionIsInvalid() {
        properties.setRetention(Duration.ZERO);
        when(abuseRateLimitRepository.deleteInactiveEntries(any(), any())).thenReturn(0);

        AbuseRateLimitCleanupJob job = new AbuseRateLimitCleanupJob(
                abuseRateLimitRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleRateLimits();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(abuseRateLimitRepository).deleteInactiveEntries(cutoffCaptor.capture(), any());
        assertEquals(Instant.parse("2026-05-01T18:00:00Z"), cutoffCaptor.getValue());
    }
}
