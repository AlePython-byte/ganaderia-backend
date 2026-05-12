# Checklist final de secretos y seguridad — Ganadería 4.0

## 1. Propósito

Este documento sirve para verificar, antes de una demo o entrega, que no hay secretos reales expuestos y que las credenciales sensibles de Ganadería 4.0 están separadas del código fuente.

El objetivo es revisar backend, frontend, Render, Vercel, Resend, Gemini, PostgreSQL, device ingestion, JWT, logs, scripts, documentación y repositorio Git con una lista clara de controles.

## 2. Principio general

- Los secretos nunca deben estar en código fuente.
- Los secretos nunca deben estar en README ni docs.
- Los secretos nunca deben estar en scripts versionados.
- Los secretos nunca deben estar en frontend.
- Los secretos deben estar en variables de entorno del proveedor correspondiente.
- Los ejemplos deben usar placeholders como `<JWT_SECRET>`, `<RESEND_API_KEY>` o `<DEVICE_SECRET>`.
- Si un secreto fue expuesto alguna vez, eliminarlo del archivo no basta: debe rotarse.

## 3. Inventario de secretos sensibles

| Secreto | Uso | Dónde debe estar | Dónde NO debe estar | Acción recomendada |
| --- | --- | --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL | Render/local seguro | Frontend, docs públicas, scripts con valor real | Usar variable de entorno y rotar si se compartió. |
| `DB_USERNAME` | Usuario de base de datos | Render/local seguro | Frontend, docs públicas | Mantener fuera del repositorio. |
| `DB_PASSWORD` | Password de base de datos | Render/local seguro | Frontend, README/docs, scripts | Rotar si apareció en consola, capturas o commits. |
| `JWT_SECRET` | Firma de JWT | Backend Render/local seguro | Frontend, docs, Swagger, scripts | Usar valor largo y privado. Rotar si se filtró. |
| `JWT_EXPIRATION_MS` | Expiración de JWT | Backend Render/local seguro | N/A como secreto | Revisar que tenga valor razonable para demo. |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Email admin inicial | Render/local seguro | Docs con correo real sensible | Usar solo si se requiere bootstrap. |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Password admin inicial | Render/local seguro | Frontend, docs, scripts, logs | Rotar antes/después de demo si se compartió. |
| `APP_NOTIFICATIONS_EMAIL_API_KEY` | API key Resend | Backend Render | Frontend, docs, scripts, logs | Rotar si se expuso. |
| `APP_NOTIFICATIONS_EMAIL_FROM` | Remitente email | Render/local seguro | N/A como secreto, pero revisar uso | Usar remitente autorizado en Resend. |
| `APP_NOTIFICATIONS_EMAIL_TO` | Destino email opcional | Render/local seguro | Docs si contiene correo sensible | Usar solo si aplica al ambiente. |
| `GEMINI_API_KEY` | API key Gemini | Backend Render | Frontend, docs, scripts, logs | Rotar si se mostró en demo o logs. |
| `GEMINI_MODEL` | Modelo IA | Render/local seguro | N/A como secreto | Documentar solo el nombre si no revela credenciales. |
| `DEVICE_SECRET_MASTER_KEY` | Clave maestra para secretos de dispositivos | Backend Render/local seguro | Frontend, docs, scripts | Tratar como secreto crítico. |
| `DEVICE_HMAC_PEPPER` | Pepper opcional HMAC | Backend Render/local seguro | Frontend, docs, scripts | Rotar si se expuso. |
| `DEVICE_AUTH_WINDOW_SECONDS` | Ventana temporal HMAC | Backend Render/local seguro | N/A como secreto | Mantener alineado con scripts/dispositivos. |
| DeviceSecret de collar | Firma requests IoT | Base de datos/backend seguro; parámetro temporal en scripts | Frontend, docs, logs, commits | Rotar secreto del collar si se expuso. |
| `APP_CORS_ALLOWED_ORIGINS` | Origins permitidos CORS | Render/local seguro | N/A como secreto | Usar origins exactos, no abrir innecesariamente. |
| `APP_NOTIFICATIONS_WEBHOOK_URL` | URL webhook | Render/local seguro | Docs si contiene token | Tratar como secreto si incluye token o path sensible. |
| `APP_NOTIFICATIONS_WEBHOOK_SECRET` | Secreto webhook | Backend Render | Frontend, docs, scripts | Rotar si se expuso. |
| Credenciales de Render | Acceso a plataforma/despliegue | Cuenta Render/gestor de secretos | Repo, docs, chats | Usar MFA y no compartir tokens. |
| `VITE_API_URL` | URL pública del backend para frontend | Vercel/frontend | N/A como secreto | Puede exponerse; no debe contener credenciales. |

## 4. Backend Render

Checklist:

- Variables configuradas en Render, no hardcodeadas en el repositorio.
- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` solo en Render/local seguro.
- `JWT_SECRET` fuerte y privado.
- `DEVICE_SECRET_MASTER_KEY` configurado en backend, nunca en frontend.
- EMAIL habilitado solo con `APP_NOTIFICATIONS_EMAIL_API_KEY` en Render.
- `APP_NOTIFICATIONS_EMAIL_FROM` configurado con remitente autorizado.
- `GEMINI_API_KEY` solo en Render.
- `APP_CORS_ALLOWED_ORIGINS` con origins exactos.
- `APP_BOOTSTRAP_ADMIN_PASSWORD` no usa valores débiles si la demo es pública.
- Variables de outbox/email habilitadas solo según modo de demo.
- No usar valores de prueba débiles en un entorno accesible públicamente.

## 5. Frontend Vercel

- `VITE_API_URL` puede estar en Vercel porque no es secreto.
- `VITE_API_URL` debe apuntar a `https://ganaderia-backend.onrender.com`.
- No colocar `JWT_SECRET` en Vercel.
- No colocar `APP_NOTIFICATIONS_EMAIL_API_KEY` en Vercel.
- No colocar `GEMINI_API_KEY` en Vercel.
- No colocar `DB_URL`, `DB_USERNAME` ni `DB_PASSWORD` en Vercel.
- No colocar `DEVICE_SECRET_MASTER_KEY` ni DeviceSecret de collares en Vercel.
- No exponer tokens administrativos.
- No usar `localhost` como API URL en el frontend desplegado.

## 6. Resend / EMAIL

- `APP_NOTIFICATIONS_EMAIL_API_KEY` debe vivir solo en backend.
- `APP_NOTIFICATIONS_EMAIL_FROM` debe estar configurado con un remitente válido.
- No pegar API key en docs, scripts, logs, Swagger, capturas o chats.
- Después de pruebas públicas, considerar rotar la API key.
- Verificar que los logs no impriman payload sensible, HTML, texto completo del correo, reset tokens ni claves.
- Si se usó outbox, verificar que los endpoints admin sigan mostrando previews redactados y no payload completo.

## 7. Gemini / IA

- `GEMINI_API_KEY` debe vivir solo en backend.
- No exponer API key en frontend.
- No pegar API key en documentación ni scripts.
- No incluir datos sensibles en prompts si no corresponde al caso de uso.
- El sistema tiene fallback heurístico si Gemini falla o está apagado.
- Considerar rotación después de demo si se compartió pantalla de variables o logs.

## 8. JWT y autenticación

- `JWT_SECRET` debe ser fuerte, largo y privado.
- No imprimir JWT completo en logs.
- No pegar JWT real en documentación.
- No subir tokens capturados de Postman, Swagger o navegador.
- Revisar `JWT_EXPIRATION_MS` si se requiere una expiración específica para demo.
- Usar ejemplos como:

```http
Authorization: Bearer <JWT>
```

- Si se filtró un JWT de admin, invalidar sesión si aplica y rotar `JWT_SECRET` si el riesgo lo justifica.

## 9. Device ingestion / HMAC

- `DEVICE_SECRET_MASTER_KEY` nunca debe estar en repo, frontend ni docs.
- DeviceSecret de collar nunca debe estar en repo, frontend ni docs.
- Scripts deben recibir secretos por parámetro o variable de entorno segura.
- No imprimir DeviceSecret.
- No imprimir firma HMAC completa si no es necesario.
- `X-Device-Signature` puede aparecer en requests, pero no debe considerarse reusable si nonce/timestamp son correctos.
- Rotar secretos de collares usados en pruebas si se expusieron en consola, capturas o chats.
- Revisar especialmente:
  - `scripts/send-device-location.ps1`.
  - `scripts/load-test-device-ingestion.ps1`.
  - `scripts/test-email-notification-flow.ps1`.

## 10. Base de datos PostgreSQL

- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` deben vivir solo en Render/local seguro.
- No subir dumps con datos sensibles.
- No subir connection strings reales.
- No compartir screenshots con password visible.
- No poner credenciales reales en `.env.example`.
- Rotar password si se compartió accidentalmente.
- Si se generó un backup para demo, almacenarlo fuera del repositorio y con acceso controlado.

## 11. CORS y origins

- `APP_CORS_ALLOWED_ORIGINS` no es secreto, pero debe ser específico.
- No abrir origins innecesarios.
- No usar `*` en producción si se requiere control de origins.
- Incluir `http://localhost:5173` solo para desarrollo o demo controlada.
- Incluir el dominio Vercel exacto cuando exista: `https://<FRONTEND_VERCEL_URL>`.
- Revisar diferencias entre `http` y `https`.
- Cambiar CORS no reemplaza JWT ni roles.

## 12. Logs y observabilidad

Revisar logs de Render antes de entregar si se hicieron pruebas reales.

No debe aparecer:

- JWT completo.
- API keys.
- DeviceSecret.
- Passwords.
- Reset tokens.
- Connection strings.
- Payload completo de email.
- HTML/textBody sensible.

Sí es aceptable si está sanitizado:

- `requestId`.
- Status HTTP.
- Método y path.
- Categoría de error.
- Rol o categoría operativa.
- Email enmascarado.
- Token técnico enmascarado cuando aplique.

## 13. Scripts PowerShell

Scripts a revisar:

- `scripts/send-device-location.ps1`.
- `scripts/smoke-test-backend.ps1`.
- `scripts/test-email-notification-flow.ps1`.
- `scripts/test-email-outbox-flow.ps1`.
- `scripts/seed-demo-data.ps1`.
- `scripts/load-test-device-ingestion.ps1`.

Checklist:

- No contienen secretos hardcodeados.
- Usan parámetros o placeholders.
- No imprimen JWT completo.
- No imprimen DeviceSecret.
- No imprimen API keys.
- No generan archivos temporales con secretos.
- Ejemplos usan valores de ejemplo o placeholders, no credenciales reales.

## 14. Documentación

Revisar:

- `README.md`.
- `docs/`.
- `.env.example`.
- Swagger/OpenAPI si aplica.

Checklist:

- Solo placeholders.
- No claves reales.
- No tokens reales.
- No correos personales sensibles salvo ejemplos controlados.
- No connection strings reales.
- No screenshots con variables sensibles.
- No links de reset password reales.

## 15. Commits e historial Git

`git status` limpio no garantiza que nunca hubo secretos en el historial.

Antes de una entrega pública conviene buscar secretos en el contenido actual y, si hubo sospecha de exposición, revisar historial de forma coordinada.

No reescribir historia sin coordinación si el repo ya está compartido.

Si un secreto fue commiteado alguna vez, eliminarlo del repo no basta: hay que rotarlo.

Comandos no destructivos sugeridos:

```powershell
git grep -n "RESEND_API_KEY"
git grep -n "GEMINI_API_KEY"
git grep -n "JWT_SECRET"
git grep -n "DATABASE_URL"
git grep -n "DB_PASSWORD"
git grep -n "DeviceSecret"
git grep -n "DEVICE_SECRET"
git grep -n "DEVICE_SECRET_MASTER_KEY"
git grep -n "postgresql://"
git grep -n "APP_NOTIFICATIONS_EMAIL_API_KEY"
git grep -n "APP_NOTIFICATIONS_WEBHOOK_SECRET"
```

También revisar específicamente:

```powershell
git grep -n "secret" -- docs scripts src
git grep -n "password" -- docs scripts src
git grep -n "token" -- docs scripts src
```

Estos comandos buscan patrones; requieren interpretación humana porque pueden encontrar placeholders o nombres de variables legítimos.

## 16. Rotación recomendada antes/después de demo

| Secreto | Cuándo rotar | Motivo |
| --- | --- | --- |
| `JWT_SECRET` | Si se expuso o si se compartió un ambiente con terceros | Invalida confianza en tokens emitidos. |
| `APP_NOTIFICATIONS_EMAIL_API_KEY` | Después de demo pública si se compartieron pantallas/logs | Protege envío de emails por Resend. |
| `GEMINI_API_KEY` | Después de exposición o demo con variables visibles | Evita uso no autorizado de cuota/API. |
| `DB_PASSWORD` | Si apareció en logs, capturas, chat o commits | Protege PostgreSQL y datos reales. |
| `DEVICE_SECRET_MASTER_KEY` | Solo con estrategia clara; inmediatamente si se expuso | Impacta secretos derivados/firmas de dispositivos. |
| DeviceSecret/collar secrets | Después de pruebas si se compartieron por consola o chat | Evita ingestiones falsas con collar demo. |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Antes de entrega final si varios tuvieron acceso | Reduce riesgo de acceso admin no controlado. |
| `APP_NOTIFICATIONS_WEBHOOK_SECRET` | Si se expuso o si la URL webhook contiene token | Protege integraciones externas. |

Reglas:

- Rotar inmediatamente si se expuso.
- Rotar después de pruebas públicas si se compartieron pantallas/logs.
- Rotar credenciales demo antes de entrega final si varios compañeros tuvieron acceso.
- Actualizar Render/local después de rotar.
- Verificar health y login después de rotar.

## 17. Checklist rápido antes de entrega

- `git status` limpio.
- `git grep` sin secretos reales.
- README sin secretos reales.
- `docs/` sin secretos reales.
- Scripts sin secretos hardcodeados.
- `.env` real no versionado.
- `.env.example` solo con placeholders o valores claramente de ejemplo.
- Render variables configuradas.
- Vercel no contiene secretos backend.
- Logs Render revisados.
- Swagger no expone secretos.
- Admin password demo controlado.
- DeviceSecret demo no expuesto.
- EMAIL funciona sin exponer `APP_NOTIFICATIONS_EMAIL_API_KEY`.
- IA funciona sin exponer `GEMINI_API_KEY`.
- CORS usa origins exactos.
- No hay JWT reales en documentación o tickets.

## 18. Qué hacer si se filtró un secreto

1. No seguir usando ese secreto.
2. Rotarlo en el proveedor correspondiente.
3. Actualizar Render, Vercel o entorno local según aplique.
4. Invalidar tokens si aplica.
5. Revisar logs, commits y documentación.
6. No pegar el secreto en issues, chats ni documentos.
7. Documentar internamente el incidente sin exponer el valor.
8. Verificar que el sistema siga funcionando con el nuevo secreto.

## 19. Criterio de aceptación

La subfase se considera lista si:

- Existe `docs/security-secrets-checklist.md`.
- Usa nombres reales de variables del proyecto.
- No contiene secretos reales.
- Explica dónde debe vivir cada secreto.
- Incluye comandos de revisión no destructivos.
- Incluye rotación recomendada.
- No tocó código ni configuración.

## 20. Conclusión

Este checklist reduce el riesgo de exposición de credenciales y ayuda a entregar Ganadería 4.0 con mayor responsabilidad técnica.

La seguridad final no depende solo de que el backend compile o pase tests: también requiere que secretos, tokens, API keys, passwords, logs y documentación estén controlados antes de publicar, presentar o compartir el proyecto.
