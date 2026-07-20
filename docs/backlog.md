# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada.

Rama canónica: `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | `main` y documentación canónica |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | matriz técnica verde |
| SEG-002 | Identidad, usuarios y RBAC | PLANNED | SEG-001 | usuarios persistentes y permisos probados |
| SEG-003 | Ficha integral, contactos, búsqueda y tags | PLANNED | SEG-002 | CRUD, filtros y trazabilidad |
| SEG-004 | Campañas, plantillas y adjuntos | PLANNED | SEG-003 | borrador y preview sin envío |
| SEG-005 | Safety gate y kill switch operativo | PLANNED | SEG-004 | guardas probadas |
| SEG-006 | OAuth Google de desarrollo | PLANNED | SEG-002 | conexión segura |
| SEG-007 | MIME y Gmail fake | PLANNED | SEG-004, SEG-006 | MIME probado con fake |
| SEG-008 | Google Sheets bidireccional | PLANNED | SEG-003, SEG-006 | preview y conflictos |
| SEG-009 | Reservas de comunicación | PLANNED | SEG-005, SEG-007 | idempotencia |
| SEG-010 | Despacho local simulado | PLANNED | SEG-009 | rate limit/backoff sin Gmail |
| SEG-011 | Gmail lectura y threads | PLANNED | SEG-006, SEG-009 | reconciliación |
| SEG-012 | Respuestas, rebotes y bajas | PLANNED | SEG-011 | acciones determinísticas |
| SEG-013 | Seguimientos programados | PLANNED | SEG-012 | cancelación y NO_RESPONSE |
| SEG-014 | Oportunidades, ventas y tareas | PLANNED | SEG-003 | pipeline |
| SEG-015 | Pruebas, pilotos y cotizaciones | PLANNED | SEG-014 | conversión |
| SEG-016 | Reporting | PLANNED | SEG-013, SEG-015 | métricas |
| SEG-017 | Observabilidad y hardening | PLANNED | SEG-010 | logs/métricas/trazas |
| SEG-018 | Terraform staging | PLANNED | SEG-017 | infraestructura |
| SEG-019 | Tasks, Pub/Sub y Scheduler | PLANNED | SEG-018 | workers/DLQ |
| SEG-020 | Workspace y delegación | PLANNED | SEG-018 | cuenta de servicio |
| SEG-021 | Staging E2E | PLANNED | SEG-019, SEG-020 | flujos completos |
| SEG-022 | Piloto controlado | PLANNED | SEG-021 | aprobación humana |
| SEG-023 | Producción | PLANNED | SEG-022 | DR/SLO/runbook |

## SEG-001 — finalizado a nivel de implementación

### Producto

- [x] backend Java/Spring Boot/PostgreSQL/Flyway;
- [x] instituciones, contactos, canales, prospectos y exclusiones;
- [x] importaciones CSV/XLSX;
- [x] preview y ejecución confirmada;
- [x] idempotencia y deduplicación;
- [x] exclusiones dominantes;
- [x] auditoría;
- [x] API/OpenAPI/RFC 7807;
- [x] frontend;
- [x] `excludedRows` visible;
- [x] pruebas implementadas;
- [x] documentación.

### Hardening

- [x] exclusiones importadas auditadas;
- [x] preview con elegibilidad;
- [x] revisiones ambiguas;
- [x] duplicados exactos enlazados;
- [x] recuperación por fila;
- [x] parser endurecido;
- [x] UTC, 10 MB, HTTP 413 y filenames;
- [x] Basic Auth UTF-8;
- [x] credenciales frontend no anulables;
- [x] tipos Vite/CSS.

### Operación

- [x] todo consolidado en `main`;
- [x] perfiles app/smoke;
- [x] imágenes backend/frontend;
- [x] Nginx y health checks;
- [x] preflight y smoke multiplataforma;
- [x] Makefile;
- [x] generadores de lockfile;
- [x] contextos Docker seguros;
- [x] puerto PostgreSQL host configurable;
- [x] valor recomendado 55432;
- [x] preflight valida puerto/URL;
- [x] parser PowerShell tolera BOM.

### CI

- [x] backend verify;
- [x] frontend install/typecheck/build;
- [x] sintaxis Unix/PowerShell;
- [x] preflight fail-closed;
- [x] Compose app/smoke;
- [x] imágenes, stack y smoke;
- [x] logs y cleanup;
- [x] sin caché npm hasta lockfile.

## Evidencia ejecutada

- [x] YAML Compose/CI;
- [x] shell y Make;
- [x] preflight PowerShell real;
- [x] guardas de envío reales;
- [x] npm install frontend;
- [x] fallo TypeScript reproducido;
- [x] errores TypeScript corregidos;
- [x] imágenes frontend/backend exportadas desde caché;
- [x] conflicto de puerto 5432 reproducido;
- [x] corrección de puerto versionada;
- [x] evidencias fechadas.

## Bloqueantes para cierre

- [ ] actualizar local al último `main`;
- [ ] revisar/restaurar `mvnw.cmd` local;
- [ ] configurar POSTGRES_HOST_PORT=55432;
- [ ] actualizar DATABASE_URL a 55432;
- [ ] repetir preflight;
- [ ] build frontend sin caché;
- [ ] build backend sin caché;
- [ ] levantar stack;
- [ ] confirmar PostgreSQL/backend/frontend healthy;
- [ ] validar Flyway/Hibernate;
- [ ] smoke PowerShell;
- [ ] smoke contenedorizado;
- [ ] `mvn verify`/Spotless/tests/Testcontainers;
- [ ] generar/versionar package-lock;
- [ ] migrar a npm ci y caché npm;
- [ ] repetir imágenes/smoke;
- [ ] escanear secretos/datos;
- [ ] registrar evidencia final;
- [ ] marcar SEG-001 COMPLETE;
- [ ] activar SEG-002.

## Mejoras no bloqueantes

- [ ] resolución auditada de DuplicateReview;
- [ ] retry de ImportJob;
- [ ] filtros/exportación;
- [ ] pruebas HTTP;
- [ ] concurrencia de idempotencia;
- [ ] accesibilidad;
- [ ] actor/retención de auditoría.

## SEG-002 — preparado, no iniciado

Después del cierre:

- organización/tenant;
- usuarios persistentes;
- roles OWNER, ADMIN, SALES, REVIEWER y READ_ONLY;
- hashing de credenciales;
- actor de auditoría;
- autorización por endpoint;
- matriz de permisos;
- administración de usuarios.

## Priorización

1. seguridad;
2. datos;
3. idempotencia/exclusiones;
4. compilación/migraciones;
5. pruebas/reproducibilidad;
6. operación;
7. UX;
8. optimización.

No comenzar SEG-002 mientras existan bloqueantes.
