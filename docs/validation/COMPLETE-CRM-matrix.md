# Matriz de validación integral del CRM

Actualizado: 2026-07-21

Estados permitidos: `NOT_STARTED`, `IN_PROGRESS`, `IMPLEMENTED_NOT_RUN`, `EXECUTED_PASS`,
`EXECUTED_FAIL`, `BLOCKED_EXTERNAL`, `NOT_APPLICABLE`.

| Capacidad | Estado | Evidencia actual |
|---|---|---|
| baseline SEG-001 Windows | EXECUTED_PASS | validador 2026-07-21, commit `7db7e4c`, 29/29 |
| inventario/gap analysis | EXECUTED_PASS | plan integral y análisis de código completo |
| documentación viva | IN_PROGRESS | plan, progreso y esta matriz creados |
| organización/tenant | EXECUTED_PASS | V6 desde vacío y V1-V5; FK/índices/backfill |
| usuarios persistentes | EXECUTED_PASS | hash, alta, activación y cambio de contraseña |
| roles y permisos | EXECUTED_PASS | ADMIN/MANAGER/SALES/VIEWER y 18 permisos |
| sesión cookie/CSRF | EXECUTED_PASS | tests MockMvc y smoke host/contenedor |
| login/logout/bloqueo | EXECUTED_PASS | 10/10 security integration tests |
| aislamiento entre tenants | EXECUTED_PASS | segundo tenant persistente devuelve 404 |
| bootstrap admin seguro | EXECUTED_PASS | solo sin admin y con variables completas |
| auditoría de identidad | EXECUTED_PASS | login/fallo/logout/usuario/contraseña |
| prospect CRUD | EXECUTED_PASS | integración PostgreSQL y UI real |
| contactos CRUD | EXECUTED_PASS | create/update/delete y normalización integrados |
| archivo/restauración | EXECUTED_PASS | estado previo y elegibilidad preservados |
| búsqueda/filtros/paginación | EXECUTED_PASS | JDBC paginado, allow-list y prueba integrada |
| exportación CSV segura | EXECUTED_PASS | prueba de neutralización de fórmula |
| optimistic locking 409 | EXECUTED_PASS | conflicto integrado y Problem Details |
| ciclo comercial | EXECUTED_PASS | máquina explícita y flujo integrado |
| historial de estados | EXECUTED_PASS | persistencia y timeline verificados |
| notas y sanitización | EXECUTED_PASS | script escapado en integración |
| actividades | EXECUTED_PASS | actividad outbound y metadata JSONB |
| tareas/seguimientos | EXECUTED_PASS | create/complete/próxima acción y UI |
| timeline | EXECUTED_PASS | paginado combinado y recorrido Playwright |
| importación sintética | EXECUTED_PASS | SEG-001 100 prospectos/16 exclusiones |
| preview/exclusiones | EXECUTED_PASS | SEG-001 integración |
| resolución de duplicados | EXECUTED_PASS | V8, seis acciones, API/UI y Playwright |
| merge transaccional | EXECUTED_PASS | 3/3 Testcontainers: referencias, idempotencia y rollback |
| oportunidades | NOT_STARTED | — |
| pipeline/forecast | NOT_STARTED | — |
| campañas | NOT_STARTED | esquema V1 sin casos de uso |
| audiencias congeladas | NOT_STARTED | — |
| plantillas versionadas | NOT_STARTED | esquema parcial V1 |
| renderer seguro | NOT_STARTED | — |
| secuencias declarativas | NOT_STARTED | — |
| aprobación/simulación | NOT_STARTED | — |
| email no-op | NOT_STARTED | — |
| email fake | NOT_STARTED | — |
| email real adapter | NOT_STARTED | requerirá credenciales para conexión |
| WhatsApp no-op | NOT_STARTED | — |
| WhatsApp fake | NOT_STARTED | — |
| WhatsApp real adapter | NOT_STARTED | requerirá credenciales para conexión |
| policy/kill switch persistente | IN_PROGRESS | settings V1 existen; no hay punto de salida |
| outbox | NOT_STARTED | — |
| workers/retry/dead-letter | NOT_STARTED | — |
| idempotencia asíncrona | NOT_STARTED | — |
| inbound/webhook fake | NOT_STARTED | — |
| frontend integral | IN_PROGRESS | prospectos/contactos/tareas/timeline operativos; dominios SEG-005+ pendientes |
| frontend E2E | NOT_STARTED | cero tests frontend al inicio |
| dashboard/reportes | NOT_STARTED | — |
| búsqueda PostgreSQL | NOT_STARTED | — |
| configuración/etiquetas | NOT_STARTED | — |
| threat model | NOT_STARTED | — |
| controles de seguridad | NOT_STARTED | — |
| observabilidad integral | NOT_STARTED | health/logs base solamente |
| preview XLSX real | NOT_STARTED | archivo externo no inspeccionado todavía |
| backup/restore | NOT_STARTED | — |
| perfil producción local | NOT_STARTED | — |
| CI integral | NOT_STARTED | cuatro jobs SEG-001 |
| validador completo Windows | NOT_STARTED | — |
| validador completo Unix | NOT_STARTED | — |
| repetición limpia | NOT_STARTED | — |
| despliegue producción | NOT_APPLICABLE | no autorizado |
| envío real | NOT_APPLICABLE | deshabilitado por política |
