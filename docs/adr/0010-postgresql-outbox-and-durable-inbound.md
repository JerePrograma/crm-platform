# ADR 0010: PostgreSQL outbox e inbound durable

- Estado: aceptado
- Fecha: 2026-07-22
- Segmento: SEG-009

## Contexto

El CRM ya confirma mutaciones comerciales, simulaciones y resultados de
mensajería dentro de PostgreSQL. SEG-009 necesita desacoplar trabajo diferido y
recibir respuestas sin habilitar redes ni proveedores reales. Incorporar otro
sistema distribuido aumentaría la superficie operativa sin mejorar la garantía
que hoy necesita el monolito modular.

## Decisión

PostgreSQL sigue siendo la fuente de verdad y aloja `outbox_event` e
`inbound_message`. Una publicación outbox ocurre dentro de la misma transacción
que su mutación de dominio; un rollback revierte ambas. La garantía es
**at-least-once**. No se promete exactly-once: cada consumidor debe ser
idempotente y las claves quedan acotadas por organización.

El claim se hace en una transacción corta mediante `FOR UPDATE SKIP LOCKED`. El
evento pasa a `PROCESSING`, recibe `locked_by`, `locked_at` y
`lock_expires_at`, y la transacción termina antes del procesamiento. El resultado
se persiste con compare-and-set que exige el mismo worker y estado. Nunca se
mantiene una transacción abierta durante una futura llamada de red.

Un `PROCESSING` cuyo lease venció vuelve a `RETRY`; el apagado ordenado deja de
aceptar nuevos lotes y los leases permiten recuperar trabajo abandonado. Los
reintentos usan backoff exponencial acotado y jitter determinístico derivado del
ID del evento, para que las pruebas no dependan del reloj ni de sleeps. Los
errores se clasifican como `RETRYABLE`, `NON_RETRYABLE`, `POLICY_BLOCK`,
`CONFIGURATION_BLOCK`, `CANCELLED` o `DUPLICATE`. Solo el primero reintenta;
alcanzar `max_attempts` produce `DEAD`.

Los eventos `DEAD` se pueden reencolar sin cambiar el payload, solo dentro del
tenant y con `SETTINGS_MANAGE`. Requeue y cancelación se auditan. Un evento
`BLOCKED`, `CANCELLED` o exitoso no se transforma en un envío real.

## Idempotencia y payload

`idempotency_key` es única por organización. Se persiste además un hash
canónico del request: repetir la misma clave y contenido devuelve el mismo
evento; reutilizarla con otro contenido produce `409`. Los receipts inbound son
únicos por organización, provider y `external_event_id`, y el nonce firmado
también tiene unicidad tenant-scoped.

Cada evento declara `event_type` y `event_version`. El payload es un objeto JSON
limitado a 64 KiB y solo contiene IDs y metadata necesaria. No se serializan
entidades JPA, contraseñas, cookies, firmas, access tokens ni refresh tokens. El
inbound persiste hash, referencias externas, contactos normalizados y un extracto
sanitizado y acotado, no el webhook completo. Los timestamps se obtienen de un
`Clock` UTC inyectable.

## Inbound y seguridad

El modo ejecutable es `FAKE_INBOUND`. La firma HMAC-SHA256 cubre timestamp,
nonce, organización y bytes exactos del body; se compara en tiempo constante.
Se exige `application/json`, límite de bytes, ventana temporal, nonce,
correlation ID, rate limit y JSON estricto. La persistencia del receipt y la
publicación `INBOUND_RECEIVED_V1` comparten transacción. El worker asocia por
thread, mensaje relacionado o contacto normalizado dentro de la organización;
la ambigüedad termina en quarantine.

La asociación crea actividad inbound y tarea manual, actualiza
`last_contact_at`, intenta `REPLIED` solo si la máquina de estados lo permite,
cancela trabajo outbound pendiente compatible y deja auditoría. Nunca responde
automáticamente ni reactiva un prospecto `DO_NOT_CONTACT` o `CUSTOMER`.

## Kill switches y aislamiento

El worker reevalúa política al procesar, no solo al publicar. La configuración de
entorno domina a PostgreSQL. Mientras cualquiera de los cuatro bloqueos
canónicos permanezca activo o el provider real no esté conectado, el resultado
solo puede ser simulado, no-op, bloqueado o cancelado. Toda consulta, claim,
transición, idempotencia, detalle y métrica lleva `organization_id`; no existe
requeue ni asociación cross-tenant.

## Límites y operación

Batch, polling, lease, intentos y payload son configurables con límites
conservadores. La UI expone metadata sanitizada, salud, métricas, dead-letter y
quarantine; no permite editar payload, organización ni bloqueos de entorno. La
retención inicial es de 90 días para receipts procesados y un año para eventos
terminales; el borrado se implementará como tarea operativa auditable, nunca en
el camino de procesamiento.

## Rollback

V12 es aditiva. Volver a una imagen V11 deja las tablas sin uso y no modifica
datos previos. El rollback operativo es detener el scheduler, esperar o expirar
leases y desplegar la imagen anterior. Eliminar tablas requiere backup y una
migración posterior explícita; no se edita V12 una vez versionada.

## Riesgos residuales

- At-least-once exige que todo consumidor futuro conserve idempotencia.
- PostgreSQL comparte carga OLTP y outbox; métricas y query plans deben vigilar
  batches, índices y retención.
- Un crash después de un efecto externo y antes del CAS requeriría idempotencia
  del provider; durante esta misión no hay efectos de red reales.
- Los contratos Gmail y WhatsApp siguen implementados pero no conectados y no
  fueron probados con credenciales externas.
