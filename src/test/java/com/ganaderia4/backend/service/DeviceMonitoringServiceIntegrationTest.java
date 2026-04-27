package com.ganaderia4.backend.service;

import com.ganaderia4.backend.model.Alert;
import com.ganaderia4.backend.model.AlertStatus;
import com.ganaderia4.backend.model.AlertType;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.AuditLogRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceMonitoringServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DeviceMonitoringService deviceMonitoringService;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        auditLogRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
    }

    @Test
    void shouldMarkOfflineCollarAndCreatePendingOfflineAlert() {
        Cow cow = createCow("VACA-OFF-001", "Luna");
        Collar collar = createCollar(
                "COLLAR-OFF-001",
                cow,
                LocalDateTime.now().minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.SIN_SENAL, updatedCollar.getSignalStatus());

        List<Alert> alerts = alertRepository.findAll();
        assertEquals(1, alerts.size());

        Alert alert = alerts.get(0);
        assertEquals(AlertType.COLLAR_OFFLINE, alert.getType());
        assertEquals(AlertStatus.PENDIENTE, alert.getStatus());
        assertEquals(cow.getId(), alert.getCow().getId());
        assertTrue(alert.getMessage().contains("COLLAR-OFF-001"));
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void shouldNotCreateDuplicatePendingOfflineAlertForSameCow() {
        Cow cow = createCow("VACA-OFF-002", "Canela");
        Collar collar = createCollar(
                "COLLAR-OFF-002",
                cow,
                LocalDateTime.now().minusMinutes(50),
                DeviceSignalStatus.MEDIA,
                true
        );

        Alert existing = new Alert();
        existing.setCow(cow);
        existing.setType(AlertType.COLLAR_OFFLINE);
        existing.setStatus(AlertStatus.PENDIENTE);
        existing.setMessage("Alerta previa");
        existing.setObservations("ya existía");
        existing.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        alertRepository.save(existing);

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.SIN_SENAL, updatedCollar.getSignalStatus());

        List<Alert> alerts = alertRepository.findAll();
        assertEquals(1, alerts.size());
        assertEquals("Alerta previa", alerts.get(0).getMessage());
    }

    @Test
    void shouldIgnoreRecentEnabledCollars() {
        Cow cow = createCow("VACA-OFF-003", "Estrella");
        Collar collar = createCollar(
                "COLLAR-OFF-003",
                cow,
                LocalDateTime.now().minusMinutes(3),
                DeviceSignalStatus.FUERTE,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.FUERTE, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldIgnoreInactiveCollarsDuringOfflineMonitoring() {
        Cow cow = createCow("VACA-OFF-004", "Nube");
        Collar collar = createCollar(
                "COLLAR-OFF-004",
                cow,
                LocalDateTime.now().minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true,
                CollarStatus.INACTIVO
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldIgnoreMaintenanceCollarsDuringOfflineMonitoring() {
        Cow cow = createCow("VACA-OFF-005", "Brisa");
        Collar collar = createCollar(
                "COLLAR-OFF-005",
                cow,
                LocalDateTime.now().minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true,
                CollarStatus.MANTENIMIENTO
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    private Cow createCow(String token, String name) {
        Cow cow = new Cow();
        cow.setToken(token);
        cow.setInternalCode("INT-" + token);
        cow.setName(name);
        cow.setStatus(CowStatus.DENTRO);
        return cowRepository.save(cow);
    }

    private Collar createCollar(String token,
                                Cow cow,
                                LocalDateTime lastSeenAt,
                                DeviceSignalStatus signalStatus,
                                boolean enabled) {
        return createCollar(token, cow, lastSeenAt, signalStatus, enabled, CollarStatus.ACTIVO);
    }

    private Collar createCollar(String token,
                                Cow cow,
                                LocalDateTime lastSeenAt,
                                DeviceSignalStatus signalStatus,
                                boolean enabled,
                                CollarStatus status) {
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(status);
        collar.setBatteryLevel(80);
        collar.setLastSeenAt(lastSeenAt);
        collar.setSignalStatus(signalStatus);
        collar.setEnabled(enabled);
        return collarRepository.save(collar);
    }
}
