package com.ganaderia4.backend.observability;

import com.ganaderia4.backend.model.GpsAccuracyQuality;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainMetricsServiceTest {

    @Test
    void shouldIncrementGpsAccuracyQualityMetricForGood() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementGpsAccuracyQuality(GpsAccuracyQuality.GOOD);

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.gps.accuracy.quality.count",
                "quality", "GOOD"
        ).count());
    }

    @Test
    void shouldIncrementGpsAccuracyQualityMetricForModerate() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementGpsAccuracyQuality(GpsAccuracyQuality.MODERATE);

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.gps.accuracy.quality.count",
                "quality", "MODERATE"
        ).count());
    }

    @Test
    void shouldIncrementGpsAccuracyQualityMetricForLow() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementGpsAccuracyQuality(GpsAccuracyQuality.LOW);

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.gps.accuracy.quality.count",
                "quality", "LOW"
        ).count());
    }

    @Test
    void shouldIncrementGpsAccuracyQualityMetricForUnknownWhenQualityIsNull() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementGpsAccuracyQuality(null);

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.gps.accuracy.quality.count",
                "quality", "UNKNOWN"
        ).count());
    }

    @Test
    void shouldIncrementAiSummaryGeneratedMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementAiSummaryGenerated("AI", "HIGH");

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.ai.summary.generated.count",
                "source", "AI",
                "riskLevel", "HIGH"
        ).count());
    }

    @Test
    void shouldIncrementAiSummaryFallbackMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementAiSummaryFallback("provider_error");

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.ai.summary.fallback.count",
                "reason", "provider_error"
        ).count());
    }

    @Test
    void shouldIncrementAiProviderRequestMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DomainMetricsService service = new DomainMetricsService(meterRegistry);

        service.incrementAiProviderRequest("gemini", "failure");

        assertEquals(1.0, meterRegistry.counter(
                "ganaderia.ai.summary.provider.request.count",
                "provider", "gemini",
                "outcome", "failure"
        ).count());
    }
}
