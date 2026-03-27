package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        return authService.login(requestDTO);
    }
}