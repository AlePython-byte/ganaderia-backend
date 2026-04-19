package com.ganaderia4.backend.observability;

import com.ganaderia4.backend.model.AlertType;
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
