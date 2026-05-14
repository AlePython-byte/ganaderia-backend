# Guia de performance con k6 - Ganaderia 4.0

## 1. Proposito

k6 complementa los scripts PowerShell existentes con herramientas estandar para pruebas de performance reproducibles del backend.

Scripts disponibles:

- `performance/k6/device-ingestion.js`: prueba IoT con HMAC.
- `performance/k6/operational-read.js`: prueba lecturas protegidas con JWT.

La guia especifica de lectura operativa esta en `docs/k6-operational-read-guide.md`.

## 2. Endpoint IoT evaluado

Endpoint:

`POST /api/device/locations`

Headers:

- `X-Device-Token`.
- `X-Device-Timestamp`.
- `X-Device-Nonce`.
- `X-Device-Signature`.

Cada request se firma con HMAC SHA-256 y utiliza nonce unico.

## 3. Requisitos

- k6 instalado.
- Backend local o Render disponible.
- Device token valido.
- Device secret valido.
- Collar activo y habilitado.
- Variables de entorno configuradas de forma segura.

## 4. Ejecucion segura

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

- **Smoke:** escenario conservador por defecto, adecuado para validar conectividad, firma HMAC y estabilidad basica.
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

## 6. Lectura operativa

Para validar endpoints GET protegidos con JWT:

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e VUS=2 `
  -e DURATION=30s `
  performance/k6/operational-read.js
```

Detalles: `docs/k6-operational-read-guide.md`.

## 7. Metricas k6 importantes

- `http_req_duration`.
- `http_req_failed`.
- `checks`.
- `iterations`.
- `vus`.
- `data_received`.
- `data_sent`.

## 8. Interpretacion de errores

| Error | Causa probable | Accion |
| --- | --- | --- |
| `200` | Request procesada correctamente. | Confirmar tendencia de latencia y checks. |
| `400` | Payload o parametros invalidos. | Revisar body, formatos, rangos y query params. |
| `401` | JWT/firma/token/timestamp invalido segun script. | Revisar credenciales, headers y reloj. |
| `403` | Rol insuficiente o endpoint admin con JWT no admin. | Usar rol adecuado. |
| `429` | Rate limit o proteccion de abuso. | Reducir tasa o revisar configuracion del ambiente. |
| `5xx` | Error backend o saturacion. | Revisar logs, metricas y request correlation. |

## 9. Relacion con Prometheus/Grafana

Durante la prueba puede levantarse el stack local de observabilidad:

```powershell
docker compose -f docker-compose.observability.yml up -d
```

URLs:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## 10. Que valida k6

- Concurrencia basica.
- Latencia.
- Tasa de error.
- Estabilidad de endpoints.
- HMAC o JWT segun script.

## 11. Que no valida

- Hardware GPS real.
- Bateria real.
- Millones de requests.
- Escalamiento horizontal.
- Produccion sostenida.
- Resiliencia ante caida de base de datos.

## 12. Seguridad

- No versionar secretos.
- No pegar `DEVICE_SECRET` ni `JWT` en documentos.
- No publicar outputs con tokens operativos.
- No saturar Render.
- No desactivar seguridad para que la prueba pase.

## 13. Criterio de aceptacion

La subfase queda lista si:

- Existen los scripts k6 requeridos.
- Los scripts no contienen secretos reales.
- Los scripts usan variables de entorno.
- No se toca codigo productivo.
- No se ejecuta carga agresiva por defecto.
