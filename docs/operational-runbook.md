# Runbook operativo — Ganadería 4.0

## 1. Propósito del documento

Este runbook documenta operación, validación y troubleshooting del backend Ganadería 4.0. Está pensado para guiar a un jurado, profesor, operador técnico o desarrollador externo durante una demo, una revisión de despliegue o una investigación de fallos.

No reemplaza el código fuente ni la configuración real del entorno. La fuente de verdad operativa sigue siendo la configuración activa de Spring Boot, las variables de entorno y los logs del backend.

## 2. Ambientes soportados

### Local

Ambiente de desarrollo en la máquina del equipo. Se usa para ejecutar el backend con PostgreSQL local o externo, validar Swagger, probar scripts PowerShell y depurar flujos funcionales.

### Test

Ambiente de pruebas automatizadas. La suite usa Maven Wrapper, JUnit, MockMvc, Testcontainers, JaCoCo y SpotBugs.

### Render/demo

Ambiente desplegado para demostración:

```text
https://ganaderia-backend.onrender.com
```

Algunos endpoints requieren JWT y roles específicos. Swagger puede no estar disponible si el perfil de producción lo deshabilita.

## 3. Requisitos locales

- Java 17 o superior, según `pom.xml` y Maven Enforcer.
- Maven Wrapper incluido en el repositorio (`mvnw.cmd` en Windows).
- Docker Desktop activo para tests de integración con Testcontainers.
- PostgreSQL para ejecución local manual.
- PowerShell para scripts operativos.
- Variables de entorno configuradas con placeholders seguros, sin secretos reales en archivos versionados.

## 4. Variables de entorno

No incluir valores reales en documentación, commits, capturas o tickets. Usar placeholders.

### Base de datos

```env
DB_URL=<DB_URL>
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<POSTGRES_PASSWORD>
```

### JWT

```env
JWT_SECRET=<JWT_SECRET>
JWT_EXPIRATION_MS=86400000
```

### Bootstrap admin

```env
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_NAME=<ADMIN_NAME>
APP_BOOTSTRAP_ADMIN_EMAIL=<ADMIN_EMAIL>
APP_BOOTSTRAP_ADMIN_PASSWORD=<ADMIN_PASSWORD>
APP_BOOTSTRAP_ADMIN_UPDATE_EXISTING=false
APP_BOOTSTRAP_ADMIN_RESET_PASSWORD=false
```

### CORS

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://<FRONTEND_DOMAIN>
```

### Email / Resend

```env
APP_NOTIFICATIONS_EMAIL_ENABLED=true
APP_NOTIFICATIONS_EMAIL_PROVIDER=resend
APP_NOTIFICATIONS_EMAIL_API_KEY=<RESEND_API_KEY>
APP_NOTIFICATIONS_EMAIL_FROM=<EMAIL_FROM>
APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=direct
APP_FRONTEND_PASSWORD_RESET_URL=<FRONTEND_RESET_PASSWORD_URL>
```

En la demo, EMAIL puede estar habilitado. En local o ambientes seguros puede apagarse por configuración.

### IA analítica

El proveedor por defecto es **Claude (Anthropic)**:

```env
AI_ENABLED=true
AI_PROVIDER=claude
CLAUDE_API_KEY=<CLAUDE_API_KEY>
```

Proveedores alternativos:

```env
# Gemini
AI_PROVIDER=gemini
GEMINI_API_KEY=<GEMINI_API_KEY>
GEMINI_MODEL=gemini-2.5-flash

# DeepSeek
AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=<DEEPSEEK_API_KEY>
```

En la demo, IA puede estar habilitada. Si el proveedor falla o la IA está apagada, el backend usa fallback heurístico automáticamente.

### Device ingestion / HMAC

```env
DEVICE_SECRET_MASTER_KEY=<DEVICE_SECRET>
DEVICE_AUTH_WINDOW_SECONDS=300
DEVICE_HMAC_PEPPER=
```

El secreto real de un collar no debe documentarse ni compartirse.

### Outbox / notificaciones

```env
APP_NOTIFICATIONS_WEBHOOK_ENABLED=false
APP_NOTIFICATIONS_WEBHOOK_URL=<WEBHOOK_URL>
APP_NOTIFICATIONS_WEBHOOK_SECRET=<WEBHOOK_SECRET>

APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=false
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_FIXED_DELAY=30s
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_BATCH_SIZE=20
APP_NOTIFICATIONS_OUTBOX_EMAIL_RETRY_BACKOFF=1m
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSING_TIMEOUT=5m
```

Para una prueba controlada de EMAIL por outbox:

```env
APP_NOTIFICATIONS_EMAIL_ENABLED=true
APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox
APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true
```

### Actuator / observabilidad

```env
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

## 5. Arranque local

1. Clonar el repositorio.
2. Configurar PostgreSQL y variables de entorno.
3. Ejecutar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Validar health:

```powershell
Invoke-RestMethod http://localhost:8080/healthz
Invoke-RestMethod http://localhost:8080/actuator/health
```

5. Validar Swagger en entorno local si está habilitado:

```text
http://localhost:8080/swagger-ui/index.html
```

Si se usa otro puerto, reemplazar `8080` por el puerto configurado.

## 6. Validación de despliegue en Render

Dominio:

```text
https://ganaderia-backend.onrender.com
```

Validaciones básicas:

```text
GET https://ganaderia-backend.onrender.com/healthz
GET https://ganaderia-backend.onrender.com/actuator/health
GET https://ganaderia-backend.onrender.com/swagger-ui/index.html
```

Notas:

- Render puede tardar en responder el primer request si el servicio está frío.
- Algunos endpoints requieren `Authorization: Bearer <token>`.
- Swagger puede estar deshabilitado según el perfil activo.

## 7. Autenticación y roles

Login:

```http
POST /api/auth/login
```

Los endpoints protegidos usan:

```http
Authorization: Bearer <token>
```

Roles principales:

- `ADMINISTRADOR`
- `SUPERVISOR`
- `TECNICO`
- `OPERADOR`

Endpoints de auth sin JWT (públicos):

- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

No documentar credenciales reales. Para pruebas usar usuarios demo controlados o credenciales gestionadas por el entorno.

## 8. Device ingestion IoT

Endpoint:

```http
POST /api/device/locations
```

Headers requeridos:

- `X-Device-Token`
- `X-Device-Timestamp`
- `X-Device-Nonce`
- `X-Device-Signature`

El endpoint valida:

- Firma HMAC del request.
- Token de dispositivo.
- Timestamp dentro de ventana válida.
- Nonce único persistido para evitar replay.
- Collar existente, habilitado y operativo.
- Payload con latitud, longitud y timestamp.
- `batteryLevel`.
- `gpsAccuracy`.

Scripts relacionados:

```powershell
.\scripts\send-device-location.ps1
.\scripts\load-test-device-ingestion.ps1
```

No incluir `DeviceSecret` real en documentación, consola compartida o capturas.

## 9. EMAIL y recuperación de contraseña

Endpoints públicos:

```http
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Características:

- Envío de email con Resend.
- EMAIL habilitado para demo y configurable por ambiente.
- Puede apagarse en local o ambientes seguros.
- `forgot-password` no revela si el email existe.
- Tokens de recuperación seguros y hasheados en base de datos.
- Limpieza programada de tokens usados o expirados.

Scripts relacionados:

```powershell
.\scripts\test-email-notification-flow.ps1
.\scripts\test-email-outbox-flow.ps1
```

## 10. Outbox EMAIL

Outbox implementado para EMAIL.

Estados:

- `PENDING`
- `PROCESSING`
- `SENT`
- `FAILED`
- `DEAD`

Endpoints admin:

```http
GET /api/admin/notification-outbox
GET /api/admin/notification-outbox/{id}
POST /api/admin/notification-outbox/{id}/requeue
```

Notas operativas:

- Requiere rol `ADMINISTRADOR`.
- El endpoint de requeue permite recuperar mensajes `FAILED` o `DEAD`.
- El endpoint admin no debe exponer payload completo, tokens, passwords, secrets, `htmlBody` ni `textBody`.
- No es un outbox universal; está implementado para EMAIL.

## 11. IA analítica

Endpoints:

```http
GET /api/alert-analysis/summary
GET /api/alert-analysis/top-priorities?limit=5
GET /api/alert-analysis/ai-summary
```

Características:

- IA con Claude (Anthropic) como proveedor por defecto (modelo `claude-haiku-4-5-20251001`).
- Proveedores alternativos: Gemini y DeepSeek (seleccionables con `AI_PROVIDER`).
- IA habilitada para demo y configurable por entorno.
- Fallback heurístico automático si el proveedor falla o si IA está apagada.
- Métricas de uso, resultado y fallback.

Campos esperados en respuestas analíticas:

- `summary`
- `riskLevel`
- `recommendations`
- `source`
- `fallbackUsed`
- `generatedAt`

No incluir `GEMINI_API_KEY` real.

## 12. Observabilidad

El backend usa:

- Logs estructurados con `event=...`.
- Correlación por `X-Request-Id`.
- Health endpoint propio `/healthz`.
- Spring Boot Actuator.
- Métricas Prometheus.

Endpoints:

```http
GET /healthz
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

Algunos endpoints de Actuator pueden requerir JWT con rol `ADMINISTRADOR`, según la configuración de seguridad activa.

## 13. Pruebas y calidad

Comando principal:

```powershell
.\mvnw.cmd clean verify
```

Último resultado conocido:

- Tests unitarios: 277.
- Tests de integración: 198.
- Tests totales: 475.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- JaCoCo OK.
- SpotBugs OK.

Notas:

- Los tests de integración usan Testcontainers.
- Docker Desktop debe estar iniciado.
- Si Docker no está activo, puede fallar la fase de integración.
- Este runbook documenta el último resultado conocido; no implica que `clean verify` se haya ejecutado durante cada cambio documental.

## 14. Scripts operativos

### `scripts/smoke-test-backend.ps1`

Valida health, login y endpoints de análisis principales.

Parámetros principales:

- `BaseUrl`
- `Email`
- `Password`

### `scripts/seed-demo-data.ps1`

Crea o reutiliza datos demo: vacas, collares, geocerca, ubicaciones y preferencias de notificación.

Parámetros principales:

- `BaseUrl`
- `AdminEmail`
- `AdminPassword`
- `Prefix`
- `SkipExisting`
- `IncludeEmailPreferences`
- `IncludeGeofence`

### `scripts/send-device-location.ps1`

Envía una ubicación firmada con HMAC a `POST /api/device/locations`.

Parámetros principales:

- `BaseUrl`
- `DeviceToken`
- `DeviceSecret`
- `Latitude`
- `Longitude`
- `BatteryLevel`
- `GpsAccuracy`

### `scripts/test-email-notification-flow.ps1`

Valida flujos operativos de email/notificación, incluyendo escenarios como baja batería.

Parámetros principales:

- `BaseUrl`
- `Email`
- `Password`
- `Mode`
- `CollarToken`
- `DeviceToken`
- `DeviceSecret`
- `BatteryLevel`
- `PollAttempts`
- `PollDelaySeconds`

### `scripts/test-email-outbox-flow.ps1`

Valida forgot-password con EMAIL por outbox y consulta el endpoint admin del outbox.

Parámetros principales:

- `BaseUrl`
- `AdminEmail`
- `AdminPassword`
- `ForgotPasswordEmail`
- `WaitSeconds`

### `scripts/load-test-device-ingestion.ps1`

Ejecuta carga controlada contra `POST /api/device/locations`.

Parámetros principales:

- `BaseUrl`
- `DeviceToken`
- `DeviceSecret`
- `Requests`
- `Concurrency`
- `DelayMs`
- `Latitude`
- `Longitude`
- `BatteryLevel`
- `GpsAccuracy`
- `VerboseErrors`

## 15. Troubleshooting

| Problema | Síntoma | Causa probable | Acción recomendada |
| --- | --- | --- | --- |
| 401 Unauthorized | Respuesta 401 en endpoint protegido | JWT ausente, inválido o expirado | Ejecutar login, revisar `Authorization: Bearer <token>` y expiración. |
| 403 Forbidden | Usuario autenticado sin acceso | Rol insuficiente | Validar rol real del usuario y matriz de permisos. |
| CORS bloqueado | El navegador bloquea la llamada | Origen frontend no permitido | Revisar `APP_CORS_ALLOWED_ORIGINS`. |
| Render lento al primer request | Primer request demora o parece colgar | Servicio frío o inicializando | Esperar y reintentar; validar logs y `/healthz`. |
| Testcontainers falla | `Could not find a valid Docker environment` | Docker Desktop apagado | Iniciar Docker Desktop y repetir `.\mvnw.cmd clean verify`. |
| EMAIL no llega | Forgot-password responde OK pero no llega correo | Email deshabilitado, API key incorrecta, remitente inválido o provider fallando | Revisar variables Resend, logs `password_reset_email_*` y outbox si aplica. |
| IA no responde | AI summary usa fallback o falla | API key ausente, cuota, timeout o proveedor no disponible | Revisar `AI_ENABLED`, `AI_PROVIDER`, la API key del proveedor activo, logs y `fallbackUsed`. |
| Outbox queda en FAILED | Mensajes no enviados | Error de provider o payload/configuración inválida | Revisar `lastErrorSummary`, logs y configuración EMAIL. |
| Outbox queda en DEAD | Mensaje agotó reintentos | Fallo persistente tras max attempts | Revisar detalle admin y usar requeue solo si la causa fue corregida. |
| Device ingestion 401 por firma inválida | Request IoT rechazado | Canonical request, secreto o body firmado no coinciden | Usar `send-device-location.ps1` y confirmar que body firmado es el mismo enviado. |
| Device ingestion 401 por nonce repetido | Request rechazado por replay | Nonce ya usado | Generar nonce único por request. |
| Device ingestion 400 por payload inválido | Error de validación | Timestamp futuro/antiguo, lat/lon inválidos, batería o GPS fuera de rango | Revisar payload, timestamp local/UTC y rangos. |
| Actuator metrics 403 | `/actuator/metrics` o `/actuator/prometheus` denegado | Falta rol `ADMINISTRADOR` o endpoint restringido | Usar JWT admin y revisar exposición Actuator. |

## 16. Checklist rápido de demo técnica

- Backend Render health OK: `https://ganaderia-backend.onrender.com/healthz`.
- Login OK.
- EMAIL OK.
- IA OK.
- Seed demo OK.
- Device ingestion OK.
- Outbox admin OK.
- Tests conocidos en verde: 475 totales, 0 failures, 0 errors.
- Swagger accesible si el perfil lo permite.
- No hay secretos reales en consola compartida, README, runbook o capturas.

## Referencias internas

- [README principal](../README.md)
- [Arquitectura técnica](architecture.md)
- [Matriz de permisos](permissions-matrix.md)
- [Guía de demo técnica](demo-guide.md)
- [Lifecycle de collares](collar-lifecycle.md)
- [Lifecycle de vacas](cow-lifecycle.md)
- [Política temporal UTC](time-policy.md)
- [Rate limiting y abuse protection](rate-limiting-guide.md)
- [Email throttle guide](email-throttle-guide.md)
- [Pruebas de carga device ingestion](device-ingestion-load-test.md)
- [Guía k6 performance](k6-performance-guide.md)
- [Observabilidad local](observability-local-guide.md)
