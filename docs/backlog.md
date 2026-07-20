# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

La rama canónica es `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | `main`, AGENTS, estado, backlog y puntero canónicos |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | Importación, deduplicación, exclusiones, UI y stack local con matriz verde |
| SEG-002 | Identidad, usuarios y RBAC | PLANNED | SEG-001 | Usuarios persistentes y matriz de permisos probada |
| SEG-003 | Ficha integral, contactos, búsqueda y tags | PLANNED | SEG-002 | CRUD completo, filtros combinables y trazabilidad |
| SEG-004 | Campañas, plantillas y adjuntos | PLANNED | SEG-003 | Borrador aprobado, preview y hashes, sin envío |
| SEG-005 | Safety gate y kill switch operativo | PLANNED | SEG-004 | Guardas acumulativas probadas y panel de control |
| SEG-006 | OAuth Google de desarrollo | PLANNED | SEG-002 | Cuenta personal conectada de forma segura |
| SEG-007 | MIME y Gmail fake | PLANNED | SEG-004, SEG-006 | MIME multipart probado contra adaptador fake |
| SEG-008 | Google Sheets bidireccional | PLANNED | SEG-003, SEG-006 | Preview, conflictos, auditoría y DB dominante |
| SEG-009 | Reservas e idempotencia de comunicación | PLANNED | SEG-005, SEG-007 | Reserva única, intentos y reconciliación |
| SEG-010 | Despacho local simulado | PLANNED | SEG-009 | Tareas locales, rate limit y backoff sin Gmail real |
| SEG-011 | Gmail lectura, threads y reconciliación | PLANNED | SEG-006, SEG-009 | IDs Gmail/RFC y estados conciliados |
| SEG-012 | Respuestas, rebotes y bajas | PLANNED | SEG-011 | Clasificación y acciones determinísticas |
| SEG-013 | Seguimientos programados | PLANNED | SEG-012 | Días hábiles, cancelación y `NO_RESPONSE` |
| SEG-014 | Oportunidades, ventas y tareas | PLANNED | SEG-003 | Pipeline, actividades, responsables y vencimientos |
| SEG-015 | Pruebas, pilotos y cotizaciones | PLANNED | SEG-014 | Versionado, estados y conversión |
| SEG-016 | Reporting y dashboard completo | PLANNED | SEG-013, SEG-015 | Métricas comerciales y desempeño temporal |
| SEG-017 | Observabilidad y hardening | PLANNED | SEG-010 | Logs, métricas, trazas, rate limiting y alertas |
| SEG-018 | Terraform base de staging | PLANNED | SEG-017 | APIs, IAM, Registry, Run, SQL, Secrets, red y monitoring |
| SEG-019 | Cloud Tasks, Pub/Sub y Scheduler | PLANNED | SEG-018 | Workers idempotentes y DLQ documentada |
| SEG-020 | Google Workspace y delegación | PLANNED | SEG-018 | Cuenta de servicio y delegación de dominio |
| SEG-021 | Staging E2E | PLANNED | SEG-019, SEG-020 | Flujos completos con límites cerrados |
| SEG-022 | Piloto controlado | PLANNED | SEG-021 | Aprobación humana y límite extremadamente bajo |
| SEG-023 | Producción | PLANNED | SEG-022 | DR, SLO, runbook y despliegue manual aprobado |

## SEG-001 — tareas activas

### Producto finalizado

- [x] backend Java/Spring Boot/PostgreSQL/Flyway;
- [x] dominio de instituciones, contactos, canales, prospectos y exclusiones;
- [x] importaciones CSV/XLSX persistentes;
- [x] preview, ejecución confirmada y evidencia por fila;
- [x] idempotencia, deduplicación exacta y ambigua;
- [x] exclusiones dominantes y retroactivas;
- [x] auditoría;
- [x] API/OpenAPI/RFC 7807;
- [x] frontend operativo;
- [x] resumen UI con `excludedRows` visible como `Bloqueadas`;
- [x] pruebas implementadas;
- [x] documentación técnica.

### Hardening finalizado

- [x] exclusiones importadas auditadas;
- [x] preview con elegibilidad real;
- [x] métricas de bloqueados separadas;
- [x] revisiones ambiguas persistentes;
- [x] duplicados exactos enlazados;
- [x] validación y recuperación por fila;
- [x] parser CSV/XLSX endurecido;
- [x] UTC, límites, HTTP 413 y filenames seguros;
- [x] Basic Auth UTF-8;
- [x] credenciales no anulables dentro del render autenticado;
- [x] tipos Vite e imports CSS declarados.

### Consolidación y operación finalizadas

- [x] todo consolidado en `main` por fast-forward;
- [x] `main` como única fuente canónica;
- [x] entorno DB coherente;
- [x] documentación Windows/Linux/Docker-only;
- [x] perfil `app` PostgreSQL/backend/frontend;
- [x] perfil `smoke` efímero;
- [x] imágenes backend/frontend;
- [x] health checks encadenados;
- [x] proxy Nginx;
- [x] preflight Unix/PowerShell;
- [x] smoke de host Unix/PowerShell;
- [x] smoke E2E contenedorizado;
- [x] Makefile con `smoke-container` y `frontend-lock`;
- [x] scripts Docker de generación de lockfile para Windows/Unix;
- [x] `.dockerignore` y `.gitattributes`;
- [x] README, quickstarts y scripts documentados.

### CI finalizado a nivel de implementación

- [x] backend verify;
- [x] frontend install/typecheck/build;
- [x] sintaxis Unix/PowerShell, incluidos scripts de lockfile;
- [x] preflight fail-closed;
- [x] perfiles Compose app/smoke;
- [x] build de ambas imágenes;
- [x] arranque completo;
- [x] smoke E2E;
- [x] logs en fallo;
- [x] cleanup obligatorio;
- [x] sin caché npm hasta lockfile.

### Evidencia ejecutada

- [x] YAML Compose parsea con servicios esperados;
- [x] YAML CI parsea con job E2E;
- [x] scripts Unix pasan `sh -n`;
- [x] Makefile pasa `make -n`, incluido smoke-container;
- [x] preflight PowerShell `-ContainerOnly` pasa;
- [x] guardas de envío reales verificadas;
- [x] imagen PostgreSQL descargada;
- [x] capas base Maven/Temurin/Node/Nginx alcanzadas;
- [x] `npm install` frontend ejecutado, 24 paquetes;
- [x] frontend TypeScript ejecutado y tres errores reproducidos;
- [x] errores frontend corregidos y leídos desde `main`;
- [x] evidencia real documentada.

### Bloqueantes para cierre

- [ ] actualizar el checkout local a los commits correctivos;
- [ ] reconstruir frontend corregido;
- [ ] confirmar typecheck y Vite build en `PASS`;
- [ ] construir backend de forma aislada;
- [ ] ejecutar `mvn verify`;
- [ ] corregir compilación/Spotless/tests si falla;
- [ ] validar Testcontainers/Flyway/Hibernate;
- [ ] levantar stack y confirmar tres servicios saludables;
- [ ] ejecutar smoke PowerShell y/o contenedorizado;
- [ ] generar y versionar `package-lock.json`;
- [ ] migrar a `npm ci` y caché npm;
- [ ] validar Compose semántico y ambas imágenes;
- [ ] escanear secretos/datos;
- [ ] corregir fallos nuevos y repetir matriz;
- [ ] registrar evidencia completa;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

### Mejoras no bloqueantes

- [ ] resolución auditada de DuplicateReview;
- [ ] retry de ImportJob;
- [ ] filtros/exportación;
- [ ] pruebas HTTP adicionales;
- [ ] concurrencia de idempotencia;
- [ ] accesibilidad;
- [ ] actor/retención de auditoría.

## SEG-002 — preparado, no iniciado

Cuando SEG-001 cierre:

- organización/tenant;
- usuarios persistentes;
- roles `OWNER`, `ADMIN`, `SALES`, `REVIEWER`, `READ_ONLY`;
- hashing y lifecycle de credenciales;
- actor de auditoría;
- autorización por endpoint/método;
- restricciones de exclusiones, auditoría e importación;
- migración desde bootstrap;
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

No comenzar SEG-002 mientras SEG-001 tenga controles bloqueantes pendientes.