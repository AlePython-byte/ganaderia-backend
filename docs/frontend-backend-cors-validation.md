# Validación CORS, Render y Vercel — Ganadería 4.0

## 1. Propósito

Este documento guía la validación de integración entre el frontend y el backend desplegado de Ganadería 4.0. El objetivo es reducir el riesgo principal de demo: que el backend esté funcionando en Render, pero el navegador bloquee las llamadas del frontend por configuración CORS, variables de entorno incorrectas o uso accidental de `localhost`.

## 2. Ambientes involucrados

### Backend

- Plataforma: Render.
- URL: `https://ganaderia-backend.onrender.com`.

### Frontend

- Local: `http://localhost:5173`.
- Vercel: `[https://<FRONTEND_VERCEL_URL>](http://localhost:5173,https://ganaderia-frontend-7yzwkkmsf-david-cabrera00s-projects.vercel.app,https://ganaderia-frontend.vercel.app)`.

`<FRONTEND_VERCEL_URL>` debe reemplazarse por el dominio real de Vercel cuando esté definido.

## 3. Variables de entorno del backend en Render

La variable real de CORS verificada en el backend es:

```text
APP_CORS_ALLOWED_ORIGINS
```

Internamente se mapea a:

```text
app.cors.allowed-origins
```

Ejemplo seguro para demo local + Vercel:

```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://<FRONTEND_VERCEL_URL>
```

Variables del backend que conviene revisar en Render:

- CORS:
  - `APP_CORS_ALLOWED_ORIGINS`.
- Profile activo:
  - variable de perfil Spring si el despliegue la usa.
- Base de datos:
  - `<DATABASE_URL>` o variables equivalentes del proveedor.
- JWT:
  - `<JWT_SECRET>` o variable equivalente configurada en el proyecto.
- Resend/EMAIL:
  - `<RESEND_API_KEY>`.
  - variable de remitente EMAIL.
  - variables de activación de EMAIL.
- Gemini/IA:
  - `<GEMINI_API_KEY>`.
  - variables de activación/proveedor de IA.

No pegar secretos reales en documentación, capturas, consola compartida ni repositorio.

## 4. Variables de entorno del frontend en Vercel

El frontend debe apuntar al backend de Render:

```text
VITE_API_URL=https://ganaderia-backend.onrender.com
```

Reglas operativas:

- Después de cambiar variables en Vercel, redeployar/recompilar el frontend.
- No usar `localhost` como API URL en Vercel.
- Evitar slash final si el cliente HTTP del frontend concatena rutas asumiendo base URL sin slash, salvo que el frontend esté preparado para normalizarlo.

## 5. Validación básica del backend

Pruebas básicas:

```http
GET https://ganaderia-backend.onrender.com/healthz
GET https://ganaderia-backend.onrender.com/actuator/health
GET https://ganaderia-backend.onrender.com/swagger-ui/index.html
```

Resultado esperado:

- Health responde HTTP 200.
- `/actuator/health` responde estado correcto.
- Swagger UI carga si está habilitado en el ambiente.
- Algunos endpoints Actuator pueden requerir rol `ADMINISTRADOR` según configuración.

## 6. Validación desde frontend local

Pasos:

1. Configurar frontend local con:

```text
VITE_API_URL=https://ganaderia-backend.onrender.com
```

2. Ejecutar frontend:

```powershell
npm run dev
```

3. Abrir:

```text
http://localhost:5173
```

4. Probar login.
5. Probar un endpoint protegido.
6. Revisar la pestaña Network del navegador.

Resultado esperado:

- No aparece error CORS.
- Login responde 200.
- Se recibe JWT.
- Las requests protegidas envían:

```http
Authorization: Bearer <JWT>
```

## 7. Validación desde Vercel

Pasos:

1. Configurar variable en Vercel:

```text
VITE_API_URL=https://ganaderia-backend.onrender.com
```

2. Confirmar dominio Vercel real:

```text
https://<FRONTEND_VERCEL_URL>
```

3. Agregar ese dominio al CORS del backend en Render:

```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://<FRONTEND_VERCEL_URL>
```

4. Redeploy/restart backend si el cambio de variable CORS requiere reinicio.
5. Redeploy frontend si cambió `VITE_API_URL`.
6. Abrir frontend Vercel.
7. Probar login y un módulo protegido.

Resultado esperado:

- No hay bloqueo CORS.
- El backend responde.
- JWT se usa correctamente.
- El frontend no intenta llamar a `localhost`.

## 8. Validación de preflight OPTIONS

Una request preflight es una llamada `OPTIONS` que el navegador envía antes de algunas requests reales, especialmente cuando hay headers como `Authorization`, `Content-Type: application/json` o métodos como `PATCH`.

En este backend, `OPTIONS /**` está permitido por seguridad y CORS está configurado para permitir los headers esperados.

Resultado esperado:

- `OPTIONS` no debe ser bloqueado.
- La respuesta debe incluir headers CORS correctos.
- Debe permitir `Authorization` y `Content-Type`.

Ejemplo genérico con placeholders:

```bash
curl -i -X OPTIONS "https://ganaderia-backend.onrender.com/api/auth/login" \
  -H "Origin: https://<FRONTEND_VERCEL_URL>" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type,Authorization"
```

## 9. Métodos HTTP esperados

CORS está configurado para permitir:

- `GET`.
- `POST`.
- `PUT`.
- `PATCH`.
- `DELETE`.
- `OPTIONS`.

El backend usa `GET`, `POST`, `PUT` y `PATCH` en endpoints reales. `DELETE` está permitido en CORS aunque no sea el método principal documentado para los flujos actuales.

Atención especial:

- `PATCH` se usa en acciones como resolver/descartar alertas y habilitar/deshabilitar/rotar/asignar collares.
- El frontend debe usar métodos consistentes con los endpoints reales del backend.

## 10. Headers esperados

Headers comunes:

- `Authorization`.
- `Content-Type`.
- `X-Request-Id`.

Headers HMAC solo para device ingestion:

- `X-Device-Token`.
- `X-Device-Timestamp`.
- `X-Device-Nonce`.
- `X-Device-Signature`.

El frontend web normalmente no debe usar `DeviceSecret` ni firmar requests IoT, salvo que exista una herramienta administrativa explícita para pruebas. El `DeviceSecret` nunca debe exponerse en Vercel ni en código frontend.

## 11. Errores comunes y diagnóstico

| Error | Síntoma | Causa probable | Solución |
| --- | --- | --- | --- |
| CORS error en navegador | La consola muestra bloqueo por origin | Dominio frontend no está en `APP_CORS_ALLOWED_ORIGINS`; dominio incorrecto; falta redeploy/restart; diferencia `http` vs `https` | Agregar dominio exacto, redeploy/restart backend y validar Network. |
| Frontend llama a localhost en Vercel | Network muestra `http://localhost:...` | `VITE_API_URL` mal configurado o falta redeploy frontend | Configurar `VITE_API_URL=https://ganaderia-backend.onrender.com` y redeployar. |
| 401 Unauthorized | Endpoint protegido rechaza request | No se envió JWT, JWT expirado o login falló | Rehacer login y verificar `Authorization: Bearer <JWT>`. |
| 403 Forbidden | Login funciona pero endpoint rechaza | Usuario autenticado sin rol suficiente | Usar rol adecuado y revisar matriz de permisos. |
| Mixed content | Navegador bloquea llamada | Frontend HTTPS llama backend HTTP | Usar backend HTTPS. |
| 404 Not Found | Ruta no existe o URL queda duplicada | Base URL mal concatenada o endpoint incorrecto | Revisar `VITE_API_URL` y rutas del cliente HTTP. |
| 500 Internal Server Error | Backend responde error interno | Error real backend o variable faltante | Revisar logs de Render y usar `X-Request-Id` si aparece. |

## 12. Qué NO hacer

- No usar `*` en producción si se requiere control estricto de origins.
- No poner secretos en variables frontend de Vercel.
- No exponer `JWT_SECRET` en frontend.
- No exponer `RESEND_API_KEY` en frontend.
- No exponer `GEMINI_API_KEY` en frontend.
- No exponer `DeviceSecret` en frontend.
- No usar `localhost` como API URL en Vercel.
- No desactivar Spring Security para “arreglar” CORS.
- No commitear archivos `.env` con secretos.
- No pegar JWT reales en documentación, issues, chats o capturas.

Nota técnica: este backend tiene `allowCredentials=false` en CORS. La autenticación esperada viaja por header `Authorization: Bearer <JWT>`, no por cookies con credenciales de navegador.

## 13. Checklist previo a demo frontend/backend

- Backend Render health OK.
- Swagger OK.
- Frontend local o Vercel abre correctamente.
- `VITE_API_URL` apunta a Render.
- Dominio frontend está permitido en `APP_CORS_ALLOWED_ORIGINS`.
- Login OK.
- JWT se guarda/usa según el frontend.
- Endpoint protegido responde.
- No hay CORS errors en consola.
- No hay llamadas a `localhost` desde Vercel.
- EMAIL habilitado si se muestra password reset.
- IA habilitada si se muestra resumen IA.
- No hay secretos visibles en consola, Network o repositorio.

## 14. Criterio de aceptación de la subfase

La integración se considera lista para demo si:

- El frontend carga desde local o Vercel.
- El frontend puede hacer login contra Render.
- El frontend puede consumir al menos un endpoint protegido.
- No hay errores CORS en consola.
- El backend mantiene seguridad JWT y roles.
- No se exponen secretos en el frontend.

## 15. Conclusión

Esta validación reduce el principal riesgo de demo frontend/backend: que el backend funcione correctamente, pero el navegador impida consumirlo por CORS o variables de entorno mal configuradas.

La regla práctica es mantener tres piezas alineadas: `VITE_API_URL` en el frontend, `APP_CORS_ALLOWED_ORIGINS` en Render y uso correcto de `Authorization: Bearer <JWT>` en requests protegidas.
