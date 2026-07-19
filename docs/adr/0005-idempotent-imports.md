# ADR-0005 — Importaciones idempotentes y revisión humana

- Estado: Accepted
- Fecha: 2026-07-19

## Contexto

Los archivos pueden cargarse varias veces y contienen coincidencias exactas o ambiguas. Fusionar automáticamente por nombre puede corromper historial y exclusiones.

## Decisión

- calcular SHA-256 del archivo;
- usar clave idempotente por archivo+modo;
- persistir trabajo y filas;
- procesar cada fila en transacción independiente;
- tratar coincidencias exactas como duplicado;
- enviar similitud nominal a `DuplicateReview`;
- prohibir fusión automática ambigua.

## Consecuencias

- reimportar no duplica;
- un error aislado no pierde todo el trabajo;
- existe evidencia por fila;
- las revisiones requieren UI/acciones posteriores;
- un retry del mismo trabajo fallido debe diseñarse explícitamente.
