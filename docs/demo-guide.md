# Guía de demo técnica — Ganadería 4.0

## 1. Propósito de la demo

Esta guía permite presentar el backend Ganadería 4.0 de forma ordenada, reproducible y defendible ante un jurado, profesor o evaluador técnico. El objetivo es demostrar que el sistema no es solo un CRUD, sino una plataforma backend con seguridad, integración IoT, alertas operativas, notificaciones, email real, outbox, IA, reportes, observabilidad y pruebas automatizadas.

## 2. Alcance de la demo

Durante la demo se recomienda demostrar:

- Backend desplegado.
- Seguridad con JWT y roles.
- Ingesta IoT con HMAC.
- Gestión de vacas, collares, geocercas, ubicaciones y alertas.
- Notificaciones LOG, WEBHOOK y EMAIL.
- EMAIL real con Resend.
- Outbox para EMAIL.
- Recuperación de contraseña.
- IA analítica con Claude (Anthropic) como proveedor por defecto, Gemini y DeepSeek como alternativos, y fallback heurístico.
- Reportes CSV.
- Observabilidad con health checks, Actuator, Prometheus y logs estructurados.
- Calidad técnica con tests, JaCoCo y SpotBugs.

## 3. Ambiente de demo

- Backend Render: https://ganaderia-backend.onrender.com
- EMAIL: encendido para la demo, si las variables de entorno están configuradas.
- IA: encendida para la demo si `CLAUDE_API_KEY` está configurada (`AI_PROVIDER=claude` por defecto).
- Base de datos: PostgreSQL demo/configurada.
- Scripts PowerShell disponibles en `scripts/`.
- Swagger/OpenAPI: disponible en `/swagger-ui/index.html`.

No se deben mostrar credenciales reales, API keys, JWT, DeviceSecret ni secretos de Render durante la presentación.

## 4. Checklist previo a la demo

- Backend Render responde.
- Variables de entorno configuradas.
- EMAIL habilitado.
- GEMINI/IA habilitada.
- Usuario administrador disponible.
- No hay secretos visibles en consola, navegador o documentación.
- Swagger accesible.
- Scripts PowerShell disponibles.
- Frontend listo si se va a mostrar junto al backend.
- Navegador limpio o ventana incógnita si se van a mostrar flujos de autenticación.

## 5. Orden recomendado de presentación

Para una demo de 10 a 15 minutos:

1. Presentación breve del problema.
2. Arquitectura general del backend.
3. Health check.
4. Swagger/OpenAPI.
5. Login JWT.
6. Roles y autorización.
7. Vacas y collares.
8. Seed demo.
9. Device ingestion IoT.
10. Alertas operativas.
11. Email y outbox.
12. IA analítica.
13. Reportes.
14. Observabilidad.
15. Pruebas y calidad.
16. Cierre técnico.

## 6. Paso 1 — Validar salud del backend

Endpoints:

```http
GET https://ganaderia-backend.onrender.com/healthz
GET https://ganaderia-backend.onrender.com/actuator/health
```

Resultado esperado:

- HTTP 200.
- Estado `UP` o respuesta de health correcta.

Qué explicar al jurado:

- El backend está desplegado en Render.
- Tiene health checks para validar disponibilidad.
- Está preparado para monitoreo básico y verificación operacional.

## 7. Paso 2 — Mostrar Swagger/OpenAPI

Endpoint:

```http
GET https://ganaderia-backend.onrender.com/swagger-ui/index.html
```

Resultado esperado:

- Swagger UI carga correctamente.
- Se pueden ver endpoints agrupados por módulo.

Qué explicar:

- El backend expone un contrato HTTP explorable.
- Swagger facilita integración con frontend, pruebas manuales y revisión técnica.
- Los endpoints protegidos requieren JWT.

## 8. Paso 3 — Login JWT

Endpoint:

```http
POST /api/auth/login
```

Ejemplo con placeholders:

```json
{
  "email": "<ADMIN_EMAIL>",
  "password": "<ADMIN_PASSWORD>"
}
```

Resultado esperado:

- JWT.
- Información del usuario.
- Rol asignado.

Qué explicar:

- La autenticación es stateless con JWT.
- El token se usa en los endpoints protegidos con:

```http
Authorization: Bearer <JWT>
```

No se deben mostrar credenciales reales ni copiar el JWT completo en pantalla.

## 9. Paso 4 — Autorización por roles

Cómo demostrar:

- Acceso permitido con rol `ADMINISTRADOR`.
- Acceso denegado con roles insuficientes.
- HTTP 401 cuando no se envía token.
- HTTP 403 cuando el token es válido pero el rol no tiene permiso.

Endpoints sugeridos:

```http
GET /api/users/page
GET /api/admin/notification-outbox
GET /api/cows/page
GET /api/alerts
```

Qué explicar:

- 401 significa que falta autenticación o el token no es válido.
- 403 significa que el usuario está autenticado, pero no autorizado.
- Los roles separan responsabilidades operativas: `ADMINISTRADOR`, `SUPERVISOR`, `TECNICO` y `OPERADOR`.

## 10. Paso 5 — Gestión de vacas y collares

Qué demostrar:

- Crear vaca sin enviar token técnico.
- El backend genera tokens de vaca como `COW-001`, `COW-002`.
- El backend genera código interno de vaca como `INT-001`, `INT-002`.
- Crear collar sin enviar token técnico.
- El backend genera tokens de collar como `COLLAR-001`, `COLLAR-002`.

Endpoints:

```http
POST /api/cows
GET /api/cows
GET /api/cows/page
POST /api/collars
GET /api/collars
GET /api/collars/page
PATCH /api/collars/{id}/rotate-secret
PATCH /api/collars/{id}/assign/{cowId}
PATCH /api/collars/{id}/enable
PATCH /api/collars/{id}/disable
```

Qué explicar:

- El frontend no inventa tokens técnicos.
- La identidad técnica la controla el backend.
- Hay constraints únicos.
- Hay retry acotado ante conflictos de concurrencia en generación de tokens.
- El DeviceSecret del collar no se debe exponer públicamente.

## 11. Paso 6 — Cargar datos demo

Script:

```powershell
scripts/seed-demo-data.ps1
```

Qué hace:

- Hace login vía JWT.
- Crea o reutiliza vacas demo.
- Crea o reutiliza collares demo.
- Crea o reutiliza geocerca demo.
- Crea ubicaciones demo.
- Configura preferencias de notificación cuando está habilitado.
- No inserta SQL directo.
- No imprime JWT completo ni secretos.

Resultado esperado conocido:

- `CargaDemo Luna`.
- `CargaDemo Estrella`.
- `COLLAR-001`.
- `COLLAR-002`.
- Geofence creada o reutilizada.
- Ubicaciones creadas.
- `Final result: PASS`.

Para parámetros exactos, consultar:

```powershell
Get-Help .\scripts\seed-demo-data.ps1
```

Si el script no muestra ayuda extendida, revisar el encabezado del archivo.

## 12. Paso 7 — Device ingestion IoT firmado

Endpoint:

```http
POST /api/device/locations
```

Headers:

```http
X-Device-Token: <DEVICE_TOKEN>
X-Device-Timestamp: <UTC_TIMESTAMP>
X-Device-Nonce: <UNIQUE_NONCE>
X-Device-Signature: <HMAC_SIGNATURE>
```

Script:

```powershell
scripts/send-device-location.ps1
```

Qué explicar:

- Cada request IoT se firma con HMAC.
- El backend valida token de dispositivo, timestamp, nonce y firma.
- Los nonces persistentes evitan replay attacks.
- El timestamp tiene validación temporal.
- El collar debe estar habilitado y en estado válido.
- El endpoint acepta `batteryLevel` y `gpsAccuracy`.
- El DeviceSecret nunca debe imprimirse ni documentarse.
- El header timestamp usa UTC con `Z`.
- El body timestamp usa formato compatible con el backend.

Resultado esperado:

- HTTP 200.
- Ubicación registrada.
- `lastSeenAt` del collar actualizado.
- Alertas generadas si las reglas operativas aplican.

Para pruebas de carga controladas:

```powershell
scripts/load-test-device-ingestion.ps1
```

La evidencia técnica de pruebas locales está documentada en `docs/device-ingestion-load-test.md`.

## 13. Paso 8 — Alertas operativas

Tipos principales:

- `COLLAR_OFFLINE`.
- `EXIT_GEOFENCE`.
- `LOW_BATTERY`.

Endpoints sugeridos:

```http
GET /api/alerts
GET /api/alerts/page
GET /api/alerts/status/PENDIENTE
GET /api/alerts/pending/priority-queue
GET /api/alerts/type/{type}
PATCH /api/alerts/{id}/resolve
PATCH /api/alerts/{id}/discard
```

Qué explicar:

- Las alertas no son solo CRUD.
- Hay reglas de monitoreo y severidad.
- La cola de prioridad permite ordenar atención operativa.
- `LOW_BATTERY` también pasa por el dispatcher de notificaciones.

## 14. Paso 9 — Notificaciones y EMAIL real

Canales:

- `LOG`.
- `WEBHOOK`.
- `EMAIL`.

Script relacionado:

```powershell
scripts/test-email-notification-flow.ps1
```

Qué explicar:

- EMAIL usa Resend como proveedor configurable.
- EMAIL está encendido para la demo si las variables están configuradas.
- Las preferencias de usuario permiten controlar canales y severidad mínima.
- `notificationEmail` puede usarse como destino o hacer fallback a `User.email`.
- No se deben mostrar API keys ni correos sensibles durante la demo.

Resultado esperado:

- Se genera un evento.
- Se solicita envío por el canal configurado.
- El correo llega si Resend y la cuenta destinataria están correctamente configurados.

## 15. Paso 10 — Outbox EMAIL

El outbox está implementado para EMAIL.

Estados:

- `PENDING`.
- `PROCESSING`.
- `SENT`.
- `FAILED`.
- `DEAD`.

Endpoints admin:

```http
GET /api/admin/notification-outbox
GET /api/admin/notification-outbox/{id}
POST /api/admin/notification-outbox/{id}/requeue
```

Script:

```powershell
scripts/test-email-outbox-flow.ps1
```

Qué explicar:

- El outbox evita depender de envío directo inmediato.
- Permite reintentos controlados.
- Permite recuperación de mensajes `PROCESSING` atascados.
- Un ADMINISTRADOR puede reencolar mensajes `FAILED` o `DEAD`.
- El endpoint de requeue no envía directamente; el processor se encarga.
- No es un outbox universal para todos los canales; está implementado para EMAIL.
- Los endpoints admin no exponen payload completo ni cuerpos HTML/texto sensibles.

## 16. Paso 11 — Recuperación de contraseña

Endpoints públicos:

```http
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

Qué explicar:

- El flujo no revela si el email existe.
- El token no se devuelve por HTTP.
- El token se almacena hasheado en base de datos.
- Hay limpieza programada de tokens usados o expirados.
- El email se envía con Resend.
- Puede operar en modo directo o por outbox según configuración.

No se debe mostrar un token real de recuperación.

## 17. Paso 12 — IA analítica con Gemini

Endpoints:

```http
GET /api/alert-analysis/summary
GET /api/alert-analysis/top-priorities?limit=5
GET /api/alert-analysis/ai-summary
```

Respuesta esperada con campos como:

- `summary`.
- `riskLevel`.
- `recommendations`.
- `source`.
- `fallbackUsed`.
- `generatedAt`.

Qué explicar:

- IA está encendida para la demo si la clave del proveedor está configurada.
- El proveedor se selecciona con `AI_PROVIDER=gemini` (default) o `AI_PROVIDER=deepseek`.
- Si el proveedor falla o está apagado, el sistema responde con fallback heurístico (`RULE_BASED_FALLBACK`).
- La operación no se rompe por caída del proveedor externo.
- `fallbackUsed` y `source` en la respuesta permiten validar el modo de operación.
- Hay métricas de IA y fallback registradas en Actuator/Prometheus.

No se debe mostrar la API key de Gemini ni de DeepSeek.

## 18. Paso 13 — Reportes

Endpoints de reportes verificados:

```http
GET /api/reports/alerts
GET /api/reports/alerts/page
GET /api/reports/alerts/trend
GET /api/reports/alerts/type-recurrence
GET /api/reports/alerts/export.csv
GET /api/reports/offline-collars
GET /api/reports/offline-collars/staleness
GET /api/reports/cows-most-incidents
GET /api/reports/cows-incident-recurrence
```

Qué explicar:

- El backend ofrece reportes operativos, no solo pantallas CRUD.
- Existe exportación CSV para alertas.
- La exportación CSV contempla protección contra CSV injection.
- Los reportes ayudan a revisar recurrencia, incidentes y collares offline.

## 19. Paso 14 — Observabilidad

Endpoints y mecanismos:

```http
GET /healthz
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

También:

- Header `X-Request-Id`.
- Logs estructurados con `event=...`.
- Métricas de IA, outbox, ingesta y operación.

Resultado esperado:

- Health disponible.
- Métricas disponibles según configuración y permisos.
- Algunos endpoints de Actuator pueden requerir `ADMINISTRADOR`.

Qué explicar:

- El sistema tiene trazabilidad por request.
- Los logs están pensados para diagnóstico.
- Prometheus permite integrar monitoreo externo.

## 20. Paso 15 — Calidad técnica

Comando:

```powershell
.\mvnw.cmd clean verify
```

Resultado conocido:

- 277 tests unitarios.
- 198 tests de integración.
- 475 tests totales.
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- JaCoCo OK.
- SpotBugs OK.

Qué explicar:

- Hay pruebas unitarias e integración.
- Los tests de integración usan Testcontainers.
- Flyway se valida contra PostgreSQL real en pruebas.
- Docker Desktop debe estar activo para ejecutar la suite completa con Testcontainers.

## 21. Plan B si algo falla en demo

| Problema | Qué hacer | Cómo explicarlo |
| --- | --- | --- |
| Render tarda en despertar | Esperar el primer request y repetir health | Render puede tener cold start en planes gratuitos o de bajo consumo. |
| Swagger no carga | Probar `/healthz` y `/actuator/health` | Si el backend responde, el problema puede ser carga de UI o red del navegador. |
| Login falla | Verificar usuario, password y ambiente | No exponer credenciales; usar usuario demo válido. |
| Email no llega | Revisar configuración Resend, destino y spam | El backend solicita envío, pero el proveedor y la bandeja destino también influyen. |
| IA responde con fallback | Mostrar `fallbackUsed` y `source` | El sistema está diseñado para degradar con fallback heurístico cuando el proveedor no está disponible. |
| Device ingestion falla por firma | Revisar token, timestamp, nonce, body y secret | HMAC protege el endpoint; cualquier cambio rompe la firma. |
| Nonce repetido | Generar un nonce nuevo | La protección anti-replay rechaza nonces reutilizados. |
| Outbox queda `FAILED` o `DEAD` | Revisar detalle admin y usar requeue si aplica | El outbox permite diagnóstico y recuperación controlada. |
| CORS bloquea frontend | Validar origen configurado | CORS protege acceso desde navegadores y debe alinearse con el dominio frontend. |
| Docker no está activo para tests | Iniciar Docker Desktop y repetir | Testcontainers necesita Docker para levantar PostgreSQL en integración. |

## 22. Cierre recomendado ante jurado

Guion breve:

> Ganadería 4.0 no es solo un CRUD. El backend implementa seguridad con JWT y roles, ingesta IoT firmada con HMAC, protección anti-replay, alertas operativas, notificaciones reales, outbox para EMAIL, recuperación de contraseña, IA con fallback, reportes CSV, observabilidad y pruebas automatizadas. Además está desplegado en Render y documentado para operación, demo y diagnóstico técnico.

Puntos finales a reforzar:

- Seguridad aplicada a usuarios y dispositivos.
- IoT realista con firma, nonce y timestamp.
- Resiliencia en EMAIL mediante outbox.
- IA con fallback para no depender totalmente del proveedor.
- Observabilidad para diagnosticar producción.
- Calidad con tests, JaCoCo y SpotBugs.
- Documentación operativa para sostener el proyecto después de la demo.
