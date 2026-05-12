package com.ganaderia4.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
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
                        .title("Ganadería 4.0 Backend API")
                        .description("""
                                API REST para monitoreo ganadero con autenticación JWT, operación IoT,
                                telemetría GPS, geocercas, alertas, notificaciones, IA analítica,
                                reportes y observabilidad.

                                La API usa dos mecanismos de autenticación:
                                - JWT Bearer para usuarios del backend.
                                - HMAC por headers para ingestiones desde dispositivos.
                                """)
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Equipo Ganadería 4.0"))
                        .license(new License()
                                .name("Uso académico")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Entorno local"),
                        new Server()
                                .url("https://ganaderia-backend.onrender.com")
                                .description("Despliegue demo en Render")
                ))
                .tags(List.of(
                        new Tag().name("Autenticacion").description("Login, sesion activa y recuperacion de contrasena"),
                        new Tag().name("Usuarios").description("Administracion de usuarios internos"),
                        new Tag().name("Vacas").description("Gestion operativa de vacas y sus identificadores"),
                        new Tag().name("Collares").description("Gestion de collares, asociacion y secretos HMAC"),
                        new Tag().name("Geocercas").description("Administracion de geocercas del dominio ganadero"),
                        new Tag().name("Ubicaciones").description("Registro manual y consultas historicas de ubicaciones"),
                        new Tag().name("Alertas").description("Consulta y gestion operativa de alertas"),
                        new Tag().name("Reportes").description("Analitica y exportaciones CSV"),
                        new Tag().name("Dispositivos").description("Ingestion de telemetria desde collares usando HMAC"),
                        new Tag().name("Alert Analysis").description("Analisis heuristico y asistido por IA"),
                        new Tag().name("Preferencias de notificacion").description("Configuracion administrativa de preferencias de notificacion"),
                        new Tag().name("Notification outbox admin").description("Diagnostico y requeue administrativo del outbox EMAIL"),
                        new Tag().name("Dashboard").description("Indicadores operativos del monitoreo"),
                        new Tag().name("Auditoria").description("Consulta administrativa de eventos auditables"),
                        new Tag().name("Salud del sistema").description("Health check publico del backend")
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

    @Bean
    public OpenApiCustomizer standardUnexpectedErrorResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        ApiResponses responses = operation.getResponses();
                        if (responses == null) {
                            responses = new ApiResponses();
                            operation.setResponses(responses);
                        }

                        responses.computeIfAbsent("500", ignored -> internalServerErrorResponse());
                    })
            );
        };
    }

    private ApiResponse internalServerErrorResponse() {
        return new ApiResponse()
                .description("Error inesperado gestionado por el manejador global de excepciones")
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponseDTO"))
                ));
    }
}
