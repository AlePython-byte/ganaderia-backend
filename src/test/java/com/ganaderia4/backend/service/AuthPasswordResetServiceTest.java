package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.ForgotPasswordRequestDTO;
import com.ganaderia4.backend.dto.ForgotPasswordResponseDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthPasswordResetServiceTest {

    @Test
    void shouldGenerateTokenAndSendEmailForActiveExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenService tokenService = mock(PasswordResetTokenService.class);
        PasswordResetEmailService emailService = mock(PasswordResetEmailService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = user("admin@test.com", true);

        when(userRepository.findByEmailIgnoreCase("admin@test.com")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(eq(user), anyString(), anyString())).thenReturn(
                new PasswordResetTokenIssueResult(user.getId(), "raw-token", Instant.parse("2026-05-03T12:15:00Z"))
        );

        AuthPasswordResetService service = new AuthPasswordResetService(
                userRepository,
                tokenService,
                emailService,
                passwordEncoder
        );

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail(" admin@test.com ");

        ForgotPasswordResponseDTO response = service.forgotPassword(request, "127.0.0.1", "JUnit");

        assertEquals("Si el correo existe, recibirás instrucciones para recuperar tu contraseña.", response.getMessage());
        verify(tokenService).generateToken(eq(user), eq("127.0.0.1"), eq("JUnit"));
        verify(emailService).sendPasswordResetEmail(eq(user), any(PasswordResetTokenIssueResult.class));
    }

    @Test
    void shouldSkipTokenGenerationAndEmailForUnknownUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenService tokenService = mock(PasswordResetTokenService.class);
        PasswordResetEmailService emailService = mock(PasswordResetEmailService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.findByEmailIgnoreCase("missing@test.com")).thenReturn(Optional.empty());

        AuthPasswordResetService service = new AuthPasswordResetService(
                userRepository,
                tokenService,
                emailService,
                passwordEncoder
        );

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("missing@test.com");

        ForgotPasswordResponseDTO response = service.forgotPassword(request, "127.0.0.1", "JUnit");

        assertEquals("Si el correo existe, recibirás instrucciones para recuperar tu contraseña.", response.getMessage());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(emailService);
    }

    @Test
    void shouldSkipTokenGenerationAndEmailForInactiveUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordResetTokenService tokenService = mock(PasswordResetTokenService.class);
        PasswordResetEmailService emailService = mock(PasswordResetEmailService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = user("inactive@test.com", false);

        when(userRepository.findByEmailIgnoreCase("inactive@test.com")).thenReturn(Optional.of(user));

        AuthPasswordResetService service = new AuthPasswordResetService(
                userRepository,
                tokenService,
                emailService,
                passwordEncoder
        );

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("inactive@test.com");

        ForgotPasswordResponseDTO response = service.forgotPassword(request, "127.0.0.1", "JUnit");

        assertEquals("Si el correo existe, recibirás instrucciones para recuperar tu contraseña.", response.getMessage());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(emailService);
    }

    private User user(String email, boolean active) {
        User user = new User();
        user.setId(1L);
        user.setName("Admin");
        user.setEmail(email);
        user.setPassword("hash");
        user.setRole(Role.ADMINISTRADOR);
        user.setActive(active);
        return user;
    }
}
