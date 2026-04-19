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
- canal inicial por logs
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

## Tecnologías utilizadas

- **Java 17**
- **Spring Boot**
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
- **observability**: métricas, correlation id, health
- **notification**: servicios de notificación desacoplados

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

APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
APP_DEVICE_MONITOR_OFFLINE_THRESHOLD_MINUTES=15
APP_DEVICE_MONITOR_OFFLINE_CHECK_MS=60000

APP_BOOTSTRAP_ADMIN_NAME=
APP_BOOTSTRAP_ADMIN_EMAIL=
APP_BOOTSTRAP_ADMIN_PASSWORD=

MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
```

### Ejecutar con Docker

Construir imagen:

```bash
docker build -t ganaderia4backend .
```

### Ejecutar el contenedor
```bash
docker run -p 10000:10000 ^
  -e SPRING_PROFILES_ACTIVE=dev ^
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ganaderia4 ^
  -e DB_USERNAME=postgres ^
  -e DB_PASSWORD=TU_DB_PASSWORD ^
  -e JWT_SECRET=TU_JWT_SECRET ^
  -e APP_BOOTSTRAP_ADMIN_NAME=GanaderoPro ^
  -e APP_BOOTSTRAP_ADMIN_EMAIL=admin@ganaderia.com ^
  -e APP_BOOTSTRAP_ADMIN_PASSWORD=TU_BOOTSTRAP_ADMIN_PASSWORD ^
  ganaderia4backend
```
