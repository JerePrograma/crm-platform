# Índice de documentación

## Continuidad

- `../AGENTS.md` — reglas obligatorias y rama canónica;
- `status.md` — estado real, tareas finalizadas, pendientes y riesgos;
- `next-step.md` — única próxima acción autorizada;
- `backlog.md` — segmentos, dependencias y checklist ejecutable;
- `segments/SEG-001.md` — alcance y criterios del segmento activo;
- `validation/SEG-001.md` — matriz principal de evidencia y bloqueos;
- `validation/SEG-001-static-automation-2026-07-20.md` — controles YAML, shell y Make;
- `validation/SEG-001-container-build-2026-07-20.md` — primer preflight/build Docker, errores frontend y correcciones;
- `validation/SEG-001-rerun-2026-07-20.md` — imágenes cacheadas, conflicto 5432 y corrección de puerto configurable;
- `main-consolidation.md` — consolidación fast-forward en `main`.

## Inicio y operación

- `containerized-quickstart.md` — PostgreSQL, backend, frontend y smoke con Compose;
- `local-development-and-usage.md` — procesos separados, variables, flujo y troubleshooting;
- `../scripts/README.md` — preflight, builds limpios, smoke, lockfile y Makefile;
- `manual-operations.md` — procedimientos diarios;
- `runbook.md` — incidentes;
- `disaster-recovery.md` — backups y restauración;
- `testing.md` — estrategia de pruebas;
- `deployment.md` — ambientes, gates y rollback.

## Diseño

- `architecture.md`;
- `domain.md`;
- `data-model.md`;
- `roadmap.md`;
- `adr/0001-modular-monolith.md`;
- `adr/0002-postgresql-source-of-truth.md`;
- `adr/0003-react-vite-frontend.md`;
- `adr/0004-fail-closed-sending.md`;
- `adr/0005-idempotent-imports.md`;
- `adr/0006-preview-parity-and-excluded-metrics.md`.

## Seguridad

- `security.md`;
- `campaign-safety.md`;
- `../SECURITY.md`;
- `gmail-deliverability.md`.

## Datos e integraciones

- `import-existing-data.md`;
- `import-hardening.md`;
- `google-integration.md`.

## Entrada principal

- `../README.md`;
- `../Makefile`;
- `../CONTRIBUTING.md`;
- `../CHANGELOG.md`.

## Lectura por tarea

### Levantar todo con Docker

1. `../README.md`;
2. `containerized-quickstart.md`;
3. `../scripts/README.md`;
4. `status.md`.

### Procesos separados

1. `local-development-and-usage.md`;
2. `../scripts/README.md`;
3. `status.md`.

### Resolver el estado actual

1. `next-step.md`;
2. `validation/SEG-001-rerun-2026-07-20.md`;
3. `validation/SEG-001.md`;
4. `containerized-quickstart.md`;
5. `../scripts/README.md`.

### Validación de cierre

1. `validation/SEG-001.md`;
2. `next-step.md`;
3. `validation/SEG-001-container-build-2026-07-20.md`;
4. `validation/SEG-001-rerun-2026-07-20.md`;
5. `validation/SEG-001-static-automation-2026-07-20.md`.

### Continuar desarrollo

1. `../AGENTS.md`;
2. `status.md`;
3. `next-step.md`;
4. `backlog.md`;
5. `validation/SEG-001.md`;
6. ADR y documentación del módulo.

### Importar datos

1. `local-development-and-usage.md`;
2. `import-existing-data.md`;
3. `import-hardening.md`;
4. `manual-operations.md`.

### Revisar seguridad

1. `security.md`;
2. `campaign-safety.md`;
3. `../SECURITY.md`;
4. ADR 0004.

## Regla de precedencia

1. invariantes de seguridad y ADR;
2. `status.md`;
3. `next-step.md`;
4. segmento y validación;
5. contratos normativos;
6. guías operativas;
7. resto.
