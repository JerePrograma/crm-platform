# SEG-009 — Evidencia de outbox, workers e inbound

Fecha: 2026-07-22

Rama: `feat/complete-crm-platform`

Commit inicial: `1a6a98bc5bd2f9cf97a7c2978cd633ba77f74cb3`

## Resultado

```text
SEG_009=EXECUTED_PASS
OUTBOX=EXECUTED_PASS
WORKERS=EXECUTED_PASS
LEASE_RECOVERY=EXECUTED_PASS
RETRY_DEAD_LETTER_REQUEUE=EXECUTED_PASS
IDEMPOTENCY=EXECUTED_PASS
INBOUND_FAKE=EXECUTED_PASS
WEBHOOK_SECURITY=EXECUTED_PASS
QUARANTINE=EXECUTED_PASS
REAL_PROVIDERS=NOT_CONNECTED
REAL_NETWORK_CALLS=0
FORBIDDEN_MESSAGE_STATES=0
```

## Migración y modelo

`V12__transactional_outbox_and_durable_inbound.sql` agregó `outbox_event` e
`inbound_message`, completó idempotencia de `message_record`, FK/constraints,
índices de polling/lease/aggregate/correlation y unicidades por organización.
Flyway ejecutó V1→V12 desde vacío y V11→V12 sobre el volumen sintético local;
Hibernate `validate` pasó.

## Validaciones ejecutadas

```text
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
PASS — 69/69 — 02:28 min — Java 21.0.11 — PostgreSQL 17 Testcontainers
Spotless — 148/148
ArchUnit — PASS

npm run typecheck
PASS — TypeScript strict

npm run build
PASS — Vite — 17 módulos — 270 ms en la corrida posterior al ajuste UI

powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1
  -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173 -KeepRunning
PASS — build sin caché, V11→V12, tres servicios healthy y smoke autenticado
```

Las 69 pruebas incluyen seis casos PostgreSQL de SEG-009 y regresiones de
mensajería/seguridad: commit/rollback transaccional, colisión de idempotencia,
dos workers sin doble efecto, lease vencido, retry/dead, HMAC/replay,
quarantine, asociación, dominio inbound, permisos, provider bloqueado y cero
`SENT`.

La ejecución focal directa desde Windows no ejecutó casos porque Ryuk no pudo
conectarse de regreso por named pipe (`BLOCKED_PLATFORM`, 0 tests); el mismo
test se ejecutó dentro del validador contenedorizado soportado y pasó 6/6. No
se convirtió el diagnóstico local fallido en evidencia funcional.

## E2E sobre stack real

Playwright CLI con Chrome del sistema y PostgreSQL real ejecutó:

```text
login
→ crear prospecto/contacto sintético
→ crear/congelar/aprobar/simular campaña FAKE
→ observar CAMPAIGN_SIMULATED_V1
→ worker manual
→ BLOCKED por política de envío
→ webhook HMAC válido 202
→ repetición idéntica 200 sin efecto duplicado
→ worker manual
→ actividad inbound + last_contact_at + tarea + timeline
→ preparar transición comercial válida hasta CONTACTED
→ segundo inbound
→ CONTACTED → REPLIED automático
→ webhook no asociable
→ QUARANTINED
→ asociación manual tenant-scoped
→ requeue y PROCESSED
→ /api/v1/messages/send = 404
→ health = 200
```

Pruebas HTTP vivas: firma inválida `401`, timestamp vencido `401`, media type
inválido `415`, payload de 40.000 bytes `413`, replay idéntico `200`.

## Fallos encontrados y corregidos

1. El inbound incrementaba la versión del prospecto y la ficha abierta quedaba
   obsoleta. La API frontend ahora conserva el status HTTP, trata `409`,
   recarga el detalle y “Actualizar” refresca también el prospecto seleccionado.
2. La UI no exponía el registro de actividad operativa ya existente. Se agregó
   el formulario accesible y protegido para registrar llamadas/reuniones/demos
   o comunicaciones manuales explícitas.
3. La asociación manual sin `contactId` usaba `? IS NULL` y PostgreSQL no podía
   inferir su tipo. Se separaron dos consultas estáticas y se agregó regresión
   PostgreSQL para la asociación válida sin contacto opcional.

## Evidencia PostgreSQL final del segmento

```text
system_setting:
  sending.enabled=false
  sending.dry-run=true
  sending.daily-limit=0
  sending.kill-switch=true
message_record: RECEIVED=3, SIMULATED=1
message_record SENT|DELIVERED|READ=0
outbox_event: BLOCKED=1, SUCCEEDED=4
inbound_message: PROCESSED/ASSOCIATED=3
inbound tasks=3
inbound activities=3
```

## Advertencias

- la consola conservó errores de red esperados de `auth/me` anónimo y un login
  deliberadamente fallido después de reiniciar el backend; no hubo error de la
  aplicación autenticada en el recorrido final;
- Mockito mantiene su aviso de self-attach futuro;
- Spring Data advierte sobre serialización directa de `PageImpl`; el contrato
  actual sigue validado y la estabilización DTO pertenece al hardening SEG-010;
- el webhook fake volvió a quedar deshabilitado y sin secreto tras la prueba.
