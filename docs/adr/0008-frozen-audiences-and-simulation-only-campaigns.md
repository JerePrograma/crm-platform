# ADR-0008 — Audiencias congeladas y campañas solo simuladas

- Estado: Accepted
- Fecha: 2026-07-21

## Contexto

Una audiencia recalculada durante la ejecución no es reproducible y puede omitir
una exclusión creada después de la aprobación. A la vez, esta misión prohíbe
cualquier comunicación real.

## Decisión

- los filtros se evalúan en PostgreSQL y el resultado se materializa por campaña;
- cada prospecto conserva decisión, canal, contacto, validación y motivo;
- una campaña debe pasar `DRAFT → READY_FOR_REVIEW → APPROVED → SIMULATED`;
- la plantilla utilizada es una versión inmutable y sus variables pertenecen a
  una allow-list sin expresiones ejecutables;
- la simulación requiere los cuatro bloqueos ambientales y persistentes, usa
  idempotencia y registra hashes/actividades, no cuerpos ni entregas;
- las secuencias solo admiten seis tipos y condiciones declarativas enumeradas.

## Consecuencias

- aprobación y simulación usan la misma audiencia congelada;
- una exclusión queda explicada en vez de desaparecer del conteo;
- no existe endpoint ni botón de envío en SEG-007;
- los providers y la reevaluación de política por intento pertenecen a SEG-008.
