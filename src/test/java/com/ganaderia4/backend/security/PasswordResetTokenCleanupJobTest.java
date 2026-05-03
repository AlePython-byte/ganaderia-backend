package com.ganaderia4.backend.security;

import com.ganaderia4.backend.config.PasswordResetCleanupProperties;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.PasswordResetTokenRepository;
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
class PasswordResetTokenCleanupJobTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-03T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetCleanupProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private DomainMetricsService domainMetricsService;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetCleanupProperties();
        properties.setEnabled(true);
        properties.setFixedDelay(Duration.ofMinutes(30));
        properties.setRetention(Duration.ofHours(24));
        meterRegistry = new SimpleMeterRegistry();
        domainMetricsService = new DomainMetricsService(meterRegistry);
    }

    @Test
    void shouldDeleteStaleTokensAndRecordMetric(CapturedOutput output) {
        when(passwordResetTokenRepository.deleteStaleTokens(Instant.parse("2026-05-02T18:00:00Z")))
                .thenReturn(5);

        PasswordResetTokenCleanupJob job = new PasswordResetTokenCleanupJob(
                passwordResetTokenRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleTokens();

        verify(passwordResetTokenRepository).deleteStaleTokens(Instant.parse("2026-05-02T18:00:00Z"));
        assertEquals(
                5.0,
                meterRegistry.counter("ganaderia.auth.password_reset.cleanup.deleted.count").count()
        );
        assertTrue(output.getOut().contains("event=password_reset_token_cleanup_completed"));
        assertTrue(output.getOut().contains("deleted=5"));
    }

    @Test
    void shouldNotDeleteWhenCleanupIsDisabled() {
        properties.setEnabled(false);

        PasswordResetTokenCleanupJob job = new PasswordResetTokenCleanupJob(
                passwordResetTokenRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleTokens();

        verify(passwordResetTokenRepository, never()).deleteStaleTokens(any());
    }

    @Test
    void shouldUseDefaultRetentionWhenConfiguredRetentionIsInvalid() {
        properties.setRetention(Duration.ZERO);
        when(passwordResetTokenRepository.deleteStaleTokens(any())).thenReturn(0);

        PasswordResetTokenCleanupJob job = new PasswordResetTokenCleanupJob(
                passwordResetTokenRepository,
                properties,
                domainMetricsService,
                FIXED_CLOCK
        );

        job.cleanupStaleTokens();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(passwordResetTokenRepository).deleteStaleTokens(captor.capture());
        assertEquals(Instant.parse("2026-05-02T18:00:00Z"), captor.getValue());
    }
}
