# ADR-0002 — PostgreSQL como fuente de verdad

- Estado: Accepted
- Fecha: 2026-07-19

## Contexto

Sheets será útil para revisión e importación, pero no garantiza transacciones, idempotencia ni auditoría adecuadas para comunicaciones comerciales.

## Decisión

PostgreSQL domina estados comerciales, exclusiones, auditoría, importaciones, reservas futuras, Gmail IDs y reconciliación. Sheets será una interfaz auxiliar con sincronización controlada.

## Consecuencias

- toda decisión crítica se toma con datos de PostgreSQL;
- conflictos de Sheets se detectan por versión/timestamp;
- una fila de Sheet no puede sobrescribir silenciosamente un estado de envío;
- backups y DR se centran en PostgreSQL;
- integraciones deben ser idempotentes.
