# Validación final de entrega — Ganadería 4.0

## 1. Propósito

Este documento resume la validación final antes de demo, defensa o entrega del backend Ganadería 4.0. Funciona como checklist de cierre para confirmar que el repositorio, la documentación, la calidad técnica, el despliegue, la seguridad y los flujos críticos están listos antes de afirmar que el backend está listo para evaluación.

## 2. Estado esperado del repositorio

Checklist:

- `git status --short` sin salida.
- Rama correcta para entrega.
- Últimos cambios commiteados.
- Últimos cambios pusheados.
- `README.md` actualizado.
- Documentos finales presentes.
- No archivos temporales.
- No `.env` con secretos versionado.
- No archivos generados innecesarios.
- No cambios locales sin explicación.

## 3. Documentación final esperada

| Documento | Propósito |
| --- | --- |
| `README.md` | Resumen principal del backend, funcionalidades, stack, ejecución, despliegue y endpoints. |
| `docs/operational-runbook.md` | Guía operativa para levantar, validar y diagnosticar el backend. |
| `docs/demo-guide.md` | Guía práctica para presentar la demo técnica en orden. |
| `docs/architecture.md` | Explicación de arquitectura, capas, módulos, flujos críticos y decisiones técnicas. |
| `docs/testing-quality-report.md` | Reporte formal de pruebas, calidad, JaCoCo, SpotBugs, Testcontainers y Maven verify. |
| `docs/device-ingestion-load-test.md` | Reporte de carga controlada para device ingestion IoT. |
| `docs/frontend-backend-cors-validation.md` | Guía para validar CORS, Render, Vercel y consumo desde frontend. |
| `docs/security-secrets-checklist.md` | Checklist final de secretos, variables sensibles y rotación. |
| `docs/final-delivery-validation.md` | Checklist final de cierre de entrega. |

## 4. Validación local de calidad

Comando:

```powershell
.\mvnw.cmd clean verify
```

Resultado esperado:

- Compilación OK.
- Tests unitarios OK.
- Tests de integración OK.
- JaCoCo OK.
- SpotBugs OK.
- Maven Enforcer OK.
- Jar generado OK.

Resultado conocido más reciente:

- 277 tests unitarios.
- 198 tests de integración.
- 475 tests totales.
- 0 failures.
- 0 errors.
- 0 skipped.
- 15 migraciones Flyway validadas.
- PostgreSQL Testcontainers usado.

Si se ejecuta nuevamente, registrar fecha, hora y resultado real.

## 5. Validación de Render

Dominio real:

```text
https://ganaderia-backend.onrender.com
```

Checklist:

- `GET /healthz` responde.
- `GET /actuator/health` responde.
- Swagger carga en `/swagger-ui/index.html`.
- Logs de Render no muestran errores críticos.
- Variables de entorno están configuradas.
- Base de datos conectada.
- EMAIL habilitado para demo.
- IA habilitada para demo.

No incluir valores reales de variables ni credenciales en evidencias públicas.

## 6. Validación de autenticación y autorización

Checklist:

- `POST /api/auth/login` responde con JWT.
- `GET /api/auth/me` responde con usuario autenticado.
- Sin JWT, endpoints protegidos devuelven 401.
- Con rol insuficiente, endpoints restringidos devuelven 403.
- `ADMINISTRADOR` accede a endpoints administrativos.
- JWT no se imprime completo en logs.

Ejemplo conceptual:

```json
{
  "email": "<ADMIN_EMAIL>",
  "password": "<ADMIN_PASSWORD>"
}
```

Usar en requests protegidas:

```http
Authorization: Bearer <JWT>
```

## 7. Validación de recuperación de contraseña

Checklist:

- `POST /api/auth/forgot-password` responde sin revelar si el email existe.
- Email real llega mediante Resend.
- `POST /api/auth/reset-password` cambia la contraseña con token válido.
- Token expirado/usado no funciona.
- Logs no exponen token real.
- Modo direct/outbox corresponde a la configuración activa.

No incluir tokens reales en documentación, consola compartida ni capturas.

## 8. Validación de gestión principal

Checklist:

- Crear vaca sin enviar token técnico.
- Backend genera `COW-xxx`.
- Backend genera `INT-xxx` si aplica.
- Crear collar sin enviar token técnico.
- Backend genera `COLLAR-xxx`.
- Listar vacas.
- Listar collares.
- Validar geocercas.
- Validar ubicaciones.

## 9. Validación device ingestion IoT

Checklist:

- Script `scripts/send-device-location.ps1` funciona.
- Request firmada con HMAC.
- Header timestamp usa UTC con `Z`.
- Body timestamp usa formato compatible.
- Nonce único por request.
- Respuesta HTTP 200.
- Ubicación registrada.
- `lastSeenAt` actualizado.
- No se imprime DeviceSecret real.
- Nonce repetido es rechazado.
- Firma inválida es rechazada.

## 10. Validación de alertas

Checklist:

- `COLLAR_OFFLINE` puede generarse/consultarse.
- `EXIT_GEOFENCE` puede generarse/consultarse.
- `LOW_BATTERY` puede generarse/consultarse.
- `LOW_BATTERY` dispara flujo de notificación.
- Alertas pendientes se listan.
- Cola de prioridad funciona.
- Resolver alerta funciona.
- Descartar alerta funciona.

## 11. Validación de notificaciones y outbox

Checklist:

- LOG channel funciona.
- WEBHOOK channel no rompe flujo si falla.
- EMAIL channel con Resend funciona.
- Preferencias de usuario se respetan.
- Outbox EMAIL encola mensajes.
- Processor cambia estados.
- `SENT` confirmado si envío fue exitoso.
- `FAILED`/`DEAD` pueden diagnosticarse.
- Requeue admin funciona para `FAILED`/`DEAD`.
- No se imprimen API keys.

## 12. Validación de IA analítica

Checklist:

- `GET /api/alert-analysis/summary` funciona.
- `GET /api/alert-analysis/top-priorities?limit=5` funciona.
- `GET /api/alert-analysis/ai-summary` funciona.
- Gemini responde si está habilitado.
- Fallback heurístico responde si Gemini falla.
- Respuesta incluye:
  - `summary`.
  - `riskLevel`.
  - `recommendations`.
  - `source`.
  - `fallbackUsed`.
  - `generatedAt`.
- No se expone `GEMINI_API_KEY`.

## 13. Validación de reportes

Checklist:

- Reportes CSV descargan correctamente.
- CSV no ejecuta fórmulas peligrosas.
- Filtros principales funcionan.
- Respuesta tiene content-type esperado.
- No se exponen datos sensibles innecesarios.

## 14. Validación de observabilidad

Checklist:

- `X-Request-Id` aparece o se genera.
- Logs usan `event=...`.
- `/healthz` funciona.
- `/actuator/health` funciona.
- `/actuator/metrics` funciona según permisos.
- `/actuator/prometheus` funciona según permisos.
- Métricas de dominio disponibles.
- Métricas IA/outbox/cleanup disponibles si fueron configuradas.

## 15. Validación frontend/backend

Checklist:

- Frontend local usa:

```text
VITE_API_URL=https://ganaderia-backend.onrender.com
```

- Frontend Vercel usa:

```text
VITE_API_URL=https://ganaderia-backend.onrender.com
```

- Dominio Vercel real está en `APP_CORS_ALLOWED_ORIGINS`.
- No hay error CORS en navegador.
- Login desde frontend funciona.
- Endpoint protegido desde frontend funciona.
- Frontend no llama `localhost` en producción.
- Frontend no contiene secretos backend.

## 16. Validación de secretos

Checklist:

- README sin secretos reales.
- `docs/` sin secretos reales.
- Scripts sin secretos hardcodeados.
- `.env` no versionado.
- `.env.example` solo placeholders.
- `JWT_SECRET` no expuesto.
- `APP_NOTIFICATIONS_EMAIL_API_KEY` no expuesta.
- `GEMINI_API_KEY` no expuesta.
- DeviceSecret no expuesto.
- `DB_URL` real no expuesta.
- Logs Render revisados.
- Secretos usados en demo rotados si fueron compartidos.

## 17. Validación de carga IoT

Resultado conocido:

Smoke:

- 10/10 exitosas.
- 0 errores.

Light:

- 100/100 exitosas.
- 0 errores.

Medium:

- 500/500 exitosas.
- 0 errores.

Conclusión:

- 610 requests acumuladas exitosas.
- La prueba valida carga controlada, no producción masiva.
- Reejecutar si cambia ambiente.

## 18. Checklist de demo final

Orden recomendado:

1. Health Render.
2. Swagger.
3. Login.
4. Roles.
5. Vacas/collares.
6. Seed demo.
7. Device ingestion.
8. Alertas.
9. EMAIL.
10. Outbox.
11. Password reset.
12. IA.
13. Reportes.
14. Observabilidad.
15. Pruebas/calidad.
16. Cierre técnico.

## 19. Criterio final de aceptación

El backend puede considerarse listo para entrega si:

- Repositorio limpio.
- Documentación final presente.
- `clean verify` en verde o último resultado conocido aceptado.
- Render health OK.
- Login OK.
- Device ingestion OK.
- EMAIL OK.
- IA OK.
- Outbox OK.
- No secretos expuestos.
- Frontend puede consumir backend o existe guía clara para configurar CORS.
- Demo puede ejecutarse siguiendo `docs/demo-guide.md`.

## 20. Pendientes aceptables

Pendientes que no bloquean entrega:

- Grafana visual si no está configurado.
- k6/JMeter avanzado.
- SMS real si no existe proveedor.
- E2E frontend/backend más profundo.
- Escalamiento horizontal.
- Monitoreo productivo continuo.

## 21. Pendientes que sí bloquean entrega

Bloquean entrega:

- Secretos reales en repo.
- `clean verify` fallando sin explicación.
- Render health caído.
- Login roto.
- Base de datos inaccesible.
- Endpoint device ingestion roto.
- EMAIL/IA prometidos en demo pero no configurados.
- README contradice el estado real.
- `git status` sucio sin explicación.

## 22. Conclusión

Esta validación permite entregar el backend Ganadería 4.0 con evidencia técnica, operativa y documental.

El cierre no depende solo de que el código compile: también requiere documentación consistente, pruebas conocidas, despliegue verificable, secretos protegidos, flujos críticos demostrables y un camino claro para presentar el sistema ante evaluación técnica.
