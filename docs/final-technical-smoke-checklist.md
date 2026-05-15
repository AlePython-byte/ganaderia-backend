# Checklist de smoke final técnico — Ganadería 4.0

## 1. Propósito

Este checklist se usa como validación final antes de una demo, entrega o defensa técnica del backend Ganadería 4.0. Su objetivo es ordenar las pruebas mínimas, el resultado esperado y la evidencia que debe guardarse antes de declarar el backend listo.

## 2. Alcance del smoke

El smoke final valida:

- Repositorio.
- Build.
- Tests.
- Render.
- Swagger.
- Autenticación.
- Recuperación de contraseña.
- Device ingestion.
- Alertas.
- Notificaciones.
- Outbox.
- IA.
- Reportes.
- Rate limiting.
- Throttle EMAIL.
- k6.
- Observabilidad.
- Secretos.

## 3. Precondiciones

- Working tree limpio antes de la entrega final.
- Backend desplegado en Render: `https://ganaderia-backend.onrender.com`.
- Base de datos disponible.
- Variables de entorno configuradas.
- EMAIL/IA encendidos si se van a demostrar.
- Admin demo disponible.
- Frontend local o Vercel disponible si se va a probar CORS.
- No usar secretos reales en documentación.
- No ejecutar pruebas agresivas contra Render.

## 4. Validación de Git

Comandos:

```powershell
git status --short
git branch --show-current
git log -1 --oneline
```

Resultado esperado:

- `git status --short` sin salida.
- Rama correcta.
- Último commit corresponde al cierre.

Evidencia sugerida:

- Captura o salida del comando.
- Hash corto del último commit.

## 5. Validación local de calidad

Comando:

```powershell
.\mvnw.cmd clean verify
```

Resultado esperado:

- `BUILD SUCCESS`.
- Tests unitarios OK.
- Tests de integración OK.
- JaCoCo OK.
- SpotBugs OK.
- Maven Enforcer OK.
- Flyway OK.

Si no se ejecuta en el momento de la demo, registrar el último resultado conocido, fecha y responsable de la ejecución.

Resultado conocido al cierre documental:

- `clean verify`: `BUILD SUCCESS`.
- 491 tests.
- 0 failures.
- 0 errors.
- 0 skipped.
- JaCoCo OK.
- SpotBugs OK.
- Maven Enforcer OK.
- Flyway validó/aplicó 15 migraciones en tests.

## 6. Validación Render básica

URLs:

- `https://ganaderia-backend.onrender.com/healthz`
- `https://ganaderia-backend.onrender.com/actuator/health`
- `https://ganaderia-backend.onrender.com/swagger-ui/index.html`

Resultado esperado:

- Health responde OK.
- Swagger carga.
- No hay errores críticos en logs.

Evidencia sugerida:

- Captura de health.
- Captura de Swagger.
- Extracto sanitizado de logs si aplica.

## 7. Validación de Swagger/OpenAPI

Checklist:

- Servers local/Render visibles.
- Tags por módulo.
- Bearer JWT documentado.
- HMAC device ingestion documentado.
- 500 global documentado.
- 429 en endpoints con rate limiting.
- ErrorResponseDTO incluye `requestId`.
- `fieldErrors` documentado como opcional.
- No hay ejemplos con secretos reales.

## 8. Validación de login JWT

Endpoint:

- `POST /api/auth/login`

Resultado esperado:

- 200 OK.
- Devuelve JWT.
- No se imprime JWT completo en logs.
- Auth/Me funciona con JWT.

También probar:

- `GET /api/auth/me`

Ejemplos deben usar:

- `<ADMIN_EMAIL>`
- `<ADMIN_PASSWORD>`
- `<JWT>`

## 9. Validación de autorización

Checklist:

- Endpoint protegido sin JWT devuelve 401.
- Rol insuficiente devuelve 403.
- ADMINISTRADOR accede a endpoints admin.
- Errores contienen `requestId`.

## 10. Validación de recuperación de contraseña

Endpoints:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Checklist:

- `forgot-password` público.
- No requiere JWT.
- Respuesta genérica.
- No revela si el email existe.
- Puede enviar email real si EMAIL está habilitado.
- 429 funciona si se excede límite.
- `reset-password` con token válido cambia contraseña.
- Token inválido/usado/expirado devuelve error controlado.
- `APP_FRONTEND_PASSWORD_RESET_URL` apunta al frontend correcto.

No incluir tokens reales. Usar `<RESET_TOKEN>`.

## 11. Validación de vacas y collares

Checklist:

- Crear vaca sin enviar token técnico.
- Backend genera `COW-xxx`.
- Backend genera `INT-xxx` si aplica.
- Crear collar sin enviar token técnico.
- Backend genera `COLLAR-xxx`.
- Listar/paginar vacas.
- Listar/paginar collares.
- Constraints únicos funcionan.

## 12. Validación device ingestion

Endpoint:

- `POST /api/device/locations`

Checklist:

- HMAC headers presentes.
- Timestamp header UTC con `Z`.
- Body timestamp compatible.
- Nonce único.
- Firma válida.
- Ubicación registrada.
- `lastSeenAt` actualizado.
- Nonce repetido rechazado.
- Firma inválida rechazada.
- Rate limiting device activo.

Para prueba real se recomienda:

```powershell
scripts/send-device-location.ps1
```

No incluir `<DEVICE_SECRET>` real.

## 13. Validación de alertas

Checklist:

- LOW_BATTERY puede generarse/consultarse.
- COLLAR_OFFLINE puede generarse/consultarse.
- EXIT_GEOFENCE puede generarse/consultarse.
- Alertas pendientes se listan.
- Priority queue funciona.
- Resolver alerta funciona.
- Descartar alerta funciona.

## 14. Validación de notificaciones EMAIL/outbox

Checklist:

- Preferencias de usuario se respetan.
- `emailEnabled` funciona.
- `minimumSeverity` funciona.
- `notificationEmail`/fallback funciona.
- Modo direct funciona si está habilitado.
- Modo outbox encola.
- Processor envía.
- Estados SENT/FAILED/DEAD funcionan.
- Requeue admin funciona para estados permitidos.
- Requeue admin tiene rate limiting.
- Throttle EMAIL no rompe alerta principal.
- Throttle EMAIL evita enviar/encolar cuando excede límite.

## 15. Validación IA

Endpoints:

- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities?limit=5`
- `GET /api/alert-analysis/ai-summary`

Checklist:

- Summary responde.
- Top-priorities responde.
- AI-summary responde con Gemini si está habilitado.
- Fallback funciona si Gemini falla/no está disponible.
- AI-summary tiene rate limiting.
- No se expone `GEMINI_API_KEY`.

## 16. Validación reportes

Checklist:

- Reportes CSV responden.
- Content-Type correcto.
- Filtros principales funcionan.
- Mitigación CSV injection activa.
- No se exponen datos sensibles innecesarios.

## 17. Validación error handling

Checklist:

- 400 validation incluye `message`.
- 400 validation incluye `fieldErrors`.
- 401 incluye `requestId`.
- 403 incluye `requestId`.
- 404 incluye `requestId`.
- 409 controlado.
- 429 incluye `Retry-After`.
- 500 no expone stacktrace al cliente.
- JSON malformado devuelve 400 claro.

## 18. Validación rate limiting

Checklist:

- Login rate limit.
- Device ingestion rate limit.
- Forgot-password rate limit.
- Reset-password rate limit.
- AI-summary rate limit.
- Outbox requeue rate limit.
- ErrorResponseDTO + `requestId` + `Retry-After` en 429.
- No se guardan claves sensibles en claro.

## 19. Validación throttle EMAIL

Checklist:

- `EMAIL_RECIPIENT`.
- `EMAIL_RECIPIENT_EVENT_TYPE`.
- `EMAIL_CHANNEL`.
- Direct mode no llama Resend si está throttled.
- Outbox mode no encola si está throttled.
- Alerta principal sigue funcionando.
- Métrica `ganaderia.notifications.email.throttled.count` existe.
- No se exponen emails/API keys en logs.

## 20. Validación k6

Resultado real conocido de operational-read contra Render:

- checks 160/160.
- checks success 100%.
- `http_req_failed` 0.00%.
- `http_reqs` 40.
- p95 787.07 ms.
- Threshold `p95<1500` PASS.

Comando seguro:

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e VUS=1 `
  -e DURATION=20s `
  -e P95_THRESHOLD_MS=1500 `
  performance/k6/operational-read.js
```

Device ingestion k6:

- Script existe.
- Validado con inspect.
- Smoke real pendiente con `<DEVICE_TOKEN>` y `<DEVICE_SECRET>`.

## 21. Validación observabilidad local

Checklist:

- Prometheus levanta con Docker Compose.
- Grafana levanta.
- Datasource Prometheus provisionado.
- Dashboard Ganadería 4.0 visible.
- Métricas HTTP/JVM visibles.
- Métricas de dominio aparecen tras generar eventos.
- No hay secretos en dashboards.

Comandos:

```powershell
docker compose -f docker-compose.observability.yml up -d
docker compose -f docker-compose.observability.yml ps
docker compose -f docker-compose.observability.yml down
```

## 22. Validación Bruno/HTTP files

Checklist:

- `api-tests/` existe.
- `bruno/` existe.
- Environments local/render usan placeholders.
- No hay JWT reales.
- No hay DeviceSecret real.
- Auth/Login funciona si se configuran variables localmente.
- Auth/Me funciona con JWT.

## 23. Validación CORS frontend/backend

Checklist:

- `VITE_API_URL` en frontend apunta a `https://ganaderia-backend.onrender.com`.
- `APP_CORS_ALLOWED_ORIGINS` incluye `http://localhost:5173`.
- `APP_CORS_ALLOWED_ORIGINS` incluye dominio Vercel real si existe.
- Login desde frontend funciona.
- Forgot-password desde frontend funciona.
- Reset-password usa `APP_FRONTEND_PASSWORD_RESET_URL` correcto.

Usar `<FRONTEND_VERCEL_URL>` cuando el dominio real no esté definido.

## 24. Validación secretos

Checklist:

- README sin secretos reales.
- Docs sin secretos reales.
- Scripts sin secretos reales.
- Bruno sin secretos reales.
- api-tests sin secretos reales.
- k6 sin secretos reales.
- Observability sin secretos reales.
- `.env` no versionado.
- `.env.example` solo placeholders.
- `JWT_SECRET` rotado si fue expuesto.
- `GEMINI_API_KEY` rotada si fue expuesta.
- `RESEND_API_KEY` rotada si fue expuesta.
- `DEVICE_SECRET_MASTER_KEY` rotada si fue expuesta.
- Admin demo password segura.

## 25. Matriz de resultado final

| Área | Comando/prueba | Resultado esperado | Resultado real | Evidencia |
| --- | --- | --- | --- | --- |
| Git | `git status --short` | Sin salida | Pendiente de completar en ejecución final | Salida del comando |
| Build local | `.\mvnw.cmd clean verify` | BUILD SUCCESS | Resultado conocido: BUILD SUCCESS, 491 tests, 0 failures, 0 errors, 0 skipped | Log Maven |
| Render health | `/healthz` | OK | Pendiente de completar en ejecución final | Captura/salida HTTP |
| Swagger | `/swagger-ui/index.html` | Carga correctamente | Pendiente de completar en ejecución final | Captura |
| Login | `POST /api/auth/login` | 200 + JWT | Pendiente de completar en ejecución final | Respuesta sanitizada |
| Auth me | `GET /api/auth/me` | 200 con usuario autenticado | Pendiente de completar en ejecución final | Respuesta sanitizada |
| Password reset | Forgot/reset password | Flujo controlado | Pendiente de completar si se demuestra | Captura/email sanitizado |
| Device ingestion | `scripts/send-device-location.ps1` | 200 con firma válida | Pendiente de completar en ejecución final | Salida script sanitizada |
| Alertas | Listar/priority/acciones | Respuestas correctas | Pendiente de completar en ejecución final | Captura/API |
| EMAIL/outbox | Enviar/encolar/procesar | SENT o estado esperado | Pendiente de completar si se demuestra | Captura/log sanitizado |
| IA | `ai-summary` | Gemini o fallback | Pendiente de completar si se demuestra | Respuesta sanitizada |
| Reportes | CSV | Content-Type y CSV válidos | Pendiente de completar en ejecución final | Archivo/captura |
| Error handling | 400/401/403/404/429/500 | ErrorResponseDTO consistente | Pendiente de completar en ejecución final | Respuestas sanitizadas |
| k6 operational-read | `performance/k6/operational-read.js` | Thresholds PASS | Resultado conocido: PASS, p95 787.07 ms | Log k6 |
| k6 device ingestion | `performance/k6/device-ingestion.js` | 200 con HMAC válido | Pendiente con secretos controlados | Log k6 sanitizado |
| Observabilidad | Docker Compose | Prometheus/Grafana UP | Pendiente de completar si se demuestra | Captura |
| CORS | Frontend local/Vercel | Sin error CORS | Pendiente de completar si se demuestra | Captura navegador |
| Secretos | Revisión repo | Sin secretos reales | Pendiente de completar en ejecución final | Salida grep/revisión |

## 26. Criterios de aprobación final

El backend se considera listo si:

- Git status limpio.
- `clean verify` OK.
- Render health OK.
- Login OK.
- Swagger OK.
- Password reset OK si se va a demostrar.
- IA OK si se va a demostrar.
- EMAIL/outbox OK si se va a demostrar.
- Device ingestion OK.
- No hay secretos expuestos.
- Documentación final coherente.

## 27. Pendientes aceptables

- Device ingestion k6 real pendiente si no se ejecuta aún.
- Pruebas de estrés real no ejecutadas.
- Grafana visual pendiente si no se abre en navegador.
- Frontend Vercel pendiente si el deploy no está terminado.
- SMS no implementado si no hay proveedor.

## 28. Pendientes bloqueantes

- `clean verify` fallando.
- Render caído.
- Login roto.
- Base de datos inaccesible.
- Secretos reales en repo.
- Swagger contradice rutas reales.
- EMAIL/IA prometidos en demo pero apagados.
- Frontend no puede consumir backend por CORS.

## 29. Conclusión

Este checklist sirve como control final de calidad para presentar el backend Ganadería 4.0 con evidencia técnica y operativa. Debe completarse durante el smoke final, dejando claros los resultados reales, las evidencias guardadas y cualquier pendiente aceptable o bloqueante antes de la entrega.
