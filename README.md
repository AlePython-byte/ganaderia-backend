# Ganadería 4.0 Backend

Backend del sistema **Ganadería 4.0**, orientado al monitoreo ganadero mediante collares con telemetría GPS, geocercas, alertas operativas, autenticación JWT y endpoints de integración para dispositivos.

## Estado actual del proyecto

El proyecto se encuentra en un estado funcional y validado para presentación técnica y académica.

- Compila correctamente con Maven Wrapper.
- La validación completa se ejecuta con `./mvnw clean verify`.
- La última evidencia local disponible en `target/surefire-reports` y `target/failsafe-reports` registra `238` pruebas ejecutadas.
- El backend expone módulos operativos para vacas, collares, geocercas, ubicaciones, alertas, reportes, dashboard y auditoría.
- La seguridad combina autenticación JWT para usuarios del backend y autenticación HMAC para dispositivos en `/api/device/locations`.
- El proyecto incluye migraciones versionadas con Flyway, observabilidad con Actuator, documentación Swagger/OpenAPI, contenedorización con Docker y automatización con GitHub Actions.

## Descripción

Ganadería 4.0 Backend implementa una API REST en **Spring Boot** para soportar la operación de monitoreo de ganado. El sistema administra entidades del dominio, procesa ubicaciones reportadas por collares, evalúa geocercas, genera alertas operativas y ofrece capacidades de consulta, trazabilidad y despliegue automatizado.

El backend está orientado a resolver flujos como:

- seguimiento de vacas y collares
- registro y consulta de ubicaciones
- detección de salida de geocerca
- monitoreo de collares sin señal o sin telemetría reciente
- consulta de alertas, dashboard y reportes operativos
- trazabilidad básica de acciones mediante auditoría

## Stack tecnológico

- Java 17
- Spring Boot 4.0.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator
- Micrometer + Prometheus
- JUnit 5
- MockMvc
- Testcontainers
- Docker
- GitHub Actions

## Funcionalidades implementadas

### Seguridad y control de acceso

- autenticación con JWT para usuarios
- autorización por roles (`ADMINISTRADOR`, `SUPERVISOR`, `OPERADOR`, `TECNICO`)
- endpoint de login público en `/api/auth/login`
- protección de endpoints según rol y método HTTP
- respuestas de error sanitizadas para flujos de autenticación y autorización
- soporte de CORS configurable por variables de entorno

### Ingestión segura desde dispositivos

- endpoint dedicado en `POST /api/device/locations`
- autenticación HMAC por headers `X-Device-Token`, `X-Device-Timestamp`, `X-Device-Nonce` y `X-Device-Signature`
- protección anti-replay mediante almacenamiento y validación de `nonce`
- ventana temporal de autenticación configurable
- controles de abuso y rate limiting para el canal de dispositivos

### Gestión del dominio

- gestión de vacas
- gestión de collares
- gestión de geocercas
- gestión de ubicaciones
- gestión de alertas
- consultas de usuarios y eventos de auditoría

### Lógica operativa

- registro de ubicaciones desde API interna y desde dispositivos
- evaluación de geocercas sobre eventos de ubicación
- generación automática de alertas operativas
- monitoreo de collares offline según umbral configurable
- soporte de idempotencia y control de duplicados en ubicaciones

### Dashboard, reportes y trazabilidad

- dashboard operativo en `/api/dashboard/**`
- reportes de alertas, collares offline y vacas con mayor recurrencia de incidentes
- exportación CSV del reporte de alertas
- consulta de auditoría en `/api/audit-logs`

### Observabilidad y documentación

- Actuator habilitado
- métricas técnicas y de dominio
- endpoint Prometheus habilitable por configuración
- `X-Request-Id` por request mediante filtro de correlación
- Swagger UI y OpenAPI habilitados en `local` y `dev`
- `health` e `info` disponibles para chequeos operativos; `metrics` y `prometheus` requieren rol `ADMINISTRADOR`

### Contenedorización y automatización

- `Dockerfile` multi-stage para construir y ejecutar la aplicación
- workflow de CI principal con build, pruebas, cobertura y SpotBugs
- workflow de análisis de dependencias con OWASP Dependency-Check
- workflow de escaneo de contenedor con Trivy
- despliegue automatizado por hook hacia Render desde el workflow principal

## Arquitectura general

El backend sigue una arquitectura en capas y mantiene separación explícita de responsabilidades:

- `controller`: endpoints REST
- `service`: lógica de negocio
- `repository`: acceso a datos
- `model`: entidades y enums del dominio
- `dto`: contratos de entrada y salida
- `config`: seguridad, OpenAPI, CORS, scheduling y configuración transversal
- `security`: JWT, HMAC, anti-replay y protección de abuso
- `observability`: métricas, correlación y health indicators
- `notification`: dispatcher y canales base de notificación
- `pattern`: implementaciones de patrones aplicados al flujo de monitoreo

## Módulos y endpoints relevantes

Los módulos principales expuestos por el backend incluyen:

- `/api/auth`
- `/api/cows`
- `/api/collars`
- `/api/geofences`
- `/api/locations`
- `/api/alerts`
- `/api/dashboard`
- `/api/reports`
- `/api/audit-logs`
- `/api/device/locations`
- `/actuator`

## Swagger / OpenAPI

En perfiles `local` y `dev`:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

En `prod`, Swagger/OpenAPI se encuentra deshabilitado por configuración actual.

Uso esperado:

- los endpoints de usuario usan `Authorization: Bearer <token>`
- `POST /api/device/locations` no usa JWT y requiere autenticación HMAC
- no deben cargarse secretos reales en ejemplos o pruebas manuales desde Swagger UI

## Variables de entorno

Las siguientes variables están respaldadas por los archivos de configuración del proyecto. Los valores deben definirse según el entorno y **no** deben versionarse con secretos reales.

```env
SPRING_PROFILES_ACTIVE=local
PORT=8080

DB_URL=jdbc:postgresql://localhost:5432/ganaderia4
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=replace-with-a-secure-jwt-secret
JWT_EXPIRATION_MS=86400000

DEVICE_SECRET_MASTER_KEY=replace-with-a-secure-device-master-key
DEVICE_AUTH_WINDOW_SECONDS=300
DEVICE_HMAC_PEPPER=

APP_CORS_ALLOWED_ORIGINS=http://localhost:5173

APP_BOOTSTRAP_ADMIN_NAME=
APP_BOOTSTRAP_ADMIN_EMAIL=
APP_BOOTSTRAP_ADMIN_PASSWORD=

APP_DEVICE_MONITOR_OFFLINE_THRESHOLD_MINUTES=15
APP_DEVICE_MONITOR_OFFLINE_CHECK_MS=60000

APP_PAGINATION_DEFAULT_SIZE=20
APP_PAGINATION_MAX_SIZE=100

REPORT_ALERTS_CSV_MAX_ROWS=5000

APP_NOTIFICATIONS_WEBHOOK_ENABLED=false
APP_NOTIFICATIONS_WEBHOOK_URL=
APP_NOTIFICATIONS_WEBHOOK_CONNECT_TIMEOUT=5s
APP_NOTIFICATIONS_WEBHOOK_READ_TIMEOUT=5s
APP_NOTIFICATIONS_WEBHOOK_SECRET=
APP_NOTIFICATIONS_WEBHOOK_PROCESSOR_ENABLED=true
APP_NOTIFICATIONS_WEBHOOK_PROCESSOR_FIXED_DELAY=15s
APP_NOTIFICATIONS_WEBHOOK_PROCESSOR_BATCH_SIZE=20
APP_NOTIFICATIONS_WEBHOOK_MAX_ATTEMPTS=3
APP_NOTIFICATIONS_WEBHOOK_RETRY_BACKOFF=30s

APP_ABUSE_PROTECTION_ENABLED=true
APP_ABUSE_PROTECTION_TRUST_FORWARDED_HEADERS=false
APP_ABUSE_PROTECTION_LOGIN_ENABLED=true
APP_ABUSE_PROTECTION_LOGIN_WINDOW=15m
APP_ABUSE_PROTECTION_LOGIN_MAX_ATTEMPTS=5
APP_ABUSE_PROTECTION_LOGIN_BLOCK_DURATION=15m
APP_ABUSE_PROTECTION_DEVICE_ENABLED=true
APP_ABUSE_PROTECTION_DEVICE_WINDOW=1m
APP_ABUSE_PROTECTION_DEVICE_MAX_ATTEMPTS=300
APP_ABUSE_PROTECTION_DEVICE_BLOCK_DURATION=5m

MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

### Perfiles disponibles

- `local`: perfil por defecto, pensado para desarrollo local con Swagger habilitado y valores por defecto para base de datos local.
- `dev`: perfil para entorno compartido de desarrollo, requiere credenciales explícitas de base de datos y secretos.
- `prod`: perfil de despliegue, usa `PORT=10000` por defecto, deshabilita Swagger y expone por defecto `health,info` en Actuator.
- `test`: perfil usado durante la ejecución de pruebas.

## Ejecución local

Requisitos previos:

- Java 17
- Maven Wrapper incluido en el proyecto
- PostgreSQL
- Docker disponible para pruebas de integración con Testcontainers

Con base de datos local disponible:

```bash
./mvnw spring-boot:run
```

Si se necesita crear un administrador inicial sobre una base vacía, pueden definirse `APP_BOOTSTRAP_ADMIN_NAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` antes del arranque.

Para levantar con otro perfil:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Validación técnica

La validación principal del proyecto se ejecuta con el siguiente comando:

```bash
./mvnw clean verify
```

Este flujo ejecuta y verifica:

- pruebas unitarias
- pruebas de integración
- reportes JaCoCo de unit, integration y merged coverage
- umbrales mínimos de cobertura configurados en Maven
- análisis estático con SpotBugs

Reglas de calidad verificadas en `pom.xml`:

- Java `17+`
- Maven `3.9.0+`
- cobertura mínima combinada JaCoCo en `LINE=0.50`
- cobertura mínima combinada JaCoCo en `BRANCH=0.35`

Adicionalmente, el proyecto cuenta con pruebas que cubren áreas como:

- autenticación y autorización
- endpoint de dispositivos con HMAC
- protección anti-replay
- dashboard y reportes
- observabilidad y métricas
- pipeline base de notificaciones
- persistencia y componentes de seguridad

## CI/CD

El repositorio incluye los siguientes workflows en `.github/workflows`:

- `backend-ci.yml`: ejecuta `./mvnw clean verify`, publica artifacts de JaCoCo y SpotBugs, construye la imagen Docker y dispara despliegue por hook a Render cuando la rama es `develop`.
- `dependency-check.yml`: ejecuta OWASP Dependency-Check con el perfil `security-scan`.
- `container-scan.yml`: construye una imagen temporal y ejecuta escaneo de vulnerabilidades y configuración con Trivy.

No se agrega badge en este README porque la URL exacta depende del repositorio remoto y aquí no está fijada de forma verificable.

## Despliegue

### Docker

Construcción de imagen:

```bash
docker build -t ganaderia4backend .
```

Ejecución del contenedor:

```bash
docker run -p 10000:10000 ^
  -e SPRING_PROFILES_ACTIVE=prod ^
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ganaderia4 ^
  -e DB_USERNAME=postgres ^
  -e DB_PASSWORD=YOUR_DB_PASSWORD ^
  -e JWT_SECRET=YOUR_JWT_SECRET ^
  -e DEVICE_SECRET_MASTER_KEY=YOUR_DEVICE_SECRET_MASTER_KEY ^
  -e APP_BOOTSTRAP_ADMIN_NAME=GanaderiaAdmin ^
  -e APP_BOOTSTRAP_ADMIN_EMAIL=admin@ganaderia.com ^
  -e APP_BOOTSTRAP_ADMIN_PASSWORD=YOUR_BOOTSTRAP_ADMIN_PASSWORD ^
  ganaderia4backend
```

El `Dockerfile` actual:

- usa build multi-stage con Maven y Eclipse Temurin 17
- expone el puerto `10000`
- define `HEALTHCHECK` contra `/actuator/health`
- ejecuta la aplicación con usuario no root

### Producción

Según el workflow `backend-ci.yml`, el flujo de despliegue actual está integrado con Render mediante `RENDER_DEPLOY_HOOK_URL`. El despliegue se dispara solo después de pasar CI y construir la imagen Docker en la rama `develop`.

## Migraciones y base de datos

Las migraciones se gestionan con Flyway y actualmente existen scripts versionados desde `V1__init_schema.sql` hasta `V11__add_read_path_indexes.sql`, incluyendo cambios de auditoría, idempotencia de ubicaciones, protección anti-replay y soporte de notificaciones webhook.

## Documentación complementaria

Se mantienen documentos técnicos adicionales en [docs](docs):

- [collar-lifecycle.md](docs/collar-lifecycle.md)
- [operational-runbook.md](docs/operational-runbook.md)
- [permissions-matrix.md](docs/permissions-matrix.md)
- [phase-1-hardening-summary.md](docs/phase-1-hardening-summary.md)
- [time-policy.md](docs/time-policy.md)

## Limitaciones conocidas / próximos pasos

Las siguientes observaciones están formuladas como límites o trabajo esperable, no como funcionalidades faltantes inventadas:

- el README no documenta ejemplos completos de firma HMAC extremo a extremo; eso podría agregarse como guía operativa separada
- no se observa en la raíz del proyecto un `docker-compose.yml`; el arranque de dependencias sigue siendo manual o externo al repositorio
- Swagger/OpenAPI está deshabilitado en `prod` por configuración, por lo que la exploración interactiva de endpoints queda limitada a `local` y `dev`
- el flujo de despliegue automatizado visible depende de un hook externo de Render; la infraestructura completa de producción no está descrita en este repositorio
- las notificaciones cuentan con base de implementación y canal webhook configurable, pero este README no asume integraciones externas adicionales que no estén versionadas aquí

## Resumen de cambios realizados

- se reorganizó el README con una estructura más profesional y fácil de defender
- se agregaron las secciones `Estado actual del proyecto`, `Funcionalidades implementadas`, `Validación técnica`, `Variables de entorno`, `Despliegue` y `Limitaciones conocidas / próximos pasos`
- se ajustaron las afirmaciones para alinearlas con `pom.xml`, `Dockerfile`, `application*.properties`, controladores y workflows reales
- se conservaron referencias útiles a la documentación técnica existente en `docs/`
