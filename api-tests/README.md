# Colección API manual — Ganadería 4.0

## Propósito

Estos archivos permiten probar la API del backend Ganadería 4.0 desde IntelliJ IDEA HTTP Client. La colección ayuda a validar endpoints principales, preparar demos técnicas y compartir ejemplos de contrato con frontend/QA sin depender únicamente de Swagger.

## Requisitos

- IntelliJ IDEA con HTTP Client.
- Backend local o Render activo.
- Variables configuradas en el ambiente HTTP Client.
- JWT válido para endpoints protegidos.
- DeviceSecret solo para pruebas controladas de device ingestion.

## Cómo usar

1. Copiar `http-client.env.json.example` a un archivo local de entorno si se desea.
2. Reemplazar placeholders localmente.
3. Ejecutar login en `ganaderia4-auth.http`.
4. Copiar JWT al ambiente si no se usa extracción automática.
5. Ejecutar requests protegidas con `Authorization: Bearer {{jwt}}`.

## Seguridad

- No commitear secretos.
- No commitear JWT reales.
- No commitear DeviceSecret.
- No commitear API keys.
- No pegar credenciales reales en los `.http`.
- Usar placeholders para valores sensibles.

## Archivos incluidos

- `http-client.env.json.example`: ejemplo seguro de ambientes `local` y `render`.
- `ganaderia4-auth.http`: health, login, usuario actual, forgot/reset password.
- `ganaderia4-cows-collars.http`: vacas y collares con tokens generados por backend.
- `ganaderia4-device-ingestion.http`: ejemplo conceptual de device ingestion HMAC.
- `ganaderia4-alerts.http`: consultas y acciones de alertas.
- `ganaderia4-alert-analysis.http`: análisis heurístico e IA de alertas.
- `ganaderia4-notification-outbox.http`: diagnóstico admin y requeue de outbox EMAIL.
- `ganaderia4-reports.http`: reportes operativos y CSV.
