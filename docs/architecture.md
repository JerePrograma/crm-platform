# Arquitectura

Actualizado: 2026-07-22

## Unidad de despliegue

Gestudio CRM es un monolito modular Java 21/Spring Boot 4.1 con un frontend
React/TypeScript y PostgreSQL 17 como única fuente de verdad. No usa Kafka,
Redis, Elasticsearch, Kubernetes ni microservicios. El perfil local y el perfil
productivo local construyen las mismas aplicaciones; el segundo endurece redes,
usuarios, filesystem, límites y configuración, pero no constituye un despliegue.

```text
Browser
  -> frontend nginx no-root
  -> /api/v1
  -> Spring Boot modular monolith no-root
       identity/security/settings
       prospect/contact/exclusion/import/deduplication
       activity/sales/campaign/messaging
       outbox/inbound/reporting/audit
  -> PostgreSQL 17
```

Gmail y WhatsApp están detrás de puertos internos. Sus adaptadores existen y
tienen contract tests locales, pero no se conectan ni se inicializan en los
perfiles ejecutables de esta misión. `NOOP`, `FAKE` y `FAKE_INBOUND` son las
únicas fronteras ejecutadas.

## Persistencia y migraciones

- Flyway V1–V13 es forward-only; V1–V11 permanecen inmutables.
- V12 agrega outbox e inbound durable; V13 agrega configuración operativa,
  etiquetas e índices de reporting/búsqueda.
- Hibernate usa `ddl-auto=validate`.
- UUID, `organization_id`, optimistic version y timestamps UTC forman parte del
  contrato persistente.
- Un rollback productivo usa imagen compatible o forward-fix; un restore se
  realiza solo desde backup verificado y sobre una base aislada antes de decidir
  una recuperación destructiva.

## Límites modulares

- `identity` y `security`: sesión, roles, permisos, actor y tenant.
- `prospect`, `contact`, `exclusion`, `imports`, `deduplication`: captación,
  elegibilidad, datos operativos e importación segura.
- `activity`: notas, actividades, tareas y timeline.
- `sales`: oportunidades, pipeline, aging y cierres.
- `campaign` y `messaging`: audiencia congelada, plantillas, simulación y policy
  fail-closed.
- `outbox`: publicación transaccional, claim, lease, retry, dead-letter y
  administración.
- `inbound`: HMAC, replay, normalización, asociación, quarantine y efectos de
  dominio sin respuesta automática.
- `reporting`: lecturas agregadas tenant-scoped, CSV y métricas operativas.
- `settings` y `prospect` tags: configuración y datos maestros acotados.
- `audit` y `common`: auditoría, correlation ID, errores y utilidades sin
  secretos.

ArchUnit valida que controllers no accedan repositorios directamente y que los
límites de infraestructura no inviertan las dependencias. Los controllers usan
DTOs explícitos y los errores HTTP usan Problem Details.

## Outbox e inbound

La garantía del outbox es at-least-once, no exactly-once. El publisher escribe
en la misma transacción de dominio. Un claim corto usa PostgreSQL
`FOR UPDATE SKIP LOCKED`, asigna un lease y confirma antes del procesamiento. La
finalización usa compare-and-set; los locks vencidos se recuperan. Idempotencia
tenant-scoped evita efectos duplicados.

Los workers reevaluan kill switches, configuración, campaña, exclusiones,
consentimiento, contacto, elegibilidad, permisos y provider. Nunca mantienen
una transacción abierta durante una futura llamada de red. El webhook fake
persiste un receipt idempotente y publica el inbound antes de responder; el
worker asocia, crea actividad/tarea/timeline y detiene secuencia cuando aplica.

Decisión completa: `docs/adr/0010-postgresql-outbox-and-durable-inbound.md`.

## Búsqueda, reporting y operación

La búsqueda usa PostgreSQL, parámetros enlazados, allow-lists de orden/filtros,
paginación y GIN trigram para institución, contacto, canal, ubicación, website y
notas. Las etiquetas son tenant-scoped. Reporting agrega en SQL y agrupa valores
por moneda; nunca suma monedas diferentes.

Micrometer expone health/liveness/readiness y métricas Prometheus. Health es
público; `/actuator/metrics` y demás endpoints operativos requieren sesión. El
filtro de correlation/request ID propaga contexto a logs, auditoría y outbox sin
registrar cuerpos, tokens o contactos completos.

## Runtime y perfil productivo

El backend runtime usa una JRE Chainguard fijada por digest, usuario `65532` y
una sonda Java sin shell. El frontend usa nginx no-root. El perfil productivo
local usa filesystems read-only, `tmpfs`, red privada para PostgreSQL/backend,
healthchecks, resource limits y shutdown ordenado. TLS termina en un reverse
proxy provider-neutral no incluido.

Documentación operativa: `docs/production/` y `docs/runbooks/`.

## Decisiones externas pendientes

- proveedor/entorno de despliegue y dominio TLS;
- secretos y base productiva;
- OAuth Gmail y cuenta verificada de WhatsApp Cloud;
- política legal de retención por jurisdicción.

Ninguna de esas decisiones es necesaria para ejecutar localmente el CRM con
datos sintéticos y comunicaciones bloqueadas.
