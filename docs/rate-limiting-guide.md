# Guía de rate limiting y abuse protection — Ganadería 4.0

## 1. Propósito

Esta guía documenta cómo funciona la protección anti-abuso del backend Ganadería 4.0, qué flujos protege, cómo se configura y cómo se valida antes de una demo o entrega técnica.

El objetivo es que un desarrollador, evaluador o administrador pueda entender qué límites existen, qué riesgos mitigan y cómo ajustar los valores sin modificar código fuente.

## 2. Visión general

El backend usa rate limiting persistente para flujos sensibles. La protección se basa en registros guardados en base de datos mediante `AbuseRateLimitEntry`, agrupados por `scope` y una clave `abuseKey`.

Características principales:

- Las claves sensibles se almacenan como hashes, no como emails, tokens, IPs o identificadores en claro.
- Cada flujo usa scopes específicos por tipo de acción.
- Los límites se configuran con ventana, máximo de intentos y duración de bloqueo.
- Al superar el límite se responde `429 Too Many Requests`.
- La respuesta incluye el header `Retry-After`.
- El body usa `ErrorResponseDTO` e incluye `requestId`.
- Existe limpieza programada para entradas antiguas o inactivas.

## 3. Componentes principales

| Componente | Responsabilidad |
| --- | --- |
| `AbuseRateLimitEntry` | Entidad persistente que guarda scope, clave hasheada, ventana, contador y bloqueo. |
| `AbuseRateLimitRepository` | Acceso a datos para consultar, insertar, actualizar y limpiar entradas de rate limit. |
| `JpaAbuseProtectionService` | Implementación persistente de `AbuseProtectionService`; evalúa ventanas, contadores y bloqueos. |
| `LoginAbuseProtectionService` | Protege `POST /api/auth/login` contra fuerza bruta. |
| `DeviceAbuseProtectionService` | Protege `POST /api/device/locations` contra abuso de device ingestion. |
| `PasswordResetAbuseProtectionService` | Protege `forgot-password` y `reset-password`. |
| `AiAnalysisAbuseProtectionService` | Protege `GET /api/alert-analysis/ai-summary` para evitar abuso de Gemini/IA. |
| `OutboxAdminAbuseProtectionService` | Protege el requeue administrativo de mensajes de outbox. |
| `AbuseRateLimitCleanupJob` | Limpia entradas antiguas/inactivas de rate limit. |
| `TooManyRequestsException` | Excepción usada para representar exceso de límite. |
| `GlobalExceptionHandler` | Convierte `TooManyRequestsException` en respuesta HTTP `429` con `Retry-After` y `ErrorResponseDTO`. |

## 4. Modelo de persistencia

La entidad `AbuseRateLimitEntry` se persiste en la tabla `abuse_rate_limits`. Tiene una restricción única por `scope` y `abuse_key`, lo que permite mantener contadores independientes por flujo y criterio.

| Campo | Uso |
| --- | --- |
| `id` | Identificador interno de la entrada. |
| `scope` | Tipo de límite aplicado, por ejemplo `LOGIN_IP` o `AI_SUMMARY_USER`. |
| `abuseKey` | Hash de la clave sensible normalizada. No debe contener email, token, IP ni JWT en claro. |
| `windowStart` | Inicio de la ventana de conteo actual. |
| `attemptCount` | Cantidad de intentos acumulados dentro de la ventana. |
| `blockedUntil` | Instante hasta el cual la clave queda bloqueada, si excedió el límite. |
| `createdAt` | Fecha de creación del registro. |
| `updatedAt` | Fecha de última actualización del registro. |

`abuseKey` guarda hashes SHA-256 generados a partir de valores normalizados. Esto reduce el riesgo de exposición si se inspecciona la tabla o aparecen logs operativos.

## 5. Flujos protegidos

| Flujo | Endpoint | Criterios | Scopes reales | Riesgo mitigado |
| --- | --- | --- | --- | --- |
| Login | `POST /api/auth/login` | IP, email, IP+email | `LOGIN_IP`, `LOGIN_EMAIL`, `LOGIN_IP_EMAIL` | Fuerza bruta de credenciales. |
| Device ingestion | `POST /api/device/locations` | device token, IP+token, IP sin token | `DEVICE_TOKEN`, `DEVICE_IP_TOKEN`, `DEVICE_IP` | Spam IoT, abuso con firmas inválidas, presión sobre HMAC/persistencia. |
| Forgot password | `POST /api/auth/forgot-password` | IP, email, IP+email | `PASSWORD_RESET_FORGOT_IP`, `PASSWORD_RESET_FORGOT_EMAIL`, `PASSWORD_RESET_FORGOT_IP_EMAIL` | Spam de recuperación, abuso de Resend, enumeración indirecta. |
| Reset password | `POST /api/auth/reset-password` | IP, token, IP+token | `PASSWORD_RESET_TOKEN_IP`, `PASSWORD_RESET_TOKEN`, `PASSWORD_RESET_TOKEN_IP_TOKEN` | Intentos repetidos con token inválido, usado o expirado. |
| AI summary | `GET /api/alert-analysis/ai-summary` | usuario, IP, usuario+IP | `AI_SUMMARY_USER`, `AI_SUMMARY_IP`, `AI_SUMMARY_USER_IP` | Abuso/costo de Gemini y saturación del endpoint IA. |
| Outbox requeue admin | `POST /api/admin/notification-outbox/{id}/requeue` | usuario, IP, usuario+mensaje, usuario+IP | `OUTBOX_REQUEUE_USER`, `OUTBOX_REQUEUE_IP`, `OUTBOX_REQUEUE_USER_MESSAGE`, `OUTBOX_REQUEUE_USER_IP` | Reintentos administrativos abusivos, carga de outbox y posibles emails repetidos. |

Notas funcionales:

- `GET /api/alert-analysis/summary` y `GET /api/alert-analysis/top-priorities` no se limitan en la fase actual.
- `GET /api/admin/notification-outbox` y `GET /api/admin/notification-outbox/{id}` no se limitan en la fase actual.
- El processor automático del outbox no usa el rate limit de requeue admin.

## 6. Respuesta cuando se supera el límite

Cuando una clave excede el límite configurado, el cliente recibe:

- HTTP status: `429 Too Many Requests`.
- Header: `Retry-After`.
- Body: `ErrorResponseDTO`.
- `code`: `TOO_MANY_REQUESTS`.
- `requestId`: incluido para correlación con logs.

Ejemplo seguro:

```json
{
  "timestamp": "...",
  "status": 429,
  "error": "Too Many Requests",
  "code": "TOO_MANY_REQUESTS",
  "message": "Demasiadas solicitudes. Intenta nuevamente más tarde.",
  "path": "/api/auth/forgot-password",
  "requestId": "<REQUEST_ID>"
}
```

La respuesta no debe exponer JWT, DeviceSecret, tokens de reset, API keys, connection strings ni detalles internos.

## 7. Variables de configuración

Las propiedades reales se definen bajo `app.abuse-protection...` y pueden alimentarse desde variables de entorno.

| Variable / propiedad | Flujo | Default | Descripción |
| --- | --- | --- | --- |
| `app.abuse-protection.enabled` / `APP_ABUSE_PROTECTION_ENABLED` | Global | `true` | Activa o desactiva la protección anti-abuso general. |
| `app.abuse-protection.client-ip.trust-forwarded-headers` / `APP_ABUSE_PROTECTION_TRUST_FORWARDED_HEADERS` | Resolución IP | `false` | Permite confiar en headers forwarded si el despliegue/proxy lo requiere. |
| `app.abuse-protection.login.enabled` / `APP_ABUSE_PROTECTION_LOGIN_ENABLED` | Login | `true` | Activa rate limiting de login. |
| `app.abuse-protection.login.window` / `APP_ABUSE_PROTECTION_LOGIN_WINDOW` | Login | `15m` | Ventana de conteo. |
| `app.abuse-protection.login.max-attempts` / `APP_ABUSE_PROTECTION_LOGIN_MAX_ATTEMPTS` | Login | `5` | Máximo de intentos en la ventana. |
| `app.abuse-protection.login.block-duration` / `APP_ABUSE_PROTECTION_LOGIN_BLOCK_DURATION` | Login | `15m` | Duración del bloqueo. |
| `app.abuse-protection.device.enabled` / `APP_ABUSE_PROTECTION_DEVICE_ENABLED` | Device ingestion | `true` | Activa rate limiting de device ingestion. |
| `app.abuse-protection.device.window` / `APP_ABUSE_PROTECTION_DEVICE_WINDOW` | Device ingestion | `1m` | Ventana de conteo. |
| `app.abuse-protection.device.max-attempts` / `APP_ABUSE_PROTECTION_DEVICE_MAX_ATTEMPTS` | Device ingestion | `300` | Máximo de requests por ventana. |
| `app.abuse-protection.device.block-duration` / `APP_ABUSE_PROTECTION_DEVICE_BLOCK_DURATION` | Device ingestion | `5m` | Duración del bloqueo. |
| `app.abuse-protection.password-reset.forgot.enabled` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_FORGOT_ENABLED` | Forgot password | `true` | Activa rate limiting de solicitud de recuperación. |
| `app.abuse-protection.password-reset.forgot.window` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_FORGOT_WINDOW` | Forgot password | `15m` | Ventana de conteo. |
| `app.abuse-protection.password-reset.forgot.max-attempts` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_FORGOT_MAX_ATTEMPTS` | Forgot password | `3` | Máximo de solicitudes en la ventana. |
| `app.abuse-protection.password-reset.forgot.block-duration` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_FORGOT_BLOCK_DURATION` | Forgot password | `15m` | Duración del bloqueo. |
| `app.abuse-protection.password-reset.reset.enabled` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_RESET_ENABLED` | Reset password | `true` | Activa rate limiting de consumo de token. |
| `app.abuse-protection.password-reset.reset.window` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_RESET_WINDOW` | Reset password | `15m` | Ventana de conteo. |
| `app.abuse-protection.password-reset.reset.max-attempts` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_RESET_MAX_ATTEMPTS` | Reset password | `5` | Máximo de intentos en la ventana. |
| `app.abuse-protection.password-reset.reset.block-duration` / `APP_ABUSE_PROTECTION_PASSWORD_RESET_RESET_BLOCK_DURATION` | Reset password | `15m` | Duración del bloqueo. |
| `app.abuse-protection.ai-summary.enabled` / `APP_ABUSE_PROTECTION_AI_SUMMARY_ENABLED` | AI summary | `true` | Activa rate limiting del resumen IA. |
| `app.abuse-protection.ai-summary.window` / `APP_ABUSE_PROTECTION_AI_SUMMARY_WINDOW` | AI summary | `10m` | Ventana de conteo. |
| `app.abuse-protection.ai-summary.max-attempts` / `APP_ABUSE_PROTECTION_AI_SUMMARY_MAX_ATTEMPTS` | AI summary | `10` | Máximo de solicitudes en la ventana. |
| `app.abuse-protection.ai-summary.block-duration` / `APP_ABUSE_PROTECTION_AI_SUMMARY_BLOCK_DURATION` | AI summary | `10m` | Duración del bloqueo. |
| `app.abuse-protection.outbox-requeue.enabled` / `APP_ABUSE_PROTECTION_OUTBOX_REQUEUE_ENABLED` | Outbox requeue admin | `true` | Activa rate limiting de requeue admin. |
| `app.abuse-protection.outbox-requeue.window` / `APP_ABUSE_PROTECTION_OUTBOX_REQUEUE_WINDOW` | Outbox requeue admin | `10m` | Ventana de conteo. |
| `app.abuse-protection.outbox-requeue.max-attempts` / `APP_ABUSE_PROTECTION_OUTBOX_REQUEUE_MAX_ATTEMPTS` | Outbox requeue admin | `5` | Máximo de requeues en la ventana. |
| `app.abuse-protection.outbox-requeue.block-duration` / `APP_ABUSE_PROTECTION_OUTBOX_REQUEUE_BLOCK_DURATION` | Outbox requeue admin | `10m` | Duración del bloqueo. |
| `app.abuse-protection.cleanup.enabled` / `APP_ABUSE_PROTECTION_CLEANUP_ENABLED` | Cleanup | `true` | Activa limpieza programada. |
| `app.abuse-protection.cleanup.fixed-delay` / `APP_ABUSE_PROTECTION_CLEANUP_FIXED_DELAY` | Cleanup | `30m` | Frecuencia entre ejecuciones. |
| `app.abuse-protection.cleanup.retention` / `APP_ABUSE_PROTECTION_CLEANUP_RETENTION` | Cleanup | `24h` | Retención mínima antes de eliminar entradas inactivas. |

## 8. Defaults recomendados para demo

Los defaults actuales son razonables para una demo normal:

- Login: `5` intentos cada `15m`, bloqueo `15m`.
- Device ingestion: `300` requests cada `1m`, bloqueo `5m`.
- Forgot password: `3` solicitudes cada `15m`, bloqueo `15m`.
- Reset password: `5` intentos cada `15m`, bloqueo `15m`.
- AI summary: `10` solicitudes cada `10m`, bloqueo `10m`.
- Outbox requeue admin: `5` intentos cada `10m`, bloqueo `10m`.

Para una demo con muchas pruebas repetidas desde la misma IP, puede ser necesario aumentar temporalmente algunos límites en Render. Es preferible ajustar variables de entorno de forma controlada antes que desactivar la protección completa.

Si una prueba queda bloqueada, respetar `Retry-After`, esperar a que termine el bloqueo o ajustar temporalmente el límite del flujo específico.

## 9. Logs y observabilidad

Los servicios de abuse protection registran eventos de bloqueo con información sanitizada:

- `event=abuse_protection_limited`
- `scope`
- prefijo de `keyHash`
- `retryAfterSeconds`
- `status=429`
- `requestId` cuando está disponible por correlación de request

Los logs no deben exponer:

- email real;
- JWT;
- token de recuperación;
- DeviceSecret;
- API keys;
- connection strings;
- payload sensible de outbox.

Métrica real de cleanup:

- `ganaderia.abuse.rate_limit.cleanup.deleted.count`

Esta métrica se incrementa cuando `AbuseRateLimitCleanupJob` elimina entradas antiguas.

## 10. Cleanup

El job `AbuseRateLimitCleanupJob` ejecuta limpieza programada usando:

- `app.abuse-protection.cleanup.enabled`
- `app.abuse-protection.cleanup.fixed-delay`
- `app.abuse-protection.cleanup.retention`

Comportamiento:

- Calcula un `cutoff` con base en la retención configurada.
- Elimina entradas inactivas anteriores al cutoff.
- No elimina entradas que siguen bloqueadas si `blockedUntil` aún está vigente.
- Registra log `event=abuse_rate_limit_cleanup_completed`.
- En caso de error registra `event=abuse_rate_limit_cleanup_failed`.
- Incrementa `ganaderia.abuse.rate_limit.cleanup.deleted.count` cuando elimina registros.

El default actual ejecuta cada `30m` y conserva entradas por `24h`.

## 11. Seguridad

Reglas de seguridad aplicables:

- No guardar secretos en claro como `abuseKey`.
- No guardar emails, tokens, JWT, DeviceSecret ni IPs en claro dentro de la clave persistida.
- No imprimir secretos en logs.
- No incluir claves reales en documentación, issues, capturas o scripts versionados.
- No exponer JWT ni DeviceSecret al frontend.
- No desactivar rate limiting en producción.
- Ajustar límites por flujo específico antes que apagar `app.abuse-protection.enabled`.

## 12. Validación manual

Estas validaciones deben hacerse con cuidado para no saturar Render ni proveedores externos.

### Validar login rate limit

Usar credenciales controladas y repetir intentos fallidos hasta observar `429`. Confirmar que el body tiene `code=TOO_MANY_REQUESTS` y que existe `Retry-After`.

No usar contraseñas reales en documentación ni logs compartidos.

### Validar forgot password rate limit

Probar con un email controlado. Este flujo puede generar correo real vía Resend, por lo que no debe ejecutarse en loops agresivos.

Validar que la respuesta normal no revela si el email existe y que, al exceder el límite, aparece `429`.

### Validar ai-summary rate limit

Ejecutar `GET /api/alert-analysis/ai-summary` con JWT válido. Este endpoint puede consumir Gemini si IA está habilitada, por lo que no debe usarse en loops.

Validar que al exceder el límite no se debe seguir consumiendo proveedor externo y que aparece `429`.

### Validar outbox requeue rate limit

Requiere JWT con rol `ADMINISTRADOR` y un mensaje de outbox en estado permitido para requeue. Validar que al exceder el límite se recibe `429` y que el mensaje no cambia de estado.

No reencolar mensajes repetidamente sin necesidad operativa.

## 13. Troubleshooting

| Problema | Causa probable | Acción recomendada |
| --- | --- | --- |
| `429` inesperado en demo | Muchas pruebas desde la misma IP o mismo usuario. | Revisar `Retry-After`, esperar el desbloqueo o aumentar temporalmente el límite del flujo. |
| Forgot password no envía correo | El flujo fue bloqueado antes de enviar email o EMAIL/Resend no está configurado. | Revisar status HTTP, `Retry-After` y logs de email sin exponer API keys. |
| `ai-summary` devuelve `429` | Se excedió límite por usuario/IP. | Esperar `Retry-After` o aumentar temporalmente `APP_ABUSE_PROTECTION_AI_SUMMARY_MAX_ATTEMPTS`. |
| Device ingestion devuelve `429` | Alto volumen desde el mismo token/IP o pruebas repetidas. | Reducir carga, revisar script de prueba y ajustar límites solo si corresponde. |
| Requeue admin devuelve `429` | Reintentos repetidos del mismo admin/IP/mensaje. | Evitar loops de requeue, esperar `Retry-After` o ajustar límite para demo. |
| Muchas pruebas desde la misma IP | Laboratorio, aula o demo comparten salida de red. | Aumentar temporalmente límites por IP para el flujo afectado. |
| Render detrás de proxy y forwarded headers | El backend podría ver la IP del proxy si no se confía en headers forwarded. | Revisar `APP_ABUSE_PROTECTION_TRUST_FORWARDED_HEADERS` según el ambiente y política de confianza. |
| Cleanup no elimina entradas esperadas | La retención no venció o la entrada sigue bloqueada. | Revisar `APP_ABUSE_PROTECTION_CLEANUP_RETENTION`, `blockedUntil` y logs del cleanup. |

## 14. Qué NO hacer

- No desactivar rate limiting en producción.
- No bajar seguridad para que pase una demo.
- No subir secretos reales al repositorio.
- No commitear `.env`.
- No hacer pruebas agresivas contra Render.
- No activar `ai-summary` en loops.
- No reencolar outbox repetidamente sin necesidad.
- No pegar JWT, DeviceSecret, tokens de reset o API keys en documentación.

## 15. Criterio de aceptación

La documentación queda lista si:

- Existe `docs/rate-limiting-guide.md`.
- Lista todos los flujos protegidos.
- Usa nombres reales de clases y properties.
- No contiene secretos reales.
- Explica `429` y `Retry-After`.
- Explica cleanup.
- Explica ajustes para demo.
- No se tocó código ni configuración.

## 16. Conclusión

El backend Ganadería 4.0 protege sus flujos más sensibles contra fuerza bruta, spam de email, abuso de Gemini, abuso de IoT y reintentos administrativos excesivos. La protección es persistente, configurable por flujo, usa claves hasheadas y mantiene respuestas HTTP claras para clientes y operadores.
