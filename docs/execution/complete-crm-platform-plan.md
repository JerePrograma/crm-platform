# Plan integral de ejecución del CRM

Actualizado: 2026-07-22

## Alcance

Esta ejecución completó SEG-002–SEG-009, implementó el candidato SEG-010 y
activó SEG-011 para su validación limpia repetida. El objetivo sigue siendo una
operación comercial completa sin desplegar producción ni habilitar
comunicaciones reales.

Incluye identidad persistente, aislamiento por organización, prospectos y contactos operativos,
timeline, tareas, duplicados, oportunidades, campañas simuladas, mensajería fail-closed, outbox,
inbound sintético, reportes, seguridad, observabilidad, backup/restore, perfil productivo local,
CI, documentación y validación reproducible.

## Arquitectura actual

- monolito modular Spring Boot 4.1 sobre Java 21;
- PostgreSQL 17, Flyway V1–V13 y Hibernate `validate`;
- módulos `prospect`, `institution`, `contact`, `exclusion`, `imports`,
  `deduplication`, `activity`, `sales`, `campaign`, `messaging`, `outbox`,
  `inbound`, `reporting`, `audit`, `security`, `settings` y `common`;
- frontend React 19, TypeScript strict y Vite;
- Docker Compose local, Maven/Testcontainers, smoke y validadores Windows/Unix;
- importación CSV/XLSX idempotente, exclusión dominante y revisión/merge
  persistente;
- sesión cookie/CSRF, usuarios persistentes, RBAC y tenant isolation;
- outbox/inbound fake, reporting/search/settings/tags y perfil productivo local.

## Brechas encontradas

| Área | Estado de inicio | Brecha concreta |
|---|---|---|
| identidad | parcial | Basic stateless, sin usuario persistente, sesión, roles ni bloqueo |
| organización | ausente | todas las tablas son globales |
| prospectos/contactos | parcial | creación/listado; sin edición, archivo, contactos operativos ni búsqueda |
| ciclo comercial | parcial | enum amplio, transiciones arbitrarias y sin historial |
| timeline/tareas | ausente | sin notas, actividades, tareas o próxima acción operativa |
| duplicados | parcial | bandeja read-only; sin resolución o merge |
| ventas | ausente | sin oportunidades ni pipeline |
| campañas | esquema inerte | tablas V1 sin casos de uso, audiencia, aprobación o simulación |
| mensajería | ausente | sin contratos, providers, policy, outbox o inbound |
| auditoría | parcial | payload JSONB sin actor, organización o correlation ID |
| frontend | parcial | cinco secciones, credenciales Basic en memoria, sin tests |
| reportes/búsqueda | ausente | no hay agregaciones operativas ni búsqueda integral |
| seguridad | parcial | sin CSRF para sesión, rate limit, headers o threat model completo |
| operación | parcial | health y logs; sin backup/restore probado ni perfil productivo |
| CI | parcial | cuatro jobs agregados; sin validador integral del CRM completo |

## Arquitectura objetivo

Se conserva un único despliegue y PostgreSQL como fuente de verdad. Los límites internos son:

```text
identity -> organization
prospect -> contact, exclusion, audit
activity -> prospect, identity
deduplication -> importing, prospect, audit
sales -> prospect, identity, activity
campaign -> prospect, contact, messaging
messaging -> settings, outbox, audit
integration -> messaging
reporting -> consultas read-only
shared -> contratos técnicos comunes
```

No se introducen Kafka, Redis, Elasticsearch, Kubernetes ni microservicios. PostgreSQL cubre
persistencia, búsqueda, locking, idempotencia y outbox. Los proveedores externos quedan detrás de
puertos internos y deshabilitados por configuración.

## Fases y checkpoints

1. baseline, inventario y documentación viva;
2. SEG-002: organización, usuarios, sesiones y RBAC;
3. SEG-003: prospectos, contactos y ciclo comercial;
4. SEG-004: actividades, notas, tareas y timeline;
5. SEG-005: resolución y merge de duplicados;
6. SEG-006: oportunidades y pipeline;
7. SEG-007: campañas, audiencias, plantillas y simulación;
8. SEG-008: policy de mensajería, no-op/fake/manual y adaptadores reales no conectados;
9. SEG-009: outbox, workers e inbound sintético;
10. SEG-010: frontend, reportes, configuración, seguridad, observabilidad y producción local;
11. SEG-011: validación integral repetible y cierre documental.

SEG-009 quedó ejecutado el 2026-07-22 con V12, 69/69 pruebas backend y E2E
outbox/inbound. SEG-010 tiene V13, 79/79 y focales operativos verdes, pero se
mantiene `IMPLEMENTED_NOT_RUN` hasta el validador integral. SEG-011 es el único
segmento activo.

Cada checkpoint exige prueba focalizada, compilación, Flyway/Hibernate, diff y escaneo de
seguridad. Un estado escrito pero no ejecutado queda `IMPLEMENTED_NOT_RUN`.

## Riesgos

- el cambio de autenticación afecta smoke, frontend y todos los tests de integración;
- agregar `organization_id` a datos existentes requiere bootstrap/backfill determinístico;
- la migración de unicidades globales a unicidades por tenant debe preservar SEG-001;
- merge, workers y campañas concentran riesgo transaccional e idempotencia;
- una UI integral puede degradar accesibilidad si no mantiene navegación y foco simples;
- los adaptadores reales no pueden probarse sin credenciales y cuentas externas;
- el validador completo es costoso y debe separar evidencia funcional de chequeos estructurales.

## Decisiones

- monolito modular y despliegue único;
- sesión same-origin con cookie `HttpOnly`, CSRF y rotación al autenticar;
- contraseñas con encoder delegado de Spring Security;
- tenant explícito en persistencia y principal autenticado, nunca elegido libremente por cliente;
- Flyway forward-only desde V6;
- PostgreSQL para búsqueda, outbox y locking;
- providers `NOOP`, `FAKE` y manual habilitables; providers de red apagados y fail-closed;
- plantillas con variables allow-list y reemplazo determinístico, sin lenguaje de scripting;
- tiempos persistidos en UTC y `Clock` inyectable para lógica programada;
- datos reales nunca ingresan al repositorio ni a evidencia versionada.

Las decisiones de sesión/tenant, mensajería y outbox se registran en ADR específicos al
implementar cada límite.

## Dependencias externas

Pueden quedar `BLOCKED_EXTERNAL` solamente:

- OAuth y cuenta Gmail/Workspace;
- cuenta, token, business account y phone number de WhatsApp Cloud API;
- dominio, DNS, TLS y proveedor de hosting;
- secretos y base de datos productivos;
- ejecución funcional Unix si no existe host compatible;
- preview del XLSX real si el archivo externo no está accesible.

## Criterios de aceptación

Una capacidad pasa solo con modelo/migración, autorización, API, UI cuando corresponde,
auditoría, errores, pruebas, documentación y ejecución desde entorno limpio. Los recorridos de
envío deben terminar exclusivamente en borrador, simulación o bloqueo.

## Estrategia de migración

- no editar V1-V5;
- migraciones aditivas y ordenadas desde V6;
- crear organización bootstrap y backfill de filas existentes antes de `NOT NULL`/FK;
- cambiar restricciones globales solo después del backfill;
- índices tenant-first para consultas operativas;
- validar desde esquema vacío y desde V1-V5 con datos sintéticos;
- migraciones voluminosas separan columna, backfill, constraint e índice cuando corresponda.

## Estrategia de rollback

Flyway Community no ejecuta down migrations. Cada cambio documenta:

- rollback de aplicación a imagen anterior cuando el esquema sea compatible;
- forward-fix para errores de datos o constraints;
- backup antes de despliegue futuro;
- restore solo desde copia verificada y en procedimiento de incidente;
- ningún borrado físico de prospectos absorbidos, auditoría o outbox.

## Estrategia de pruebas

- unitarias para normalización, transiciones, renderer, policy y backoff;
- integración PostgreSQL/Testcontainers para repositorios, tenant, Flyway y transacciones;
- MockMvc para sesión, CSRF, permisos, Problem Details y webhooks;
- pruebas frontend y E2E sobre stack real cuando la herramienta quede incorporada;
- contract tests locales con fake providers/servidores sin red externa;
- restore drill con datos sintéticos;
- validador final Windows dos veces sin caché y validación Bash según entorno disponible.

## Política de datos reales

- fixtures con dominios `.test` y datos sintéticos;
- XLSX real siempre fuera del repositorio, CI, imágenes y logs;
- preview real solo después del cierre sintético;
- evidencia real exclusivamente agregada y sanitizada;
- no ejecutar importación definitiva de datos operativos sin autorización específica.

## Política de comunicaciones

Las cuatro guardas ambientales permanecen cerradas:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL conserva además `sending.kill-switch=true`. Cada salida reevalúa organización,
permiso, elegibilidad, exclusión, campaña, plantilla, límites, ventana, idempotencia y ambos kill
switches. Los providers de red no son seleccionables en test ni local por defecto.

## Definición de terminado

El CRM se considera integralmente cerrado cuando el recorrido comercial sintético completo pasa,
el validador se repite desde árbol limpio, backup/restore está probado, la matriz diferencia
providers conectados/bloqueados y producción continúa `NOT_DEPLOYED`.
