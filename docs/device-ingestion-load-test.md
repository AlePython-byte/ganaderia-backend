# Reporte de carga — Device ingestion IoT

## 1. Propósito

Este reporte evalúa el comportamiento del endpoint IoT de Ganadería 4.0 bajo escenarios controlados de carga. La prueba busca demostrar que el backend puede recibir reportes de ubicación firmados con HMAC, validar seguridad por request, registrar datos operativos y responder de forma estable en una carga básica reproducible.

El objetivo no es certificar capacidad máxima de producción, sino aportar evidencia técnica defendible de que la integración IoT funciona bajo concurrencia controlada.

## 2. Endpoint evaluado

Endpoint:

```http
POST /api/device/locations
```

Headers de seguridad:

- `X-Device-Token`.
- `X-Device-Timestamp`.
- `X-Device-Nonce`.
- `X-Device-Signature`.

Cada request debe estar firmada con HMAC y usar un nonce único. El backend valida el token del dispositivo, timestamp, nonce, firma, estado del collar y payload antes de persistir la ubicación.

## 3. Alcance de la prueba

La prueba validó:

- Recepción de ubicaciones.
- Validación HMAC por request.
- Nonces únicos.
- Timestamps compatibles con el backend.
- Persistencia de ubicación.
- Respuesta HTTP 200 bajo carga controlada.
- Conteo de éxitos y errores.
- Latencia promedio.
- Latencia mínima.
- Latencia máxima.
- Percentil 95 simple.

También se validó que el script operativo no imprima el `DeviceSecret` real en el resumen.

## 4. Ambiente de prueba

Las pruebas fueron ejecutadas desde un entorno local usando PowerShell contra el backend configurado para la demo. El resultado puede variar según red, cold start de Render, carga del servicio, estado de la base de datos y recursos disponibles en la máquina cliente.

Dominio real del backend de demo:

```text
https://ganaderia-backend.onrender.com
```

Datos conocidos del entorno usado:

- Backend probado en modo local/demo.
- BaseUrl usada en los comandos registrados: `http://localhost:8080`.
- DeviceToken usado: `COLLAR-001`.
- DeviceSecret: enmascarado.
- Datos demo generados por: `scripts/seed-demo-data.ps1`.
- Fecha aproximada de ejecución: 2026-05-11 / 2026-05-12, según timestamps de consola.

Datos demo relevantes:

- Cow CargaDemo Luna -> `COW-001`.
- Cow CargaDemo Estrella -> `COW-002`.
- Collar CargaDemo Luna -> `COLLAR-001`.
- Collar CargaDemo Estrella -> `COLLAR-002`.
- `COLLAR-001` estaba `ACTIVO` y `enabled=true`.
- Se rotó el secreto de `COLLAR-001` antes de la prueba.

Estos resultados no representan producción ni capacidad máxima del backend en Render.

## 5. Script utilizado

Script:

```powershell
scripts/load-test-device-ingestion.ps1
```

Parámetros reales verificados:

- `BaseUrl`, default `http://localhost:8080`.
- `DeviceToken`, obligatorio.
- `DeviceSecret`, obligatorio.
- `Requests`, default `20`.
- `Concurrency`, default `1`.
- `DelayMs`, default `100`.
- `Latitude`, default `1.2136`.
- `Longitude`, default `-77.2811`.
- `BatteryLevel`, default `85`.
- `GpsAccuracy`, default `8.5`.
- `VerboseErrors`, switch opcional.

El script:

- Envía múltiples requests firmadas.
- Genera nonce único por request.
- Usa timestamp de header en UTC con `Z`.
- Usa timestamp de body compatible con `LocalDateTime`.
- Firma el mismo body que envía.
- No imprime `DeviceSecret` real.
- Enmascara `DeviceToken`.
- Reporta status codes, `successCount`, `errorCount`, duración y latencias.
- Calcula latencia promedio, mínima, máxima y p95 simple.

## 6. Corrección importante de timestamps

Durante la estabilización del script se identificó que enviar el timestamp del body en UTC sin zona podía ser interpretado por el backend como una fecha futura, porque el DTO del body usa un formato tipo `LocalDateTime`.

Estado final del script:

- Header `X-Device-Timestamp`: UTC con `Z`, por ejemplo `yyyy-MM-ddTHH:mm:ssZ`.
- Body `timestamp`: hora local segura en el pasado, sin `Z`, por ejemplo `yyyy-MM-ddTHH:mm:ss`.

Esto mantiene compatibilidad con:

- La firma HMAC.
- La validación temporal del header.
- El contrato del DTO del body.
- La regla que rechaza ubicaciones reportadas demasiado en el futuro.

## 7. Escenarios ejecutados

| Escenario | Requests | Concurrency | DelayMs | Objetivo |
| --- | ---: | ---: | ---: | --- |
| Smoke | 10 | 1 | 100 | Validar flujo básico con carga mínima y firma HMAC correcta. |
| Light | 100 | 5 | 50 | Validar estabilidad con concurrencia baja y volumen moderado. |
| Medium | 500 | 20 | 20 | Validar persistencia y seguridad IoT bajo concurrencia controlada más exigente. |

## 8. Resultados obtenidos

| Escenario | Requests | 200 OK | Success | Errors | DurationMs | Req/s | Avg ms | Min ms | Max ms | P95 ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Smoke | 10 | 10 | 10 | 0 | 6887 | 1.45 | 156.8 | 132 | 237 | 237 |
| Light | 100 | 100 | 100 | 0 | 24467 | 4.09 | 153.13 | 133 | 257 | 178 |
| Medium | 500 | 500 | 500 | 0 | 110358 | 4.53 | 142.25 | 115 | 441 | 162 |

Resultados detallados:

### Smoke

- Requests: 10.
- Status 200: 10.
- successCount: 10.
- errorCount: 0.
- durationMs: 6887.
- req/s aproximado: 1.45.
- averageLatencyMs: 156.8.
- minLatencyMs: 132.
- maxLatencyMs: 237.
- p95LatencyMs: 237.

### Light

- Requests: 100.
- Status 200: 100.
- successCount: 100.
- errorCount: 0.
- durationMs: 24467.
- req/s aproximado: 4.09.
- averageLatencyMs: 153.13.
- minLatencyMs: 133.
- maxLatencyMs: 257.
- p95LatencyMs: 178.

### Medium

- Requests: 500.
- Status 200: 500.
- successCount: 500.
- errorCount: 0.
- durationMs: 110358.
- req/s aproximado: 4.53.
- averageLatencyMs: 142.25.
- minLatencyMs: 115.
- maxLatencyMs: 441.
- p95LatencyMs: 162.

## 9. Interpretación técnica

Los tres escenarios terminaron con 0 errores. El endpoint respondió HTTP 200 en todas las requests.

La latencia promedio se mantuvo estable entre los escenarios:

- Smoke: 156.8 ms.
- Light: 153.13 ms.
- Medium: 142.25 ms.

El p95 se mantuvo bajo para una demo académica/controlada:

- Smoke: 237 ms.
- Light: 178 ms.
- Medium: 162 ms.

El escenario Medium validó 500 ubicaciones con concurrencia 20 y 0 errores. Esto demuestra estabilidad funcional bajo carga controlada, incluyendo validación HMAC, nonce, timestamp, payload y persistencia.

La tasa de req/s no debe interpretarse como máximo absoluto del sistema. Está influenciada por PowerShell, `DelayMs`, red, entorno local/demo, base de datos y configuración del backend.

## 10. Qué valida esta prueba

Esta prueba valida:

- Firma HMAC por request.
- Nonces únicos.
- Ausencia de replay accidental.
- Timestamp compatible con el contrato del backend.
- Persistencia bajo concurrencia controlada.
- Respuestas HTTP estables.
- Manejo básico de latencia.
- Inclusión de `batteryLevel` y `gpsAccuracy` en payload de prueba.
- Actualización del flujo operativo de device ingestion bajo carga básica.

## 11. Qué NO valida esta prueba

Esta prueba no valida:

- Millones de requests.
- Capacidad máxima real del sistema.
- Escalamiento horizontal.
- Tolerancia a caídas de base de datos.
- Comportamiento de Render bajo carga sostenida larga.
- Consumo real de batería/GPS de hardware físico.
- Múltiples dispositivos físicos reales simultáneos.
- CPU/memoria con precisión, porque no se usó monitoreo externo dedicado.
- Un reemplazo de k6, JMeter, Gatling u otra herramienta profesional de carga.

## 12. Riesgos detectados

Riesgos razonables para futuras pruebas:

- Render puede tener cold start y afectar la primera medición.
- La red puede alterar latencias.
- PostgreSQL externo puede convertirse en cuello de botella.
- Si se aumenta concurrencia sin estrategia, pueden aparecer conflictos, rate limits o saturación.
- Rate limiting/abuse protection puede afectar pruebas si no se configura adecuadamente.
- HMAC agrega costo computacional por request, aunque es necesario por seguridad.
- El cliente PowerShell y `DelayMs` limitan el throughput observado.

## 13. Recomendaciones futuras

Recomendaciones:

- Crear pruebas con k6, JMeter o Gatling para escenarios más reproducibles.
- Medir CPU y memoria durante la carga.
- Medir `/actuator/prometheus` o métricas Prometheus mientras se ejecuta la prueba.
- Probar múltiples dispositivos/collares.
- Probar escenarios con errores esperados:
  - firma inválida.
  - nonce repetido.
  - timestamp expirado.
  - collar deshabilitado.
- Definir un SLO académico, por ejemplo:
  - 0 errores en smoke/light/medium.
  - p95 bajo 500 ms en carga controlada.
- Documentar configuración exacta del ambiente antes de cada corrida.
- Separar pruebas locales de pruebas contra Render para comparar latencia y cold start.

## 14. Conclusión

El endpoint device ingestion demostró estabilidad bajo escenarios controlados. Las 610 requests acumuladas de Smoke + Light + Medium terminaron exitosamente, sin errores HTTP ni fallos funcionales.

La prueba respalda que la integración IoT no es solo teórica: el backend recibió reportes firmados, validó HMAC, nonce y timestamp, procesó payloads de ubicación y respondió correctamente bajo carga básica.

Para producción real se requerirían pruebas más exigentes, monitoreo externo, escenarios de larga duración y herramientas especializadas de carga.

## Comandos reproducibles

### Smoke

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "<DEVICE_TOKEN>" `
  -DeviceSecret "<DEVICE_SECRET>" `
  -Requests 10 `
  -Concurrency 1 `
  -DelayMs 100
```

### Light

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "<DEVICE_TOKEN>" `
  -DeviceSecret "<DEVICE_SECRET>" `
  -Requests 100 `
  -Concurrency 5 `
  -DelayMs 50
```

### Medium

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
  -BaseUrl "http://localhost:8080" `
  -DeviceToken "<DEVICE_TOKEN>" `
  -DeviceSecret "<DEVICE_SECRET>" `
  -Requests 500 `
  -Concurrency 20 `
  -DelayMs 20
```

Para probar contra otro ambiente, reemplazar `BaseUrl` por `<BACKEND_URL>`.

## Nota de seguridad

No publicar `DeviceSecret`, JWT, API keys ni credenciales usadas durante la prueba. Si el secreto de `COLLAR-001` fue compartido en consola, chat, capturas o documentación durante las pruebas, se recomienda rotarlo después de terminar la validación.
