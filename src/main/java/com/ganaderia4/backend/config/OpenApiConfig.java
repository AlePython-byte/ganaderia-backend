package com.ganaderia4.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY_SCHEME = "bearerAuth";
    public static final String DEVICE_TOKEN_SCHEME = "deviceTokenHeader";
    public static final String DEVICE_TIMESTAMP_SCHEME = "deviceTimestampHeader";
    public static final String DEVICE_NONCE_SCHEME = "deviceNonceHeader";
    public static final String DEVICE_SIGNATURE_SCHEME = "deviceSignatureHeader";

    @Bean
    public OpenAPI ganaderiaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ganaderia 4.0 Backend API")
                        .description("""
                                API REST para monitoreo ganadero con collares, telemetria GPS, geocercas,
                                alertas operativas, dashboard, reportes y observabilidad.

                                La API usa dos mecanismos de autenticacion:
                                - JWT Bearer para usuarios del backend.
                                - HMAC por headers para ingestiones desde dispositivos.
                                """)
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Equipo Ganaderia 4.0")
                                .email("admin@ganaderia.com"))
                        .license(new License()
                                .name("Uso academico")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Entorno local o dev"),
                        new Server()
                                .url("http://localhost:10000")
                                .description("Referencia de despliegue local con perfil prod")
                ))
                .tags(List.of(
                        new Tag().name("Autenticacion").description("Login y recuperacion del usuario autenticado"),
                        new Tag().name("Dispositivos").description("Ingestion de telemetria desde collares usando HMAC")
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                JWT_SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(JWT_SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                        .addSecuritySchemes(
                                DEVICE_TOKEN_SCHEME,
                                new SecurityScheme()
                                        .name("X-Device-Token")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Token publico del collar o dispositivo")
                        )
                        .addSecuritySchemes(
                                DEVICE_TIMESTAMP_SCHEME,
                                new SecurityScheme()
                                        .name("X-Device-Timestamp")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Timestamp ISO-8601 UTC usado en la firma HMAC")
                        )
                        .addSecuritySchemes(
                                DEVICE_NONCE_SCHEME,
                                new SecurityScheme()
                                        .name("X-Device-Nonce")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Nonce unico por solicitud para prevenir replay")
                        )
                        .addSecuritySchemes(
                                DEVICE_SIGNATURE_SCHEME,
                                new SecurityScheme()
                                        .name("X-Device-Signature")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Firma HMAC-SHA256 Base64 de la solicitud canonica")
                        ));
    }
}
