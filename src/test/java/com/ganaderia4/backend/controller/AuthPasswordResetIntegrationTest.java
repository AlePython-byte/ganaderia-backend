package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.PasswordResetToken;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.AbuseRateLimitRepository;
import com.ganaderia4.backend.repository.PasswordResetTokenRepository;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.service.PasswordResetTokenIssueResult;
import com.ganaderia4.backend.service.PasswordResetTokenService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.abuse-protection.password-reset.forgot.max-attempts=2",
        "app.abuse-protection.password-reset.forgot.window=10m",
        "app.abuse-protection.password-reset.forgot.block-duration=5m",
        "app.abuse-protection.password-reset.reset.max-attempts=2",
        "app.abuse-protection.password-reset.reset.window=10m",
        "app.abuse-protection.password-reset.reset.block-duration=5m"
})
class AuthPasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String GENERIC_FORGOT_MESSAGE =
            "Si el correo existe, recibirás instrucciones para recuperar tu contraseña.";
    private static final String RESET_SUCCESS_MESSAGE =
            "La contraseña fue actualizada correctamente.";
    private static final String INVALID_TOKEN_MESSAGE =
            "El token de recuperación es inválido o expiró.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AbuseRateLimitRepository abuseRateLimitRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        abuseRateLimitRepository.deleteAll();
        userRepository.deleteAll();

        activeUser = userRepository.save(user("admin@test.com", "12345678", true));
        inactiveUser = userRepository.save(user("inactive@test.com", "12345678", false));
    }

    @Test
    void shouldAcceptForgotPasswordForExistingActiveUser() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "  ADMIN@test.com  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertEquals(1, passwordResetTokenRepository.count());
        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertEquals(activeUser.getId(), token.getUser().getId());
        assertNull(token.getUsedAt());
        assertNotNull(token.getExpiresAt());
    }

    @Test
    void shouldReturnGenericResponseWhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "missing@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertEquals(0, passwordResetTokenRepository.count());
    }

    @Test
    void shouldReturnGenericResponseWhenUserIsInactive() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "inactive@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

        assertEquals(0, passwordResetTokenRepository.count());
    }

    @Test
    void shouldRateLimitForgotPasswordAndNotStoreEmailInPlainText() throws Exception {
        Map<String, String> request = Map.of("email", "ADMIN@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "forgot-rate-limit-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.requestId").value("forgot-rate-limit-test"));

        assertEquals(2, passwordResetTokenRepository.count());
        assertTrue(abuseRateLimitRepository.findAll().stream()
                .noneMatch(entry -> entry.getAbuseKey().contains("admin@test.com")));
    }

    @Test
    void shouldReturnSameRateLimitedResponseForUnknownForgotPasswordEmail() throws Exception {
        Map<String, String> request = Map.of("email", "missing@test.com");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GENERIC_FORGOT_MESSAGE));
        }

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "forgot-unknown-rate-limit-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.requestId").value("forgot-unknown-rate-limit-test"));

        assertEquals(0, passwordResetTokenRepository.count());
        assertTrue(abuseRateLimitRepository.findAll().stream()
                .noneMatch(entry -> entry.getAbuseKey().contains("missing@test.com")));
    }

    @Test
    void shouldResetPasswordWithValidTokenAndAllowNewLogin() throws Exception {
        PasswordResetTokenIssueResult issued = passwordResetTokenService.generateToken(activeUser, "127.0.0.1", "JUnit");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", issued.rawToken(),
                                "newPassword", "NuevaClave123*",
                                "confirmPassword", "NuevaClave123*"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(RESET_SUCCESS_MESSAGE));

        User refreshedUser = userRepository.findById(activeUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NuevaClave123*", refreshedUser.getPassword()));
        assertFalse(passwordEncoder.matches("12345678", refreshedUser.getPassword()));

        PasswordResetToken refreshedToken = passwordResetTokenRepository.findAll().get(0);
        assertNotNull(refreshedToken.getUsedAt());

        assertLoginFails("admin@test.com", "12345678");
        assertLoginSucceeds("admin@test.com", "NuevaClave123*");
    }

    @Test
    void shouldRejectUsedToken() throws Exception {
        PasswordResetTokenIssueResult issued = passwordResetTokenService.generateToken(activeUser, null, null);
        passwordResetTokenService.consumeToken(issued.rawToken());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", issued.rawToken(),
                                "newPassword", "NuevaClave123*",
                                "confirmPassword", "NuevaClave123*"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        PasswordResetTokenIssueResult issued = passwordResetTokenService.generateToken(activeUser, null, null);
        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        token.setExpiresAt(Instant.now().minusSeconds(1));
        passwordResetTokenRepository.save(token);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", issued.rawToken(),
                                "newPassword", "NuevaClave123*",
                                "confirmPassword", "NuevaClave123*"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void shouldRejectPasswordMismatch() throws Exception {
        PasswordResetTokenIssueResult issued = passwordResetTokenService.generateToken(activeUser, null, null);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", issued.rawToken(),
                                "newPassword", "NuevaClave123*",
                                "confirmPassword", "OtraClave123*"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden"));

        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertNull(token.getUsedAt());
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        PasswordResetTokenIssueResult issued = passwordResetTokenService.generateToken(activeUser, null, null);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", issued.rawToken(),
                                "newPassword", " 12345678",
                                "confirmPassword", " 12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nueva contraseña no puede tener espacios al inicio o al final"));
    }

    private void assertLoginFails(String email, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private void assertLoginSucceeds(String email, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRateLimitResetPasswordAndNotStoreTokenInPlainText() throws Exception {
        String invalidToken = "invalid-reset-token-value";
        Map<String, String> request = Map.of(
                "token", invalidToken,
                "newPassword", "NuevaClave123*",
                "confirmPassword", "NuevaClave123*"
        );

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_TOKEN_MESSAGE));
        }

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "reset-rate-limit-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.requestId").value("reset-rate-limit-test"));

        assertTrue(abuseRateLimitRepository.findAll().stream()
                .noneMatch(entry -> entry.getAbuseKey().contains(invalidToken)));
    }

    private User user(String email, String password, boolean active) {
        User user = new User();
        user.setName("Administrador");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(active);
        return user;
    }
}
