# GanaderÃ­a 4.0 Backend

Backend del sistema **GanaderÃ­a 4.0**, una plataforma para monitoreo ganadero con collares GPS, geocercas, alertas operativas, autenticaciÃ³n JWT, reportes y observabilidad.

## DescripciÃ³n

Este proyecto implementa un backend en **Spring Boot** para gestionar el monitoreo de ganado mediante dispositivos de rastreo. El sistema permite administrar vacas, collares, geocercas, ubicaciones y alertas, ademÃ¡s de ofrecer endpoints de autenticaciÃ³n, reportes, observabilidad y despliegue automatizado.

El objetivo principal es detectar eventos relevantes del negocio, como:

- salida de una vaca de su geocerca
- collares sin seÃ±al o sin reportes recientes
- historial de ubicaciones
- seguimiento operativo de alertas
- reportes de incidencias

---

## CaracterÃ­sticas principales

### AutenticaciÃ³n y seguridad
- autenticaciÃ³n con **JWT**
- control de acceso por roles
- seguridad endurecida
- CORS configurado
- respuestas de error sanitizadas
- persistencia de sesiÃ³n mediante token

### GestiÃ³n de dominio
- gestiÃ³n de **vacas**
- gestiÃ³n de **collares**
- gestiÃ³n de **geocercas**
- gestiÃ³n de **ubicaciones**
- gestiÃ³n de **alertas**

### LÃ³gica operativa
- creaciÃ³n automÃ¡tica de alertas por salida de geocerca
- creaciÃ³n automÃ¡tica de alertas por collar offline
- auto-resoluciÃ³n de alertas cuando el estado se recupera
- validaciÃ³n defensiva del endpoint de dispositivos
- control de duplicados en ubicaciones reportadas por collares

### Observabilidad
- `X-Request-Id` por request
- Actuator habilitado
- mÃ©tricas tÃ©cnicas y de dominio
- endpoint Prometheus
- health indicator del monitoreo offline

### Notificaciones
- arquitectura desacoplada de notificaciones
- dispatcher central
- canal por logs para local/test
- canal webhook saliente configurable
- disparo para alertas crÃ­ticas

### Reportes
- reporte de alertas con filtros
- reporte de collares offline
- reporte de vacas con mÃ¡s incidencias
- exportaciÃ³n CSV del reporte de alertas

### Calidad y despliegue
- pruebas unitarias e integraciÃ³n
- **SpringBootTest**
- **MockMvc**
- **Testcontainers con PostgreSQL**
- **Flyway activo en tests**
- GitHub Actions para CI/CD
- despliegue en **Render**
- validaciÃ³n Docker en pipeline

---

## Plataforma tÃ©cnica

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

El comando recomendado para validaciÃ³n local y CI es:

```bash
./mvnw clean verify
```

Ese flujo ejecuta:

- tests unitarios
- tests de integraciÃ³n
- reportes JaCoCo de unit tests, integration tests y cobertura combinada
- verificaciÃ³n de cobertura mÃ­nima con JaCoCo
- SpotBugs

Cobertura mÃ­nima configurada sobre la cobertura global combinada:

- `LINE`: `0.50`
- `BRANCH`: `0.35`

El workflow de GitHub Actions usa el mismo comando para que el quality gate se aplique tambiÃ©n en CI.

---

## Mantenimiento de dependencias

Dependabot revisa semanalmente dependencias de Maven, GitHub Actions y Docker.

Dependabot abre pull requests automÃ¡ticos con labels de dependencias y seguridad para mantener el proyecto actualizado sin intervenir manualmente en cada revisiÃ³n.

Cada pull request de actualizaciÃ³n debe pasar `./mvnw clean verify` antes de fusionarse.

Las actualizaciones mayores no deben aceptarse sin revisar changelog, notas de compatibilidad e impacto sobre CI, build y despliegue.

---

## Escaneo de vulnerabilidades

Dependabot revisa actualizaciones disponibles y ayuda a detectar alertas de dependencias sobre Maven, GitHub Actions y Docker.

OWASP Dependency-Check analiza vulnerabilidades conocidas en las dependencias del proyecto y genera reportes en HTML, JSON y JUNIT.

El escaneo corre manualmente o semanalmente en GitHub Actions mediante un workflow separado.

No corre en cada push para evitar lentitud adicional y falsos positivos iniciales sobre el pipeline principal.

Si se detectan vulnerabilidades con `CVSS >= 7.0`, el workflow de escaneo debe fallar.

Los reportes del anÃ¡lisis quedan publicados como artifacts del workflow para revisiÃ³n posterior.

---

## Escaneo de contenedores

Trivy escanea la imagen Docker del backend para revisar vulnerabilidades en la imagen base, paquetes del sistema y componentes presentes en la imagen final.

El workflow corre manualmente o semanalmente en GitHub Actions y construye una imagen temporal solo para anÃ¡lisis.

La imagen no se publica en ningÃºn registry como parte de este proceso.

El workflow falla inicialmente solo por vulnerabilidades `CRITICAL`.

Las vulnerabilidades `HIGH` se reportan para revisiÃ³n, pero todavÃ­a no bloquean el flujo.

Los reportes del escaneo quedan publicados como artifacts del workflow.

---

## Lifecycle de collares

El lifecycle oficial de collares, incluyendo la semÃ¡ntica de `status`, `enabled`, `signalStatus` y `lastSeenAt`, estÃ¡ documentado en [docs/collar-lifecycle.md](/C:/Users/ALEJANDRO/IdeaProjects/ganaderia4backend/docs/collar-lifecycle.md:1).

Ese documento tambiÃ©n deja explÃ­citas las reglas objetivo para procesamiento de ubicaciones y monitoreo offline, junto con los cambios de cÃ³digo recomendados para el siguiente bloque.

---

## PolÃ­tica temporal UTC

El backend adopta UTC como polÃ­tica temporal interna.

Por compatibilidad, varios campos siguen usando `LocalDateTime`.

En esta fase esos valores se interpretan como UTC y el contrato JSON pÃºblico no cambia.

La polÃ­tica completa estÃ¡ documentada en [docs/time-policy.md](/C:/Users/ALEJANDRO/IdeaProjects/ganaderia4backend/docs/time-policy.md:1).

---

## Resumen de Fase 1

El cierre formal de la Fase 1 de hardening, incluyendo seguridad, observabilidad, lifecycle y polÃ­tica temporal, estÃ¡ resumido en [docs/phase-1-hardening-summary.md](/C:/Users/ALEJANDRO/IdeaProjects/ganaderia4backend/docs/phase-1-hardening-summary.md:1).

---

## Swagger / OpenAPI

En `local` y `dev`, Swagger UI esta disponible en `/swagger-ui.html` y el documento OpenAPI JSON en `/v3/api-docs`.

En `prod`, Swagger/OpenAPI esta deshabilitado por seguridad segun la configuracion actual del backend.

Uso esperado:

- `POST /api/auth/login` es un endpoint publico para obtener JWT Bearer.
- Los endpoints protegidos del backend usan `Authorization: Bearer <token>`.
- `POST /api/device/locations` no usa JWT; usa autenticacion HMAC por headers `X-Device-Token`, `X-Device-Timestamp`, `X-Device-Nonce` y `X-Device-Signature`.
- No deben cargarse secretos reales ni claves de dispositivos dentro de Swagger UI.

---

## Matriz de permisos

La matriz oficial de permisos y la politica actual de RBAC del backend estan documentadas en [docs/permissions-matrix.md](/C:/Users/ALEJANDRO/IdeaProjects/ganaderia4backend/docs/permissions-matrix.md:1).

---

## TecnologÃ­as utilizadas

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
- **service**: contiene lÃ³gica de negocio
- **repository**: acceso a base de datos
- **model**: entidades del dominio
- **dto**: contratos de entrada y salida
- **config**: seguridad, CORS y configuraciÃ³n general
- **observability**: mÃ©tricas, correlation id y health
- **notification**: servicios de notificaciÃ³n desacoplados
- **pattern**: implementaciÃ³n de patrones de diseÃ±o usados por el flujo de monitoreo

---

## MÃ³dulos del sistema

### 1. AutenticaciÃ³n
Permite iniciar sesiÃ³n y proteger el acceso al resto del sistema mediante JWT.

### 2. Vacas
Permite registrar, consultar y actualizar vacas.

### 3. Collares
Permite registrar, actualizar, habilitar, deshabilitar y reasignar collares a vacas.

### 4. Geocercas
Permite administrar Ã¡reas geogrÃ¡ficas vÃ¡lidas para monitoreo.

### 5. Ubicaciones
Permite registrar y consultar ubicaciones histÃ³ricas de las vacas reportadas por collares.

### 6. Alertas
Permite consultar y gestionar alertas operativas, tanto manuales como automÃ¡ticas.

### 7. Reportes
Permite consultar indicadores y exportar reportes operativos.

### 8. Observabilidad
Permite revisar health, mÃ©tricas y trazabilidad de requests.

MÃ©tricas operativas principales:

- `ganaderia.alerts.created` con tag `type`
- `ganaderia.alerts.resolved` con tag `type`
- `ganaderia.alerts.discarded` con tag `type`
- `ganaderia.collars.marked_offline`
- `ganaderia.device.requests.accepted`
- `ganaderia.device.requests.rejected` con tag `reason`
- `ganaderia.notifications.sent` con tags `channel` y `eventType`
- `ganaderia.notifications.failed` con tags `channel` y `eventType`

En `prod`, Actuator expone por defecto `health,info`. Para habilitar scraping operativo de mÃ©tricas sin tocar cÃ³digo:

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

La autorizaciÃ³n depende del endpoint y del mÃ©todo HTTP.

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

- `local`: perfil por defecto para desarrollo local. Usa PostgreSQL local en `localhost:5432/ganaderia4`, Swagger habilitado y secretos locales no aptos para producciÃ³n.
- `dev`: perfil para entornos de desarrollo compartidos. Requiere `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `DEVICE_SECRET_MASTER_KEY`.
- `prod`: perfil de producciÃ³n. Requiere secretos por variables de entorno, mantiene Swagger apagado y expone por defecto solo `health,info` en Actuator.
- `test`: perfil usado por las pruebas con Testcontainers.

En `dev` y `prod`, `JWT_SECRET` y `DEVICE_SECRET_MASTER_KEY` deben tener al menos 32 bytes. Si faltan o son demasiado cortos, la aplicaciÃ³n falla al iniciar.

---

## EjecuciÃ³n local

Levantar PostgreSQL local con una base `ganaderia4` y credenciales por defecto `postgres/postgres`, o definir `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

```bash
./mvnw spring-boot:run
```

Para crear un administrador inicial en una base vacÃ­a, definir `APP_BOOTSTRAP_ADMIN_NAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` antes de iniciar.

El perfil `local` queda activo por defecto. Para usar otro perfil:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

La validaciÃ³n de CI usa Java 17 y ejecuta:

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
