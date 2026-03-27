package com.ganaderia4.backend.service;

import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.exception.BadRequestException;
import com.ganaderia4.backend.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        User user = userService.getUserEntityByEmail(requestDTO.getEmail());

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("El usuario está inactivo");
        }

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                "Inicio de sesión exitoso"
        );
    }
}