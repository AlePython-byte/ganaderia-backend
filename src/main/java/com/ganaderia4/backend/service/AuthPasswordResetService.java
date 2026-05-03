package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.ForgotPasswordRequestDTO;
import com.ganaderia4.backend.dto.ForgotPasswordResponseDTO;
import com.ganaderia4.backend.dto.ResetPasswordRequestDTO;
import com.ganaderia4.backend.dto.ResetPasswordResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.PasswordResetToken;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.observability.OperationalLogSanitizer;
import com.ganaderia4.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(AuthPasswordResetService.class);
    private static final String FORGOT_PASSWORD_MESSAGE = "Si el correo existe, recibirás instrucciones para recuperar tu contraseña.";
    private static final String RESET_PASSWORD_SUCCESS_MESSAGE = "La contraseña fue actualizada correctamente.";
    private static final String INVALID_TOKEN_MESSAGE = "El token de recuperación es inválido o expiró.";

    private final UserRepository userRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthPasswordResetService(UserRepository userRepository,
                                    PasswordResetTokenService passwordResetTokenService,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO requestDTO,
                                                    String requestIp,
                                                    String userAgent) {
        String normalizedEmail = normalizeEmail(requestDTO.getEmail());
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOptional.isPresent() && Boolean.TRUE.equals(userOptional.get().getActive())) {
            passwordResetTokenService.generateToken(userOptional.get(), requestIp, userAgent);
        }

        log.info(
                "event=password_reset_requested requestId={} email={} outcome=accepted",
                OperationalLogSanitizer.requestId(),
                OperationalLogSanitizer.maskEmail(normalizedEmail)
        );

        return new ForgotPasswordResponseDTO(FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public ResetPasswordResponseDTO resetPassword(ResetPasswordRequestDTO requestDTO) {
        validatePasswordResetRequest(requestDTO);

        PasswordResetToken token;
        try {
            token = passwordResetTokenService.consumeToken(requestDTO.getToken());
        } catch (InvalidPasswordResetTokenException ex) {
            log.warn(
                    "event=password_reset_failed requestId={} reason=invalid_token",
                    OperationalLogSanitizer.requestId()
            );
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);

        log.info(
                "event=password_reset_completed requestId={} userId={}",
                OperationalLogSanitizer.requestId(),
                user.getId()
        );

        return new ResetPasswordResponseDTO(RESET_PASSWORD_SUCCESS_MESSAGE);
    }

    private void validatePasswordResetRequest(ResetPasswordRequestDTO requestDTO) {
        String newPassword = requestDTO.getNewPassword();
        String confirmPassword = requestDTO.getConfirmPassword();

        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            log.warn(
                    "event=password_reset_failed requestId={} reason=validation_error",
                    OperationalLogSanitizer.requestId()
            );
            throw new BadRequestException("La nueva contrasena es obligatoria");
        }

        if (!newPassword.equals(confirmPassword)) {
            log.warn(
                    "event=password_reset_failed requestId={} reason=password_mismatch",
                    OperationalLogSanitizer.requestId()
            );
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        if (!newPassword.equals(newPassword.trim())) {
            log.warn(
                    "event=password_reset_failed requestId={} reason=validation_error",
                    OperationalLogSanitizer.requestId()
            );
            throw new BadRequestException("La nueva contrase\u00f1a no puede tener espacios al inicio o al final");
        }

        if (newPassword.length() < 8 || newPassword.length() > 100) {
            log.warn(
                    "event=password_reset_failed requestId={} reason=validation_error",
                    OperationalLogSanitizer.requestId()
            );
            throw new BadRequestException("La nueva contrase\u00f1a debe tener entre 8 y 100 caracteres");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
