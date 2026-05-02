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

    private String normalizeTagValue(String value) {
        return value != null && !value.isBlank() ? value : "UNKNOWN";
    }
}
