package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.notification.NotificationMessage;
import com.ganaderia4.backend.notification.RecordingNotificationService;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.service.AlertService;
import com.ganaderia4.backend.service.DeviceMonitoringService;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(NotificationPipelineIntegrationTest.TestNotificationConfig.class)
class NotificationPipelineIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestNotificationConfig {
        @Bean
        RecordingNotificationService recordingNotificationService() {
            return new RecordingNotificationService();
        }
    }

    @Autowired
    private AlertService alertService;

    @Autowired
    private DeviceMonitoringService deviceMonitoringService;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RecordingNotificationService recordingNotificationService;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
        recordingNotificationService.clear();
    }

    @Test
    void shouldDispatchNotificationWhenCreatingOfflineAlert() {
        Cow cow = createCow("VACA-NOTIF-001", "Luna");
        Collar collar = createCollar(
                "COLLAR-NOTIF-001",
                cow,
                LocalDateTime.now().minusMinutes(40),
                DeviceSignalStatus.MEDIA,
                true
        );

        var alert = alertService.createCollarOfflineAlert(collar);

        assertNotNull(alert);

        List<NotificationMessage> messages = recordingNotificationService.getSentMessages();
        assertEquals(1, messages.size());

        NotificationMessage notification = messages.get(0);
        assertEquals("CRITICAL_ALERT_CREATED", notification.getEventType());
        assertEquals("Nueva alerta crítica", notification.getTitle());
        assertEquals("HIGH", notification.getSeverity());
        assertEquals("COLLAR_OFFLINE", notification.getMetadata().get("alertType"));
        assertEquals("VACA-NOTIF-001", notification.getMetadata().get("cowToken"));
        assertEquals("Luna", notification.getMetadata().get("cowName"));
        assertEquals(String.valueOf(alert.getId()), notification.getMetadata().get("alertId"));
    }

    @Test
    void shouldDispatchNotificationWhenOfflineMonitoringCreatesCriticalAlert() {
        Cow cow = createCow("VACA-NOTIF-002", "Canela");
        createCollar(
                "COLLAR-NOTIF-002",
                cow,
                LocalDateTime.now().minusMinutes(50),
                DeviceSignalStatus.FUERTE,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        List<NotificationMessage> messages = recordingNotificationService.getSentMessages();
        assertEquals(1, messages.size());

        NotificationMessage notification = messages.get(0);
        assertEquals("CRITICAL_ALERT_CREATED", notification.getEventType());
        assertEquals("HIGH", notification.getSeverity());
        assertEquals("COLLAR_OFFLINE", notification.getMetadata().get("alertType"));
        assertEquals("VACA-NOTIF-002", notification.getMetadata().get("cowToken"));
    }

    @Test
    void shouldNotDispatchDuplicateNotificationWhenPendingOfflineAlertAlreadyExists() {
        Cow cow = createCow("VACA-NOTIF-003", "Estrella");
        Collar collar = createCollar(
                "COLLAR-NOTIF-003",
                cow,
                LocalDateTime.now().minusMinutes(55),
                DeviceSignalStatus.MEDIA,
                true
        );

        var firstAlert = alertService.createCollarOfflineAlert(collar);
        assertNotNull(firstAlert);
        assertEquals(1, recordingNotificationService.getSentMessages().size());

        var duplicated = alertService.createCollarOfflineAlert(collar);

        assertNull(duplicated);
        assertEquals(1, recordingNotificationService.getSentMessages().size());
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
        Collar collar = new Collar();
        collar.setToken(token);
        collar.setCow(cow);
        collar.setStatus(CollarStatus.ACTIVO);
        collar.setBatteryLevel(80);
        collar.setLastSeenAt(lastSeenAt);
        collar.setSignalStatus(signalStatus);
        collar.setEnabled(enabled);
        return collarRepository.save(collar);
    }
}