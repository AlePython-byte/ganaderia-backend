# Runbook operativo — Ganadería 4.0

## Propósito

Este runbook documenta cómo validar, operar y diagnosticar el backend Ganadería 4.0 en ambientes `local`, `test` y `producción`.

Su objetivo es servir como guía base para:

- verificar que el backend está sano;
- levantar el sistema localmente de forma consistente;
- validar seguridad, ingestión device y observabilidad;
- diagnosticar fallos comunes;
- ejecutar checks mínimos antes de desplegar o entregar.

No reemplaza la revisión del código ni de la configuración real del entorno. La referencia ejecutable sigue siendo la configuración Spring Boot, `SecurityConfig`, Flyway y las variables de entorno activas en cada ambiente.

## Ambientes

### Local

Ambiente de desarrollo en la máquina del equipo.

Uso típico:

- levantar PostgreSQL con Docker;
- ejecutar Spring Boot con perfil `local`;
- validar Swagger/OpenAPI;
- probar login, JWT, `/healthz`, `/actuator/health` y flujos básicos.

Archivo de referencia principal:

- `src/main/resources/application-local.properties`

### Test

Ambiente de pruebas automatizadas.

Uso típico:

- `./mvnw test`
- `./mvnw clean verify`

Características:

- unit tests y integration tests;
- integration tests con Testcontainers y PostgreSQL efímero;
- validación de Flyway, seguridad, controladores, reportes y métricas;
- quality gate de JaCoCo y análisis con SpotBugs durante `verify`.

### Producción / Render

Ambiente desplegado en Render.

Uso típico:

- ejecutar el backend con perfil `prod`;
- usar PostgreSQL gestionado o configurado externamente;
- exponer `/healthz`;
- mantener Swagger/OpenAPI deshabilitado por seguridad;
- restringir métricas y endpoints administrativos según la matriz de permisos actual.

Archivo de referencia principal:

- `src/main/resources/application-prod.properties`

## Variables de entorno principales

Las siguientes variables son las más relevantes para operación básica. No deben documentarse ni compartirse con valores reales.

| Variable | Propósito |
|---|---|
| `PORT` | Puerto HTTP del backend. En local suele ser `8080`; en Render puede ser inyectado por la plataforma. |
| `DB_URL` | URL JDBC de PostgreSQL. Ejemplo local: `jdbc:postgresql://localhost:5432/ganaderia4`. |
| `DB_USERNAME` | Usuario de conexión a PostgreSQL. |
| `DB_PASSWORD` | Contraseña de conexión a PostgreSQL. |
| `JWT_SECRET` | Secreto usado para firmar y validar JWT. Debe ser fuerte y privado. |
| `DEVICE_SECRET_MASTER_KEY` | Clave maestra usada en el flujo HMAC de dispositivos. Es crítica y no debe exponerse. |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes frontend permitidos por CORS. |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Lista de endpoints Actuator expuestos por HTTP. |
| `APP_DEVICE_MONITOR_OFFLINE_THRESHOLD_MINUTES` | Umbral en minutos para considerar un collar como offline. |
| `APP_DEVICE_MONITOR_OFFLINE_CHECK_MS` | Frecuencia del monitoreo offline en milisegundos. |
| `APP_BOOTSTRAP_ADMIN_NAME` | Nombre del usuario administrador bootstrap, si se inicializa desde entorno. |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Correo del usuario administrador bootstrap. |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña inicial del usuario administrador bootstrap. |

Variables adicionales importantes, aunque no sean el foco principal de este bloque:

- `JWT_EXPIRATION_MS`
- `DEVICE_AUTH_WINDOW_SECONDS`
- `APP_ABUSE_PROTECTION_*`
- `APP_NOTIFICATIONS_WEBHOOK_*`

## Validación local rápida

### 1. Levantar PostgreSQL local con Docker

Puerto estándar `5432`:

```powershell
docker run --name ganaderia4-postgres `
  -e POSTGRES_DB=ganaderia4 `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:16-alpine
```

Si `5432` está ocupado, usar `5433`:

```powershell
docker run --name ganaderia4-postgres `
  -e POSTGRES_DB=ganaderia4 `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5433:5432 `
  -d postgres:16-alpine
```

### 2. Definir variables de entorno en PowerShell

Con PostgreSQL en `5432`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ganaderia4"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
```

Con PostgreSQL en `5433`:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5433/ganaderia4"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
```

Opcionalmente, definir secretos locales explícitos:

```powershell
$env:JWT_SECRET="local-dev-jwt-secret-change-me-32-bytes-minimum"
$env:DEVICE_SECRET_MASTER_KEY="local-dev-device-master-key-change-me-32-bytes-minimum"
```

### 3. Levantar la aplicación

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 4. Validar Swagger/OpenAPI

Abrir en navegador:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

Si `PORT` se configuró distinto, reemplazar `8080` por el puerto real.

## Validación de salud

### `GET /healthz`

Health check ligero, público y orientado a operación básica.

Uso esperado:

- validación rápida desde navegador, `curl`, balanceador o plataforma;
- verificación simple de que la aplicación está respondiendo.

Respuesta sana esperada:

```json
{"status":"ok"}
```

### `GET /actuator/health`

Health endpoint de Actuator.

Uso esperado:

- validación técnica más alineada con Spring Boot Actuator;
- integración con plataformas, monitoreo o revisiones operativas.

### `GET /actuator/info`

Expone metadatos básicos de la aplicación, por ejemplo nombre, descripción y versión.

### Diferencia entre `healthz` y `actuator/health`

- `/healthz`: endpoint público, ligero y estable para checks rápidos.
- `/actuator/health`: endpoint Actuator del framework, orientado a observabilidad y runtime Spring.

En un estado sano, ambos deben responder sin error HTTP.

## Validación de seguridad

### Login

Endpoint público:

- `POST /api/auth/login`

Uso esperado:

- enviar credenciales válidas;
- recibir JWT Bearer;
- usar ese token en endpoints protegidos.

### JWT Bearer

Para endpoints protegidos:

```http
Authorization: Bearer <token>
```

### Diferencia entre `401` y `403`

- `401 Unauthorized`: el request no está autenticado correctamente.
  - token ausente;
  - token inválido;
  - token expirado.

- `403 Forbidden`: el request sí está autenticado, pero el rol no tiene permiso.

### Roles actuales

- `ADMINISTRADOR`
- `SUPERVISOR`
- `TECNICO`
- `OPERADOR`

La matriz oficial vigente está en:

- [docs/permissions-matrix.md](permissions-matrix.md)

## Validación de device ingestion

Endpoint:

- `POST /api/device/locations`

Este endpoint es público para Spring Security, pero no es anónimo funcionalmente. El flujo real exige autenticación HMAC y validaciones adicionales.

### Headers requeridos

- `X-Device-Token`
- `X-Device-Timestamp`
- `X-Device-Nonce`
- `X-Device-Signature`

### Comportamiento esperado

- el request debe incluir headers consistentes;
- la firma HMAC debe corresponder exactamente al payload enviado;
- el timestamp debe estar dentro de la ventana permitida;
- el nonce no debe estar repetido;
- el payload debe cumplir validaciones JSON y de negocio.

### Errores esperados

Casos frecuentes:

- token o header faltante;
- firma inválida;
- nonce repetido (`replayed nonce`);
- timestamp expirado;
- timestamp demasiado en el futuro;
- payload inválido o inconsistente con reglas de dominio.

No usar secretos reales en pruebas manuales ni en Swagger.

## Logs y correlación

### `X-Request-Id`

El backend usa correlación de requests mediante `X-Request-Id`.

Uso recomendado:

- enviar `X-Request-Id` desde cliente o frontend cuando sea posible;
- si no se envía, el backend puede generar uno;
- usar ese identificador para rastrear una petición de extremo a extremo.

### Request correlation logs

Los logs incluyen un patrón con `requestId`, lo que permite buscar una misma petición en:

- autenticación;
- controlador;
- validaciones;
- errores;
- métricas y filtros HTTP.

### Cómo rastrear una petición

1. capturar el `X-Request-Id` de la respuesta o del cliente;
2. buscar ese valor en logs;
3. revisar la secuencia completa:
   - entrada HTTP;
   - warnings de seguridad;
   - validaciones;
   - excepción final si existe.

### Tipos de logs

- logs operativos normales:
  - requests HTTP;
  - completitud de jobs;
  - eventos esperados.

- warnings de seguridad:
  - JWT inválido;
  - acceso prohibido;
  - HMAC inválido;
  - replay de nonce.

- errores reales:
  - excepciones no esperadas;
  - fallos de persistencia;
  - problemas de infraestructura;
  - fallos de integración.

## Métricas y Actuator

Endpoints relevantes:

- `/actuator/metrics`
- `/actuator/prometheus`

Según la matriz actual, estos endpoints están restringidos a `ADMINISTRADOR`.

Qué revisar si no responden:

1. que el usuario tenga el rol correcto;
2. que el JWT sea válido;
3. que `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` incluya `metrics` y `prometheus`;
4. que el perfil activo no los haya restringido;
5. que `SecurityConfig` no esté bloqueando el acceso por rol.

En producción, es normal que la exposición sea más limitada que en `local` o `dev`.

## Pruebas y calidad

### Comandos principales

Unit tests:

```powershell
./mvnw test
```

Suite completa:

```powershell
./mvnw clean verify
```

### Testcontainers

Los integration tests usan Testcontainers con PostgreSQL efímero.

Requisito:

- Docker debe estar activo.

Si aparece:

`Could not find a valid Docker environment`

acciones recomendadas:

1. iniciar Docker Desktop;
2. verificar que el daemon Docker responde;
3. reintentar `./mvnw clean verify`;
4. evitar ejecutar dos procesos Maven simultáneos sobre el mismo `target`.

### JaCoCo

El proyecto tiene quality gate de cobertura. `verify` valida que el umbral actual se cumpla.

### SpotBugs

`verify` ejecuta análisis estático con SpotBugs. El build falla si aparecen hallazgos bloqueantes según la configuración actual.

### OWASP Dependency-Check

Está documentado y configurado en workflow separado para detectar dependencias vulnerables.

### Trivy

Está documentado y configurado en workflow separado para análisis de contenedor.

## Diagnóstico de fallos comunes

| Síntoma | Causa probable | Acción recomendada |
|---|---|---|
| `401 Unauthorized` | JWT ausente, inválido o expirado | Validar login, header `Authorization`, expiración del token y formato `Bearer`. |
| `403 Forbidden` | El usuario está autenticado pero su rol no tiene permiso | Revisar la matriz en `docs/permissions-matrix.md` y confirmar el rol real del usuario. |
| Error de conexión a PostgreSQL | Base caída, `DB_URL` incorrecta, credenciales erróneas o puerto ocupado | Verificar contenedor/instancia, credenciales, puerto y conectividad. |
| `Flyway migration failed` | Script inválido, esquema inconsistente o base en estado inesperado | Revisar logs de Flyway, versión actual de la base y scripts en `db/migration`. |
| `Could not find a valid Docker environment` | Docker/Testcontainers no disponible | Levantar Docker Desktop y reejecutar `./mvnw clean verify`. |
| Swagger no carga | Perfil incorrecto, `springdoc` deshabilitado o puerto distinto | Confirmar perfil `local` o `dev`, revisar `/v3/api-docs` y el puerto activo. |
| Render muestra servicio caído | Variables faltantes, fallo de arranque, error DB o health check fallando | Revisar logs del deploy, variables de entorno, conexión a DB y `/healthz`. |
| `POST /api/device/locations` devuelve `401` | HMAC inválido, nonce repetido, timestamp fuera de ventana o token device desconocido | Revisar headers HMAC, ventana temporal, nonce y firma exacta del payload. |
| `POST /api/device/locations` devuelve `400` | Payload inválido o regla funcional rechazada | Revisar JSON, formato del timestamp, collar habilitado/activo y validaciones de dominio. |
| Export CSV falla o devuelve vacío | Filtros sin datos, error de consulta o acceso no permitido | Revisar filtros, permisos del usuario y datos disponibles en la base. |

## Checklist antes de desplegar o entregar

- `git status` limpio o cambios intencionales controlados;
- `./mvnw test`;
- `./mvnw clean verify`;
- Docker activo si se van a correr integration tests;
- variables de entorno revisadas;
- `/healthz` validado;
- `/actuator/health` validado;
- Swagger revisado en `local` o `dev` si aplica;
- ningún secreto real expuesto en documentación, logs o Swagger;
- `README.md` y `docs/` actualizados.

## Referencias internas

- [Matriz de permisos](permissions-matrix.md)
- [Política temporal UTC](time-policy.md)
- [Lifecycle de collares](collar-lifecycle.md)
- [Resumen Fase 1 Hardening](phase-1-hardening-summary.md)
- [README.md](../README.md)
