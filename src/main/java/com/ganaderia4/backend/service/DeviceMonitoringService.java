package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.CollarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMonitoringService.class);

    private final CollarRepository collarRepository;
    private final AlertService alertService;
    private final DomainMetricsService domainMetricsService;
    private final Clock clock;

    @Value("${app.device-monitor.offline-threshold-minutes:15}")
    private long offlineThresholdMinutes;

    public DeviceMonitoringService(CollarRepository collarRepository,
                                   AlertService alertService,
                                   DomainMetricsService domainMetricsService,
                                   Clock clock) {
        this.collarRepository = collarRepository;
        this.alertService = alertService;
        this.domainMetricsService = domainMetricsService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.device-monitor.offline-check-ms:60000}")
    @Transactional
    public void monitorOfflineCollars() {
        long startedAt = System.nanoTime();
        String requestId = OperationalLogSanitizer.requestIdOr("scheduled");
        int processed = 0;
        int markedOffline = 0;
        int alertsRequested = 0;
        int lowBatteryCreated = 0;
        int lowBatteryResolved = 0;

        try {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime threshold = now.minusMinutes(offlineThresholdMinutes);
            List<Collar> offlineCollars =
                    collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(CollarStatus.ACTIVO, threshold);

            for (Collar collar : offlineCollars) {
                processed++;
                boolean wasAlreadyOffline = collar.getSignalStatus() == DeviceSignalStatus.SIN_SENAL;

                collar.setSignalStatus(DeviceSignalStatus.SIN_SENAL);
                collarRepository.save(collar);

                if (!wasAlreadyOffline) {
                    domainMetricsService.incrementCollarMarkedOffline();
                    markedOffline++;
                }

                alertService.createCollarOfflineAlert(collar);
                alertsRequested++;
            }

            List<Collar> activeEnabledCollars = collarRepository.findByEnabledTrueAndStatus(CollarStatus.ACTIVO);
            for (Collar collar : activeEnabledCollars) {
                if (alertService.createLowBatteryAlert(collar) != null) {
                    lowBatteryCreated++;
                }

                if (alertService.resolvePendingLowBatteryAlert(collar, now) != null) {
                    lowBatteryResolved++;
                }
            }

            logMonitorSummary(
                    requestId,
                    processed,
                    markedOffline,
                    alertsRequested,
                    lowBatteryCreated,
                    lowBatteryResolved,
                    elapsedMs(startedAt)
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=offline_monitoring_failed requestId={} processed={} affected={} alertsRequested={} lowBatteryCreated={} lowBatteryResolved={} durationMs={} errorType={} error={}",
                    requestId,
                    processed,
                    markedOffline,
                    alertsRequested,
                    lowBatteryCreated,
                    lowBatteryResolved,
                    elapsedMs(startedAt),
                    ex.getClass().getSimpleName(),
                    OperationalLogSanitizer.safe(ex.getMessage()),
                    ex
            );
            throw ex;
        }
    }

    private void logMonitorSummary(String requestId,
                                   int processed,
                                   int markedOffline,
                                   int alertsRequested,
                                   int lowBatteryCreated,
                                   int lowBatteryResolved,
                                   long durationMs) {
        String message = "event=offline_monitoring_completed requestId={} processed={} affected={} "
                + "alertsRequested={} lowBatteryCreated={} lowBatteryResolved={} durationMs={}";

        if (processed == 0 && markedOffline == 0 && alertsRequested == 0
                && lowBatteryCreated == 0 && lowBatteryResolved == 0) {
            log.debug(message, requestId, processed, markedOffline, alertsRequested, lowBatteryCreated, lowBatteryResolved, durationMs);
            return;
        }

        log.info(message, requestId, processed, markedOffline, alertsRequested, lowBatteryCreated, lowBatteryResolved, durationMs);
    }

    private long elapsedMs(long startedAt) {
        return java.time.Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
