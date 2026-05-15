# Reporte de resultados k6 — Ganadería 4.0

## 1. Propósito

Este documento registra los resultados de smoke tests ejecutados con k6 sobre el backend Ganadería 4.0.

k6 complementa otras evidencias de calidad y performance del proyecto:

- pruebas automatizadas Maven;
- load test PowerShell de device ingestion;
- observabilidad local con Prometheus/Grafana.

## 2. Alcance del reporte

Este reporte cubre:

- Smoke real de lecturas operativas contra Render.
- Estado del script k6 de device ingestion.
- Relación con pruebas previas de carga IoT en PowerShell.
- Limitaciones y próximos pasos.

No documenta un stress test ni una prueba de producción sostenida.

## 3. Ambiente de prueba

| Elemento | Valor |
| --- | --- |
| Backend | `https://ganaderia-backend.onrender.com` |
| Herramienta | k6 |
| Tipo de prueba | Smoke controlado |
| Script | `performance/k6/operational-read.js` |
| VUS | `1` |
| Duración | `20s` |
| Threshold p95 | `1500 ms` |
| Autenticación | JWT por variable de entorno |
| Ambiente | Render/demo |
| Fecha de ejecución | Completar manualmente si se requiere trazabilidad formal |

No se incluye JWT real ni ningún secreto en este reporte.

## 4. Script ejecutado

Script:

`performance/k6/operational-read.js`

Características:

- Usa `JWT` por variable de entorno.
- Solo ejecuta requests `GET`.
- No modifica datos.
- Envía `X-Request-Id` por request.
- No ejecuta `ai-summary` por defecto.
- No ejecuta outbox admin por defecto.
- Usa thresholds configurables por ambiente mediante `P95_THRESHOLD_MS`.

## 5. Comando usado

Comando seguro con placeholders:

```powershell
k6 run `
  -e BASE_URL=https://ganaderia-backend.onrender.com `
  -e JWT=<JWT> `
  -e VUS=1 `
  -e DURATION=20s `
  -e P95_THRESHOLD_MS=1500 `
  performance/k6/operational-read.js
```

## 6. Endpoints evaluados

| Módulo | Endpoint | Resultado esperado |
| --- | --- | --- |
| Cows | `GET /api/cows/page?sort=id&direction=ASC&page=0&size=10` | `200 OK` |
| Collars | `GET /api/collars/page?sort=id&direction=ASC&page=0&size=10` | `200 OK` |
| Alerts | `GET /api/alerts` | `200 OK` |
| Alerts | `GET /api/alerts/status/PENDIENTE` | `200 OK` |
| Alerts | `GET /api/alerts/pending/priority-queue` | `200 OK` |
| Dashboard | `GET /api/dashboard/summary` | `200 OK` |
| Alert analysis | `GET /api/alert-analysis/summary` | `200 OK` |
| Alert analysis | `GET /api/alert-analysis/top-priorities?limit=5` | `200 OK` |

No se incluyeron por defecto:

- `GET /api/alert-analysis/ai-summary`, para evitar consumo innecesario de Gemini.
- `GET /api/admin/notification-outbox`, porque requiere JWT con rol `ADMINISTRADOR` y se activa solo con `INCLUDE_ADMIN_OUTBOX=true`.

## 7. Resultados del smoke operational-read

| Métrica | Resultado |
| --- | --- |
| `checks_total` | `160` |
| `checks_succeeded` | `160` |
| `checks_failed` | `0` |
| Checks success rate | `100.00%` |
| `http_req_failed` | `0.00%` |
| `http_reqs` | `40` |
| `iterations` | `5` |
| VUS | `1` |
| Avg latency | `416.91 ms` |
| Min latency | `205.44 ms` |
| Median latency | `349.87 ms` |
| Max latency | `884.96 ms` |
| p90 latency | `613.49 ms` |
| p95 latency | `787.07 ms` |

También se confirmó que en el smoke no hubo:

- `401 Unauthorized`.
- `403 Forbidden`.
- `500 Internal Server Error`.

## 8. Thresholds

| Threshold | Resultado | Estado |
| --- | --- | --- |
| `checks rate>0.99` | `100.00%` | PASS |
| `http_req_failed rate<0.01` | `0.00%` | PASS |
| `http_req_duration p95<1500` | `787.07 ms` | PASS |

## 9. Interpretación técnica

La prueba fue funcionalmente exitosa:

- Todos los endpoints incluidos respondieron `200 OK`.
- No hubo respuestas `401`, `403` ni `500`.
- No hubo fallos HTTP.
- Todos los checks pasaron.
- El p95 quedó por debajo del threshold calibrado para Render/demo.

La latencia observada es razonable para un backend desplegado en Render, consultado por red externa y con base de datos remota.

Este resultado valida un smoke controlado de endpoints de lectura operativa. No debe interpretarse como una prueba de estrés, de alta concurrencia o de capacidad máxima del sistema.

## 10. Comparación con ejecución previa

Antes de configurar `P95_THRESHOLD_MS`, una ejecución previa contra Render había fallado el threshold fijo `p(95)<700ms`, aunque funcionalmente tenía `0%` de errores.

Interpretación:

- `p95<700ms` era demasiado exigente para un ambiente remoto como Render.
- `P95_THRESHOLD_MS=1500` es más realista para demo/remoto.
- La calibración no reduce la calidad de la prueba; ajusta el criterio al ambiente donde se ejecuta.
- El análisis debe considerar `checks`, `http_req_failed`, status HTTP y p95 en conjunto.

## 11. Estado de device-ingestion.js

El script `performance/k6/device-ingestion.js` existe y está preparado para probar:

`POST /api/device/locations`

Características:

- Replica la firma HMAC esperada por el backend.
- Usa `BASE_URL`, `DEVICE_TOKEN` y `DEVICE_SECRET` por variables de entorno.
- Genera nonce único por request.
- Genera timestamp de header en UTC con `Z`.
- Genera timestamp de body compatible con el DTO del backend.
- Usa `P95_THRESHOLD_MS` para threshold p95 configurable.

Estado actual:

- Fue validado previamente con `k6 inspect`.
- No se ejecutó todavía un smoke real k6 con `DEVICE_TOKEN` y `DEVICE_SECRET` reales en esta fase.
- No se deben guardar secretos en archivos, documentación ni outputs compartidos.

## 12. Relación con load test IoT PowerShell

La carga IoT ya fue validada previamente con el script PowerShell documentado en:

`docs/device-ingestion-load-test.md`

Resultados previos conocidos:

| Escenario | Resultado | Errores |
| --- | --- | --- |
| Smoke | `10/10` exitosas | `0` |
| Light | `100/100` exitosas | `0` |
| Medium | `500/500` exitosas | `0` |

Interpretación:

- k6 device ingestion queda como prueba complementaria estándar.
- PowerShell ya aporta evidencia real de carga IoT controlada.
- k6 IoT debe ejecutarse posteriormente con secretos controlados y sin publicar tokens.

## 13. Limitaciones

- La prueba `operational-read` es smoke, no stress test.
- `VUS=1` no representa alta concurrencia.
- No se probó `ai-summary` por defecto para evitar consumo innecesario de Gemini.
- No se probó outbox admin por defecto.
- No se ejecutó k6 device ingestion todavía con secretos reales.
- Render puede variar por red, cold start y carga externa.
- Los resultados pueden cambiar según datos en base de datos y estado del backend.

## 14. Recomendaciones futuras

- Ejecutar `operational-read` con `VUS=2` o `VUS=5` en ventana corta si el ambiente lo permite.
- Ejecutar `device-ingestion.js` con `DEVICE_TOKEN` y `DEVICE_SECRET` seguros.
- Comparar resultados local vs Render.
- Observar Prometheus/Grafana durante la prueba.
- No ejecutar cargas agresivas contra Render.
- Documentar resultados nuevos si se repiten.

## 15. Seguridad

- No versionar JWT.
- No versionar `DEVICE_SECRET`.
- No pegar tokens reales en reportes.
- Limpiar variables de entorno después de pruebas si se usaron secretos.
- No publicar outputs con headers sensibles.
- No ejecutar loops agresivos contra endpoints IA.

Ejemplos sensibles deben mantenerse como placeholders:

- `<JWT>`
- `<DEVICE_TOKEN>`
- `<DEVICE_SECRET>`
- `<REQUEST_ID>`

## 16. Conclusión

El smoke k6 de lecturas operativas contra Render fue exitoso. La API protegida respondió correctamente con `200 OK`, sin errores HTTP y con `checks` al `100.00%`.

Los thresholds calibrados por ambiente permiten interpretar mejor la latencia real en Render. Esta evidencia k6 complementa las pruebas Maven, la documentación técnica y el load test IoT previo ejecutado con PowerShell.
