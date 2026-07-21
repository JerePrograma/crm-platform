# Segmentos de completitud del CRM

Actualizado: 2026-07-21

| Segmento | Estado | Gate ejecutable |
|---|---|---|
| SEG-002 — Identidad, usuarios y RBAC | ACTIVE | sesión/RBAC/tenant/Flyway/security tests PASS |
| SEG-003 — Prospectos operativos y contactos | PLANNED | CRUD/ciclo/búsqueda/409/smoke PASS |
| SEG-004 — Actividades, tareas y timeline | PLANNED | recorrido de seguimiento E2E PASS |
| SEG-005 — Resolución de duplicados | PLANNED | acciones y merge transaccional/idempotente PASS |
| SEG-006 — Oportunidades y pipeline | PLANNED | oportunidad ganada/perdida E2E PASS |
| SEG-007 — Campañas y plantillas | PLANNED | campaña fake aprobada/simulada PASS |
| SEG-008 — Mensajería segura y adaptadores | PLANNED | no-op/fake/manual y bloqueo total PASS |
| SEG-009 — Outbox, workers e inbound | PLANNED | retry/dead-letter/concurrencia/webhook fake PASS |
| SEG-010 — Reportes, seguridad y producción | PLANNED | reportes/observabilidad/production smoke PASS |
| SEG-011 — Validación integral y cierre | PLANNED | dos recorridos limpios y matriz final |

Solo un segmento permanece `ACTIVE`. El siguiente se activa al cerrar el gate
anterior con evidencia registrada en la matriz integral.
