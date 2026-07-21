# Índice de documentación

## Continuidad

- `../AGENTS.md` — reglas obligatorias y rama canónica;
- `status.md` — alcance, progreso, tareas finalizadas, pendientes y riesgos;
- `next-step.md` — única próxima acción autorizada;
- `backlog.md` — segmentos y checklist ejecutable;
- `segments/SEG-001.md` — alcance y criterios del segmento activo;
- `validation/SEG-001.md` — matriz principal;
- `main-consolidation.md` — consolidación fast-forward en `main`.

## Evidencias SEG-001

- `validation/SEG-001-static-automation-2026-07-20.md` — YAML, shell, Make y controles aislados;
- `validation/SEG-001-container-build-2026-07-20.md` — primer build real y errores TypeScript;
- `validation/SEG-001-rerun-2026-07-20.md` — exportación cacheada y conflicto 5432;
- `validation/SEG-001-local-orchestration-2026-07-20.md` — tres puertos, orquestador Docker y npm ci condicional;
- `validation/SEG-001-complete-validation-automation-2026-07-20.md` — Maven/Testcontainers contenedorizado, lockfile seguro, seguridad y validador integral;
- `validation/SEG-001-cross-platform-validation-2026-07-20.md` — paridad PowerShell/Bash, propiedad del lockfile Unix y CI/Make.

## Inicio y operación

- `containerized-quickstart.md` — validación integral y stack Docker en Windows/Linux/macOS;
- `local-development-and-usage.md` — procesos separados y flujo funcional;
- `../scripts/README.md` — puertos, preflight, validadores, Maven Docker, smoke, lockfile, seguridad y Makefile;
- `manual-operations.md` — procedimientos operativos diarios;
- `runbook.md` — respuesta ante incidentes;
- `disaster-recovery.md` — backups y restauración;
- `testing.md` — estrategia y comandos de pruebas;
- `deployment.md` — ambientes, gates y rollback.

## Diseño

- `architecture.md` — monolito modular y límites;
- `domain.md` — lenguaje ubicuo e invariantes;
- `data-model.md` — tablas, relaciones y migraciones;
- `roadmap.md` — fases de entrega;
- `adr/0001-modular-monolith.md`;
- `adr/0002-postgresql-source-of-truth.md`;
- `adr/0003-react-vite-frontend.md`;
- `adr/0004-fail-closed-sending.md`;
- `adr/0005-idempotent-imports.md`;
- `adr/0006-preview-parity-and-excluded-metrics.md`.

## Seguridad

- `security.md` — postura técnica y brechas;
- `campaign-safety.md` — guardas futuras;
- `../SECURITY.md` — reporte de vulnerabilidades;
- `gmail-deliverability.md` — diseño futuro de entregabilidad;
- `../scripts/check-repository-safety.ps1` — escaneo local Windows;
- `../scripts/check-repository-safety.sh` — escaneo local Unix.

## Datos e integraciones

- `import-existing-data.md` — incorporación segura del lote operativo;
- `import-hardening.md` — contrato del parser, preview, ejecución y recuperación;
- `google-integration.md` — diseño futuro de OAuth, Gmail, Sheets y Drive.

## Entradas principales

- `../README.md` — inicio rápido y flujo de uso;
- `../Makefile` — comandos repetibles Unix;
- `../CONTRIBUTING.md` — contribución;
- `../CHANGELOG.md` — cambios acumulados.

## Lectura recomendada

### Ejecutar validación integral en Windows

1. `next-step.md`;
2. `../scripts/README.md`;
3. `validation/SEG-001-complete-validation-automation-2026-07-20.md`;
4. `validation/SEG-001-cross-platform-validation-2026-07-20.md`;
5. `validation/SEG-001.md`;
6. `containerized-quickstart.md`.

### Ejecutar validación integral en Linux/macOS

1. `next-step.md`;
2. `../scripts/README.md`;
3. `validation/SEG-001-cross-platform-validation-2026-07-20.md`;
4. `validation/SEG-001.md`;
5. `containerized-quickstart.md`.

### Levantar todo con Docker

1. `../README.md`;
2. `containerized-quickstart.md`;
3. `../scripts/README.md`;
4. `status.md`.

### Levantar procesos separados

1. `../README.md`;
2. `local-development-and-usage.md`;
3. `../scripts/README.md`;
4. `status.md`.

### Cerrar SEG-001

1. `validation/SEG-001.md`;
2. `next-step.md`;
3. `validation/SEG-001-complete-validation-automation-2026-07-20.md`;
4. `validation/SEG-001-cross-platform-validation-2026-07-20.md`;
5. `validation/SEG-001-container-build-2026-07-20.md`;
6. `validation/SEG-001-rerun-2026-07-20.md`;
7. `validation/SEG-001-local-orchestration-2026-07-20.md`;
8. `containerized-quickstart.md`.

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
4. `../scripts/check-repository-safety.ps1` o `.sh`;
5. ADR 0004.

## Regla de precedencia

Cuando exista contradicción:

1. invariantes de seguridad y ADR aceptados;
2. `status.md`;
3. `next-step.md`;
4. segmento y matriz de validación;
5. contratos normativos;
6. guías operativas;
7. resto de documentos.

Actualizar referencias cruzadas al crear, mover o reemplazar documentos.
