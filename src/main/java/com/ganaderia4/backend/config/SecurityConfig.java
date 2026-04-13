package com.ganaderia4.backend.config;

import com.ganaderia4.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"No autorizado\"}");
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}