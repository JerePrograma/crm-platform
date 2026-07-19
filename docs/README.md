# Índice de documentación

## Continuidad

- `../AGENTS.md` — reglas obligatorias para sesiones;
- `status.md` — estado real;
- `next-step.md` — única próxima acción;
- `backlog.md` — segmentos y dependencias;
- `segments/SEG-001.md` — checklist activo;
- `validation/SEG-001.md` — evidencia de ejecución.

## Diseño

- `architecture.md` — monolito modular y límites;
- `domain.md` — lenguaje ubicuo e invariantes;
- `data-model.md` — tablas, relaciones y migraciones;
- `roadmap.md` — fases de entrega;
- `adr/` — decisiones arquitectónicas.

## Seguridad y operación

- `security.md` — postura técnica y brechas;
- `campaign-safety.md` — guardas futuras de campañas;
- `manual-operations.md` — procedimientos diarios;
- `runbook.md` — incidentes;
- `disaster-recovery.md` — backups y restauración;
- `testing.md` — estrategia y estado de pruebas.

## Datos e integraciones

- `import-existing-data.md` — importación segura;
- `google-integration.md` — OAuth, Workspace, Gmail, Sheets y Drive;
- `gmail-deliverability.md` — autenticación de dominio, volumen y métricas.

## Entrega

- `deployment.md` — ambientes, gates y rollback;
- `../README.md` — inicio rápido;
- `../CONTRIBUTING.md` — reglas de contribución;
- `../SECURITY.md` — reporte de vulnerabilidades;
- `../CHANGELOG.md` — cambios.

## Regla

Cuando exista contradicción:

1. seguridad y ADR aceptados;
2. `status.md`;
3. `next-step.md`;
4. documentación de segmento;
5. resto de documentos.

Actualizar referencias cruzadas al mover o reemplazar documentos.
