# Guia k6 de lectura operativa - Ganaderia 4.0

## 1. Proposito

Esta prueba valida endpoints de lectura usados por frontend y demo tecnica. Complementa la prueba k6 de device ingestion, pero usa JWT y no ejecuta operaciones destructivas.

## 2. Endpoints evaluados

Siempre incluidos:

- `GET /api/cows/page?sort=id&direction=ASC&page=0&size=10`
- `GET /api/collars/page?sort=id&direction=ASC&page=0&size=10`
- `GET /api/alerts`
- `GET /api/alerts/status/PENDIENTE`
- `GET /api/alerts/pending/priority-queue`
- `GET /api/dashboard/summary`
- `GET /api/alert-analysis/summary`
- `GET /api/alert-analysis/top-priorities?limit=5`

Opcionales:

- `GET /api/admin/notification-outbox?channel=EMAIL&page=0&size=10`, solo con `INCLUDE_ADMIN_OUTBOX=true` y JWT con rol `ADMINISTRADOR`.
- `GET /api/alert-analysis/ai-summary`, solo con `INCLUDE_AI_SUMMARY=true`.

## 3. Requisitos

- k6 instalado.
- Backend local o Render activo.
- JWT valido.
- Rol suficiente para los endpoints incluidos.
- No usar JWT real en archivos versionados.

## 4. Ejecucion contra Render

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e VUS=2 `
  -e DURATION=30s `
  performance/k6/operational-read.js
```

## 5. Ejecucion contra local

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e JWT=<JWT> `
  -e VUS=2 `
  -e DURATION=30s `
  performance/k6/operational-read.js
```

## 6. Ejecutar con outbox admin

El outbox admin requiere rol `ADMINISTRADOR`. Activarlo solo cuando el JWT tenga ese rol:

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e INCLUDE_ADMIN_OUTBOX=true `
  performance/k6/operational-read.js
```

## 7. Ejecutar con IA summary

`GET /api/alert-analysis/ai-summary` puede usar Gemini segun configuracion. Por defecto no se incluye para evitar costo o abuso de IA.

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e INCLUDE_AI_SUMMARY=true `
  performance/k6/operational-read.js
```

## 8. Metricas relevantes

- `http_req_duration`.
- `http_req_failed`.
- `checks`.
- `iterations`.
- `vus`.
- `data_received`.
- `data_sent`.

## 9. Interpretacion de errores

| Error | Causa probable | Accion |
| --- | --- | --- |
| `200` | Request procesada correctamente. | Revisar latencia, checks y tendencia. |
| `400` | Parametros invalidos. | Revisar query params y contrato del endpoint. |
| `401` | JWT faltante, expirado o invalido. | Renovar JWT y verificar header `Authorization`. |
| `403` | Rol insuficiente. | Usar un JWT con rol autorizado para esos endpoints. |
| `404` | Ruta incorrecta o endpoint no disponible. | Revisar `BASE_URL` y path. |
| `429` | Rate limiting o proteccion de abuso. | Reducir VUs/duracion o revisar configuracion. |
| `500` | Error backend. | Revisar logs y `X-Request-Id`. |

## 10. Relacion con Prometheus/Grafana

Durante la ejecucion se puede observar el backend con el stack local:

```powershell
docker compose -f docker-compose.observability.yml up -d
```

URLs:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## 11. Seguridad

- No commitear JWT.
- No pegar tokens reales en docs.
- No publicar outputs con headers.
- No ejecutar cargas agresivas contra Render.
- No activar `ai-summary` masivamente si Gemini esta encendido.

## 12. Que valida

- Lecturas protegidas con JWT.
- Latencia basica.
- Estabilidad de endpoints consultados por frontend.
- Comportamiento bajo carga moderada.
- Uso de `X-Request-Id`.

## 13. Que no valida

- Flujos de escritura.
- Device ingestion HMAC.
- Password reset.
- Email.
- Resiliencia ante caida de base de datos.
- Carga productiva masiva.
- E2E completo con frontend.

## 14. Criterio de aceptacion

La subfase queda lista si:

- Existe `performance/k6/operational-read.js`.
- Existe `docs/k6-operational-read-guide.md`.
- `performance/k6/README.md` menciona el nuevo script.
- No hay secretos reales.
- No toca Java.
- El script usa variables de entorno.
- Los endpoints incluidos fueron verificados contra controladores reales.
- No ejecuta operaciones destructivas.
