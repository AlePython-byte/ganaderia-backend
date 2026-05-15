# Colección Bruno — Ganadería 4.0 Backend API

## Propósito

Esta colección permite probar manualmente endpoints principales del backend Ganadería 4.0 desde Bruno. Complementa Swagger/OpenAPI y la colección IntelliJ HTTP Client ubicada en `api-tests/`.

## Requisitos

- Bruno instalado.
- Backend local o Render activo.
- Environment `local` o `render` seleccionado.
- Variables sensibles configuradas localmente.
- JWT válido para endpoints protegidos.
- Rol suficiente para endpoints administrativos.
- No commitear secretos reales.

## Cómo usar

1. Abrir Bruno.
2. Abrir la carpeta `bruno/` como colección.
3. Seleccionar environment `local` o `render`.
4. Reemplazar placeholders sensibles localmente.
5. Ejecutar `Auth/Login`.
6. Copiar manualmente el token recibido a la variable `jwt` si no se configura extracción automática.
7. Probar requests protegidos.

## Seguridad

- No guardar JWT real en Git.
- No guardar contraseña real de administrador en Git.
- No guardar DeviceSecret en Bruno.
- No guardar API keys.
- No publicar capturas con tokens.
- No ejecutar `AI Summary` en bucle porque puede consumir Gemini y tiene rate limiting.
- No reencolar outbox repetidamente.
- No compartir environments con credenciales reales.

## Requests que modifican estado

Los siguientes requests pueden cambiar datos o disparar efectos operativos:

- `Cows/Create Cow`.
- `Collars/Create Collar`.
- `Auth/Forgot Password`, puede enviar email real si EMAIL está habilitado.
- `Auth/Reset Password`, cambia contraseña si el token es válido.
- `Notification Outbox Admin/Requeue Outbox Message`, modifica estado del outbox y tiene rate limiting.
- `Device Ingestion/Device Location Conceptual`, registra ubicación si la firma HMAC es válida.

La colección base evita incluir acciones PATCH de alertas para mantenerla orientada a demo segura.

## Variables

| Variable | Uso | Sensible | Ejemplo seguro |
| --- | --- | --- | --- |
| `baseUrl` | URL base del backend. | No | `https://ganaderia-backend.onrender.com` |
| `adminEmail` | Email usado para login/forgot-password. | Sí | `<ADMIN_EMAIL>` |
| `adminPassword` | Contraseña usada para login. | Sí | `<ADMIN_PASSWORD>` |
| `jwt` | Token Bearer para endpoints protegidos. | Sí | `<JWT>` |
| `cowId` | Id de vaca para crear collar u obtener recursos. | No | `1` |
| `collarId` | Id de collar para pruebas específicas. | No | `1` |
| `alertId` | Id de alerta para consultas si se agregan requests por id. | No | `1` |
| `outboxMessageId` | Id de mensaje outbox para detalle/requeue. | No | `<OUTBOX_MESSAGE_ID>` |
| `resetToken` | Token de recuperación de contraseña. | Sí | `<RESET_TOKEN>` |
| `newPassword` | Nueva contraseña de prueba. | Sí | `<NEW_PASSWORD>` |
| `deviceToken` | Token técnico del collar. | Sí | `<DEVICE_TOKEN>` |
| `deviceTimestamp` | Timestamp UTC para header HMAC. | No | `<UTC_TIMESTAMP_WITH_Z>` |
| `deviceNonce` | Nonce único para header HMAC. | No | `<UNIQUE_NONCE>` |
| `deviceSignature` | Firma HMAC calculada externamente. | Sí | `<HMAC_SIGNATURE>` |
| `requestId` | Correlación de logs. | No | `bruno-render-demo-001` |

## Device ingestion

Bruno no debe almacenar DeviceSecret real. Para una prueba IoT real:

1. Generar timestamp, nonce y firma HMAC con `scripts/send-device-location.ps1` o herramienta controlada.
2. Copiar solo `deviceToken`, `deviceTimestamp`, `deviceNonce` y `deviceSignature` a variables locales.
3. Ejecutar `Device Ingestion/Device Location Conceptual`.

La firma no es reutilizable si cambian nonce, timestamp o body.

## Relación con otras herramientas

Bruno complementa:

- Swagger/OpenAPI para contrato explorable.
- `api-tests/` de IntelliJ HTTP Client.
- Scripts PowerShell operativos.
- k6 para performance.
- Prometheus/Grafana para observabilidad.

## Notas de demo

- `AI Summary` puede usar Gemini y tiene rate limiting.
- `Requeue Outbox Message` requiere rol `ADMINISTRADOR`, cambia estado y tiene rate limiting.
- `Forgot Password` puede enviar email real con Resend.
- No se ejecutaron requests reales al crear esta colección.
