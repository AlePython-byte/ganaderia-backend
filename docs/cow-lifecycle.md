# Lifecycle de vacas

## Propósito

Este documento define el lifecycle oficial del campo `active` en la entidad `Cow` de **Ganadería 4.0**.

El objetivo es establecer una referencia operativa clara sobre cuándo una vaca está activa o inactiva, qué implica cada estado y qué reglas aplica el backend.

## Campo `active`

El campo `active` (booleano, `true` por defecto) representa si una vaca participa activamente en el monitoreo operativo.

Valores oficiales:

- `true`: la vaca está activa y forma parte de la operación normal.
- `false`: la vaca está desactivada operativamente. Su historial, collar asociado, alertas y ubicaciones previas se conservan intactos.

`active` no reemplaza a `status`. Ambos campos coexisten:

- `status` (`CowStatus`) describe la posición o estado de ubicación de la vaca (`ADENTRO`, `FUERA`, `SIN_UBICACION`).
- `active` describe si la vaca participa activamente en el ciclo de monitoreo.

## Migración

El campo fue agregado mediante la migración `V16__add_cow_active.sql`:

```sql
ALTER TABLE cows
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_cows_active
    ON cows (active);
```

Todas las vacas existentes antes de esta migración reciben `active = true` por defecto.

## Endpoints de ciclo de vida

### Desactivar vaca

```http
PATCH /api/cows/{id}/deactivate
```

- Marca `active = false`.
- Si la vaca ya estaba inactiva, la respuesta es exitosa y no genera cambio ni error.
- Registra auditoría `DEACTIVATE_COW`.
- No modifica `status`, collar, ubicaciones ni alertas históricas.

### Activar vaca

```http
PATCH /api/cows/{id}/activate
```

- Marca `active = true`.
- Si la vaca ya estaba activa, la respuesta es exitosa y no genera cambio ni error.
- Registra auditoría `ACTIVATE_COW`.

### Consulta paginada con filtro por `active`

```http
GET /api/cows/page?active=true&status=ADENTRO&page=0&size=20&sort=id&direction=ASC
```

El endpoint `/api/cows/page` acepta el parámetro `active` como filtro opcional, combinable con el filtro por `status`.

Campos de ordenamiento permitidos: `id`, `token`, `internalCode`, `name`, `status`, `active`.

## Roles autorizados

Las operaciones de activación y desactivación de vacas son accesibles para:

- `ADMINISTRADOR`
- `SUPERVISOR`
- `OPERADOR`

`TECNICO` no puede activar ni desactivar vacas.

## Contrato de respuesta

El campo `active` está expuesto en `CowResponseDTO`:

```json
{
  "id": 1,
  "token": "COW-001",
  "internalCode": "INT-001",
  "name": "Luna",
  "status": "ADENTRO",
  "active": true,
  "observations": "Vaca en monitoreo diario"
}
```

## Auditoria

Las transiciones de `active` se registran en el log de auditoría con los eventos:

- `DEACTIVATE_COW`: actor, entidad `COW`, id de vaca y token.
- `ACTIVATE_COW`: actor, entidad `COW`, id de vaca y token.

## Reglas operativas

| Condición | `active` | Puede recibir telemetría | Evaluada en reportes e incidentes |
|---|---|---|---|
| Vaca operativa | `true` | Depende del collar | Sí |
| Vaca desactivada | `false` | El backend no impide telemetría desde el collar, pero la vaca no participa activamente en el ciclo | Depende del reporte |

Nota: `active = false` no bloquea el collar ni invalida ubicaciones históricas. Es un marcador operativo a nivel de vaca. Si se desea bloquear completamente la ingestión desde el collar, el collar debe deshabilitarse por separado.

## Relación con el lifecycle de collares

`active` en vacas y `enabled` en collares son mecanismos complementarios pero independientes:

- Un collar solo procesa telemetría si está `ACTIVO`, `enabled=true` y tiene vaca asignada.
- `active` en la vaca no bloquea el procesamiento del collar de forma directa.
- Para suspender el monitoreo de un animal, la práctica recomendada es desactivar la vaca (`active=false`) y deshabilitar el collar asociado (`enabled=false`).

Ver también: [docs/collar-lifecycle.md](collar-lifecycle.md).

## Resumen

- `active` controla la participación operativa de la vaca sin borrar historial.
- Las transiciones se hacen mediante `PATCH /api/cows/{id}/deactivate` y `PATCH /api/cows/{id}/activate`.
- Son operaciones idempotentes: no fallan si la vaca ya está en el estado solicitado.
- El campo se filtra en paginación combinado o individualmente con `status`.
- La auditoría registra cada transición.
