# Pruebas de carga de device ingestion

## Objetivo

Validar la capacidad del endpoint IoT del backend de Ganaderia 4.0 para recibir reportes de ubicacion firmados con HMAC, procesar el payload operativo y persistir ubicaciones bajo carga controlada.

## Endpoint probado

`POST /api/device/locations`

## Seguridad del endpoint

El endpoint requiere autenticacion de dispositivo mediante:

- `X-Device-Token`: token publico del collar.
- `X-Device-Timestamp`: timestamp UTC del request.
- `X-Device-Nonce`: nonce unico por request para proteccion anti-replay.
- `X-Device-Signature`: firma HMAC SHA-256 del request canonico.
- `DeviceSecret`: secreto compartido del dispositivo, usado para firmar. No se documenta en claro.

Durante estas pruebas el `DeviceSecret` se mantuvo enmascarado y no se incluye en este documento.

## Entorno de prueba

- Backend: local.
- BaseUrl: `http://localhost:8080`.
- DeviceToken usado: `COLLAR-001`.
- DeviceSecret: enmascarado.
- Datos demo generados por: `scripts/seed-demo-data.ps1`.
- Fecha aproximada de ejecucion: 2026-05-11 / 2026-05-12, segun timestamps de consola.

Datos demo relevantes:

- Cow CargaDemo Luna -> `COW-001`.
- Cow CargaDemo Estrella -> `COW-002`.
- Collar CargaDemo Luna -> `COLLAR-001`.
- Collar CargaDemo Estrella -> `COLLAR-002`.
- `COLLAR-001` estaba `ACTIVO` y `enabled=true`.
- Se roto el secreto de `COLLAR-001` antes de la prueba.

Estos resultados corresponden a un entorno local academico. No representan produccion ni Render.

## Escenarios ejecutados

| Escenario | Requests | Concurrency | DelayMs | Status 200 | Errores | Req/s aprox. | Latencia promedio | Min | Max | P95 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Smoke | 10 | 1 | 100 | 10 | 0 | 1.45 | 156.8 ms | 132 ms | 237 ms | 237 ms |
| Light | 100 | 5 | 50 | 100 | 0 | 4.09 | 153.13 ms | 133 ms | 257 ms | 178 ms |
| Medium | 500 | 20 | 20 | 500 | 0 | 4.53 | 142.25 ms | 115 ms | 441 ms | 162 ms |

## Resultados detallados

### Smoke

- Status codes:
  - 200: 10
- totalRequests: 10
- successCount: 10
- errorCount: 0
- durationMs: 6887
- approximateRequestsPerSecond: 1.45
- averageLatencyMs: 156.8
- minLatencyMs: 132
- maxLatencyMs: 237
- p95LatencyMs: 237

### Light

- Status codes:
  - 200: 100
- totalRequests: 100
- successCount: 100
- errorCount: 0
- durationMs: 24467
- approximateRequestsPerSecond: 4.09
- averageLatencyMs: 153.13
- minLatencyMs: 133
- maxLatencyMs: 257
- p95LatencyMs: 178

### Medium

- Status codes:
  - 200: 500
- totalRequests: 500
- successCount: 500
- errorCount: 0
- durationMs: 110358
- approximateRequestsPerSecond: 4.53
- averageLatencyMs: 142.25
- minLatencyMs: 115
- maxLatencyMs: 441
- p95LatencyMs: 162

## Interpretacion tecnica

Los tres escenarios terminaron con 0 errores. El endpoint mantuvo respuestas HTTP 200 en todas las solicitudes y valido HMAC, nonce, timestamp y payload bajo carga controlada.

El escenario Medium proceso 500 reportes con 20 de concurrencia y 0 errores. Las latencias se mantuvieron en rangos aceptables para un entorno local academico, con p95 de 162 ms en el escenario Medium.

## Limitaciones

- Prueba ejecutada en entorno local, no en produccion.
- No se midio uso de CPU/RAM.
- No se uso k6/JMeter todavia.
- El throughput esta influenciado por PowerShell, `DelayMs` y el entorno local.
- No representa capacidad maxima real.
- No se debe publicar el `DeviceSecret`.

## Conclusiones

El backend soporto correctamente carga controlada de reportes IoT firmados, con 500/500 solicitudes exitosas en el escenario Medium y sin errores HTTP. Esto demuestra estabilidad funcional del endpoint bajo validacion HMAC, proteccion anti-replay por nonce y persistencia de ubicaciones.

## Comandos reproducibles

### Smoke

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<DEVICE_SECRET_MASKED>" `
  -Requests 10 `
  -Concurrency 1 `
  -DelayMs 100
```

### Light

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<DEVICE_SECRET_MASKED>" `
  -Requests 100 `
  -Concurrency 5 `
  -DelayMs 50
```

### Medium

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "COLLAR-001" `
  -DeviceSecret "<DEVICE_SECRET_MASKED>" `
  -Requests 500 `
  -Concurrency 20 `
  -DelayMs 20
```

## Nota de seguridad

Si el secreto de `COLLAR-001` fue compartido en consola, chat, capturas o documentacion durante las pruebas, se recomienda rotarlo despues de terminar la validacion.
