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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DeviceMonitoringServiceTest {

    @Test
    void shouldLogOfflineMonitorSummary(CapturedOutput output) {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry())
        );
        Collar collarToMark = collar(DeviceSignalStatus.MEDIA);
        Collar alreadyOffline = collar(DeviceSignalStatus.SIN_SENAL);

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(org.mockito.ArgumentMatchers.eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenReturn(List.of(collarToMark, alreadyOffline));

        service.monitorOfflineCollars();

        String logs = output.getOut();

        assertTrue(logs.contains("event=device_offline_monitor_completed"));
        assertTrue(logs.contains("result=success"));
        assertTrue(logs.contains("processed=2"));
        assertTrue(logs.contains("markedOffline=1"));
        assertTrue(logs.contains("alertsRequested=2"));
        assertTrue(logs.contains("durationMs="));
        verify(alertService).createCollarOfflineAlert(collarToMark);
        verify(alertService).createCollarOfflineAlert(alreadyOffline);
    }

    @Test
    void shouldLogOfflineMonitorFailureWithStacktrace(CapturedOutput output) {
        CollarRepository collarRepository = mock(CollarRepository.class);
        AlertService alertService = mock(AlertService.class);
        DeviceMonitoringService service = new DeviceMonitoringService(
                collarRepository,
                alertService,
                new DomainMetricsService(new SimpleMeterRegistry())
        );

        when(collarRepository.findByEnabledTrueAndStatusAndLastSeenAtBefore(org.mockito.ArgumentMatchers.eq(CollarStatus.ACTIVO), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, service::monitorOfflineCollars);

        String logs = output.getOut() + output.getErr();

        assertTrue(logs.contains("event=device_offline_monitor_completed"));
        assertTrue(logs.contains("result=failure"));
        assertTrue(logs.contains("processed=0"));
        assertTrue(logs.contains("markedOffline=0"));
        assertTrue(logs.contains("alertsRequested=0"));
        assertTrue(logs.contains("errorType=IllegalStateException"));
        assertTrue(logs.contains("database_unavailable"));
        assertTrue(logs.contains("java.lang.IllegalStateException"));
    }

    private Collar collar(DeviceSignalStatus signalStatus) {
        Collar collar = new Collar();
        collar.setSignalStatus(signalStatus);
        return collar;
    }
}
