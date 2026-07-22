# Matriz de validación integral del CRM

Actualizado: 2026-07-22

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
| oportunidades | EXECUTED_PASS | V9, 3/3 integración, API/UI y Playwright |
| pipeline/forecast | EXECUTED_PASS | kanban, métricas exactas, aging y cierre WON |
| campañas | EXECUTED_PASS | V10, estados, API/UI y Playwright |
| audiencias congeladas | EXECUTED_PASS | filtros SQL, decisiones persistidas y E2E |
| plantillas versionadas | EXECUTED_PASS | versión inmutable, API/UI y preview |
| renderer seguro | EXECUTED_PASS | allow-list, escaping e inyección rechazada |
| secuencias declarativas | EXECUTED_PASS | tipos/condiciones limitados, API/test y UI |
| aprobación/simulación | EXECUTED_PASS | RBAC, idempotencia, fake activity y Playwright |
| email no-op | EXECUTED_PASS | provider seleccionado, integración y UI |
| email fake | EXECUTED_PASS | simulación persistida, actividad y recorrido visual |
| email real adapter | BLOCKED_EXTERNAL | draft-only implementado/contract test; OAuth no conectado |
| WhatsApp no-op | EXECUTED_PASS | NOOP seleccionado por DEEPLINK_ONLY |
| WhatsApp fake | EXECUTED_PASS | contract/integration fake sin red |
| WhatsApp real adapter | BLOCKED_EXTERNAL | contrato implementado; Meta/credenciales no conectados |
| borradores y enlaces manuales | EXECUTED_PASS | API/UI mailto y wa.me sin apertura automática |
| message policy tenant/exclusión | EXECUTED_PASS | PostgreSQL, consentimiento y bloqueo antes de persistir cuerpo |
| inexistencia de endpoint send | EXECUTED_PASS | MockMvc administrador autenticado obtiene 404 |
| policy/kill switch persistente | EXECUTED_PASS | ambiente + DB verificados antes de simular |
| outbox | EXECUTED_PASS | V12, publicación transaccional, UI/API y PostgreSQL real |
| workers/retry/dead-letter | EXECUTED_PASS | SKIP LOCKED, lease, retry/dead/requeue y doble worker |
| idempotencia asíncrona | EXECUTED_PASS | scope tenant, request hash, repetición y colisión 409 |
| inbound/webhook fake | EXECUTED_PASS | HMAC/replay/quarantine/asociación/dominio y E2E |
| frontend integral | IN_PROGRESS | operación SEG-002–009 navegable; cierre funcional SEG-010 activo |
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
