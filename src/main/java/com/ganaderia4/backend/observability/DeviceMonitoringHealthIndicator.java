package com.ganaderia4.backend.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("deviceMonitoring")
public class DeviceMonitoringHealthIndicator implements HealthIndicator {

    @Value("${app.device-monitor.offline-threshold-minutes:15}")
    private long offlineThresholdMinutes;

    @Value("${app.device-monitor.offline-check-ms:60000}")
    private long offlineCheckMs;

    @Override
    public Health health() {
        boolean thresholdValid = offlineThresholdMinutes > 0;
        boolean delayValid = offlineCheckMs > 0;

        if (thresholdValid && delayValid) {
            return Health.up()
                    .withDetail("offlineThresholdMinutes", offlineThresholdMinutes)
                    .withDetail("offlineCheckMs", offlineCheckMs)
                    .withDetail("component", "deviceMonitoring")
                    .build();
        }

        return Health.down()
                .withDetail("offlineThresholdMinutes", offlineThresholdMinutes)
                .withDetail("offlineCheckMs", offlineCheckMs)
                .withDetail("reason", "Configuración inválida del monitoreo offline")
                .build();
    }
}