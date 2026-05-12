# Reporte de pruebas y calidad — Ganadería 4.0

## 1. Propósito del reporte

Este documento resume la estrategia de pruebas y calidad del backend Ganadería 4.0. Su propósito es demostrar, de forma clara y verificable, que el proyecto fue validado con automatización, pruebas unitarias, pruebas de integración, base de datos real mediante Testcontainers, migraciones versionadas, cobertura y análisis estático.

El reporte está pensado para jurado universitario, profesor o evaluador técnico que necesite entender qué se probó, qué herramientas se usan y qué nivel de confianza aporta el pipeline de calidad.

## 2. Alcance de validación

La validación del backend cubre:

- Compilación del proyecto.
- Reglas de entorno con Maven Enforcer.
- Tests unitarios.
- Tests de integración.
- Validación/aplicación de migraciones Flyway.
- PostgreSQL real mediante Testcontainers.
- Medición y check de cobertura con JaCoCo.
- Análisis estático con SpotBugs.
- Empaquetado del jar.
- Reempaquetado Spring Boot.

Este alcance valida el backend. No reemplaza una prueba E2E completa frontend/backend ni monitoreo real de producción.

## 3. Comando principal de validación

Comando principal en Windows:

```powershell
.\mvnw.cmd clean verify
```

Este comando ejecuta el pipeline local de calidad: limpia artefactos previos, compila, ejecuta tests, valida integración, genera cobertura, aplica checks configurados y empaqueta el backend.

En Linux/macOS puede usarse:

```bash
./mvnw clean verify
```

Notas operativas:

- En Windows se usa Maven Wrapper con `.\mvnw.cmd`.
- Las pruebas de integración requieren Docker Desktop activo porque usan Testcontainers.
- Si Docker no está disponible, puede fallar la fase de integración aunque la lógica del backend esté correcta.

## 4. Resultado conocido más reciente

Último resultado conocido de `.\mvnw.cmd clean verify`:

- Tests unitarios: 277.
- Tests de integración: 198.
- Total: 475.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- JaCoCo: OK.
- SpotBugs: OK.
- Maven Enforcer: OK.
- Flyway: 15 migraciones validadas/aplicadas en entorno de test.
- PostgreSQL Testcontainers: usado en integración.

Este resultado corresponde al último `clean verify` conocido y debe reejecutarse antes de una entrega final si hubo cambios posteriores.

## 5. Distribución de pruebas

| Tipo de prueba | Cantidad conocida | Objetivo | Herramienta |
| --- | ---: | --- | --- |
| Unitarias | 277 | Validar servicios, reglas de negocio y componentes aislados | JUnit, Mockito, Maven Surefire |
| Integración | 198 | Validar flujos HTTP, seguridad, persistencia, Flyway y PostgreSQL real | Spring Boot Test, MockMvc, Testcontainers, Maven Failsafe |
| Calidad estática | N/A | Detectar defectos estáticos en bytecode | SpotBugs |
| Cobertura | N/A | Medir cobertura y aplicar umbrales mínimos | JaCoCo |
| Migraciones | 15 migraciones conocidas | Validar evolución del esquema sobre PostgreSQL | Flyway |
| Empaquetado | 1 build | Generar jar ejecutable y reempaquetado Spring Boot | Maven, Spring Boot Maven Plugin |

## 6. Pruebas unitarias

Las pruebas unitarias validan servicios, reglas de negocio y componentes aislados sin levantar todo el contexto productivo.

Áreas cubiertas por archivos reales en `src/test/java`:

- Auth/bootstrap admin: `AdminBootstrapConfigTest`.
- CowService: `CowServiceTest`.
- CollarService: `CollarServiceTest`.
- AlertService: `AlertServiceTest`.
- AlertAnalysisService: `AlertAnalysisServiceTest`.
- AlertAiAnalysisService: `AlertAiAnalysisServiceTest`.
- AlertPriorityScorer: `AlertPriorityScorerTest`.
- DeviceRequestAuthenticationService: `DeviceRequestAuthenticationServiceTest`.
- LocationService: `LocationServiceTest`.
- DeviceMonitoringService: `DeviceMonitoringServiceTest`.
- Notification services: `DefaultNotificationDispatcherTest`, `EmailNotificationServiceTest`, `LoggingNotificationServiceTest`, `WebhookNotificationServiceTest`.
- Outbox processor y servicio: `NotificationOutboxEmailProcessorTest`, `NotificationOutboxServiceTest`.
- Password reset: `AuthPasswordResetServiceTest`, `PasswordResetTokenServiceTest`, `PasswordResetEmailServiceTest`, `PasswordResetEmailTemplateBuilderTest`.
- ReportCsvService: `ReportCsvServiceTest`.
- Observability: `RequestCorrelationFilterTest`, `DomainMetricsServiceTest`.
- Validación por patrones: `LocationValidationChainTest`, `MonitoringFacadeTest`.

Estas pruebas reducen el riesgo de regresiones en lógica crítica sin depender de proveedores externos reales.

## 7. Pruebas de integración

Las pruebas de integración validan flujos HTTP, seguridad, base de datos, Flyway y comportamiento con PostgreSQL real.

Áreas cubiertas por archivos reales:

- Auth: `AuthControllerIntegrationTest`, `AuthPasswordResetIntegrationTest`.
- Users: `SecurityAuthorizationIntegrationTest`, `UserNotificationPreferenceControllerIntegrationTest`.
- Cows: `CowControllerTest`.
- Collars: `CollarControllerIntegrationTest`.
- Geofences: `GeofenceServiceTest` y flujos asociados.
- Locations: `LocationServiceTest`, `DeviceControllerIntegrationTest`.
- Alerts: `AlertControllerIntegrationTest`.
- Alert analysis: `AlertAnalysisControllerIntegrationTest`.
- Reports CSV: `AlertReportCsvExportIntegrationTest`, `ReportControllerIntegrationTest`, `CowIncidentReportIntegrationTest`, `OfflineCollarReportIntegrationTest`.
- Device ingestion: `DeviceControllerIntegrationTest`.
- Notification outbox admin: `AdminNotificationOutboxControllerIntegrationTest`.
- Actuator metrics: `ActuatorMetricsIntegrationTest`.
- Security authorization: `SecurityAuthorizationIntegrationTest`, `CorsIntegrationTest`.
- Observability HTTP: `RequestCorrelationIntegrationTest`, `SecurityHttpEventLoggingIntegrationTest`, `DomainMetricsIntegrationTest`.

Estas pruebas dan confianza en contratos HTTP, reglas de autorización, serialización, errores y persistencia.

## 8. Testcontainers y PostgreSQL real

Los tests de integración usan PostgreSQL mediante Testcontainers. Esto acerca el entorno de prueba al entorno productivo porque valida SQL real, constraints, tipos de datos, índices, migraciones y comportamiento de JPA/Hibernate contra PostgreSQL.

Ventajas:

- Evita diferencias típicas de motores embebidos como H2.
- Valida Flyway sobre un contenedor real.
- Detecta errores de esquema antes del despliegue.
- Permite probar repositorios y flujos HTTP con persistencia real.

Problema común:

Si Docker Desktop no está activo, Testcontainers no puede iniciar PostgreSQL. En ese caso el fallo suele ser de entorno local, no necesariamente de lógica del backend.

## 9. Flyway y migraciones

Flyway gestiona migraciones versionadas del esquema. El último resultado conocido indica 15 migraciones validadas/aplicadas en entorno de test.

Migraciones conocidas:

- `V1__init_schema.sql`.
- `V2__enhance_collar_model.sql`.
- `V3__add_audit_logs.sql`.
- `V4__add_collar_id_to_locations.sql`.
- `V5__enforce_locations_collar_integrity.sql`.
- `V6__add_location_idempotency_constraint.sql`.
- `V7__add_device_replay_nonces.sql`.
- `V8__add_collar_device_secret_salt.sql`.
- `V9__add_abuse_rate_limits.sql`.
- `V10__add_webhook_notification_deliveries.sql`.
- `V11__add_read_path_indexes.sql`.
- `V12__add_locations_gps_accuracy.sql`.
- `V13__add_user_notification_preferences.sql`.
- `V14__add_password_reset_tokens.sql`.
- `V15__add_notification_outbox.sql`.

Esto ayuda a detectar errores de esquema y mantiene trazabilidad de evolución de base de datos. No se debe modificar una migración ya aplicada sin una estrategia clara de reparación o migración incremental.

## 10. JaCoCo

JaCoCo mide cobertura de pruebas. El build conocido pasa el check de cobertura.

Umbrales verificados en `pom.xml`:

- Cobertura de líneas mínima del bundle: 0.50.
- Cobertura de ramas mínima del bundle: 0.35.

No se documenta un porcentaje final de cobertura porque no fue recalculado en esta subfase. El valor relevante para este reporte es que el último `clean verify` conocido pasó el check configurado.

## 11. SpotBugs

SpotBugs ejecuta análisis estático sobre bytecode y ayuda a detectar riesgos como:

- Posibles null dereferences.
- Malas prácticas de concurrencia o igualdad.
- Patrones peligrosos.
- Defectos estáticos no evidentes en tests funcionales.

El último build conocido pasó SpotBugs. Esto complementa los tests porque detecta problemas estructurales que no siempre se manifiestan en una ejecución puntual.

## 12. Maven Enforcer

Maven Enforcer valida que el entorno de build sea compatible.

Reglas verificadas en `pom.xml`:

- Java requerido: 17 o superior.
- Maven requerido: 3.9.0 o superior.

Esto evita builds con versiones incompatibles que podrían producir errores inconsistentes entre máquinas.

## 13. Seguridad validada por pruebas

Casos de seguridad cubiertos por tests reales:

- 401 sin token en endpoints protegidos.
- 403 con rol insuficiente.
- Acceso permitido según rol.
- Restricciones admin para métricas y outbox.
- CORS y headers permitidos, incluyendo headers HMAC del endpoint IoT.
- HMAC inválido o ausente en device ingestion.
- Nonce repetido.
- Timestamp inválido o fuera de ventana.
- Password reset seguro: usuario inexistente/inactivo sin revelar existencia, token válido, token usado, expirado o inválido.

Archivos representativos:

- `SecurityAuthorizationIntegrationTest`.
- `CorsIntegrationTest`.
- `ActuatorMetricsIntegrationTest`.
- `AdminNotificationOutboxControllerIntegrationTest`.
- `DeviceControllerIntegrationTest`.
- `DeviceRequestAuthenticationServiceTest`.
- `AuthPasswordResetIntegrationTest`.
- `PasswordResetTokenServiceTest`.

## 14. Calidad de API y errores

La suite cubre comportamiento de API y manejo de errores:

- 400 por datos inválidos.
- 404 por recurso inexistente.
- 409 por conflictos.
- 401/403 por seguridad.
- Contratos de respuesta para endpoints paginados.
- Errores globales manejados por `GlobalExceptionHandler`.
- Logs estructurados para errores HTTP y eventos de seguridad.

Esto ayuda a mantener contratos predecibles para frontend, scripts y operadores.

## 15. Reportes y CSV

Los reportes y exportaciones CSV están cubiertos por pruebas unitarias e integración.

Archivos representativos:

- `ReportCsvServiceTest`.
- `AlertReportCsvExportIntegrationTest`.
- `ReportControllerIntegrationTest`.
- `CowIncidentReportIntegrationTest`.
- `OfflineCollarReportIntegrationTest`.

Validaciones cubiertas:

- Exportación CSV de alertas.
- Acceso por roles.
- Límite de filas exportables.
- Escapado de comillas.
- Protección contra CSV injection para valores que inician con `=`, `+`, `-` o `@`.

## 16. Observabilidad validada

La observabilidad se valida mediante pruebas dedicadas.

Áreas cubiertas:

- `RequestCorrelationFilter`.
- Header `X-Request-Id`.
- Actuator metrics.
- Endpoint Prometheus.
- Métricas de dominio.
- Métricas asociadas a IA/outbox/notificaciones según servicios instrumentados.
- Logs de eventos HTTP/seguridad.

Archivos representativos:

- `RequestCorrelationFilterTest`.
- `RequestCorrelationIntegrationTest`.
- `ActuatorMetricsIntegrationTest`.
- `DomainMetricsServiceTest`.
- `DomainMetricsIntegrationTest`.
- `SecurityHttpEventLoggingIntegrationTest`.

## 17. Interpretación de fallos comunes

| Fallo | Posible causa | Acción recomendada |
| --- | --- | --- |
| Docker Desktop apagado | Testcontainers no puede iniciar PostgreSQL | Iniciar Docker Desktop y repetir `.\mvnw.cmd clean verify`. |
| Testcontainers no puede iniciar PostgreSQL | Docker sin recursos, red local o imagen no disponible | Revisar Docker, reiniciar servicio y verificar conectividad local. |
| Puerto ocupado | Algún test o ejecución local usa un puerto requerido | Cerrar procesos previos o revisar configuración del entorno. |
| Variables de entorno faltantes | Perfil local/demo incompleto | Configurar placeholders reales en ambiente seguro, no en documentación. |
| Fallo de SpotBugs | Defecto estático detectado | Revisar reporte en `target/site/spotbugs` o salida de Maven y corregir causa raíz. |
| Fallo de JaCoCo | Cobertura bajo umbral | Agregar tests relevantes o revisar código no cubierto. |
| Migración Flyway inválida | SQL incompatible, checksum alterado o migración modificada | Crear nueva migración incremental o reparar de forma controlada según ambiente. |
| Test con timestamp viejo | Datos fijos quedaron fuera de ventana temporal | Usar `Clock` fijo o timestamps relativos al reloj del test. |
| Diferencias de zona horaria | Uso incorrecto de UTC vs `LocalDateTime` | Revisar contrato del endpoint y normalizar timestamps en tests/scripts. |
| Error por dependencia externa simulada | Mock/stub no representa el caso esperado | Ajustar fixture o revisar contrato de integración simulada. |

## 18. Limitaciones de la estrategia actual

Limitaciones conocidas:

- Las pruebas no reemplazan monitoreo en producción.
- `clean verify` valida backend, no frontend completo.
- EMAIL y Gemini reales dependen de variables externas y disponibilidad del proveedor.
- Las pruebas de carga están documentadas aparte en `docs/device-ingestion-load-test.md`.
- E2E frontend/backend puede requerir validación adicional.
- Algunos resultados son último estado conocido, no ejecución permanente.
- El entorno local debe tener Docker Desktop y recursos suficientes para Testcontainers.

## 19. Checklist antes de entrega

- Ejecutar `.\mvnw.cmd clean verify`.
- Confirmar Docker Desktop activo.
- Confirmar 0 failures.
- Confirmar 0 errors.
- Confirmar 0 skipped.
- Confirmar JaCoCo OK.
- Confirmar SpotBugs OK.
- Confirmar Maven Enforcer OK.
- Confirmar git status limpio.
- Confirmar README y docs sincronizados.
- Confirmar que no hay secretos en docs, scripts ni logs.
- Confirmar Render health OK.

## 20. Conclusión técnica

La calidad del backend Ganadería 4.0 está respaldada por automatización y por una estrategia de pruebas que combina unit tests, integration tests con PostgreSQL real, migraciones Flyway, análisis estático, cobertura y verificación de empaquetado.

Esto diferencia el proyecto de un CRUD académico básico: los flujos críticos de seguridad, IoT, password reset, outbox, reportes, observabilidad y reglas operativas tienen validación automatizada y pueden defenderse técnicamente ante revisión externa.
