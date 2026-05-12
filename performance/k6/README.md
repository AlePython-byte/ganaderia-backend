# k6 performance tests — Ganadería 4.0

## Propósito

Esta carpeta contiene una prueba de performance con k6 para el endpoint IoT `POST /api/device/locations`. Complementa el script PowerShell de carga existente con una herramienta más estándar para medir latencia, errores y concurrencia básica.

## Requisitos

- k6 instalado.
- Backend local o Render disponible.
- `DEVICE_TOKEN` válido.
- `DEVICE_SECRET` válido.
- Collar habilitado y activo.
- No guardar secretos reales en archivos versionados.

## Variables de entorno

| Variable | Obligatoria | Descripción | Ejemplo seguro |
| --- | --- | --- | --- |
| `BASE_URL` | Sí | URL base del backend. | `https://ganaderia-backend.onrender.com` |
| `DEVICE_TOKEN` | Sí | Token técnico del collar. | `<DEVICE_TOKEN>` |
| `DEVICE_SECRET` | Sí | Secreto HMAC del collar. | `<DEVICE_SECRET>` |
| `REQUESTS_PER_SECOND` | No | Tasa controlada para `constant-arrival-rate`. | `2` |
| `DURATION` | No | Duración total del escenario. | `30s` |
| `VUS` | No | Usuarios virtuales base. | `1` |
| `LATITUDE` | No | Latitud enviada en el payload. | `1.2136` |
| `LONGITUDE` | No | Longitud enviada en el payload. | `-77.2811` |
| `BATTERY_LEVEL` | No | Batería de ejemplo. | `85` |
| `GPS_ACCURACY` | No | Precisión GPS de ejemplo. | `8.5` |

## Ejecutar smoke test

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e DEVICE_TOKEN=<DEVICE_TOKEN> `
  -e DEVICE_SECRET=<DEVICE_SECRET> `
  performance/k6/device-ingestion.js
```

Por defecto el script usa un escenario conservador de `1` VU durante `30s`.

## Ejecutar contra local

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e DEVICE_TOKEN=<DEVICE_TOKEN> `
  -e DEVICE_SECRET=<DEVICE_SECRET> `
  -e DURATION=20s `
  performance/k6/device-ingestion.js
```

## Ejecutar contra Render

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e DEVICE_TOKEN=<DEVICE_TOKEN> `
  -e DEVICE_SECRET=<DEVICE_SECRET> `
  -e REQUESTS_PER_SECOND=2 `
  -e VUS=2 `
  -e DURATION=30s `
  performance/k6/device-ingestion.js
```

No se recomienda aplicar carga agresiva contra Render durante una demo.

## Seguridad

- No commitear `DEVICE_SECRET`.
- No imprimir secretos en consola.
- No publicar outputs si contienen parámetros operativos sensibles.
- No reemplazar la seguridad HMAC para simplificar la prueba.
- No usar credenciales reales en documentación.

## Interpretación

- `200 OK` es el resultado esperado.
- `http_req_failed` debe mantenerse bajo.
- `p(95)` permite observar el comportamiento de latencia.
- Errores `401` suelen indicar firma inválida, token incorrecto, nonce repetido o timestamp no aceptado.
- Errores `400` suelen indicar payload inválido.

El script firma cada request con HMAC SHA-256 sobre la canonicalización real del backend y codifica la firma en Base64.
