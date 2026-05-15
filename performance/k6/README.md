# k6 performance tests - Ganaderia 4.0

## Proposito

Esta carpeta contiene pruebas k6 para validar performance basica del backend Ganaderia 4.0 con herramientas reproducibles.

## Scripts incluidos

| Script | Proposito | Seguridad |
| --- | --- | --- |
| `device-ingestion.js` | Prueba `POST /api/device/locations` con firma HMAC por request. | Requiere `DEVICE_TOKEN` y `DEVICE_SECRET`. |
| `operational-read.js` | Prueba endpoints de lectura protegidos con JWT usados por frontend/demo. | Requiere `JWT`. No ejecuta escrituras. |

## Requisitos generales

- k6 instalado.
- Backend local o Render disponible.
- Variables de entorno configuradas.
- No guardar secretos reales en archivos versionados.
- No ejecutar carga agresiva contra Render.

## Device ingestion

Variables principales:

| Variable | Obligatoria | Descripcion | Ejemplo seguro |
| --- | --- | --- | --- |
| `BASE_URL` | Si | URL base del backend. | `https://ganaderia-backend.onrender.com` |
| `DEVICE_TOKEN` | Si | Token tecnico del collar. | `<DEVICE_TOKEN>` |
| `DEVICE_SECRET` | Si | Secreto HMAC del collar. | `<DEVICE_SECRET>` |
| `REQUESTS_PER_SECOND` | No | Tasa controlada para `constant-arrival-rate`. | `2` |
| `DURATION` | No | Duracion total del escenario. | `30s` |
| `VUS` | No | Usuarios virtuales base. | `1` |
| `P95_THRESHOLD_MS` | No | Threshold p95 de latencia en milisegundos. Default: `1500`. | `1500` |

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e DEVICE_TOKEN=<DEVICE_TOKEN> `
  -e DEVICE_SECRET=<DEVICE_SECRET> `
  -e P95_THRESHOLD_MS=1500 `
  performance/k6/device-ingestion.js
```

## Lectura operativa

Variables principales:

| Variable | Obligatoria | Descripcion | Ejemplo seguro |
| --- | --- | --- | --- |
| `BASE_URL` | Si | URL base del backend. | `https://ganaderia-backend.onrender.com` |
| `JWT` | Si | Token JWT valido para endpoints protegidos. | `<JWT>` |
| `VUS` | No | Usuarios virtuales. | `2` |
| `DURATION` | No | Duracion total del escenario. | `30s` |
| `P95_THRESHOLD_MS` | No | Threshold p95 de latencia en milisegundos. Default: `1500`. | `1500` |
| `INCLUDE_ADMIN_OUTBOX` | No | Incluye outbox admin si el JWT es ADMINISTRADOR. | `true` |
| `INCLUDE_AI_SUMMARY` | No | Incluye endpoint de IA con posible costo Gemini. | `true` |

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e VUS=2 `
  -e DURATION=30s `
  -e P95_THRESHOLD_MS=1500 `
  performance/k6/operational-read.js
```

## Thresholds por ambiente

`P95_THRESHOLD_MS` permite calibrar el threshold de latencia p95 segun el ambiente:

- Local: `700`.
- Render/demo: `1500`.
- Red lenta o Render con cold start: `2000`.

Un threshold fallido no siempre significa que el backend este roto. Si `checks` esta en 100%, `http_req_failed` es 0% y los status son `200`, puede indicar que el valor de p95 esta demasiado estricto para la red o plataforma usada.

## Seguridad

- No commitear `DEVICE_SECRET`.
- No commitear `JWT`.
- No imprimir secretos en consola.
- No publicar outputs con headers o tokens reales.
- No reemplazar la seguridad HMAC/JWT para simplificar pruebas.
- No usar credenciales reales en documentacion.

## Interpretacion

- `200 OK` es el resultado esperado.
- `http_req_failed` debe mantenerse bajo.
- `p(95)` permite observar latencia.
- Errores `401` suelen indicar token, firma, timestamp o nonce invalido.
- Errores `403` suelen indicar rol insuficiente.
- Errores `400` suelen indicar payload o parametros invalidos.
