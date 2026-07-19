# Roadmap

## Principios

- entregar vertical slices verificables;
- mantener envíos cerrados hasta autorización explícita;
- PostgreSQL como fuente de verdad;
- reglas determinísticas antes que IA;
- modular monolith antes que microservicios;
- staging antes de cualquier piloto;
- producción siempre con aprobación manual.

## Fase 0 — Fundación

Estado: `COMPLETE`

- rama real;
- continuidad documental;
- backlog y estado canónicos.

## Fase 1 — Datos comerciales seguros

Segmentos: SEG-001 a SEG-003.

Resultados:

- prospectos, instituciones, contactos y exclusiones;
- importación y deduplicación;
- identidad y RBAC;
- búsqueda, tags, propietarios y ficha integral.

Gate de salida:

- CI verde;
- datos reales fuera de Git;
- permisos probados;
- importación de lote autorizada y auditable.

## Fase 2 — Preparación de campañas sin envío

Segmentos: SEG-004 y SEG-005.

Resultados:

- campañas borrador;
- plantillas versionadas;
- adjuntos y hashes;
- preview HTML/texto;
- aprobación;
- safety gate acumulativo;
- kill switch UI/API.

Gate:

- ningún adaptador real;
- pruebas exhaustivas de guardas;
- dry-run verificable.

## Fase 3 — Google en desarrollo y simulación

Segmentos: SEG-006 a SEG-010.

Resultados:

- OAuth personal;
- Gmail/Sheets adapters;
- MIME;
- reservas e idempotencia;
- despacho local fake.

Gate:

- secretos seguros;
- cero mensajes reales;
- reconciliación probada con fakes.

## Fase 4 — Conversaciones y pipeline

Segmentos: SEG-011 a SEG-016.

Resultados:

- lectura Gmail;
- respuestas, rebotes y bajas;
- seguimientos;
- oportunidades, tareas, trials y cotizaciones;
- reporting.

Gate:

- decisiones críticas determinísticas;
- auditoría completa;
- métricas operativas.

## Fase 5 — Cloud staging

Segmentos: SEG-017 a SEG-021.

Resultados:

- observabilidad y hardening;
- Terraform;
- Cloud Run/SQL/Tasks/PubSub/Scheduler/Secrets;
- Google Workspace;
- E2E staging.

Gate:

- IAM mínimo;
- DR probado;
- alertas;
- límites de envío en cero.

## Fase 6 — Piloto controlado

Segmento: SEG-022.

Resultados:

- autorización explícita;
- límite extremadamente bajo;
- aprobación humana por campaña;
- monitoreo de rebotes/bajas;
- postmortem y decisión de continuidad.

## Fase 7 — Producción

Segmento: SEG-023.

Resultados:

- despliegue manual;
- SLO;
- runbooks;
- backups y restauración;
- seguridad y entregabilidad operativas;
- soporte comercial.

No existe fecha comprometida. Cada fase depende de evidencia técnica y autorización comercial.
