package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.observability.DomainMetricsService;
import com.ganaderia4.backend.repository.CollarRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DeviceMonitoringServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-04-28T18:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

    @Test
    void shouldLogOfflineMonitorSummary(CapturedOutput output) {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry()),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(service, "offlineThresholdMinutes", 15L);
        Collar collarToMark = collar(DeviceSignalStatus.MEDIA);
        Collar alreadyOffline = collar(DeviceSignalStatus.SIN_SENAL);

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenReturn(List.of(collarToMark, alreadyOffline));
        when(collarRepository.findByEnabledTrueAndStatus(CollarStatus.ACTIVO))
                .thenReturn(List.of(collarToMark, alreadyOffline));

        service.monitorOfflineCollars();

        String logs = output.getOut();

        assertTrue(logs.contains("event=offline_monitoring_completed"));
        assertTrue(logs.contains("requestId=scheduled"));
        assertTrue(logs.contains("processed=2"));
        assertTrue(logs.contains("affected=1"));
        assertTrue(logs.contains("alertsRequested=2"));
        assertTrue(logs.contains("lowBatteryCreated=0"));
        assertTrue(logs.contains("lowBatteryResolved=0"));
        assertTrue(logs.contains("durationMs="));
        verify(collarRepository).findByEnabledTrueAndStatusAndLastSeenAtBefore(
                CollarStatus.ACTIVO,
                FIXED_NOW.minusMinutes(15)
        );
        verify(alertService).createCollarOfflineAlert(collarToMark);
        verify(alertService).createCollarOfflineAlert(alreadyOffline);
    }

    @Test
    void shouldEvaluateLowBatteryAlertsDuringMonitoring(CapturedOutput output) {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry()),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(service, "offlineThresholdMinutes", 15L);

        Collar lowBattery = collar(DeviceSignalStatus.MEDIA);
        lowBattery.setBatteryLevel(15);
        Collar recoveredBattery = collar(DeviceSignalStatus.FUERTE);
        recoveredBattery.setBatteryLevel(30);
        Collar inRangeBattery = collar(DeviceSignalStatus.FUERTE);
        inRangeBattery.setBatteryLevel(23);

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(collarRepository.findByEnabledTrueAndStatus(CollarStatus.ACTIVO))
                .thenReturn(List.of(lowBattery, recoveredBattery, inRangeBattery));
        when(alertService.createLowBatteryAlert(any(Collar.class))).thenReturn(null);
        when(alertService.resolvePendingLowBatteryAlert(any(Collar.class), eq(FIXED_NOW))).thenReturn(null);
        doReturn(new com.ganaderia4.backend.model.Alert()).when(alertService).createLowBatteryAlert(same(lowBattery));
        doReturn(new com.ganaderia4.backend.model.Alert()).when(alertService).resolvePendingLowBatteryAlert(same(recoveredBattery), eq(FIXED_NOW));

        service.monitorOfflineCollars();

        String logs = output.getOut();
        assertTrue(logs.contains("lowBatteryCreated=1"));
        assertTrue(logs.contains("lowBatteryResolved=1"));
        verify(alertService).createLowBatteryAlert(lowBattery);
        verify(alertService).resolvePendingLowBatteryAlert(lowBattery, FIXED_NOW);
        verify(alertService).createLowBatteryAlert(recoveredBattery);
        verify(alertService).resolvePendingLowBatteryAlert(recoveredBattery, FIXED_NOW);
        verify(alertService).createLowBatteryAlert(inRangeBattery);
        verify(alertService).resolvePendingLowBatteryAlert(inRangeBattery, FIXED_NOW);
    }

    @Test
    void shouldLogOfflineMonitorFailureWithStacktrace(CapturedOutput output) {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry()),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(service, "offlineThresholdMinutes", 15L);

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, service::monitorOfflineCollars);

        String logs = output.getOut() + output.getErr();

        assertTrue(logs.contains("event=offline_monitoring_failed"));
        assertTrue(logs.contains("requestId=scheduled"));
        assertTrue(logs.contains("processed=0"));
        assertTrue(logs.contains("affected=0"));
        assertTrue(logs.contains("alertsRequested=0"));
        assertTrue(logs.contains("lowBatteryCreated=0"));
        assertTrue(logs.contains("lowBatteryResolved=0"));
        assertTrue(logs.contains("errorType=IllegalStateException"));
        assertTrue(logs.contains("database_unavailable"));
        assertTrue(logs.contains("java.lang.IllegalStateException"));
    }

    @Test
    void shouldQueryUsingExactFixedOfflineThreshold() {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry()),
                FIXED_CLOCK
        );
        ReflectionTestUtils.setField(service, "offlineThresholdMinutes", 15L);

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(collarRepository.findByEnabledTrueAndStatus(CollarStatus.ACTIVO))
                .thenReturn(List.of());

        service.monitorOfflineCollars();

        verify(collarRepository).findByEnabledTrueAndStatusAndLastSeenAtBefore(
                CollarStatus.ACTIVO,
                FIXED_NOW.minusMinutes(15)
        );
        verify(collarRepository).findByEnabledTrueAndStatus(CollarStatus.ACTIVO);
    }

    private Collar collar(DeviceSignalStatus signalStatus) {
        Collar collar = new Collar();
        collar.setSignalStatus(signalStatus);
        return collar;
    }
}
