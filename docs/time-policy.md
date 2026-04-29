# Política temporal UTC — Ganadería 4.0

## 1. Propósito

Ganadería 4.0 recibe telemetría de dispositivos ganaderos, registra ubicaciones, calcula frescura de collares, genera alertas, exporta reportes y muestra información operativa en dashboard.

Por esa razón, el manejo de fechas y horas debe ser consistente. La política oficial del backend es usar UTC como referencia técnica para eventos absolutos.

Esta decisión reduce errores causados por diferencias entre:
- zona horaria del servidor;
- zona horaria de PostgreSQL;
- zona horaria del dispositivo;
- zona horaria del usuario final;
- ambientes locales, test y producción.

## 2. Decisión oficial

El backend adopta UTC como política temporal interna.

En esta fase todavía no se migran todas las entidades y DTOs desde `LocalDateTime` hacia `Instant`, porque eso afectaría el contrato REST, las consultas de reportes, los filtros por rango, las migraciones Flyway y varias pruebas existentes.

Como decisión temporal de compatibilidad:
Todo `LocalDateTime` usado actualmente en campos críticos debe interpretarse como UTC, aunque el tipo Java todavía no exprese zona horaria.

## 3. Tiempo del dispositivo vs tiempo del backend

El sistema maneja dos conceptos distintos:

Tiempo reportado por dispositivo:
- `DeviceLocationRequestDTO.timestamp`
- `Location.timestamp`
- `Collar.lastSeenAt`

Tiempo generado por backend:
- `Alert.createdAt`
- `AuditLog.createdAt`
- timestamps de errores REST
- timestamps de reintentos de webhook

## 4. Riesgos actuales

El proyecto todavía usa `LocalDateTime` en varios campos de dominio y API. Esto funciona, pero tiene riesgos:
1. `LocalDateTime` no contiene zona horaria.
2. Un cliente puede enviar una fecha creyendo que es UTC, pero el backend no lo expresa en el tipo.
3. Los filtros por rango pueden interpretarse de forma diferente si no se documenta la zona.
4. PostgreSQL `TIMESTAMP` no almacena zona horaria.
5. Pruebas que usan `LocalDateTime.now()` pueden volverse frágiles.
6. `lastSeenAt` puede representar telemetría del dispositivo, no necesariamente hora de recepción del servidor.

Además, `lastSeenAt` no debe retroceder si llega una telemetría vieja pero todavía válida dentro de la ventana aceptada.
El historial de ubicaciones puede conservar eventos antiguos válidos, pero la frescura operativa del collar debe permanecer monotónica.

## 5. Configuración técnica base

El proyecto debe mantener configuración explícita de UTC en Spring y Hibernate:

```properties
spring.jackson.time-zone=UTC
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

Además, el backend debe exponer un bean `Clock` en UTC para que nuevos servicios puedan usar tiempo inyectable en lugar de llamar directamente a `LocalDateTime.now()` o `Instant.now()`.

## 6. Reglas para nuevas funcionalidades

- Todo nuevo timestamp absoluto debe diseñarse con UTC como referencia técnica.
- Si una nueva integración externa envía tiempo absoluto, debe documentarse explícitamente si se espera UTC.
- Los nuevos servicios deben preferir `Clock` inyectable sobre llamadas directas a `LocalDateTime.now()` o `Instant.now()`.
- Si se agrega un nuevo DTO temporal público y no se puede migrar aún a `Instant` u `OffsetDateTime`, su semántica UTC debe documentarse explícitamente.
- No deben introducirse conversiones implícitas basadas en la zona horaria local del servidor.

## 7. Campos que deben migrarse primero en una fase futura

Prioridad alta para migración futura a tipos con zona explícita:
- `DeviceLocationRequestDTO.timestamp`
- `Location.timestamp`
- `Collar.lastSeenAt`
- `Alert.createdAt`
- `AuditLog.createdAt`
- timestamps de errores REST
- timestamps de reintentos y entregas de webhook

## 8. Campos que pueden migrarse después

Prioridad posterior:
- filtros secundarios de reportes que hoy dependen de `LocalDateTime`
- campos temporales internos de agregación o scoring
- contratos legacy que hoy consumen timestamps sin offset
- componentes de dashboard que dependen del formato actual

## 9. Plan futuro de migración

1. Identificar todos los campos temporales de entrada, persistencia, salida y consultas nativas.
2. Migrar primero los campos críticos de telemetría y auditoría a `Instant` o `OffsetDateTime`, con pruebas de compatibilidad.
3. Revisar serialización y deserialización REST para evitar romper consumidores actuales sin versionado.
4. Ajustar queries nativas, reportes y filtros por rango para trabajar con la semántica UTC explícita.
5. Planificar cambios de esquema solo cuando el impacto sobre compatibilidad esté controlado.
6. Eliminar gradualmente el uso de `LocalDateTime` en puntos donde represente eventos absolutos.

Como avance incremental, `DeviceMonitoringService` ya usa `Clock` inyectable para que el monitoreo offline sea determinístico y no dependa del reloj real del servidor.

## 10. Criterio actual de compatibilidad

Mientras dure esta fase:
- no se migran entidades a `Instant`;
- no se migran DTOs a `Instant`;
- no se cambian columnas `TIMESTAMP` existentes;
- no se modifica el JSON público para agregar offsets o sufijo `Z`;
- todo `LocalDateTime` crítico debe interpretarse como UTC por política de backend.
