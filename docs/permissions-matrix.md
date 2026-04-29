# Matriz de permisos — Ganadería 4.0

## Propósito

Este documento define la matriz oficial de permisos del backend Ganadería 4.0 para el MVP actual.

Su objetivo es:

- dejar explícito qué endpoints son públicos;
- dejar explícito qué endpoints requieren JWT;
- dejar explícito qué operaciones están protegidas por rol;
- documentar que la ingestión device usa HMAC fuera del modelo JWT/RBAC;
- servir como referencia de revisión antes de modificar `SecurityConfig`, agregar endpoints o cambiar reglas de autorización.

## Roles

### ADMINISTRADOR

Rol con capacidad administrativa completa sobre usuarios, auditoría, métricas, alertas mutables y operaciones sensibles del sistema.

### SUPERVISOR

Rol con capacidad operativa y de supervisión. Puede consultar dashboard, reportes, alertas, geocercas, vacas y collares dentro de la política actual.

### TECNICO

Rol orientado a soporte técnico y operación de dispositivos. Puede consultar dashboard, alertas, ubicaciones y collares. También puede crear, actualizar y operar collares en el MVP actual, excepto rotación de secreto.

### OPERADOR

Rol orientado a operación diaria. Puede consultar dashboard, alertas, vacas, collares y ubicaciones. En el MVP actual también puede crear y actualizar vacas.

### Dispositivo / HMAC

No es un rol JWT. Representa el canal de ingestión de telemetría desde collares.

`POST /api/device/locations` es público para Spring Security, pero funcionalmente exige autenticación HMAC por headers y validaciones adicionales del flujo device.

## Tipos de protección

### Endpoints públicos

No requieren JWT. Incluyen login, health checks y endpoints técnicos públicos controlados por el entorno.

### Endpoints autenticados con JWT

Requieren un token JWT válido, pero no necesariamente un rol específico. En el estado actual, el caso relevante es `GET /api/auth/me`.

### Endpoints protegidos por rol

Requieren JWT y además una regla explícita basada en `hasRole(...)`, `hasAnyRole(...)` o `@PreAuthorize(...)`.

### Endpoint device protegido por HMAC

`POST /api/device/locations` no usa JWT ni RBAC de usuario. Se protege mediante:

- headers HMAC;
- nonce anti-replay;
- validación temporal;
- control de abuso;
- validaciones de negocio del payload.

## Fuente técnica actual

La fuente técnica vigente de la autorización es:

- [SecurityConfig.java](/C:/Users/ALEJANDRO/IdeaProjects/ganaderia4backend/src/main/java/com/ganaderia4/backend/config/SecurityConfig.java:1)
- algunas anotaciones `@PreAuthorize(...)` en controllers específicos

La matriz de este documento debe mantenerse sincronizada con esas reglas.

## Endpoints públicos

Los siguientes endpoints fueron verificados como públicos en la auditoría del Bloque 14A:

- `OPTIONS /**`
- `GET /error`
- `POST /api/auth/login`
- `GET /healthz`
- `GET /actuator/health`
- `GET /actuator/health/**`
- `GET /actuator/info`
- `POST /api/device/locations`
- `GET /v3/api-docs/**` cuando `springdoc` está habilitado
- `GET /swagger-ui/**` cuando `springdoc` está habilitado
- `GET /swagger-ui.html` cuando `springdoc` está habilitado

## Matriz real actual

| Módulo | Operación | ADMINISTRADOR | SUPERVISOR | TECNICO | OPERADOR | Público / HMAC | Fuente técnica |
|---|---|---|---|---|---|---|---|
| Autenticación | `POST /api/auth/login` | No aplica | No aplica | No aplica | No aplica | Público | `permitAll` en `SecurityConfig` |
| Autenticación | `GET /api/auth/me` | Sí | Sí | Sí | Sí | No | `authenticated()` en `SecurityConfig` |
| Usuarios | `/api/users/**` | Sí | No | No | No | No | `hasRole("ADMINISTRADOR")` |
| Auditoría | `/api/audit-logs/**` | Sí | No | No | No | No | `hasRole("ADMINISTRADOR")` |
| Vacas | `GET /api/cows/**` | Sí | Sí | No | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR")` |
| Vacas | `POST /api/cows/**` | Sí | Sí | No | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR")` |
| Vacas | `PUT /api/cows/**` | Sí | Sí | No | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR")` |
| Collares | `GET /api/collars/**` | Sí | Sí | Sí | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")` |
| Collares | `POST /api/collars/**` | Sí | Sí | Sí | No | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "TECNICO")` |
| Collares | `PUT /api/collars/**` | Sí | Sí | Sí | No | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "TECNICO")` |
| Collares | `PATCH /api/collars/**` | Sí | Sí | Sí | No | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "TECNICO")` |
| Collares | `PATCH /api/collars/{id}/rotate-secret` | Sí | No | No | No | No | `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| Geocercas | `/api/geofences/**` | Sí | Sí | No | No | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR")` |
| Ubicaciones | `GET /api/locations/**` | Sí | Sí | Sí | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")` |
| Ubicaciones | `POST /api/locations/**` | Sí | Sí | Sí | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")` |
| Alertas | `GET /api/alerts/**` | Sí | Sí | Sí | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")` |
| Alertas | `PUT /api/alerts/**` | Sí | No | No | No | No | `hasRole("ADMINISTRADOR")` + `@PreAuthorize` |
| Alertas | `PATCH /api/alerts/**` | Sí | No | No | No | No | `hasRole("ADMINISTRADOR")` + `@PreAuthorize` |
| Dashboard | `GET /api/dashboard/**` | Sí | Sí | Sí | Sí | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR", "OPERADOR", "TECNICO")` |
| Reportes | `GET /api/reports/**` | Sí | Sí | No | No | No | `hasAnyRole("ADMINISTRADOR", "SUPERVISOR")` |
| Device ingestion | `POST /api/device/locations` | No aplica | No aplica | No aplica | No aplica | HMAC | `permitAll` + autenticación HMAC del flujo device |
| Healthz | `GET /healthz` | No aplica | No aplica | No aplica | No aplica | Público | `permitAll` en `SecurityConfig` |
| Actuator health/info | `GET /actuator/health`, `/actuator/health/**`, `/actuator/info` | No aplica | No aplica | No aplica | No aplica | Público | `permitAll` en `SecurityConfig` |
| Actuator metrics | `/actuator/metrics`, `/actuator/prometheus` | Sí | No | No | No | No | `hasRole("ADMINISTRADOR")` |
| Swagger / OpenAPI | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | No aplica | No aplica | No aplica | No aplica | Público condicional | `permitAll` solo si `springdoc` está habilitado |

## Matriz recomendada

| Módulo | Operación | ADMINISTRADOR | SUPERVISOR | TECNICO | OPERADOR | Público / HMAC |
|---|---|---|---|---|---|---|
| Autenticación | login | No aplica | No aplica | No aplica | No aplica | Público |
| Autenticación | usuario actual | Sí | Sí | Sí | Sí | No |
| Usuarios | administración de usuarios | Sí | No | No | No | No |
| Auditoría | consulta de logs | Sí | No | No | No | No |
| Vacas | lectura | Sí | Sí | No | Sí | No |
| Vacas | creación / actualización | Sí | Sí | No | Sí | No |
| Collares | lectura | Sí | Sí | Sí | Sí | No |
| Collares | creación / actualización / enable / disable / asignación | Sí | Sí | Sí | No | No |
| Collares | rotación de secreto | Sí | No | No | No | No |
| Geocercas | lectura / gestión | Sí | Sí | No | No | No |
| Ubicaciones | lectura histórica | Sí | Sí | Sí | Sí | No |
| Ubicaciones | registro manual / API interna | Sí | Sí | Sí | Sí | No |
| Alertas | lectura | Sí | Sí | Sí | Sí | No |
| Alertas | resolver / descartar / actualizar | Sí | No | No | No | No |
| Dashboard | consulta operativa | Sí | Sí | Sí | Sí | No |
| Reportes | consulta / exportación | Sí | Sí | No | No | No |
| Device ingestion | telemetría desde collar | No aplica | No aplica | No aplica | No aplica | HMAC |
| Healthz | health check ligero | No aplica | No aplica | No aplica | No aplica | Público |
| Actuator metrics | métricas operativas | Sí | No | No | No | No |

## Decisiones explícitas del MVP

- `OPERADOR` puede crear y actualizar vacas en el MVP actual.
- `TECNICO` puede consultar alertas y dashboard operativo.
- `POST /api/device/locations` es público para Spring Security, pero no es anónimo funcionalmente porque exige HMAC y controles adicionales del flujo device.
- `GET /healthz` es público por diseño.
- Swagger/OpenAPI puede estar público solo cuando está habilitado en entornos locales o de desarrollo.

## Riesgos y controles

### Dependencia de `SecurityConfig`

La mayor parte de la autorización actual está centralizada en `SecurityConfig` mediante `requestMatchers(...)`.

Control recomendado:

- revisar `SecurityConfig` en cada cambio de endpoint o verbo HTTP;
- mantener esta matriz sincronizada con la configuración real.

### Riesgo de nuevos endpoints o verbos

Si se agregan rutas nuevas o verbos nuevos y no se actualiza `SecurityConfig`, pueden caer en:

- `.anyRequest().authenticated()`

Eso evita acceso anónimo, pero puede dejar una operación sin el nivel de rol esperado.

Control recomendado:

- agregar pruebas de regresión de autorización por módulo;
- revisar explícitamente el RBAC en cada PR que agregue rutas o verbos nuevos.

### Pruebas de autorización por módulo

La cobertura actual valida partes importantes, pero no todos los módulos ni todas las combinaciones de rol.

Control recomendado:

- completar integration tests de autorización por controller;
- validar tanto `401 Unauthorized` como `403 Forbidden` según corresponda.

### Sincronización entre documentación y reglas reales

OpenAPI y documentación ayudan a exponer la política, pero la fuente ejecutable sigue siendo `SecurityConfig` y algunas anotaciones `@PreAuthorize`.

Control recomendado:

- actualizar la matriz oficial cada vez que cambie el RBAC;
- usar la matriz como checklist de revisión.

### Control HMAC device fuera de JWT/RBAC

La ingestión device usa un modelo de seguridad distinto al RBAC de usuarios.

Controles actuales:

- HMAC por headers;
- timestamp;
- nonce anti-replay;
- validación de firma;
- control de abuso.

Riesgo:

- su autorización no se expresa en la matriz de roles JWT.

Control recomendado:

- tratar el canal device como una política separada y mantener sus tests y documentación dedicados.

## Tests de autorización existentes

Verificados en el Bloque 14A:

- `SecurityAuthorizationIntegrationTest`
- `CollarControllerIntegrationTest`
- `ReportControllerIntegrationTest`
- `AlertReportCsvExportIntegrationTest`
- `OfflineCollarReportIntegrationTest`
- `CowIncidentReportIntegrationTest`
- `ActuatorMetricsIntegrationTest`
- `DeviceControllerIntegrationTest`
- `DeviceRequestAuthenticationServiceTest`

## Tests recomendados

- `UserControllerIntegrationTest` para roles no admin
- `AuditLogControllerIntegrationTest`
- `GeofenceControllerIntegrationTest`
- `LocationControllerIntegrationTest`
- `DashboardAuthorizationIntegrationTest`
- `CowControllerAuthorizationIntegrationTest`
- `SecurityRegressionIntegrationTest`

## Criterio operativo

Mientras no se cambie la seguridad real:

- esta matriz debe considerarse la referencia oficial del MVP;
- cualquier desalineación entre documentación y `SecurityConfig` debe tratarse como deuda técnica de seguridad;
- cualquier cambio futuro de RBAC debe actualizar primero la documentación y luego la validación por tests.
