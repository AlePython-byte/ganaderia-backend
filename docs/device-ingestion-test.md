# Prueba manual del endpoint de dispositivos

Esta guía documenta una forma reproducible de probar manualmente el endpoint:

- `POST /api/device/locations`

## Objetivo de la prueba

Validar manualmente que una solicitud firmada como lo haría un collar o dispositivo IoT:

- usa el formato real de autenticación HMAC del backend;
- supera la validación de timestamp y nonce;
- llega al `DeviceController`;
- procesa una ubicación válida asociada a un collar real.

## Lo que el backend espera realmente

Según `DeviceRequestAuthenticationService`, la firma HMAC se calcula sobre este canonical request exacto:

```text
POST
/api/device/locations
<X-Device-Timestamp>
<X-Device-Nonce>
<raw JSON body>
```

Detalles relevantes del código actual:

- `X-Device-Timestamp` debe ser un `Instant` ISO-8601 UTC, por ejemplo `2026-04-29T15:10:45Z`.
- `X-Device-Nonce` debe ser único por solicitud.
- `X-Device-Signature` debe ser `Base64(HMAC-SHA256(canonicalRequest))`.
- El body debe conservarse exactamente igual entre el cálculo de la firma y el envío HTTP.

## Body JSON real del endpoint

`DeviceLocationRequestDTO` acepta actualmente estos campos:

```json
{
  "latitude": 1.214,
  "longitude": -77.281,
  "timestamp": "2026-04-29T10:10:45",
  "batteryLevel": 18,
  "gpsAccuracy": 4.5
}
```

Notas:

- `timestamp` del body no lleva offset ni sufijo `Z`.
- El script usa UTC sin sufijo `Z` tambien en el body para evitar desfases entre Windows local, Docker y Render.
- `batteryLevel` es opcional. Si se envía, debe estar entre `0` y `100`.
- `gpsAccuracy` es opcional. Si se envía, debe ser mayor o igual a `0` y representa precisión en metros.
- El script PowerShell envía `BatteryLevel` y `GpsAccuracy` solo cuando se proporcionan.

## Datos requeridos

Para una prueba real y aceptada por el backend se necesita:

- `BaseUrl` del backend
- `DeviceToken` real del collar
- `DeviceSecret` derivado real del collar
- `latitude`
- `longitude`
- `batteryLevel` opcional
- `gpsAccuracy` opcional

## Precondiciones mínimas para que la solicitud sea aceptada

Por la validación real del backend, se necesita:

1. Una `cow` existente.
2. Un `collar` existente.
3. El `collar` debe estar asociado a una `cow`.
4. El `collar` debe estar en estado `ACTIVO`.
5. El `collar` debe estar `enabled=true`.
6. Debe existir un secreto HMAC derivable para ese `deviceToken`.

El secreto que usa la firma **no** es `DEVICE_SECRET_MASTER_KEY` directo. El backend deriva un secreto por collar a partir de:

- `deviceToken`
- `deviceSecretSalt`
- `DEVICE_SECRET_MASTER_KEY`

## Cómo obtener un `deviceSecret` útil para la prueba

Hay dos opciones reales:

1. Usar el secreto actual que ya tenga provisionado el collar en tu entorno.
2. Rotar el secreto del collar en un entorno de prueba y usar el valor devuelto por el backend.

El proyecto ya expone un endpoint real para eso:

- `PATCH /api/collars/{id}/rotate-secret`

Ese endpoint:

- requiere JWT
- requiere rol `ADMINISTRADOR`
- rota el secreto del collar
- devuelve un `deviceSecret` listo para firmar

Ejemplo en PowerShell:

```powershell
$jwt = "<JWT_ADMIN>"
$collarId = 1

Invoke-RestMethod `
  -Method Patch `
  -Uri "http://localhost:8080/api/collars/$collarId/rotate-secret" `
  -Headers @{ Authorization = "Bearer $jwt" }
```

Respuesta esperada:

```json
{
  "deviceToken": "COLLAR-001",
  "deviceSecret": "<derived-device-secret>"
}
```

Usa ese `deviceToken` y ese `deviceSecret` en el script manual.

## Script reproducible para Windows

Se agregó el script:

- [scripts/send-device-location.ps1](../scripts/send-device-location.ps1)

El script:

- construye el body JSON real del endpoint
- genera `X-Device-Timestamp` en UTC
- genera un `nonce` único
- arma el canonical request igual que el backend
- calcula `HMAC-SHA256` y la firma Base64
- envía la solicitud con `Invoke-RestMethod`
- muestra la respuesta del servidor

## Variables a configurar

En la ejecución debes definir:

- `BaseUrl`
- `DeviceToken`
- `DeviceSecret`
- `Latitude`
- `Longitude`

Opcionales:

- `BatteryLevel`
- `GpsAccuracy`

`BatteryLevel` y `GpsAccuracy` se envían al backend solo cuando se proporcionan.

## Ejecución

Ejemplo:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\send-device-location.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<derived-device-secret>" `
  -Latitude 1.214 `
  -Longitude -77.281 `
  -BatteryLevel 85 `
  -GpsAccuracy 4.2
```

## Valores que debe reemplazar el usuario

Debes reemplazar:

- `BaseUrl`: URL real del backend
- `DeviceToken`: token público real del collar
- `DeviceSecret`: secreto derivado real del collar
- `Latitude`: latitud a reportar
- `Longitude`: longitud a reportar

Opcionales en el script:

- `BatteryLevel`
- `GpsAccuracy`

`BatteryLevel` y `GpsAccuracy` se pueden enviar opcionalmente.

## Errores comunes

### Firma inválida

Síntoma esperado:

- `401`
- mensaje similar a `Firma de dispositivo invalida`

Causas típicas:

- el canonical string no coincide exactamente con el backend;
- el body firmado no es idéntico al body enviado;
- `DeviceSecret` incorrecto;
- uso de un path distinto de `/api/device/locations`.

### Timestamp fuera de ventana

Síntoma esperado:

- `401`
- mensaje similar a `Timestamp de dispositivo expirado o fuera de ventana`

Causas típicas:

- reloj local desfasado;
- reutilización de un timestamp viejo;
- generación del header fuera de la ventana configurada.

### Nonce repetido

Síntoma esperado:

- `401`
- mensaje similar a `Nonce de dispositivo ya utilizado`

Causa típica:

- reenvío de la misma solicitud o reutilización manual del mismo nonce.

### Device token inexistente

Síntoma esperado:

- `401`
- mensaje similar a `Dispositivo no autorizado`

Causa típica:

- `DeviceToken` no existe en la tabla de collares;
- el collar existe pero no tiene secreto derivable válido.

### Collar sin vaca asociada

Síntoma esperado:

- `400`
- mensaje similar a `El collar no está asociado a ninguna vaca`

Causa típica:

- el collar existe, pero no está vinculado a una `cow`.
