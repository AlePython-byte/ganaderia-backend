package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.CollarRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceMonitoringService {

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
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(offlineThresholdMinutes);

        List<Collar> offlineCollars = collarRepository.findByEnabledTrueAndLastSeenAtBefore(threshold);

        for (Collar collar : offlineCollars) {
            boolean wasAlreadyOffline = collar.getSignalStatus() == DeviceSignalStatus.SIN_SENAL;

            collar.setSignalStatus(DeviceSignalStatus.SIN_SENAL);
            collarRepository.save(collar);

            if (!wasAlreadyOffline) {
                domainMetricsService.incrementCollarMarkedOffline();
            }

            alertService.createCollarOfflineAlert(collar);
        }
    }
}