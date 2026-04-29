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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(DeviceMonitoringServiceIntegrationTest.FixedClockConfig.class)
class DeviceMonitoringServiceIntegrationTest extends AbstractIntegrationTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-04-28T18:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime FIXED_NOW = LocalDateTime.now(FIXED_CLOCK);

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
                FIXED_NOW.minusMinutes(40),
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
                FIXED_NOW.minusMinutes(50),
                DeviceSignalStatus.MEDIA,
                true
        );

        Alert existing = new Alert();
        existing.setCow(cow);
        existing.setType(AlertType.COLLAR_OFFLINE);
        existing.setStatus(AlertStatus.PENDIENTE);
        existing.setMessage("Alerta previa");
        existing.setObservations("ya existia");
        existing.setCreatedAt(FIXED_NOW.minusMinutes(10));
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
                FIXED_NOW.minusMinutes(3),
                DeviceSignalStatus.FUERTE,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.FUERTE, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldIgnoreCollarExactlyAtOfflineThreshold() {
        Cow cow = createCow("VACA-OFF-004", "Aura");
        Collar collar = createCollar(
                "COLLAR-OFF-004",
                cow,
                FIXED_NOW.minusMinutes(15),
                DeviceSignalStatus.MEDIA,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldIgnoreCollarOneMinuteBeforeOfflineThreshold() {
        Cow cow = createCow("VACA-OFF-005", "Sol");
        Collar collar = createCollar(
                "COLLAR-OFF-005",
                cow,
                FIXED_NOW.minusMinutes(14),
                DeviceSignalStatus.MEDIA,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldProcessCollarOneMinuteAfterOfflineThreshold() {
        Cow cow = createCow("VACA-OFF-006", "Rocio");
        Collar collar = createCollar(
                "COLLAR-OFF-006",
                cow,
                FIXED_NOW.minusMinutes(16),
                DeviceSignalStatus.MEDIA,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.SIN_SENAL, updatedCollar.getSignalStatus());
        assertEquals(1, alertRepository.count());
    }

    @Test
    void shouldIgnoreInactiveCollarsDuringOfflineMonitoring() {
        Cow cow = createCow("VACA-OFF-007", "Nube");
        Collar collar = createCollar(
                "COLLAR-OFF-007",
                cow,
                FIXED_NOW.minusMinutes(40),
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
        Cow cow = createCow("VACA-OFF-008", "Brisa");
        Collar collar = createCollar(
                "COLLAR-OFF-008",
                cow,
                FIXED_NOW.minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true,
                CollarStatus.MANTENIMIENTO
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldIgnoreDisabledCollarsDuringOfflineMonitoring() {
        Cow cow = createCow("VACA-OFF-009", "Perla");
        Collar collar = createCollar(
                "COLLAR-OFF-009",
                cow,
                FIXED_NOW.minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                false
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar updatedCollar = collarRepository.findById(collar.getId()).orElseThrow();
        assertEquals(DeviceSignalStatus.MEDIA, updatedCollar.getSignalStatus());
        assertEquals(0, alertRepository.count());
    }

    @Test
    void shouldNotDependOnServerRealTime() {
        Cow freshCow = createCow("VACA-OFF-010", "Menta");
        Cow staleCow = createCow("VACA-OFF-011", "Lira");

        Collar fresh = createCollar(
                "COLLAR-OFF-010",
                freshCow,
                FIXED_NOW.minusMinutes(14),
                DeviceSignalStatus.MEDIA,
                true
        );
        Collar stale = createCollar(
                "COLLAR-OFF-011",
                staleCow,
                FIXED_NOW.minusMinutes(16),
                DeviceSignalStatus.MEDIA,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        Collar freshUpdated = collarRepository.findById(fresh.getId()).orElseThrow();
        Collar staleUpdated = collarRepository.findById(stale.getId()).orElseThrow();

        assertEquals(DeviceSignalStatus.MEDIA, freshUpdated.getSignalStatus());
        assertEquals(DeviceSignalStatus.SIN_SENAL, staleUpdated.getSignalStatus());
        assertEquals(1, alertRepository.count());
        assertFalse(alertRepository.findAll().isEmpty());
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

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return FIXED_CLOCK;
        }
    }
}
