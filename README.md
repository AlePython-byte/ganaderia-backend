# Ganadería 4.0 Backend

Backend del sistema **Ganadería 4.0**, orientado al monitoreo ganadero con collares IoT, telemetria GPS, geocercas, alertas operativas, autenticacion JWT, integracion HMAC para dispositivos, notificaciones y analisis operativo asistido por IA.

## Descripcion general

La aplicacion expone una API REST en Spring Boot para administrar vacas, collares, geocercas, ubicaciones, alertas, usuarios, reportes y flujos operativos asociados al monitoreo de ganado. Tambien incluye un canal IoT seguro para recibir ubicaciones desde dispositivos mediante firmas HMAC, proteccion anti-replay y validaciones temporales.

El backend esta preparado para uso academico y tecnico, con migraciones versionadas, pruebas automatizadas, observabilidad, scripts operativos y documentacion complementaria.

Dominio backend desplegado en Render:

```text
https://ganaderia-backend.onrender.com
```

## Estado actual

El proyecto cuenta actualmente con:

- Autenticacion JWT y autorizacion por roles.
- CORS configurable para integracion con frontend.
- Swagger/OpenAPI en perfiles de desarrollo.
- Actuator y metricas operativas.
- Auditoria de acciones relevantes.
- Dashboard, reportes y analisis de alertas.
- Device ingestion por HMAC con anti-replay persistente.
- Limpiezas programadas de nonces, rate limits y tokens de recuperacion.
- Abuse protection/rate limiting para login y dispositivos.
- Notificaciones por LOG, WEBHOOK y EMAIL.
- EMAIL real con Resend.
- EMAIL habilitado para la demo y configurable por entorno.
- Preferencias de notificacion por usuario.
- Password reset por email.
- IA analitica con Gemini y fallback heuristico.
- IA habilitada para la demo y configurable por entorno.
- Notification outbox para EMAIL, processor y requeue admin.
- Generacion automatica de tokens `COW-*`, `COLLAR-*` e internal code `INT-*`.
- Scripts operativos para smoke tests, seed demo, email, outbox y carga IoT.
- Pruebas unitarias, integracion, JaCoCo y SpotBugs.
- Ultimo `clean verify` conocido: 277 tests unitarios, 198 tests de integracion, 475 tests totales, JaCoCo OK y SpotBugs OK.

## Stack tecnico

- Java 17+
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- HMAC SHA-256 para dispositivos
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator
- Micrometer / Prometheus
- Resend para email
- Gemini para IA analitica
- JUnit 5
- MockMvc
- Testcontainers
- JaCoCo
- SpotBugs
- Docker
- GitHub Actions

## Funcionalidades principales

- Gestion de vacas con token tecnico generado por backend (`COW-001`, `COW-002`, ...).
- Codigo interno de vaca generado por backend (`INT-001`, `INT-002`, ...).
- Campo `active` en vacas para activacion/desactivacion operativa sin borrar historial.
- Consulta paginada de vacas con filtros por `status` y `active`.
- Gestion de collares con token tecnico generado por backend (`COLLAR-001`, `COLLAR-002`, ...).
- Asociacion de collares a vacas.
- Rotacion de secreto HMAC de collares.
- Gestion de geocercas con activacion/desactivacion por endpoint y consulta paginada.
- Registro manual y por dispositivo de ubicaciones.
- Persistencia de `batteryLevel` y `gpsAccuracy`.
- Alertas `EXIT_GEOFENCE`, `COLLAR_OFFLINE` y `LOW_BATTERY`.
- Dashboard operativo y reportes.
- Auditoria.
- Preferencias de notificacion por usuario.
- Recuperacion de contrasena por email.
- Analisis heuristico e IA de alertas.
- Notification outbox para entrega EMAIL asincrona.

## Seguridad

El backend combina seguridad de usuarios y seguridad de dispositivos:

- JWT para usuarios del sistema.
- Roles operativos: `ADMINISTRADOR`, `SUPERVISOR`, `TECNICO`, `OPERADOR`.
- HMAC para dispositivos IoT.
- Anti-replay mediante nonces persistentes.
- Validacion temporal de requests IoT.
- Rate limiting y proteccion de abuso.
- Sanitizacion de logs y respuestas administrativas sensibles.
- Password reset con token seguro hasheado en base de datos.
- Secretos siempre configurados por variables de entorno.

No se deben versionar ni documentar valores reales de:

- `JWT_SECRET`
- `DEVICE_SECRET_MASTER_KEY`
- secretos HMAC de collares
- `GEMINI_API_KEY`
- `APP_NOTIFICATIONS_EMAIL_API_KEY`
- contrasenas reales
- JWT reales

## Device ingestion IoT

Endpoint principal:

```http
POST /api/device/locations
```

Headers requeridos:

- `X-Device-Token`
- `X-Device-Timestamp`
- `X-Device-Nonce`
- `X-Device-Signature`

Payload esperado incluye ubicacion y telemetria:

- `latitude`
- `longitude`
- `timestamp`
- `batteryLevel`
- `gpsAccuracy`

El backend valida firma HMAC, timestamp, nonce unico, rango geografico, bateria, precision GPS y estado operativo asociado al collar.

Script relacionado:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\send-device-location.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<DEVICE_SECRET>" `
  -Latitude 1.2136 `
  -Longitude -77.2811 `
  -BatteryLevel 85 `
  -GpsAccuracy 8.5
```

## Alertas y analisis

El backend genera y analiza alertas operativas:

- Salida de geocerca.
- Collar offline.
- Bateria baja.
- Priorizacion heuristica.
- Resumen operativo.
- Top de prioridades.

Endpoints principales:

- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities`

## IA analitica

El endpoint de IA genera resumen analitico de alertas:

- `GET /api/alert-analysis/ai-summary`

Caracteristicas:

- Integracion con Gemini.
- Fallback heuristico `RULE_BASED_FALLBACK`.
- Metricas de uso y resultado de IA.
- Configuracion por variables de entorno.

## Notificaciones y outbox

Canales de notificacion soportados:

- `LOG`
- `WEBHOOK`
- `EMAIL`

EMAIL puede operar en dos modos:

- `direct`: envio directo con provider.
- `outbox`: encola mensajes y el processor los envia posteriormente.

Outbox EMAIL:

- Tabla `notification_outbox`.
- Estados: `PENDING`, `PROCESSING`, `SENT`, `FAILED`, `DEAD`.
- Processor programado para EMAIL.
- Recuperacion de `PROCESSING` atascados.
- Endpoint admin de diagnostico con respuesta paginada estable.
- Endpoint admin de requeue para `FAILED` y `DEAD`.

Endpoints admin:

- `GET /api/admin/notification-outbox`
- `GET /api/admin/notification-outbox/{id}`
- `POST /api/admin/notification-outbox/{id}/requeue`

El diagnostico administrativo no expone payload completo y redacta campos sensibles como tokens, passwords, secrets, `htmlBody` y `textBody`.

## Recuperacion de contrasena

Endpoints:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Caracteristicas:

- Respuesta generica para no revelar existencia de usuarios.
- Token seguro hasheado en base de datos.
- Limpieza programada de tokens expirados/usados.
- Email de recuperacion por Resend.
- Compatible con delivery-mode `direct` y `outbox`.

## Endpoints principales

Autenticacion:

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Vacas:

- `GET /api/cows` (legacy, sin paginacion)
- `GET /api/cows/page` (paginado, filtros por `status` y `active`)
- `POST /api/cows`
- `PUT /api/cows/{id}`
- `GET /api/cows/{id}`
- `GET /api/cows/token/{token}`
- `PATCH /api/cows/{id}/deactivate`
- `PATCH /api/cows/{id}/activate`

Collares:

- `GET /api/collars`
- `POST /api/collars`
- `PATCH /api/collars/{id}/rotate-secret`

Geocercas y ubicaciones:

- `GET /api/geofences` (legacy, sin paginacion)
- `GET /api/geofences/page` (paginado, filtro por `active`)
- `POST /api/geofences`
- `GET /api/geofences/{id}`
- `PATCH /api/geofences/{id}/deactivate`
- `PATCH /api/geofences/{id}/activate`
- `GET /api/locations/cow/{cowId}`
- `POST /api/locations`

Device ingestion:

- `POST /api/device/locations`

Alertas, dashboard y reportes:

- `GET /api/alerts`
- `GET /api/dashboard/summary`
- `GET /api/reports/alerts`
- `GET /api/reports/alerts.csv`

Analisis:

- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities`
- `GET /api/alert-analysis/ai-summary`

Outbox admin:

- `GET /api/admin/notification-outbox`
- `GET /api/admin/notification-outbox/{id}`
- `POST /api/admin/notification-outbox/{id}/requeue`

Salud:

- `GET /healthz`
- `GET /actuator/health`

Swagger/OpenAPI en desarrollo:

- `/swagger-ui.html`
- `/v3/api-docs`

## Scripts operativos

Scripts disponibles:

- `scripts/smoke-test-backend.ps1`
- `scripts/send-device-location.ps1`
- `scripts/test-email-notification-flow.ps1`
- `scripts/test-email-outbox-flow.ps1`
- `scripts/seed-demo-data.ps1`
- `scripts/load-test-device-ingestion.ps1`

Ejemplo seed demo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo-data.ps1 `
  -BaseUrl "http://localhost:8080" `
  -AdminEmail "<ADMIN_EMAIL>" `
  -AdminPassword "<ADMIN_PASSWORD>" `
  -Prefix "CargaDemo"
```

Ejemplo outbox email:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-email-outbox-flow.ps1 `
  -BaseUrl "http://localhost:8080" `
  -AdminEmail "<ADMIN_EMAIL>" `
  -AdminPassword "<ADMIN_PASSWORD>" `
  -ForgotPasswordEmail "<USER_EMAIL>" `
  -WaitSeconds 10
```

## Pruebas y calidad

Validacion principal:

```powershell
.\mvnw.cmd clean verify
```

El flujo de verificacion incluye:

- Compilacion.
- Tests unitarios.
- Tests de integracion.
- Testcontainers.
- JaCoCo.
- SpotBugs.
- Validaciones de cobertura configuradas en Maven.

Ultima evidencia conocida:

- Tests unitarios: 277.
- Tests de integracion: 198.
- Tests totales: 475.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- JaCoCo: OK.
- SpotBugs: OK.

Estos valores documentan la ultima ejecucion conocida reportada para el proyecto. En esta subfase documental no se vuelve a ejecutar `clean verify`.

## Pruebas de carga

Se agrego un script controlado para carga basica sobre device ingestion:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<DEVICE_SECRET_MASKED>" `
  -Requests 10 `
  -Concurrency 1 `
  -DelayMs 100
```

Resultados locales documentados:

- Smoke: 10/10 solicitudes exitosas.
- Light: 100/100 solicitudes exitosas.
- Medium: 500/500 solicitudes exitosas.

Estos resultados fueron obtenidos en entorno local y no representan produccion ni Render.

Evidencia tecnica:

- [docs/device-ingestion-load-test.md](docs/device-ingestion-load-test.md)

## Variables de entorno

Usar `.env.example` como referencia. No versionar secretos reales.

Variables principales:

```env
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080

DB_URL=jdbc:postgresql://localhost:5432/ganaderia4
DB_USERNAME=postgres
DB_PASSWORD=<DB_PASSWORD>

JWT_SECRET=<LONG_PRIVATE_JWT_SECRET>
JWT_EXPIRATION_MS=86400000

DEVICE_SECRET_MASTER_KEY=<LONG_PRIVATE_DEVICE_MASTER_KEY>
DEVICE_AUTH_WINDOW_SECONDS=300
DEVICE_HMAC_PEPPER=

APP_CORS_ALLOWED_ORIGINS=http://localhost:5173

AI_ENABLED=false
AI_PROVIDER=gemini
GEMINI_API_KEY=<GEMINI_API_KEY>
GEMINI_MODEL=gemini-2.5-flash

APP_NOTIFICATIONS_EMAIL_ENABLED=false
APP_NOTIFICATIONS_EMAIL_PROVIDER=resend
APP_NOTIFICATIONS_EMAIL_API_KEY=<RESEND_API_KEY>
APP_NOTIFICATIONS_EMAIL_FROM=
APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=direct

APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=false
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_FIXED_DELAY=30s
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_BATCH_SIZE=20
APP_NOTIFICATIONS_OUTBOX_EMAIL_RETRY_BACKOFF=1m
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSING_TIMEOUT=5m

APP_AUTH_PASSWORD_RESET_TOKEN_TTL=15m
APP_AUTH_PASSWORD_RESET_CLEANUP_ENABLED=true
APP_FRONTEND_PASSWORD_RESET_URL=http://localhost:5173/reset-password
```

Para EMAIL por outbox en prueba controlada:

```env
APP_NOTIFICATIONS_EMAIL_ENABLED=true
APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true
APP_NOTIFICATIONS_EMAIL_PROVIDER=resend
APP_NOTIFICATIONS_EMAIL_API_KEY=<RESEND_API_KEY>
APP_NOTIFICATIONS_EMAIL_FROM=Ganaderia 4.0 <onboarding@resend.dev>
```

## Ejecucion local

Requisitos:

- Java 17+
- PostgreSQL
- Maven Wrapper incluido
- Docker para tests de integracion con Testcontainers

Arranque local:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/healthz
```

Swagger en entorno local:

```text
http://localhost:8080/swagger-ui.html
```

## Despliegue en Render

Dominio actual:

```text
https://ganaderia-backend.onrender.com
```

Para Render se recomienda:

- Perfil `prod`.
- Puerto `10000` o `PORT` inyectado por Render.
- Base de datos PostgreSQL administrada.
- Variables de entorno configuradas en el panel de Render.
- Swagger deshabilitado en produccion.
- Secretos reales fuera del repositorio.

Ejemplo de variables:

```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=10000
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<user>
DB_PASSWORD=<password>
JWT_SECRET=<long-private-secret>
DEVICE_SECRET_MASTER_KEY=<long-private-secret>
APP_CORS_ALLOWED_ORIGINS=https://<frontend-domain>
AI_ENABLED=false
```

Para la demo, EMAIL e IA pueden estar habilitados con sus respectivas variables seguras. En ambientes locales o entornos de seguridad pueden permanecer apagados por configuracion.

No activar `outbox` o `processor` en produccion sin validar primero el flujo en local.

## Documentacion adicional

- [Pruebas de carga de device ingestion](docs/device-ingestion-load-test.md)
- [Ciclo de vida de collares](docs/collar-lifecycle.md)
- [Ciclo de vida de vacas](docs/cow-lifecycle.md)
- [Runbook operativo](docs/operational-runbook.md)
- [Matriz de permisos](docs/permissions-matrix.md)
- [Politica temporal](docs/time-policy.md)

## Limitaciones conocidas

- Las pruebas de carga documentadas son locales y no representan capacidad maxima real.
- No se documentan secretos reales ni deben compartirse por consola o capturas.
- No existe integracion SMS real documentada en este backend; no debe presentarse como funcionalidad implementada.
- El outbox implementado esta orientado a EMAIL.
- Actuator y Prometheus estan disponibles como endpoints de observabilidad; Grafana visual queda como mejora si no existe configuracion externa.
- La validacion frontend/backend final depende del dominio frontend configurado en CORS y en `APP_FRONTEND_PASSWORD_RESET_URL`.
- No hay `docker-compose.yml` en la raiz del proyecto.
- El throughput de las pruebas con PowerShell depende de `DelayMs`, concurrencia y maquina local.
- Para pruebas de rendimiento formales se recomienda complementar con k6, JMeter u observabilidad de CPU/RAM.

## Proximos pasos

- Documentar escenarios de rendimiento con metricas de infraestructura.
- Agregar pruebas de carga reproducibles con k6 o JMeter.
- Mantener actualizada la matriz de endpoints y permisos.
- Revisar periodicamente variables de entorno y secretos operativos.
- Validar en entorno staging antes de cambios de delivery-mode EMAIL/outbox en produccion.
