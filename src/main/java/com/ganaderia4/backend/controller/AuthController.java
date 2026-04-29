package com.ganaderia4.backend.controller;

import com.ganaderia4.backend.config.OpenApiConfig;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.dto.LoginResponseDTO;
import com.ganaderia4.backend.dto.UserResponseDTO;
import com.ganaderia4.backend.security.ClientIpResolver;
import com.ganaderia4.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Acceso de usuarios al backend mediante JWT Bearer")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesion",
            description = "Endpoint publico que autentica al usuario y retorna un JWT Bearer para endpoints protegidos."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticacion exitosa"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud invalida o payload mal formado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales invalidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Demasiados intentos de autenticacion cuando aplique",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO requestDTO,
                                  HttpServletRequest httpServletRequest) {
        return authService.login(requestDTO, clientIpResolver.resolve(httpServletRequest));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Obtener usuario autenticado",
            description = "Retorna la informacion del usuario asociado al JWT Bearer enviado en Authorization."
    )
    @SecurityRequirement(name = OpenApiConfig.JWT_SECURITY_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado recuperado correctamente"),
            @ApiResponse(
                    responseCode = "401",
                    description = "JWT ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    public UserResponseDTO me(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }
}
