# Lifecycle de collares

## Propósito

Este documento define el lifecycle oficial del collar en **Ganadería 4.0** para reducir ambigüedades entre `status`, `enabled`, `signalStatus` y `lastSeenAt`.

El objetivo es establecer una referencia de dominio y de operación para futuros cambios de hardening, sin modificar todavía la lógica funcional existente.

## Alcance

Este documento describe la semántica objetivo del modelo de collar y las reglas operativas que el backend debe aplicar.

Importante:

- varias de estas reglas ya están parcialmente reflejadas en el código;
- otras deben considerarse la definición oficial deseada para el siguiente bloque de implementación;
- cuando una regla todavía no esté reforzada por código, se indica expresamente como recomendación futura.

## Definiciones oficiales

### `status`

`status` representa el estado administrativo y operativo del collar.

Valores oficiales:

- `ACTIVO`: el collar está operativo y habilitado para participar en el monitoreo.
- `INACTIVO`: el collar está fuera de operación.
- `MANTENIMIENTO`: el collar está pausado por revisión técnica.

`status` no representa calidad de señal ni estado de conectividad.

### `enabled`

`enabled` representa un interruptor técnico de operación.

Valores oficiales:

- `true`: el collar puede operar si además cumple las demás condiciones del lifecycle.
- `false`: el collar queda bloqueado operacionalmente, aunque conserve historial, asignación y datos previos.

`enabled` no reemplaza a `status`. Ambos campos deben evaluarse en conjunto.

### `signalStatus`

`signalStatus` representa conectividad o señal del dispositivo.

Valores oficiales:

- `FUERTE`
- `MEDIA`
- `DEBIL`
- `SIN_SENAL`

`signalStatus` no debe usarse como estado administrativo.

`SIN_SENAL` representa una desconexión detectada por monitoreo o un estado equivalente de falta de señal.

### `lastSeenAt`

`lastSeenAt` representa la última telemetría válida recibida desde el dispositivo.

Debe interpretarse como referencia temporal para evaluar frescura de reporte y posibles condiciones offline.

## Matriz de estados válidos

La siguiente matriz documenta la interpretación oficial del lifecycle:

| status | enabled | Interpretación oficial | Debe procesar ubicaciones | Debe evaluarse en offline monitoring |
| --- | --- | --- | --- | --- |
| `ACTIVO` | `true` | Collar operativo | Sí, si además tiene vaca asignada | Sí |
| `ACTIVO` | `false` | Collar administrativamente activo pero bloqueado técnicamente | No | No |
| `INACTIVO` | `true` | Collar fuera de operación | No | No |
| `INACTIVO` | `false` | Collar fuera de operación y bloqueado | No | No |
| `MANTENIMIENTO` | `true` | Collar pausado por revisión técnica | No | No |
| `MANTENIMIENTO` | `false` | Collar pausado y bloqueado | No | No |

## Regla oficial para procesar ubicaciones

Un collar solo puede procesar ubicaciones si se cumplen simultáneamente estas condiciones:

- `status == ACTIVO`
- `enabled == true`
- tiene una vaca asignada

Notas:

- esta es la definición oficial del lifecycle;
- esta regla ya debe considerarse implementada en el flujo de `/api/device/locations`.

## Regla oficial para monitoreo offline

El monitoreo offline solo debe evaluar collares que cumplan simultáneamente estas condiciones:

- `status == ACTIVO`
- `enabled == true`
- `lastSeenAt` suficientemente antiguo según el threshold configurado

Notas:

- `signalStatus` es un resultado o indicador de conectividad, no el criterio administrativo primario;
- esta regla ya debe considerarse implementada para el monitoreo offline.

## Casos de referencia

### A. Collar `ACTIVO` y `enabled=true`

Caso esperado:

- el collar está operativo;
- puede procesar ubicaciones si además tiene vaca asignada;
- puede entrar en monitoreo offline si deja de reportar dentro del threshold configurado.

### B. Collar `ACTIVO` y `enabled=false`

Caso esperado:

- el collar conserva su estado administrativo, pero queda bloqueado operacionalmente;
- no debería procesar ubicaciones;
- no debería ser evaluado por el monitoreo offline.

Importante:

- este comportamiento debe considerarse oficial;
- esta restricción ya debe considerarse implementada en código.

### C. Collar `INACTIVO`

Caso esperado:

- el collar está fuera de operación;
- no debería procesar ubicaciones, independientemente de `enabled`;
- no debería ser marcado offline por el monitoreo.

### D. Collar `MANTENIMIENTO`

Caso esperado:

- el collar está pausado por revisión técnica;
- no debería procesar ubicaciones, independientemente de `enabled`;
- no debería ser marcado offline por el monitoreo.

### E. Collar sin vaca asignada

Caso esperado:

- no debe procesar ubicaciones;
- puede conservar estado administrativo y técnico;
- la falta de asignación bloquea la operación normal del dispositivo.

### F. Collar `ACTIVO` con `lastSeenAt` antiguo

Caso esperado:

- si además `enabled == true`, debe ser candidato a monitoreo offline;
- el sistema puede reflejar desconexión mediante `signalStatus = SIN_SENAL`;
- la evaluación depende del threshold configurado.

### G. Collar que vuelve a reportar después de `SIN_SENAL`

Caso esperado:

- al recibir telemetría válida, `lastSeenAt` debe actualizarse;
- el sistema debe considerar recuperación de conectividad;
- las alertas offline pendientes deben resolverse según la lógica operativa existente.

## Observaciones sobre la implementación actual

La auditoría previa detectó estas diferencias entre la definición oficial y el comportamiento actual observado:

- `status` y `enabled` se solapan en el uso actual;
- hoy `status` controla la aceptación de ubicaciones;
- hoy `enabled` controla principalmente monitoreo offline, reportes y métricas;
- un collar con `enabled=false` todavía puede seguir reportando si `status=ACTIVO` y tiene vaca asignada;
- un collar `INACTIVO` o `MANTENIMIENTO` con `enabled=true` todavía puede ser marcado como `SIN_SENAL` por el monitoreo offline;
- `signalStatus` y `lastSeenAt` pueden quedar temporalmente desalineados.

Estas observaciones no cambian la definición oficial anterior. Solo documentan el punto de partida actual.

## Cambios de código recomendados

Implementado en este bloque:

- validación de `enabled == true` en el flujo de `/api/device/locations`;
- exclusión de collares `INACTIVO` y `MANTENIMIENTO` del offline monitoring;
- tests de integración para fijar estas reglas de lifecycle.

Recomendado para siguientes bloques:

- revisar si reportes y métricas derivadas de conectividad deben filtrar también por `status == ACTIVO`;
- seguir ampliando cobertura de integración alrededor de transiciones de lifecycle y recuperación de conectividad.

## Resumen operativo

En adelante, el lifecycle oficial del collar debe interpretarse así:

- `status` define el estado administrativo y operativo;
- `enabled` define si el collar está técnicamente habilitado para operar;
- `signalStatus` describe conectividad o señal;
- `lastSeenAt` representa la última telemetría válida;
- un collar solo debe operar cuando está `ACTIVO`, `enabled=true` y asignado a una vaca.
