package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.exception.ResourceNotFoundException;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        User user;

        try {
            user = userService.getUserEntityByEmail(requestDTO.getEmail());
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException("Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(user.getActive()) || !passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user);

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                "Inicio de sesión exitoso"
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