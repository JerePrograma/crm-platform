# ADR 0006 — Paridad de preview y métricas de exclusión

- Estado: Accepted
- Fecha: 2026-07-20

## Contexto

El preview original validaba estructura y deduplicación, pero no aplicaba exclusiones. Además, las filas bloqueadas se acumulaban dentro de `acceptedRows`. Esto podía inducir a un operador a aprobar una ejecución con una cantidad de contactos elegibles superior a la real.

## Decisión

1. Preview aplicará la misma evaluación de elegibilidad que la ejecución.
2. Preview no creará instituciones, contactos, canales, prospectos ni exclusiones.
3. Una fila bloqueada se persistirá como `ImportRow.Status.EXCLUDED`.
4. `ImportJob` tendrá un contador `excludedRows` independiente.
5. `totalRows` debe reconciliarse con accepted + excluded + rejected + duplicate + review.
6. La semántica se propagará por PostgreSQL, JPA, API, auditoría, frontend y tests.

## Consecuencias

### Positivas

- preview representa mejor la ejecución;
- operadores ven bloqueados por separado;
- auditoría y reporting no confunden elegibilidad con procesamiento correcto;
- futuras campañas pueden usar `acceptedRows` como base más confiable.

### Negativas

- requiere migración V5;
- clientes antiguos deben tolerar el campo nuevo;
- la UI actual todavía necesita una visualización específica para bloqueados.

## Alternativas descartadas

### Mantener `EXCLUDED` dentro de aceptados

Descartado porque mezcla éxito técnico con elegibilidad comercial.

### Crear prospectos temporales en preview

Descartado porque aumenta riesgo de efectos laterales, limpieza incompleta y contaminación del dominio.

### No persistir evidencia de preview

Descartado porque impide revisión humana, comparación y auditoría.
