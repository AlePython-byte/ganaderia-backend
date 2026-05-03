package com.ganaderia4.backend.observability;

import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.GpsAccuracyQuality;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DomainMetricsService {

    private static final String ALERTS_CREATED = "ganaderia.alerts.created";
    private static final String ALERTS_RESOLVED = "ganaderia.alerts.resolved";
    private static final String ALERTS_DISCARDED = "ganaderia.alerts.discarded";
    private static final String COLLARS_MARKED_OFFLINE = "ganaderia.collars.marked_offline";
    private static final String DEVICE_REQUESTS_ACCEPTED = "ganaderia.device.requests.accepted";
    private static final String DEVICE_REQUESTS_REJECTED = "ganaderia.device.requests.rejected";
    private static final String NOTIFICATIONS_SENT = "ganaderia.notifications.sent";
    private static final String NOTIFICATIONS_FAILED = "ganaderia.notifications.failed";
    private static final String NOTIFICATIONS_QUEUED = "ganaderia.notifications.queued";
    private static final String NOTIFICATIONS_RETRIED = "ganaderia.notifications.retried";
    private static final String GPS_ACCURACY_QUALITY_COUNT = "ganaderia.gps.accuracy.quality.count";
    private static final String DEVICE_REPLAY_NONCE_CLEANUP_DELETED_COUNT = "ganaderia.device.replay_nonce.cleanup.deleted.count";
    private static final String ABUSE_RATE_LIMIT_CLEANUP_DELETED_COUNT = "ganaderia.abuse.rate_limit.cleanup.deleted.count";
    private static final String PASSWORD_RESET_CLEANUP_DELETED_COUNT = "ganaderia.auth.password_reset.cleanup.deleted.count";
    private static final String AI_SUMMARY_GENERATED_COUNT = "ganaderia.ai.summary.generated.count";
    private static final String AI_SUMMARY_FALLBACK_COUNT = "ganaderia.ai.summary.fallback.count";
    private static final String AI_SUMMARY_PROVIDER_REQUEST_COUNT = "ganaderia.ai.summary.provider.request.count";
    private static final String NOTIFICATION_OUTBOX_EMAIL_SENT_COUNT = "ganaderia.notification.outbox.email.sent.count";
    private static final String NOTIFICATION_OUTBOX_EMAIL_FAILED_COUNT = "ganaderia.notification.outbox.email.failed.count";
    private static final String NOTIFICATION_OUTBOX_EMAIL_DEAD_COUNT = "ganaderia.notification.outbox.email.dead.count";

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public DomainMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementAlertCreated(AlertType type) {
        counterWithAlertType(ALERTS_CREATED, type).increment();
    }

    public void incrementAlertResolved(AlertType type) {
        counterWithAlertType(ALERTS_RESOLVED, type).increment();
    }

    public void incrementAlertDiscarded(AlertType type) {
        counterWithAlertType(ALERTS_DISCARDED, type).increment();
    }

    public void incrementCollarMarkedOffline() {
        counterWithoutTags(COLLARS_MARKED_OFFLINE).increment();
    }

    public void incrementDeviceRequestAccepted() {
        counterWithoutTags(DEVICE_REQUESTS_ACCEPTED).increment();
    }

    public void incrementDeviceRequestRejected(String reason) {
        String reasonValue = reason != null && !reason.isBlank() ? reason : "UNKNOWN";
        String key = DEVICE_REQUESTS_REJECTED + "|reason=" + reasonValue;

        counters.computeIfAbsent(key, ignored ->
                Counter.builder(DEVICE_REQUESTS_REJECTED)
                        .description("Contador de solicitudes de dispositivo rechazadas")
                        .tag("reason", reasonValue)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementNotificationSent(String channel, String eventType) {
        counterWithNotificationTags(NOTIFICATIONS_SENT, channel, eventType).increment();
    }

    public void incrementNotificationFailed(String channel, String eventType) {
        counterWithNotificationTags(NOTIFICATIONS_FAILED, channel, eventType).increment();
    }

    public void incrementNotificationQueued(String channel, String eventType) {
        counterWithNotificationTags(NOTIFICATIONS_QUEUED, channel, eventType).increment();
    }

    public void incrementNotificationRetried(String channel, String eventType) {
        counterWithNotificationTags(NOTIFICATIONS_RETRIED, channel, eventType).increment();
    }

    public void incrementGpsAccuracyQuality(GpsAccuracyQuality quality) {
        String qualityValue = quality != null ? quality.name() : GpsAccuracyQuality.UNKNOWN.name();
        String key = GPS_ACCURACY_QUALITY_COUNT + "|quality=" + qualityValue;

        counters.computeIfAbsent(key, ignored ->
                Counter.builder(GPS_ACCURACY_QUALITY_COUNT)
                        .description("Contador de calidad GPS de telemetria recibida")
                        .tag("quality", qualityValue)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementDeviceReplayNonceCleanupDeleted(long deletedCount) {
        if (deletedCount <= 0) {
            return;
        }

        counters.computeIfAbsent(DEVICE_REPLAY_NONCE_CLEANUP_DELETED_COUNT, ignored ->
                Counter.builder(DEVICE_REPLAY_NONCE_CLEANUP_DELETED_COUNT)
                        .description("Cantidad de nonces anti-replay eliminados por limpiezas programadas")
                        .register(meterRegistry)
        ).increment(deletedCount);
    }

    public void incrementAbuseRateLimitCleanupDeleted(long deletedCount) {
        if (deletedCount <= 0) {
            return;
        }

        counters.computeIfAbsent(ABUSE_RATE_LIMIT_CLEANUP_DELETED_COUNT, ignored ->
                Counter.builder(ABUSE_RATE_LIMIT_CLEANUP_DELETED_COUNT)
                        .description("Cantidad de entradas de abuse rate limit eliminadas por limpiezas programadas")
                        .register(meterRegistry)
        ).increment(deletedCount);
    }

    public void incrementPasswordResetCleanupDeleted(long deletedCount) {
        if (deletedCount <= 0) {
            return;
        }

        counters.computeIfAbsent(PASSWORD_RESET_CLEANUP_DELETED_COUNT, ignored ->
                Counter.builder(PASSWORD_RESET_CLEANUP_DELETED_COUNT)
                        .description("Cantidad de tokens de recuperacion eliminados por limpiezas programadas")
                        .register(meterRegistry)
        ).increment(deletedCount);
    }

    public void incrementAiSummaryGenerated(String source, String riskLevel) {
        String sourceValue = normalizeTagValue(source);
        String riskLevelValue = normalizeTagValue(riskLevel);
        String key = AI_SUMMARY_GENERATED_COUNT + "|source=" + sourceValue + "|riskLevel=" + riskLevelValue;

        counters.computeIfAbsent(key, ignored ->
                Counter.builder(AI_SUMMARY_GENERATED_COUNT)
                        .description("Cantidad de resúmenes operativos generados por el módulo de IA")
                        .tag("source", sourceValue)
                        .tag("riskLevel", riskLevelValue)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementAiSummaryFallback(String reason) {
        String reasonValue = normalizeTagValue(reason);
        String key = AI_SUMMARY_FALLBACK_COUNT + "|reason=" + reasonValue;

        counters.computeIfAbsent(key, ignored ->
                Counter.builder(AI_SUMMARY_FALLBACK_COUNT)
                        .description("Cantidad de activaciones del fallback heurístico del módulo de IA")
                        .tag("reason", reasonValue)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementAiProviderRequest(String provider, String outcome) {
        String providerValue = normalizeTagValue(provider);
        String outcomeValue = normalizeTagValue(outcome);
        String key = AI_SUMMARY_PROVIDER_REQUEST_COUNT + "|provider=" + providerValue + "|outcome=" + outcomeValue;

        counters.computeIfAbsent(key, ignored ->
                Counter.builder(AI_SUMMARY_PROVIDER_REQUEST_COUNT)
                        .description("Cantidad de solicitudes realizadas al proveedor de IA")
                        .tag("provider", providerValue)
                        .tag("outcome", outcomeValue)
                        .register(meterRegistry)
        ).increment();
    }

    public void incrementNotificationOutboxEmailSent(long count) {
        incrementCounterByAmount(NOTIFICATION_OUTBOX_EMAIL_SENT_COUNT, count,
                "Cantidad de mensajes EMAIL del outbox enviados exitosamente");
    }

    public void incrementNotificationOutboxEmailFailed(long count) {
        incrementCounterByAmount(NOTIFICATION_OUTBOX_EMAIL_FAILED_COUNT, count,
                "Cantidad de mensajes EMAIL del outbox que fallaron y quedaron reintentables");
    }

    public void incrementNotificationOutboxEmailDead(long count) {
        incrementCounterByAmount(NOTIFICATION_OUTBOX_EMAIL_DEAD_COUNT, count,
                "Cantidad de mensajes EMAIL del outbox marcados como irrecuperables");
    }

    private Counter counterWithAlertType(String metricName, AlertType type) {
        String typeValue = type != null ? type.name() : "UNKNOWN";
        String key = metricName + "|type=" + typeValue;

        return counters.computeIfAbsent(key, ignored ->
                Counter.builder(metricName)
                        .description("Contador de eventos de alertas del dominio")
                        .tag("type", typeValue)
                        .register(meterRegistry)
        );
    }

    private Counter counterWithNotificationTags(String metricName, String channel, String eventType) {
        String channelValue = normalizeTagValue(channel);
        String eventTypeValue = normalizeTagValue(eventType);
        String key = metricName + "|channel=" + channelValue + "|eventType=" + eventTypeValue;

        return counters.computeIfAbsent(key, ignored ->
                Counter.builder(metricName)
                        .description("Contador de notificaciones procesadas por canal")
                        .tag("channel", channelValue)
                        .tag("eventType", eventTypeValue)
                        .register(meterRegistry)
        );
    }

    private Counter counterWithoutTags(String metricName) {
        return counters.computeIfAbsent(metricName, ignored ->
                Counter.builder(metricName)
                        .description("Contador de eventos operativos del dominio")
                        .register(meterRegistry)
        );
    }

    private void incrementCounterByAmount(String metricName, long count, String description) {
        if (count <= 0) {
            return;
        }

        counters.computeIfAbsent(metricName, ignored ->
                Counter.builder(metricName)
                        .description(description)
                        .register(meterRegistry)
        ).increment(count);
    }

    private String normalizeTagValue(String value) {
        return value != null && !value.isBlank() ? value : "UNKNOWN";
    }
}
