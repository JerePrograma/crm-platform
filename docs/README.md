# Índice de documentación

## Continuidad

- `../AGENTS.md` — reglas obligatorias y rama canónica;
- `status.md` — estado real, tareas finalizadas, pendientes y riesgos;
- `next-step.md` — única próxima acción autorizada;
- `backlog.md` — segmentos, dependencias y checklist ejecutable;
- `segments/SEG-001.md` — alcance y criterios del segmento activo;
- `validation/SEG-001.md` — matriz principal de evidencia y bloqueos;
- `validation/SEG-001-static-automation-2026-07-20.md` — controles YAML, shell y Make ejecutados;
- `validation/SEG-001-container-build-2026-07-20.md` — primer preflight/build Docker real, errores frontend y correcciones;
- `main-consolidation.md` — consolidación fast-forward del trabajo en `main`.

## Inicio y operación

- `containerized-quickstart.md` — PostgreSQL, backend, frontend y smoke E2E con Docker Compose;
- `local-development-and-usage.md` — procesos separados, variables, health, UI, flujo y troubleshooting;
- `../scripts/README.md` — preflight, smoke tests, generación de lockfile y Makefile;
- `manual-operations.md` — procedimientos operativos diarios;
- `runbook.md` — respuesta ante incidentes;
- `disaster-recovery.md` — backups y restauración;
- `testing.md` — estrategia, comandos y estado de pruebas;
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
- `campaign-safety.md` — guardas futuras de campañas;
- `../SECURITY.md` — reporte de vulnerabilidades;
- `gmail-deliverability.md` — autenticación de dominio, volumen y métricas futuras.

## Datos e integraciones

- `import-existing-data.md` — incorporación segura del lote operativo;
- `import-hardening.md` — contrato normativo del parser, preview, ejecución, métricas y recuperación;
- `google-integration.md` — diseño futuro de OAuth, Workspace, Gmail, Sheets y Drive.

## Entrada principal

- `../README.md` — inicio rápido desde `main`;
- `../Makefile` — comandos repetibles para entornos Unix con Make;
- `../CONTRIBUTING.md` — reglas de contribución;
- `../CHANGELOG.md` — cambios acumulados.

## Lectura recomendada por tarea

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

### Reintentar el build corregido

1. `next-step.md`;
2. `validation/SEG-001-container-build-2026-07-20.md`;
3. `validation/SEG-001.md`;
4. `containerized-quickstart.md`;
5. `../scripts/README.md`.

### Ejecutar la validación de cierre

1. `validation/SEG-001.md`;
2. `next-step.md`;
3. `containerized-quickstart.md`;
4. `../scripts/README.md`;
5. `validation/SEG-001-static-automation-2026-07-20.md`;
6. `validation/SEG-001-container-build-2026-07-20.md`.

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

Cuando exista contradicción:

1. invariantes de seguridad y ADR aceptados;
2. `status.md`;
3. `next-step.md`;
4. documentación de segmento y validación;
5. contratos normativos de módulo;
6. guías operativas;
7. resto de documentos.

Actualizar referencias cruzadas al mover, crear o reemplazar documentos.