package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxMessage;
import com.ganaderia4.backend.notification.NotificationOutboxStatus;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNotificationOutboxControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long existingMessageId;

    @BeforeEach
    void setUp() {
        notificationOutboxRepository.deleteAll();
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);

        existingMessageId = notificationOutboxRepository.save(emailMessage(
                NotificationOutboxStatus.FAILED,
                """
                {"provider":"resend","to":"admin@test.com","subject":"Reset","textBody":"Texto muy largo sensible","htmlBody":"<p>Html sensible</p>","token":"reset-secret-token-value"}
                """,
                "x".repeat(260)
        )).getId();

        notificationOutboxRepository.save(webhookMessage(NotificationOutboxStatus.SENT));
    }

    @Test
    void shouldRejectListWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox"));
    }

    @Test
    void shouldRejectListForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox"));
    }

    @Test
    void shouldAllowAdminToListOutboxMessages() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].recipientMasked").exists())
                .andExpect(jsonPath("$.content[0].lastErrorSummary").exists());
    }

    @Test
    void shouldFilterListByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("status", "FAILED")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    void shouldFilterListByChannel() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("channel", "WEBHOOK")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].channel").value("WEBHOOK"));
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("status", "BROKEN")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox"));
    }

    @Test
    void shouldRejectInvalidChannelFilter() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("channel", "BROKEN")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox"));
    }

    @Test
    void shouldRejectDetailWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox/{id}", existingMessageId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + existingMessageId));
    }

    @Test
    void shouldRejectDetailForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox/{id}", existingMessageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + existingMessageId));
    }

    @Test
    void shouldReturnNotFoundForMissingDetail() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox/{id}", 999999L)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/999999"));
    }

    @Test
    void shouldReturnSafeDetailForAdmin() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/notification-outbox/{id}", existingMessageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingMessageId))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.recipientMasked").value("a***@test.com"))
                .andExpect(jsonPath("$.payload").doesNotExist())
                .andExpect(jsonPath("$.payloadSize").isNumber())
                .andExpect(jsonPath("$.payloadPreview").exists())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        String payloadPreview = json.get("payloadPreview").asText();
        String lastErrorSummary = json.get("lastErrorSummary").asText();

        assertTrue(payloadPreview.contains("[REDACTED]"));
        assertFalse(payloadPreview.contains("Texto muy largo sensible"));
        assertFalse(payloadPreview.contains("<p>Html sensible</p>"));
        assertFalse(payloadPreview.contains("reset-secret-token-value"));
        assertTrue(lastErrorSummary.length() <= 200);
    }

    private NotificationOutboxMessage emailMessage(NotificationOutboxStatus status, String payload, String lastError) {
        NotificationOutboxMessage message = new NotificationOutboxMessage();
        message.setChannel(NotificationChannel.EMAIL);
        message.setStatus(status);
        message.setEventType("PASSWORD_RESET_REQUESTED");
        message.setRecipient("admin@test.com");
        message.setSubject("Reset password");
        message.setPayload(payload);
        message.setAttempts(1);
        message.setMaxAttempts(3);
        message.setNextAttemptAt(Instant.parse("2026-05-03T19:30:00Z"));
        message.setLastAttemptAt(Instant.parse("2026-05-03T19:30:00Z"));
        message.setFailedAt(Instant.parse("2026-05-03T19:31:00Z"));
        message.setLastError(lastError);
        message.setCreatedAt(Instant.parse("2026-05-03T19:00:00Z"));
        message.setUpdatedAt(Instant.parse("2026-05-03T19:31:00Z"));
        return message;
    }

    private NotificationOutboxMessage webhookMessage(NotificationOutboxStatus status) {
        NotificationOutboxMessage message = new NotificationOutboxMessage();
        message.setChannel(NotificationChannel.WEBHOOK);
        message.setStatus(status);
        message.setEventType("CRITICAL_ALERT_CREATED");
        message.setRecipient("https://example.test/webhook");
        message.setSubject("Webhook");
        message.setPayload("{\"destination\":\"https://example.test/webhook\"}");
        message.setAttempts(1);
        message.setMaxAttempts(3);
        message.setNextAttemptAt(Instant.parse("2026-05-03T18:30:00Z"));
        message.setSentAt(Instant.parse("2026-05-03T18:31:00Z"));
        message.setCreatedAt(Instant.parse("2026-05-03T18:00:00Z"));
        message.setUpdatedAt(Instant.parse("2026-05-03T18:31:00Z"));
        return message;
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
