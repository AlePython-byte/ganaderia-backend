package com.ganaderia4.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.ErrorResponseDTO;
import com.ganaderia4.backend.model.ApiErrorCode;
import com.ganaderia4.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/device/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers("/api/auth/me").authenticated()

                        .requestMatchers("/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")

                        .requestMatchers("/api/geofences/**").hasAnyRole("ADMINISTRADOR", "SUPERVISOR")

                        .requestMatchers(HttpMethod.GET, "/api/alerts/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")

                        .requestMatchers(HttpMethod.PUT, "/api/alerts/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")

                        .requestMatchers(HttpMethod.PATCH, "/api/alerts/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR")

                        .requestMatchers(HttpMethod.GET, "/api/cows/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR")

                        .requestMatchers(HttpMethod.POST, "/api/cows/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR")

                        .requestMatchers(HttpMethod.GET, "/api/collars/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")

                        .requestMatchers(HttpMethod.POST, "/api/collars/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "TECNICO")

                        .requestMatchers(HttpMethod.GET, "/api/locations/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")

                        .requestMatchers(HttpMethod.POST, "/api/locations/**")
                        .hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        ApiErrorCode.UNAUTHORIZED,
                                        "No autorizado",
                                        request.getRequestURI()
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        ApiErrorCode.FORBIDDEN,
                                        "Acceso denegado",
                                        request.getRequestURI()
                                )
                        )
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    ApiErrorCode code,
                                    String message,
                                    String path) throws java.io.IOException {
        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                path,
                LocalDateTime.now()
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}