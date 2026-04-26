# Ganadería 4.0 Backend

Backend del sistema **Ganadería 4.0**, una plataforma para monitoreo ganadero con collares GPS, geocercas, alertas operativas, autenticación JWT, reportes y observabilidad.

## Descripción

Este proyecto implementa un backend en **Spring Boot** para gestionar el monitoreo de ganado mediante dispositivos de rastreo. El sistema permite administrar vacas, collares, geocercas, ubicaciones y alertas, además de ofrecer endpoints de autenticación, reportes, observabilidad y despliegue automatizado.

El objetivo principal es detectar eventos relevantes del negocio, como:

- salida de una vaca de su geocerca
- collares sin señal o sin reportes recientes
- historial de ubicaciones
- seguimiento operativo de alertas
- reportes de incidencias

---

## Características principales

### Autenticación y seguridad
- autenticación con **JWT**
- control de acceso por roles
- seguridad endurecida
- CORS configurado
- respuestas de error sanitizadas
- persistencia de sesión mediante token

### Gestión de dominio
- gestión de **vacas**
- gestión de **collares**
- gestión de **geocercas**
- gestión de **ubicaciones**
- gestión de **alertas**

### Lógica operativa
- creación automática de alertas por salida de geocerca
- creación automática de alertas por collar offline
- auto-resolución de alertas cuando el estado se recupera
- validación defensiva del endpoint de dispositivos
- control de duplicados en ubicaciones reportadas por collares

### Observabilidad
- `X-Request-Id` por request
- Actuator habilitado
- métricas técnicas y de dominio
- endpoint Prometheus
- health indicator del monitoreo offline

### Notificaciones
- arquitectura desacoplada de notificaciones
- dispatcher central
- canal por logs para local/test
- canal webhook saliente configurable
- disparo para alertas críticas

### Reportes
- reporte de alertas con filtros
- reporte de collares offline
- reporte de vacas con más incidencias
- exportación CSV del reporte de alertas

### Calidad y despliegue
- pruebas unitarias e integración
- **SpringBootTest**
- **MockMvc**
- **Testcontainers con PostgreSQL**
- **Flyway activo en tests**
- GitHub Actions para CI/CD
- despliegue en **Render**
- validación Docker en pipeline

---

## Plataforma técnica

- Java 17
- Spring Boot 4.0.3
- Maven Wrapper
- PostgreSQL
- Flyway
- JWT
- Docker
- GitHub Actions

El proyecto se mantiene en Java 17 porque es compatible con Spring Boot 4 y permite estabilidad en CI/CD y despliegue.

---

## Quality Gates

El comando recomendado para validación local y CI es:

```bash
./mvnw clean verify
```

Ese flujo ejecuta:

- tests unitarios
- tests de integración
- reportes JaCoCo de unit tests, integration tests y cobertura combinada
- verificación de cobertura mínima con JaCoCo
- SpotBugs

Cobertura mínima configurada sobre la cobertura global combinada:

- `LINE`: `0.50`
- `BRANCH`: `0.35`

El workflow de GitHub Actions usa el mismo comando para que el quality gate se aplique también en CI.

---

## Tecnologías utilizadas

- **Java 17**
- **Spring Boot 4.0.3**
- **Spring Security**
- **JWT**
- **Spring Data JPA**
- **PostgreSQL**
- **Flyway**
- **Swagger / OpenAPI**
- **Spring Boot Actuator**
- **Micrometer + Prometheus**
- **JUnit 5**
- **Mockito**
- **MockMvc**
- **Testcontainers**
- **Docker**
- **GitHub Actions**
- **Render**

---

## Arquitectura general

El proyecto sigue una arquitectura en capas:

- **controller**: expone endpoints REST
- **service**: contiene lógica de negocio
- **repository**: acceso a base de datos
- **model**: entidades del dominio
- **dto**: contratos de entrada y salida
- **config**: seguridad, CORS y configuración general
- **observability**: métricas, correlation id y health
- **notification**: servicios de notificación desacoplados
- **pattern**: implementación de patrones de diseño usados por el flujo de monitoreo

---

## Módulos del sistema

### 1. Autenticación
Permite iniciar sesión y proteger el acceso al resto del sistema mediante JWT.

### 2. Vacas
Permite registrar, consultar y actualizar vacas.

### 3. Collares
Permite registrar, actualizar, habilitar, deshabilitar y reasignar collares a vacas.

### 4. Geocercas
Permite administrar áreas geográficas válidas para monitoreo.

### 5. Ubicaciones
Permite registrar y consultar ubicaciones históricas de las vacas reportadas por collares.

### 6. Alertas
Permite consultar y gestionar alertas operativas, tanto manuales como automáticas.

### 7. Reportes
Permite consultar indicadores y exportar reportes operativos.

### 8. Observabilidad
Permite revisar health, métricas y trazabilidad de requests.

Métricas operativas principales:

- `ganaderia.alerts.created` con tag `type`
- `ganaderia.alerts.resolved` con tag `type`
- `ganaderia.alerts.discarded` con tag `type`
- `ganaderia.collars.marked_offline`
- `ganaderia.device.requests.accepted`
- `ganaderia.device.requests.rejected` con tag `reason`
- `ganaderia.notifications.sent` con tags `channel` y `eventType`
- `ganaderia.notifications.failed` con tags `channel` y `eventType`

En `prod`, Actuator expone por defecto `health,info`. Para habilitar scraping operativo de métricas sin tocar código:

```env
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
```

Los endpoints `/actuator/metrics/**` y `/actuator/prometheus` requieren rol `ADMINISTRADOR`.

---

## Roles del sistema

Los roles principales del backend son:

- `ADMINISTRADOR`
- `SUPERVISOR`
- `OPERADOR`
- `TECNICO`

La autorización depende del endpoint y del método HTTP.

---

## Requisitos previos

Antes de ejecutar el proyecto necesitas:

- Java 17
- Maven Wrapper incluido en el proyecto
- PostgreSQL
- Docker
- Git

---

## Variables de entorno

Ejemplo de variables usadas por el backend:

```env
SPRING_PROFILES_ACTIVE=prod
PORT=10000

DB_URL=jdbc:postgresql://localhost:5432/ganaderia4
DB_USERNAME=postgres
DB_PASSWORD=tu_password

JWT_SECRET=tu_clave_jwt_super_segura
JWT_EXPIRATION_MS=86400000

DEVICE_SECRET_MASTER_KEY=tu_clave_maestra_dispositivos

APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
APP_DEVICE_MONITOR_OFFLINE_THRESHOLD_MINUTES=15
APP_DEVICE_MONITOR_OFFLINE_CHECK_MS=60000

APP_PAGINATION_DEFAULT_SIZE=20
APP_PAGINATION_MAX_SIZE=100

APP_NOTIFICATIONS_WEBHOOK_ENABLED=false
APP_NOTIFICATIONS_WEBHOOK_URL=
APP_NOTIFICATIONS_WEBHOOK_CONNECT_TIMEOUT=5s
APP_NOTIFICATIONS_WEBHOOK_READ_TIMEOUT=5s
APP_NOTIFICATIONS_WEBHOOK_SECRET=

APP_BOOTSTRAP_ADMIN_NAME=
APP_BOOTSTRAP_ADMIN_EMAIL=
APP_BOOTSTRAP_ADMIN_PASSWORD=

MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

### Perfiles de Spring

- `local`: perfil por defecto para desarrollo local. Usa PostgreSQL local en `localhost:5432/ganaderia4`, Swagger habilitado y secretos locales no aptos para producción.
- `dev`: perfil para entornos de desarrollo compartidos. Requiere `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `DEVICE_SECRET_MASTER_KEY`.
- `prod`: perfil de producción. Requiere secretos por variables de entorno, mantiene Swagger apagado y expone por defecto solo `health,info` en Actuator.
- `test`: perfil usado por las pruebas con Testcontainers.

En `dev` y `prod`, `JWT_SECRET` y `DEVICE_SECRET_MASTER_KEY` deben tener al menos 32 bytes. Si faltan o son demasiado cortos, la aplicación falla al iniciar.

---

## Ejecución local

Levantar PostgreSQL local con una base `ganaderia4` y credenciales por defecto `postgres/postgres`, o definir `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

```bash
./mvnw spring-boot:run
```

Para crear un administrador inicial en una base vacía, definir `APP_BOOTSTRAP_ADMIN_NAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` antes de iniciar.

El perfil `local` queda activo por defecto. Para usar otro perfil:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

La validación de CI usa Java 17 y ejecuta:

```bash
./mvnw clean verify
```

---

## Docker

Construir imagen:

```bash
docker build -t ganaderia4backend .
```

Ejecutar el contenedor:

```bash
docker run -p 10000:10000 ^
  -e SPRING_PROFILES_ACTIVE=prod ^
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ganaderia4 ^
  -e DB_USERNAME=postgres ^
  -e DB_PASSWORD=TU_DB_PASSWORD ^
  -e JWT_SECRET=TU_JWT_SECRET ^
  -e DEVICE_SECRET_MASTER_KEY=TU_DEVICE_SECRET_MASTER_KEY ^
  -e APP_BOOTSTRAP_ADMIN_NAME=GanaderoPro ^
  -e APP_BOOTSTRAP_ADMIN_EMAIL=admin@ganaderia.com ^
  -e APP_BOOTSTRAP_ADMIN_PASSWORD=TU_BOOTSTRAP_ADMIN_PASSWORD ^
  ganaderia4backend
```
