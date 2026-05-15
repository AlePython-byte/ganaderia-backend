# Guía de throttle transversal de EMAIL — Ganadería 4.0

## 1. Propósito

Esta guía documenta la protección contra ráfagas de correos en el canal EMAIL del backend Ganadería 4.0. El objetivo es reducir spam accidental, proteger el proveedor Resend y mantener estable el flujo operativo de alertas.

## 2. Problema que resuelve

Las alertas operativas pueden generar correos hacia administradores, supervisores o un destinatario fallback. Si varias alertas se generan en poco tiempo, varios eventos pueden concentrarse en un mismo destinatario.

El modo EMAIL direct puede llamar directamente al proveedor de email. El modo EMAIL outbox puede encolar mucho volumen para envío posterior. En ambos casos, Resend puede tener límites operativos, costos o restricciones de uso. El throttle reduce ese riesgo sin romper la operación principal del backend.

## 3. Principio de diseño

El throttle de EMAIL es no bloqueante para el flujo principal.

Si se supera el límite:

- no se envía el email;
- no se encola el email;
- se registra log seguro;
- se incrementa métrica;
- la alerta o evento principal continúa.

No se devuelve `429 Too Many Requests` al cliente porque el email es un efecto secundario operativo, no el resultado principal del endpoint que generó la alerta.

## 4. Flujos protegidos

| Flujo | Canal | Modo | Protegido | Observación |
|---|---|---|---|---|
| Notificaciones EMAIL por alertas | EMAIL | direct | Sí | Se evalúa throttle antes de llamar al proveedor. |
| Notificaciones EMAIL por alertas | EMAIL | outbox | Sí | Se evalúa throttle antes de crear `NotificationOutboxMessage`. |
| Resolución de destinatarios por preferencias | EMAIL | direct/outbox | Parcial | `emailEnabled`, `minimumSeverity` y deduplicación reducen ruido antes del throttle. |

## 5. Flujos no cubiertos por este throttle

| Flujo | Motivo de exclusión | Protección existente |
|---|---|---|
| Password reset | Tiene protección granular propia y no debe mezclarse con alertas. | Rate limiting por IP, email e IP+email para forgot-password; por IP, token e IP+token para reset-password. |
| Outbox requeue admin | Es una acción administrativa protegida por separado. | Rate limiting por usuario, IP, usuario+mensaje y usuario+IP. |
| Login | No genera EMAIL. | Rate limiting por IP, email e IP+email. |
| Device ingestion | Usa HMAC y protección específica. | Rate limiting por device token, IP+token o IP sin token. |
| IA | No genera EMAIL. | Rate limiting para `GET /api/alert-analysis/ai-summary`. |
| WEBHOOK | Canal distinto a EMAIL. | Processor y retry propios del webhook. |
| LOG | Canal local no externo. | No aplica throttle de EMAIL. |

## 6. Scopes implementados

| Scope | Criterio | Riesgo mitigado |
|---|---|---|
| `EMAIL_RECIPIENT` | Destinatario normalizado y hasheado. | Ráfagas generales hacia un mismo email. |
| `EMAIL_RECIPIENT_EVENT_TYPE` | Destinatario + tipo de notificación/eventType. | Repetición de un mismo tipo de alerta para un destinatario. |
| `EMAIL_CHANNEL` | Canal EMAIL global. | Volumen total excesivo del canal EMAIL. |

`EMAIL_RECIPIENT` protege a un destinatario de ráfagas generales. `EMAIL_RECIPIENT_EVENT_TYPE` reduce repetición del mismo tipo de evento para el mismo destinatario. `EMAIL_CHANNEL` limita el volumen global del canal EMAIL cuando aplica.

## 7. Persistencia y privacidad

La protección reutiliza `AbuseRateLimitEntry`.

Las claves se almacenan hasheadas. No se guarda el email real en claro como `abuseKey`. Tampoco se guarda el subject completo, payload completo, API key de Resend ni contenido sensible del correo como clave de rate limit.

## 8. Comportamiento en modo direct

Dentro del límite, `EmailNotificationService` llama al cliente/proveedor como antes.

Fuera del límite:

- no llama a Resend ni al proveedor configurado;
- registra el evento de throttle con datos seguros;
- incrementa métrica;
- devuelve `SKIPPED` dentro del pipeline de notificación;
- no rompe la creación de la alerta principal.

## 9. Comportamiento en modo outbox

Dentro del límite, `EmailNotificationService` encola el mensaje como antes.

Fuera del límite:

- no crea `NotificationOutboxMessage`;
- no modifica el processor;
- no cambia los estados `PENDING`, `PROCESSING`, `SENT`, `FAILED` ni `DEAD`;
- no rompe la alerta principal.

El processor de outbox conserva sus reglas propias de batch, retry, `DEAD` y recuperación de `PROCESSING` atascado.

## 10. Variables de configuración

| Propiedad / variable | Default | Descripción |
|---|---:|---|
| `app.abuse-protection.email.enabled` / `APP_ABUSE_PROTECTION_EMAIL_ENABLED` | `true` | Habilita el throttle parcial del canal EMAIL. |
| `app.abuse-protection.email.recipient.window` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_WINDOW` | `10m` | Ventana para límite por destinatario. |
| `app.abuse-protection.email.recipient.max-attempts` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_MAX_ATTEMPTS` | `10` | Máximo de emails permitidos por destinatario en la ventana. |
| `app.abuse-protection.email.recipient.block-duration` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_BLOCK_DURATION` | `10m` | Bloqueo por destinatario al exceder el límite. |
| `app.abuse-protection.email.recipient-event-type.window` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_EVENT_TYPE_WINDOW` | `10m` | Ventana para destinatario + eventType. |
| `app.abuse-protection.email.recipient-event-type.max-attempts` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_EVENT_TYPE_MAX_ATTEMPTS` | `5` | Máximo por destinatario + eventType en la ventana. |
| `app.abuse-protection.email.recipient-event-type.block-duration` / `APP_ABUSE_PROTECTION_EMAIL_RECIPIENT_EVENT_TYPE_BLOCK_DURATION` | `10m` | Bloqueo por destinatario + eventType. |
| `app.abuse-protection.email.channel.window` / `APP_ABUSE_PROTECTION_EMAIL_CHANNEL_WINDOW` | `10m` | Ventana para volumen global del canal EMAIL. |
| `app.abuse-protection.email.channel.max-attempts` / `APP_ABUSE_PROTECTION_EMAIL_CHANNEL_MAX_ATTEMPTS` | `100` | Máximo global del canal EMAIL en la ventana. |
| `app.abuse-protection.email.channel.block-duration` / `APP_ABUSE_PROTECTION_EMAIL_CHANNEL_BLOCK_DURATION` | `5m` | Bloqueo global del canal EMAIL al exceder el límite. |

## 11. Métricas y logs

Métrica real:

- `ganaderia.notifications.email.throttled.count`

La métrica cuenta correos de notificación omitidos por throttle transversal. Usa tags `eventType` y `scope`.

Los logs seguros pueden incluir:

- `scope`;
- `keyHash`;
- `notificationType`;
- `channel=EMAIL`;
- `throttled=true`;
- `requestId`;
- `retryAfterSeconds`.

Los logs no deben incluir email en claro, API key, payload completo ni contenido sensible del correo.

## 12. Validación manual controlada

Para validar sin generar spam:

1. Usar ambiente local o demo controlado.
2. Bajar temporalmente los límites mediante variables de entorno.
3. Generar notificaciones de alerta controladas.
4. Confirmar que dentro del límite el correo se envía o se encola.
5. Confirmar que fuera del límite el correo no se envía ni se encola.
6. Confirmar que la alerta principal sí existe.
7. Revisar logs y la métrica `ganaderia.notifications.email.throttled.count`.
8. Restaurar los valores normales.

No usar secretos reales en comandos, documentos o capturas.

## 13. Consideraciones para Render/demo

Si se harán muchas pruebas de alertas durante la demo, revisar los límites antes.

No se recomienda desactivar el throttle salvo causa justificada. En demo suele ser mejor aumentar temporalmente los límites que apagar la protección.

Si se usa fallback global `app.notifications.email.to`, varios eventos pueden concentrarse en un mismo correo. En ese caso, el límite por destinatario puede activarse más rápido. Evitar generar ráfagas innecesarias de alertas.

## 14. Troubleshooting

| Problema | Causa probable | Acción recomendada |
|---|---|---|
| No llega email de alerta | Throttle por destinatario o canal; EMAIL deshabilitado; proveedor sin configuración. | Revisar logs, properties de EMAIL y métrica throttled. |
| No aparece mensaje en outbox | Throttle bloqueó antes de encolar; modo no es outbox; outbox deshabilitado. | Revisar `app.notifications.email.delivery-mode` y logs de throttle. |
| Métrica de throttled aumenta | Se excedió un límite de `EMAIL_RECIPIENT`, `EMAIL_RECIPIENT_EVENT_TYPE` o `EMAIL_CHANNEL`. | Ajustar límites o reducir ráfagas de alertas. |
| Muchos eventos apuntan al mismo destinatario | Uso de `notificationEmail` compartido o fallback global. | Distribuir destinatarios o ajustar límites temporalmente. |
| Modo direct no llama Resend | Throttle, EMAIL deshabilitado o configuración incompleta. | Revisar logs `email_notification_skipped` y `email_notification_throttled`. |
| Modo outbox no encola | Throttle o fallo de configuración. | Validar que el destinatario esté dentro de límites. |
| La alerta sí existe pero no hay email | Diseño esperado cuando EMAIL queda throttled. | Confirmar que la alerta se creó y revisar logs/métrica. |
| Demo genera demasiadas alertas | Pruebas repetidas desde la misma configuración/destinatario. | Subir límites temporalmente o espaciar pruebas. |

## 15. Seguridad

- No loguear emails completos si el sistema usa hashes.
- No exponer `<RESEND_API_KEY>`.
- No guardar `<DEVICE_SECRET>`, `<JWT>` ni API keys en archivos versionados.
- No subir `.env`.
- No publicar capturas con destinatarios reales si son sensibles.
- No desactivar la protección en producción.

## 16. Limitaciones actuales

- No implementa digest o resumen periódico de alertas.
- No implementa throttle por finca o tenant.
- No implementa agrupación por vaca/geocerca.
- No reemplaza límites propios del proveedor Resend.
- No evita generación de alertas; solo controla envío o encolado EMAIL.
- WEBHOOK y LOG no están cubiertos por este throttle.

## 17. Criterio de aceptación

La guía queda lista si:

- existe `docs/email-throttle-guide.md`;
- documenta modo direct y outbox;
- documenta scopes reales;
- documenta properties reales;
- documenta métrica real;
- explica por qué no lanza 429;
- no contiene secretos reales;
- no toca código ni configuración.

## 18. Conclusión

El throttle transversal parcial de EMAIL reduce el riesgo de spam, costo o abuso del canal EMAIL, manteniendo intacto el flujo principal de alertas. La protección actúa sobre el efecto secundario de envío/encolado, sin convertir una alerta válida en error para el cliente.
