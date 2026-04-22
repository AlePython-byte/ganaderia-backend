package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO requestDTO,
                                  HttpServletRequest httpServletRequest) {
        return authService.login(requestDTO, clientIpResolver.resolve(httpServletRequest));
    }

    @GetMapping("/me")
    public UserResponseDTO me(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }
}
