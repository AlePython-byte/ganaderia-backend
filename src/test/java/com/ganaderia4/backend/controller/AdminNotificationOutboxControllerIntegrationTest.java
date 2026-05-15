package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.notification.NotificationChannel;
import com.ganaderia4.backend.notification.NotificationOutboxMessage;
import com.ganaderia4.backend.notification.NotificationOutboxStatus;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import com.ganaderia4.backend.repository.NotificationOutboxRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.abuse-protection.outbox-requeue.max-attempts=2",
        "app.abuse-protection.outbox-requeue.window=10m",
        "app.abuse-protection.outbox-requeue.block-duration=5m"
})
class AdminNotificationOutboxControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private AbuseRateLimitRepository abuseRateLimitRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long existingMessageId;

    @BeforeEach
    void setUp() {
        abuseRateLimitRepository.deleteAll();
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
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.sort").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.content[0].recipientMasked").exists())
                .andExpect(jsonPath("$.content[0].lastErrorSummary").exists())
                .andExpect(jsonPath("$.content[0].payload").doesNotExist())
                .andExpect(jsonPath("$.content[0].payloadPreview").doesNotExist())
                .andExpect(jsonPath("$.content[0].payloadSize").doesNotExist());
    }

    @Test
    void shouldFilterListByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("status", "FAILED")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));
    }

    @Test
    void shouldFilterListByChannel() throws Exception {
        mockMvc.perform(get("/api/admin/notification-outbox")
                        .param("channel", "WEBHOOK")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].channel").value("WEBHOOK"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.numberOfElements").value(1));
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

    @Test
    void shouldRejectRequeueWithoutToken() throws Exception {
        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", existingMessageId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + existingMessageId + "/requeue"));
    }

    @Test
    void shouldRejectRequeueForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", existingMessageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + existingMessageId + "/requeue"));
    }

    @Test
    void shouldRequeueFailedEmailMessageForAdmin() throws Exception {
        NotificationOutboxMessage beforeRequeue = notificationOutboxRepository.findById(existingMessageId).orElseThrow();
        String originalPayload = beforeRequeue.getPayload();
        Instant previousLastAttemptAt = beforeRequeue.getLastAttemptAt();
        Instant before = Instant.now();

        MvcResult result = mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", existingMessageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingMessageId))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.payload").doesNotExist())
                .andExpect(jsonPath("$.payloadPreview").exists())
                .andExpect(jsonPath("$.payloadSize").isNumber())
                .andReturn();

        Instant after = Instant.now();
        NotificationOutboxMessage requeued = notificationOutboxRepository.findById(existingMessageId).orElseThrow();

        assertEquals(NotificationOutboxStatus.PENDING, requeued.getStatus());
        assertEquals(0, requeued.getAttempts());
        assertEquals(originalPayload, requeued.getPayload());
        assertEquals(previousLastAttemptAt, requeued.getLastAttemptAt());
        assertNull(requeued.getLastError());
        assertNull(requeued.getFailedAt());
        assertNull(requeued.getSentAt());
        assertNotNull(requeued.getNextAttemptAt());
        assertFalse(requeued.getNextAttemptAt().isBefore(before));
        assertFalse(requeued.getNextAttemptAt().isAfter(after.plusSeconds(1)));

        String content = result.getResponse().getContentAsString();
        assertFalse(content.contains("reset-secret-token-value"));
        assertFalse(content.contains("Texto muy largo sensible"));
        assertFalse(content.contains("<p>Html sensible</p>"));
    }

    @Test
    void shouldRateLimitRequeueAndKeepMessageUnchangedWhenBlocked() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", 999998L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", existingMessageId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", "outbox-requeue-rate-limit-test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.requestId").value("outbox-requeue-rate-limit-test"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + existingMessageId + "/requeue"));

        NotificationOutboxMessage unchanged = notificationOutboxRepository.findById(existingMessageId).orElseThrow();
        assertEquals(NotificationOutboxStatus.FAILED, unchanged.getStatus());
        assertEquals(1, unchanged.getAttempts());
        assertNotNull(unchanged.getFailedAt());
        assertTrue(abuseRateLimitRepository.findAll().stream()
                .allMatch(entry -> !entry.getAbuseKey().equals("admin@test.com")
                        && !entry.getAbuseKey().equals(String.valueOf(existingMessageId))
                        && !entry.getAbuseKey().contains("@")));
    }

    @Test
    void shouldRequeueDeadEmailMessageForAdmin() throws Exception {
        Long deadMessageId = notificationOutboxRepository.save(emailMessage(
                NotificationOutboxStatus.DEAD,
                "{\"provider\":\"resend\",\"to\":\"admin@test.com\",\"token\":\"dead-secret-token\"}",
                "permanent_failure"
        )).getId();

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", deadMessageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deadMessageId))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(0))
                .andExpect(jsonPath("$.payload").doesNotExist());

        NotificationOutboxMessage requeued = notificationOutboxRepository.findById(deadMessageId).orElseThrow();
        assertEquals(NotificationOutboxStatus.PENDING, requeued.getStatus());
        assertEquals(0, requeued.getAttempts());
        assertNull(requeued.getLastError());
        assertNull(requeued.getFailedAt());
        assertNull(requeued.getSentAt());
    }

    @Test
    void shouldRejectRequeueForPendingMessage() throws Exception {
        Long messageId = notificationOutboxRepository.save(emailMessage(
                NotificationOutboxStatus.PENDING,
                "{\"provider\":\"resend\",\"to\":\"admin@test.com\"}",
                null
        )).getId();

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", messageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + messageId + "/requeue"));
    }

    @Test
    void shouldRejectRequeueForProcessingMessage() throws Exception {
        Long messageId = notificationOutboxRepository.save(emailMessage(
                NotificationOutboxStatus.PROCESSING,
                "{\"provider\":\"resend\",\"to\":\"admin@test.com\"}",
                null
        )).getId();

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", messageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + messageId + "/requeue"));
    }

    @Test
    void shouldRejectRequeueForSentMessage() throws Exception {
        Long messageId = notificationOutboxRepository.save(emailMessage(
                NotificationOutboxStatus.SENT,
                "{\"provider\":\"resend\",\"to\":\"admin@test.com\"}",
                null
        )).getId();

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", messageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + messageId + "/requeue"));
    }

    @Test
    void shouldRejectRequeueForNonEmailChannel() throws Exception {
        Long messageId = notificationOutboxRepository.save(webhookMessage(NotificationOutboxStatus.FAILED)).getId();

        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", messageId)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/" + messageId + "/requeue"));

        NotificationOutboxMessage message = notificationOutboxRepository.findById(messageId).orElseThrow();
        assertEquals(NotificationOutboxStatus.FAILED, message.getStatus());
    }

    @Test
    void shouldReturnNotFoundWhenRequeueMessageDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/admin/notification-outbox/{id}/requeue", 999999L)
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/admin/notification-outbox/999999/requeue"));
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
        if (status == NotificationOutboxStatus.SENT) {
            message.setSentAt(Instant.parse("2026-05-03T19:31:00Z"));
        } else if (status == NotificationOutboxStatus.FAILED || status == NotificationOutboxStatus.DEAD) {
            message.setFailedAt(Instant.parse("2026-05-03T19:31:00Z"));
        }
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
