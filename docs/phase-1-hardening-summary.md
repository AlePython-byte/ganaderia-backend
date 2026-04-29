# Resumen de Fase 1 de Hardening — Ganadería 4.0

## Propósito

La Fase 1 de hardening se enfocó en elevar la base técnica del backend sin romper el contrato funcional del MVP. El objetivo fue reducir riesgos operativos, mejorar seguridad, reforzar validaciones, ordenar documentación crítica y hacer más determinísticos los componentes que dependen del tiempo.

## Bloques implementados

### 1. Plataforma Java 17 documentada

- Problema que resolvía:
  faltaba dejar explícita la plataforma objetivo del backend para desarrollo, CI y despliegue.
- Archivos o módulos principales afectados:
  `README.md`, documentación de plataforma y lineamientos de ejecución.
- Valor técnico:
  alinea build local, pipelines y despliegue sobre una versión soportada y predecible.
- Estado de validación:
  documentado y usado por el build del proyecto.

### 2. Patrones de diseño documentados

- Problema que resolvía:
  el flujo de monitoreo y alertas ya usaba patrones, pero su intención arquitectónica no estaba suficientemente explicada.
- Archivos o módulos principales afectados:
  documentación de arquitectura y referencias al paquete `pattern`.
- Valor técnico:
  mejora mantenibilidad, onboarding y criterio de extensión sin refactors innecesarios.
- Estado de validación:
  documentado y coherente con la implementación existente.

### 3. Protección contra CSV Injection

- Problema que resolvía:
  los reportes CSV podían exponer a ejecución de fórmulas al abrirse en hojas de cálculo.
- Archivos o módulos principales afectados:
  servicios/controladores de reportes CSV.
- Valor técnico:
  reduce un riesgo real de seguridad en exportaciones operativas.
- Estado de validación:
  implementado y cubierto por pruebas del flujo de reportes.

### 4. Endpoint de rotación de secreto restringido a `ADMINISTRADOR`

- Problema que resolvía:
  la rotación de secreto de collar es una operación sensible y no debía quedar expuesta a roles amplios.
- Archivos o módulos principales afectados:
  configuración de seguridad y endpoint de rotación.
- Valor técnico:
  endurece control de acceso sobre credenciales de dispositivos IoT.
- Estado de validación:
  implementado y validado por pruebas de seguridad/autorización.

### 5. Quality Gate de JaCoCo

- Problema que resolvía:
  el proyecto no tenía una barrera explícita de cobertura mínima para proteger la calidad del merge.
- Archivos o módulos principales afectados:
  configuración de build y documentación de calidad.
- Valor técnico:
  evita regresiones silenciosas y obliga a mantener cobertura mínima combinada.
- Estado de validación:
  activo en `clean verify` y en CI.

### 6. `/healthz` tratado como health endpoint

- Problema que resolvía:
  el tráfico de health checks generaba ruido innecesario en logs y observabilidad.
- Archivos o módulos principales afectados:
  observabilidad, filtros de logging, health endpoints.
- Valor técnico:
  mejora relación señal/ruido en operación y troubleshooting.
- Estado de validación:
  implementado y verificado en comportamiento de logs.

### 7. Variables de entorno y secretos documentados

- Problema que resolvía:
  faltaba una referencia clara de configuración requerida por perfil y despliegue.
- Archivos o módulos principales afectados:
  `README.md` y documentación operativa.
- Valor técnico:
  reduce errores de bootstrap, configuración insegura y drift entre ambientes.
- Estado de validación:
  documentado y alineado con propiedades activas del proyecto.

### 8. Dependabot

- Problema que resolvía:
  la actualización de dependencias y acciones quedaba manual y fácil de postergar.
- Archivos o módulos principales afectados:
  configuración de Dependabot.
- Valor técnico:
  automatiza visibilidad sobre deuda de dependencias y seguridad.
- Estado de validación:
  configurado en el repositorio.

### 9. OWASP Dependency-Check

- Problema que resolvía:
  faltaba un escaneo estructurado de vulnerabilidades en dependencias Java.
- Archivos o módulos principales afectados:
  workflow separado de seguridad y documentación asociada.
- Valor técnico:
  agrega verificación recurrente de CVEs con artefactos auditables.
- Estado de validación:
  configurado en workflow independiente.

### 10. Trivy container scan

- Problema que resolvía:
  la imagen del backend no tenía un control explícito de vulnerabilidades de contenedor.
- Archivos o módulos principales afectados:
  workflow separado de Trivy y documentación operativa.
- Valor técnico:
  agrega visibilidad sobre riesgo en imagen base y paquetes del contenedor.
- Estado de validación:
  configurado en workflow independiente.

### 11. Lifecycle de collares documentado

- Problema que resolvía:
  `status`, `enabled`, `signalStatus` y `lastSeenAt` necesitaban una semántica operativa explícita.
- Archivos o módulos principales afectados:
  `docs/collar-lifecycle.md`.
- Valor técnico:
  aclara reglas de dominio y reduce ambigüedad en futuras correcciones.
- Estado de validación:
  documentado como referencia oficial del lifecycle.

### 12. Reglas de lifecycle aplicadas

- Problema que resolvía:
  existía riesgo de procesar ubicaciones o monitorear collares fuera de estado operativo válido.
- Archivos o módulos principales afectados:
  flujo de ubicaciones, monitoreo offline y servicios asociados.
- Valor técnico:
  endurece consistencia de dominio y reduce falsos positivos operativos.
- Estado de validación:
  implementado con pruebas sobre `enabled=false`, `INACTIVO` y `MANTENIMIENTO`.

### 13. Política temporal UTC

- Problema que resolvía:
  el backend usaba tiempos absolutos sin una política explícita común entre servidor, base de datos, dispositivo y tests.
- Archivos o módulos principales afectados:
  `docs/time-policy.md`, `application*.properties`, `TimeConfig`, configuración Jackson/Hibernate.
- Valor técnico:
  fija una referencia temporal interna única y prepara migraciones futuras con menos riesgo.
- Estado de validación:
  documentado e implementado en configuración base UTC.

### 14. `lastSeenAt` monotónico

- Problema que resolvía:
  una telemetría vieja pero válida podía hacer retroceder `Collar.lastSeenAt` y distorsionar monitoreo y dashboard.
- Archivos o módulos principales afectados:
  flujo de monitoreo asociado a ubicaciones y `MonitoringFacade`.
- Valor técnico:
  protege la frescura operativa del collar sin perder historial de ubicaciones.
- Estado de validación:
  implementado y cubierto con pruebas de retroceso, igualdad y avance.

### 15. `DeviceMonitoringService` con `Clock`

- Problema que resolvía:
  el monitoreo offline dependía de `LocalDateTime.now()` y del reloj real del servidor.
- Archivos o módulos principales afectados:
  `DeviceMonitoringService` y sus tests unitarios/integración.
- Valor técnico:
  vuelve determinístico el cálculo del threshold offline y reduce fragilidad de pruebas.
- Estado de validación:
  implementado con `Clock` inyectable y pruebas con `Clock.fixed(...)`.

### 16. `DashboardService` con `Clock`

- Problema que resolvía:
  los cálculos de aging y freshness dependían del reloj real del proceso.
- Archivos o módulos principales afectados:
  `DashboardService`, `DashboardServiceTest`, `DashboardControllerIntegrationTest`.
- Valor técnico:
  vuelve determinísticos los KPIs operativos del dashboard y facilita pruebas sobre umbrales.
- Estado de validación:
  implementado con `Clock` inyectable y validado con tests de umbrales fijos.

## Estado actual del MVP

El backend se encuentra en un estado de MVP técnico avanzado para un piloto universitario o controlado. Ya cuenta con base razonable de seguridad, observabilidad, validaciones defensivas, quality gates, documentación operativa y reglas de dominio críticas estabilizadas.

No es todavía una plataforma multi-tenant o enterprise completa, pero sí una base suficientemente madura para validación funcional seria, operación supervisada y evolución por bloques pequeños sin necesidad de rehacer el núcleo del backend.

## Mejoras recomendadas para Fase 2

- modelo de finca / organización para segmentar datos y operación
- permisos más finos por recurso y acción
- API versionada bajo `/api/v1`
- Swagger/OpenAPI más profesional y orientado a consumidores externos
- refresh tokens y logout con invalidación controlada
- política de data retention para telemetría, auditoría y entregas de webhook
- logs JSON y runbook operativo para soporte
- pruebas de carga sobre ingestión IoT, dashboard y reportes

## Cierre de Fase 1

La Fase 1 deja una base técnica más segura, más documentada y más predecible sin romper compatibilidad del MVP actual. El backend queda listo para merge hacia `develop` desde una postura de hardening incremental, no de refactor disruptivo.
