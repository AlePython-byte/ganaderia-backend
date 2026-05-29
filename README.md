# Ganadería 4.0 Backend

Backend del sistema **Ganadería 4.0**, orientado al monitoreo ganadero con collares IoT, telemetría GPS, geocercas, alertas operativas, autenticación JWT, integración HMAC para dispositivos, notificaciones y análisis operativo asistido por IA.

Dominio backend desplegado en Render:

```text
https://ganaderia-backend.onrender.com
```

---

## Tabla de contenido

- [Descripción general](#descripción-general)
- [Estado actual](#estado-actual)
- [Stack técnico](#stack-técnico)
- [Funcionalidades principales](#funcionalidades-principales)
- [Seguridad](#seguridad)
- [Device ingestion IoT](#device-ingestion-iot)
- [Alertas y análisis](#alertas-y-análisis)
- [IA analítica](#ia-analítica)
- [Notificaciones y outbox](#notificaciones-y-outbox)
- [Recuperación de contraseña](#recuperación-de-contraseña)
- [Endpoints principales](#endpoints-principales)
- [Scripts operativos](#scripts-operativos)
- [Pruebas y calidad](#pruebas-y-calidad)
- [Pruebas de carga](#pruebas-de-carga)
- [Variables de entorno](#variables-de-entorno)
- [Ejecución local](#ejecución-local)
- [Despliegue en Render](#despliegue-en-render)
- [Documentación adicional](#documentación-adicional)
- [Limitaciones conocidas](#limitaciones-conocidas)

---

## Descripción general

La aplicación expone una API REST en Spring Boot para administrar vacas, collares, geocercas, ubicaciones, alertas, usuarios, reportes y flujos operativos asociados al monitoreo de ganado. También incluye un canal IoT seguro para recibir ubicaciones desde dispositivos mediante firmas HMAC, protección anti-replay y validaciones temporales.

El backend está preparado para uso académico y técnico, con migraciones versionadas, pruebas automatizadas, observabilidad, scripts operativos y documentación complementaria.

## Estado actual

El proyecto cuenta actualmente con:

- Autenticación JWT y autorización por roles.
- CORS configurable para integración con frontend.
- Swagger/OpenAPI en perfiles de desarrollo.
- Actuator y métricas operativas.
- Auditoría de acciones relevantes.
- Dashboard, reportes y análisis de alertas.
- Device ingestion por HMAC con anti-replay persistente.
- Limpiezas programadas de nonces, rate limits y tokens de recuperación.
- Abuse protection/rate limiting para login, device ingestion, recuperación de contraseña, IA y requeue admin.
- Notificaciones por LOG, WEBHOOK y EMAIL.
- EMAIL real con Resend.
- EMAIL habilitado para la demo y configurable por entorno.
- Preferencias de notificación por usuario.
- Password reset por email.
- IA analítica con Gemini, DeepSeek y fallback heurístico.
- IA habilitada para la demo y configurable por entorno.
- Notification outbox para EMAIL, processor y requeue admin.
- Generación automática de tokens `COW-*`, `COLLAR-*` e internal code `INT-*`.
- Scripts operativos para smoke tests, seed demo, email, outbox y carga IoT.
- Pruebas unitarias, integración, JaCoCo y SpotBugs.
- Último `clean verify` conocido: 277 tests unitarios, 198 tests de integración, 475 tests totales, JaCoCo OK y SpotBugs OK.

## Stack técnico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17+ | Lenguaje principal |
| Spring Boot | 4.0.3 | Framework base |
| Spring Security | — | Autenticación y autorización |
| Spring Data JPA / Hibernate | — | Persistencia ORM |
| PostgreSQL | — | Base de datos relacional |
| Flyway | — | Migraciones versionadas |
| JJWT | 0.12.7 | Tokens JWT |
| HMAC SHA-256 | — | Autenticación de dispositivos IoT |
| Springdoc OpenAPI | 3.0.2 | Swagger UI / contrato REST |
| Spring Boot Actuator | — | Health checks y métricas |
| Micrometer / Prometheus | — | Métricas scrapeables |
| Resend | — | Proveedor de EMAIL real |
| Google Gemini | gemini-2.5-flash | IA analítica principal |
| DeepSeek | — | IA analítica alternativa |
| JUnit 5 | — | Tests unitarios e integración |
| MockMvc | — | Tests HTTP de controladores |
| Testcontainers | — | PostgreSQL real en integración |
| JaCoCo | 0.8.13 | Cobertura de código |
| SpotBugs | 4.9.8.3 | Análisis estático |
| Docker | — | Entorno para Testcontainers |
| GitHub Actions | — | CI |

## Funcionalidades principales

- Gestión de vacas con token técnico generado por backend (`COW-001`, `COW-002`, ...).
- Código interno de vaca generado por backend (`INT-001`, `INT-002`, ...).
- Campo `active` en vacas para activación/desactivación operativa sin borrar historial.
- Consulta paginada de vacas con filtros por `status` y `active`.
- Gestión de collares con token técnico generado por backend (`COLLAR-001`, `COLLAR-002`, ...).
- Asociación de collares a vacas (`assign`), habilitación/deshabilitación (`enable`/`disable`).
- Rotación de secreto HMAC de collares (solo `ADMINISTRADOR`).
- Gestión de geocercas con activación/desactivación por endpoint y consulta paginada.
- Registro manual y por dispositivo de ubicaciones.
- Persistencia de `batteryLevel` y `gpsAccuracy`.
- Alertas `EXIT_GEOFENCE`, `COLLAR_OFFLINE` y `LOW_BATTERY`.
- Dashboard operativo y reportes (con exportación CSV).
- Auditoría de acciones relevantes por actor y entidad.
- Preferencias de notificación por usuario.
- Recuperación de contraseña por email.
- Análisis heurístico e IA de alertas.
- Notification outbox para entrega EMAIL asíncrona.

## Seguridad

El backend combina seguridad de usuarios y seguridad de dispositivos:

- JWT para usuarios del sistema.
- Roles operativos: `ADMINISTRADOR`, `SUPERVISOR`, `TECNICO`, `OPERADOR`.
- HMAC para dispositivos IoT.
- Anti-replay mediante nonces persistentes.
- Validación temporal de requests IoT.
- Rate limiting y protección de abuso en 6 flujos sensibles.
- Sanitización de logs y respuestas administrativas sensibles.
- Password reset con token seguro hasheado en base de datos.
- Secretos siempre configurados por variables de entorno.

No se deben versionar ni documentar valores reales de:

- `JWT_SECRET`
- `DEVICE_SECRET_MASTER_KEY`
- secretos HMAC de collares
- `GEMINI_API_KEY`
- `APP_NOTIFICATIONS_EMAIL_API_KEY`
- contraseñas reales
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

Payload esperado incluye ubicación y telemetría:

- `latitude`
- `longitude`
- `timestamp`
- `batteryLevel`
- `gpsAccuracy`

El backend valida firma HMAC, timestamp, nonce único, rango geográfico, batería, precisión GPS y estado operativo asociado al collar.

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

## Alertas y análisis

El backend genera y analiza alertas operativas:

- Salida de geocerca (`EXIT_GEOFENCE`).
- Collar offline (`COLLAR_OFFLINE`).
- Batería baja (`LOW_BATTERY`).
- Priorización heurística.
- Resumen operativo.
- Top de prioridades.

Endpoints principales:

- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities`

## IA analítica

El endpoint de IA genera resumen analítico de alertas:

- `GET /api/alert-analysis/ai-summary`

Características:

- Integración con Google Gemini (proveedor principal).
- Integración con DeepSeek (proveedor alternativo).
- Fallback heurístico `RULE_BASED_FALLBACK` cuando el proveedor no está disponible.
- Métricas de uso y resultado de IA.
- Configuración por variables de entorno (`AI_PROVIDER=gemini` o `AI_PROVIDER=deepseek`).

## Notificaciones y outbox

Canales de notificación soportados:

- `LOG`
- `WEBHOOK`
- `EMAIL`

EMAIL puede operar en dos modos:

- `direct`: envío directo con provider.
- `outbox`: encola mensajes y el processor los envía posteriormente.

Outbox EMAIL:

- Tabla `notification_outbox`.
- Estados: `PENDING`, `PROCESSING`, `SENT`, `FAILED`, `DEAD`.
- Processor programado para EMAIL.
- Recuperación de `PROCESSING` atascados.
- Endpoint admin de diagnóstico con respuesta paginada estable.
- Endpoint admin de requeue para `FAILED` y `DEAD`.

Endpoints admin:

- `GET /api/admin/notification-outbox`
- `GET /api/admin/notification-outbox/{id}`
- `POST /api/admin/notification-outbox/{id}/requeue`

El diagnóstico administrativo no expone payload completo y redacta campos sensibles como tokens, passwords, secrets, `htmlBody` y `textBody`.

## Recuperación de contraseña

Endpoints públicos:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Características:

- Respuesta genérica para no revelar existencia de usuarios.
- Token seguro hasheado en base de datos.
- Limpieza programada de tokens expirados/usados.
- Email de recuperación por Resend.
- Compatible con delivery-mode `direct` y `outbox`.

## Endpoints principales

### Autenticación (públicos)

- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

### Autenticación (autenticado)

- `GET /api/auth/me`

### Vacas

- `GET /api/cows` (lista completa sin paginación)
- `GET /api/cows/page` (paginado, filtros por `status` y `active`)
- `POST /api/cows`
- `GET /api/cows/{id}`
- `PUT /api/cows/{id}`
- `GET /api/cows/token/{token}`
- `PATCH /api/cows/{id}/activate`
- `PATCH /api/cows/{id}/deactivate`

### Collares

- `GET /api/collars`
- `GET /api/collars/page`
- `POST /api/collars`
- `PUT /api/collars/{id}`
- `PATCH /api/collars/{id}/assign/{cowId}`
- `PATCH /api/collars/{id}/enable`
- `PATCH /api/collars/{id}/disable`
- `PATCH /api/collars/{id}/rotate-secret` (solo `ADMINISTRADOR`)

### Geocercas y ubicaciones

- `GET /api/geofences` (lista completa sin paginación)
- `GET /api/geofences/page` (paginado, filtro por `active`)
- `POST /api/geofences`
- `GET /api/geofences/{id}`
- `PATCH /api/geofences/{id}/activate`
- `PATCH /api/geofences/{id}/deactivate`
- `GET /api/locations/cow/{cowId}`
- `GET /api/locations/cow/{cowId}/last`
- `POST /api/locations`

### Device ingestion

- `POST /api/device/locations`

### Alertas

- `GET /api/alerts`
- `GET /api/alerts/page`
- `GET /api/alerts/status/{status}`
- `GET /api/alerts/pending/priority-queue`
- `PATCH /api/alerts/{id}/resolve` (solo `ADMINISTRADOR`)
- `PATCH /api/alerts/{id}/discard` (solo `ADMINISTRADOR`)

### Dashboard

- `GET /api/dashboard/summary`

### Reportes (solo `ADMINISTRADOR` y `SUPERVISOR`)

- `GET /api/reports/alerts`
- `GET /api/reports/alerts/page`
- `GET /api/reports/alerts/trend`
- `GET /api/reports/alerts/type-recurrence`
- `GET /api/reports/alerts/export.csv`
- `GET /api/reports/offline-collars`
- `GET /api/reports/offline-collars/staleness`
- `GET /api/reports/cows-most-incidents`
- `GET /api/reports/cows-incident-recurrence`

### Análisis de alertas

- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities`
- `GET /api/alert-analysis/ai-summary`

### Administración de outbox (solo `ADMINISTRADOR`)

- `GET /api/admin/notification-outbox`
- `GET /api/admin/notification-outbox/{id}`
- `POST /api/admin/notification-outbox/{id}/requeue`

### Usuarios y auditoría (solo `ADMINISTRADOR`)

- `GET /api/users`
- `GET /api/users/page`
- `GET /api/audit-logs`

### Salud y observabilidad

- `GET /healthz` (público)
- `GET /actuator/health` (público)
- `GET /actuator/metrics` (solo `ADMINISTRADOR`)
- `GET /actuator/prometheus` (solo `ADMINISTRADOR`)

### Swagger/OpenAPI (en perfiles con Springdoc habilitado)

- `/swagger-ui.html`
- `/swagger-ui/index.html`
- `/v3/api-docs`

## Scripts operativos

Scripts disponibles en `scripts/`:

| Script | Propósito |
|---|---|
| `smoke-test-backend.ps1` | Valida health, login y endpoints de análisis |
| `seed-demo-data.ps1` | Crea o reutiliza datos demo (vacas, collares, geocerca, ubicaciones) |
| `send-device-location.ps1` | Envía una ubicación firmada con HMAC |
| `test-email-notification-flow.ps1` | Valida flujos de email y notificación |
| `test-email-outbox-flow.ps1` | Valida forgot-password con outbox y diagnóstico admin |
| `load-test-device-ingestion.ps1` | Carga controlada contra device ingestion |

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

Validación principal:

```powershell
.\mvnw.cmd clean verify
```

El flujo de verificación incluye:

- Compilación.
- Tests unitarios.
- Tests de integración con Testcontainers.
- JaCoCo (cobertura).
- SpotBugs (análisis estático).

Última evidencia conocida:

- Tests unitarios: 277.
- Tests de integración: 198.
- Tests totales: 475.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- JaCoCo: OK.
- SpotBugs: OK.

> Estos valores documentan la última ejecución conocida reportada para el proyecto.

## Pruebas de carga

Script controlado para carga básica sobre device ingestion:

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

> Estos resultados fueron obtenidos en entorno local y no representan producción ni Render.

Evidencia técnica: [docs/device-ingestion-load-test.md](docs/device-ingestion-load-test.md)

Para pruebas de rendimiento más avanzadas con k6: [docs/k6-performance-guide.md](docs/k6-performance-guide.md)

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

## Ejecución local

Requisitos:

- Java 17+
- PostgreSQL
- Maven Wrapper incluido en el repositorio
- Docker Desktop para tests de integración con Testcontainers

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
http://localhost:8080/swagger-ui/index.html
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
- Swagger deshabilitado en producción.
- Secretos reales fuera del repositorio.

Ejemplo de variables mínimas para Render:

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

Para la demo, EMAIL e IA pueden estar habilitados con sus respectivas variables seguras. No activar `outbox` o `processor` en producción sin validar primero el flujo en local.

## Documentación adicional

| Documento | Contenido |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Arquitectura técnica, módulos, flujos críticos y patrones de diseño |
| [docs/operational-runbook.md](docs/operational-runbook.md) | Runbook operativo: arranque, validación, troubleshooting |
| [docs/permissions-matrix.md](docs/permissions-matrix.md) | Matriz RBAC oficial por endpoint y rol |
| [docs/demo-guide.md](docs/demo-guide.md) | Guía paso a paso para presentación ante jurado |
| [docs/collar-lifecycle.md](docs/collar-lifecycle.md) | Lifecycle de collares: status, enabled, signalStatus |
| [docs/cow-lifecycle.md](docs/cow-lifecycle.md) | Lifecycle de vacas: campo `active` |
| [docs/time-policy.md](docs/time-policy.md) | Política UTC y manejo de timestamps |
| [docs/rate-limiting-guide.md](docs/rate-limiting-guide.md) | Abuse protection y rate limiting por flujo |
| [docs/email-throttle-guide.md](docs/email-throttle-guide.md) | Throttling de email outbox |
| [docs/device-ingestion-load-test.md](docs/device-ingestion-load-test.md) | Pruebas de carga en device ingestion |
| [docs/device-ingestion-test.md](docs/device-ingestion-test.md) | Tests funcionales de device ingestion |
| [docs/observability-local-guide.md](docs/observability-local-guide.md) | Guía de observabilidad local (Actuator, Prometheus) |
| [docs/grafana-local-guide.md](docs/grafana-local-guide.md) | Guía de Grafana local |
| [docs/k6-performance-guide.md](docs/k6-performance-guide.md) | Guía de performance con k6 |
| [docs/k6-operational-read-guide.md](docs/k6-operational-read-guide.md) | Guía operativa de k6 |
| [docs/k6-performance-results.md](docs/k6-performance-results.md) | Resultados de performance con k6 |
| [docs/frontend-backend-cors-validation.md](docs/frontend-backend-cors-validation.md) | Validación de CORS frontend-backend |
| [docs/security-secrets-checklist.md](docs/security-secrets-checklist.md) | Checklist de seguridad y secretos |
| [docs/phase-1-hardening-summary.md](docs/phase-1-hardening-summary.md) | Resumen de hardening de seguridad fase 1 |
| [docs/testing-quality-report.md](docs/testing-quality-report.md) | Reporte de calidad de testing |
| [docs/final-delivery-validation.md](docs/final-delivery-validation.md) | Validación final de entrega |
| [docs/final-technical-smoke-checklist.md](docs/final-technical-smoke-checklist.md) | Checklist técnico final de smoke tests |

## Limitaciones conocidas

- Las pruebas de carga documentadas son locales y no representan capacidad máxima real.
- No se documentan secretos reales ni deben compartirse por consola o capturas.
- No existe integración SMS real documentada en este backend; no debe presentarse como funcionalidad implementada.
- El outbox implementado está orientado a EMAIL.
- Actuator y Prometheus están disponibles como endpoints de observabilidad; Grafana visual queda como mejora si no existe configuración externa.
- La validación frontend/backend final depende del dominio frontend configurado en CORS y en `APP_FRONTEND_PASSWORD_RESET_URL`.
- No hay `docker-compose.yml` en la raíz del proyecto.
- El throughput de las pruebas con PowerShell depende de `DelayMs`, concurrencia y máquina local.
- Los campos temporales de dominio usan `LocalDateTime` interpretado como UTC por política; la migración a `Instant` u `OffsetDateTime` está pendiente para una fase futura.
