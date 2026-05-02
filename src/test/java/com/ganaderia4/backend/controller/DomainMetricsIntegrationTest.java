package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Collar;
import com.ganaderia4.backend.model.CollarStatus;
import com.ganaderia4.backend.model.Cow;
import com.ganaderia4.backend.model.CowStatus;
import com.ganaderia4.backend.model.DeviceSignalStatus;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationMessage;
import com.ganaderia4.backend.notification.NotificationService;
import com.ganaderia4.backend.repository.AlertRepository;
import com.ganaderia4.backend.repository.CollarRepository;
import com.ganaderia4.backend.repository.CowRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.service.AlertService;
import com.ganaderia4.backend.service.DeviceMonitoringService;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(DomainMetricsIntegrationTest.FailingNotificationConfig.class)
class DomainMetricsIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class FailingNotificationConfig {

        @Bean
        NotificationService failingNotificationService() {
            return new NotificationService() {
                @Override
                public NotificationChannel getChannel() {
                    return NotificationChannel.LOG;
                }

                @Override
                public com.ganaderia4.backend.notification.NotificationSendResult send(NotificationMessage notificationMessage) {
                    throw new RuntimeException("fallo simulado de canal");
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private CollarRepository collarRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private DeviceMonitoringService deviceMonitoringService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        collarRepository.deleteAll();
        cowRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
    }

    @Test
    void shouldExposeCreatedAndResolvedAlertMetrics() throws Exception {
        Cow cow = createCow("VACA-MET-001", "Luna");
        Collar collar = createCollar(
                "COLLAR-MET-001",
                cow,
                LocalDateTime.now().minusMinutes(30),
                DeviceSignalStatus.MEDIA,
                true
        );

        var createdAlert = alertService.createCollarOfflineAlert(collar);
        alertService.resolveAlert(createdAlert.getId(), "resuelta en prueba");

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/actuator/metrics/ganaderia.alerts.created")
                        .param("tag", "type:COLLAR_OFFLINE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.alerts.created"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));

        mockMvc.perform(get("/actuator/metrics/ganaderia.alerts.resolved")
                        .param("tag", "type:COLLAR_OFFLINE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.alerts.resolved"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void shouldExposeDiscardedAlertMetric() throws Exception {
        Cow cow = createCow("VACA-MET-002", "Canela");
        Collar collar = createCollar(
                "COLLAR-MET-002",
                cow,
                LocalDateTime.now().minusMinutes(45),
                DeviceSignalStatus.MEDIA,
                true
        );

        var createdAlert = alertService.createCollarOfflineAlert(collar);
        alertService.discardAlert(createdAlert.getId(), "descartada en prueba");

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/actuator/metrics/ganaderia.alerts.discarded")
                        .param("tag", "type:COLLAR_OFFLINE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.alerts.discarded"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void shouldExposeCollarsMarkedOfflineMetric() throws Exception {
        Cow cow = createCow("VACA-MET-003", "Estrella");
        createCollar(
                "COLLAR-MET-003",
                cow,
                LocalDateTime.now().minusMinutes(50),
                DeviceSignalStatus.FUERTE,
                true
        );

        deviceMonitoringService.monitorOfflineCollars();

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/actuator/metrics/ganaderia.collars.marked_offline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.collars.marked_offline"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void shouldExposeNotificationSentMetricWhenOfflineAlertIsCreated() throws Exception {
        Cow cow = createCow("VACA-MET-004", "Aurora");
        Collar collar = createCollar(
                "COLLAR-MET-004",
                cow,
                LocalDateTime.now().minusMinutes(35),
                DeviceSignalStatus.MEDIA,
                true
        );

        alertService.createCollarOfflineAlert(collar);

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/actuator/metrics/ganaderia.notifications.sent")
                        .param("tag", "channel:LOG")
                        .param("tag", "eventType:CRITICAL_ALERT_CREATED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.notifications.sent"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    @Test
    void shouldExposeNotificationFailedMetricWhenNotificationChannelFails() throws Exception {
        Cow cow = createCow("VACA-MET-005", "Nube");
        Collar collar = createCollar(
                "COLLAR-MET-005",
                cow,
                LocalDateTime.now().minusMinutes(35),
                DeviceSignalStatus.MEDIA,
                true
        );

        alertService.createCollarOfflineAlert(collar);

        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/actuator/metrics/ganaderia.notifications.failed")
                        .param("tag", "channel:LOG")
                        .param("tag", "eventType:CRITICAL_ALERT_CREATED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ganaderia.notifications.failed"))
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(1.0)));
    }

    private void createUser(String name, String email, String rawPassword, Role role, boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(active);
        userRepository.save(user);
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

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
