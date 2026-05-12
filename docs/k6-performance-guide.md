# Guía de performance con k6 — Ganadería 4.0

## 1. Propósito

k6 complementa el load test PowerShell existente con una herramienta estándar para pruebas de performance reproducibles sobre el endpoint IoT del backend.

## 2. Endpoint evaluado

Endpoint:

`POST /api/device/locations`

Headers:

- `X-Device-Token`.
- `X-Device-Timestamp`.
- `X-Device-Nonce`.
- `X-Device-Signature`.

Cada request se firma con HMAC SHA-256 y utiliza nonce único.

## 3. Requisitos

- k6 instalado.
- Backend local o Render disponible.
- Device token válido.
- Device secret válido.
- Collar activo y habilitado.
- Variables de entorno configuradas de forma segura.

## 4. Ejecución segura

```powershell
$env:BASE_URL="https://ganaderia-backend.onrender.com"
$env:DEVICE_TOKEN="<DEVICE_TOKEN>"
$env:DEVICE_SECRET="<DEVICE_SECRET>"
k6 run performance/k6/device-ingestion.js
```

Al terminar la prueba puede limpiarse el secreto del entorno:

```powershell
Remove-Item Env:DEVICE_SECRET
```

## 5. Escenarios

- **Smoke:** escenario conservador por defecto, adecuado para validar conectividad, firma HMAC y estabilidad básica.
- **Light:** puede representarse elevando `VUS`, `DURATION` o `REQUESTS_PER_SECOND` de forma moderada.
- **Medium:** puede representarse con mayor concurrencia controlada, evitando carga agresiva contra Render.

Ejemplo de carga controlada:

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

## 6. Métricas k6 importantes

- `http_req_duration`.
- `http_req_failed`.
- `checks`.
- `iterations`.
- `vus`.
- `data_received`.
- `data_sent`.

## 7. Interpretación de errores

| Error | Causa probable | Acción |
| --- | --- | --- |
| `200` | Request procesada correctamente. | Confirmar tendencia de latencia y checks. |
| `400` | Payload inválido o timestamp del body no aceptado. | Revisar body, formatos y rangos. |
| `401` | Firma inválida, nonce repetido, timestamp de autenticación inválido o token desconocido. | Revisar canonicalización, headers, secreto y reloj. |
| `403` | Regla de autorización o estado operativo que bloquee la acción, si aplica. | Revisar configuración y logs seguros. |
| `429` | Rate limit o protección de abuso. | Reducir tasa o revisar configuración del ambiente. |
| `5xx` | Error backend o saturación. | Revisar logs, métricas y request correlation. |

## 8. Relación con Prometheus/Grafana

Durante la prueba puede levantarse el stack local de observabilidad:

```powershell
docker compose -f docker-compose.observability.yml up -d
```

URLs:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## 9. Qué valida k6

- Concurrencia básica.
- Latencia.
- Tasa de error.
- Estabilidad del endpoint.
- HMAC bajo carga controlada.

## 10. Qué no valida

- Hardware GPS real.
- Batería real.
- Millones de requests.
- Escalamiento horizontal.
- Producción sostenida.
- Resiliencia ante caída de base de datos.

## 11. Seguridad

- No versionar secretos.
- No pegar `DEVICE_SECRET` en documentos.
- No publicar outputs con tokens operativos.
- No saturar Render.
- No desactivar seguridad para que la prueba pase.

## 12. Criterio de aceptación

La subfase queda lista si:

- Existe `performance/k6/device-ingestion.js`.
- Existe `performance/k6/README.md`.
- Existe `docs/k6-performance-guide.md`.
- El script no contiene secretos reales.
- El script usa variables de entorno.
- La firma HMAC se calcula de acuerdo con la canonicalización backend/script real.
- No se tocó código productivo.
- No se ejecutó carga agresiva por defecto.
