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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMonitoringService.class);

    private final CollarRepository collarRepository;
    private final AlertService alertService;
    private final DomainMetricsService domainMetricsService;

    @Value("${app.device-monitor.offline-threshold-minutes:15}")
    private long offlineThresholdMinutes;

    public DeviceMonitoringService(CollarRepository collarRepository,
                                   AlertService alertService,
                                   DomainMetricsService domainMetricsService) {
        this.collarRepository = collarRepository;
        this.alertService = alertService;
        this.domainMetricsService = domainMetricsService;
    }

    @Scheduled(fixedDelayString = "${app.device-monitor.offline-check-ms:60000}")
    @Transactional
    public void monitorOfflineCollars() {
        long startedAt = System.nanoTime();
        int processed = 0;
        int markedOffline = 0;
        int alertsRequested = 0;

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(offlineThresholdMinutes);
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

            logMonitorSummary("success", processed, markedOffline, alertsRequested, elapsedMs(startedAt));
        } catch (RuntimeException ex) {
            log.error(
                    "event=device_offline_monitor_completed result=failure processed={} markedOffline={} alertsRequested={} durationMs={} errorType={} error={}",
                    processed,
                    markedOffline,
                    alertsRequested,
                    elapsedMs(startedAt),
                    ex.getClass().getSimpleName(),
                    OperationalLogSanitizer.safe(ex.getMessage()),
                    ex
            );
            throw ex;
        }
    }

    private void logMonitorSummary(String result,
                                   int processed,
                                   int markedOffline,
                                   int alertsRequested,
                                   long durationMs) {
        String message = "event=device_offline_monitor_completed result={} processed={} markedOffline={} "
                + "alertsRequested={} durationMs={}";

        if (processed == 0 && markedOffline == 0 && alertsRequested == 0) {
            log.debug(message, result, processed, markedOffline, alertsRequested, durationMs);
            return;
        }

        log.info(message, result, processed, markedOffline, alertsRequested, durationMs);
    }

    private long elapsedMs(long startedAt) {
        return java.time.Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
