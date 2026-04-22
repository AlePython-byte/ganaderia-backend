package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.security.JwtService;
import com.ganaderia4.backend.security.LoginAbuseProtectionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final LoginAbuseProtectionService loginAbuseProtectionService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogService auditLogService,
                       LoginAbuseProtectionService loginAbuseProtectionService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.loginAbuseProtectionService = loginAbuseProtectionService;
    }

    public LoginResponseDTO login(LoginRequestDTO requestDTO, String clientIp) {
        loginAbuseProtectionService.assertLoginAllowed(clientIp, requestDTO.getEmail());

        User user;

        try {
            user = userService.getUserEntityByEmail(requestDTO.getEmail());
        } catch (ResourceNotFoundException ex) {
            auditLogService.log(
                    "LOGIN_FAILED",
                    "USER",
                    null,
                    requestDTO.getEmail(),
                    "AUTH",
                    "Intento de inicio de sesi\u00f3n con correo no registrado",
                    false
            );
            loginAbuseProtectionService.recordLoginFailure(clientIp, requestDTO.getEmail());
            throw new BadRequestException("Credenciales inv\u00e1lidas");
        }

        if (!Boolean.TRUE.equals(user.getActive())
                || !passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            auditLogService.log(
                    "LOGIN_FAILED",
                    "USER",
                    user.getId(),
                    user.getEmail(),
                    "AUTH",
                    "Credenciales inv\u00e1lidas o usuario inactivo",
                    false
            );
            loginAbuseProtectionService.recordLoginFailure(clientIp, requestDTO.getEmail());
            throw new BadRequestException("Credenciales inv\u00e1lidas");
        }

        String token = jwtService.generateToken(user);
        loginAbuseProtectionService.recordLoginSuccess(clientIp, requestDTO.getEmail());

        auditLogService.log(
                "LOGIN_SUCCESS",
                "USER",
                user.getId(),
                user.getEmail(),
                "AUTH",
                "Inicio de sesi\u00f3n exitoso",
                true
        );

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                "Inicio de sesi\u00f3n exitoso"
        );
    }

    public UserResponseDTO getCurrentUser(String email) {
        User user = userService.getUserEntityByEmail(email);

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive()
        );
    }
}
