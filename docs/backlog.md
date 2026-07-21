# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

Rama canónica: `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | fuente canónica, reglas y documentación |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | importación, exclusiones, UI y stack con matriz verde |
| SEG-002 | Identidad, usuarios y RBAC | PLANNED | SEG-001 | usuarios persistentes y permisos probados |
| SEG-003 | Ficha integral, búsqueda y tags | PLANNED | SEG-002 | CRUD y filtros completos |
| SEG-004 | Campañas, plantillas y adjuntos | PLANNED | SEG-003 | borrador y preview, sin envío |
| SEG-005 | Safety gate y kill switch operativo | PLANNED | SEG-004 | guardas acumulativas y panel |
| SEG-006 | OAuth Google de desarrollo | PLANNED | SEG-002 | cuenta conectada de forma segura |
| SEG-007 | MIME y Gmail fake | PLANNED | SEG-004, SEG-006 | MIME probado contra fake |
| SEG-008 | Google Sheets bidireccional | PLANNED | SEG-003, SEG-006 | preview, conflictos y auditoría |
| SEG-009 | Idempotencia de comunicación | PLANNED | SEG-005, SEG-007 | reservas e intentos únicos |
| SEG-010 | Despacho local simulado | PLANNED | SEG-009 | rate limit y backoff sin Gmail real |
| SEG-011 | Gmail lectura y reconciliación | PLANNED | SEG-006, SEG-009 | threads e IDs conciliados |
| SEG-012 | Respuestas, rebotes y bajas | PLANNED | SEG-011 | clasificación determinística |
| SEG-013 | Seguimientos programados | PLANNED | SEG-012 | programación y cancelación |
| SEG-014 | Oportunidades, ventas y tareas | PLANNED | SEG-003 | pipeline y vencimientos |
| SEG-015 | Pruebas, pilotos y cotizaciones | PLANNED | SEG-014 | versionado y conversión |
| SEG-016 | Reporting completo | PLANNED | SEG-013, SEG-015 | métricas comerciales |
| SEG-017 | Observabilidad y hardening | PLANNED | SEG-010 | logs, métricas y alertas |
| SEG-018 | Terraform staging | PLANNED | SEG-017 | infraestructura base |
| SEG-019 | Tasks, Pub/Sub y Scheduler | PLANNED | SEG-018 | workers y DLQ |
| SEG-020 | Workspace y delegación | PLANNED | SEG-018 | delegación de dominio |
| SEG-021 | Staging E2E | PLANNED | SEG-019, SEG-020 | flujos completos cerrados |
| SEG-022 | Piloto controlado | PLANNED | SEG-021 | aprobación humana y límite bajo |
| SEG-023 | Producción | PLANNED | SEG-022 | DR, SLO y despliegue aprobado |

## SEG-001 — producto finalizado

- [x] backend Java/Spring Boot/PostgreSQL/Flyway;
- [x] instituciones, contactos, canales y prospectos;
- [x] exclusiones dominantes y retroactivas;
- [x] importaciones CSV/XLSX persistentes;
- [x] preview y ejecución confirmada;
- [x] idempotencia;
- [x] deduplicación exacta y ambigua;
- [x] auditoría;
- [x] API/OpenAPI/RFC 7807;
- [x] frontend responsive;
- [x] filas bloqueadas visibles;
- [x] pruebas implementadas;
- [x] documentación técnica.

## SEG-001 — hardening finalizado

- [x] parser CSV/XLSX endurecido;
- [x] validación y recuperación por fila;
- [x] límites 10 MB y HTTP 413;
- [x] fechas UTC;
- [x] filenames seguros;
- [x] exclusiones importadas auditadas;
- [x] preview con elegibilidad real;
- [x] revisiones ambiguas persistentes;
- [x] duplicados exactos enlazados;
- [x] Basic Auth UTF-8;
- [x] nullability frontend corregida;
- [x] tipos Vite/CSS;
- [x] TypeScript strict conservado.

## SEG-001 — consolidación finalizada

- [x] todo en `main`;
- [x] historia conservada;
- [x] rama histórica sin cambios exclusivos;
- [x] fuentes canónicas sincronizadas;
- [x] evidencias reales fechadas;
- [x] datos operativos reales fuera del repositorio.

## SEG-001 — infraestructura y operación finalizadas

- [x] perfiles Compose `app` y `smoke`;
- [x] imágenes backend/frontend;
- [x] health checks encadenados;
- [x] Nginx y proxy;
- [x] puertos en loopback en la definición Compose;
- [x] tres puertos host configurables;
- [x] preflight Unix/PowerShell;
- [x] daemon Docker fail-fast;
- [x] validación de puertos y URL;
- [x] configurador conjunto de puertos;
- [x] wrappers retrocompatibles;
- [x] smoke host Unix/PowerShell;
- [x] smoke contenedorizado;
- [x] validador Docker Windows;
- [x] transcripts y JSON fuera de Git;
- [x] Makefile;
- [x] CI E2E preparado.

## SEG-001 — automatización completa finalizada

- [x] backend Maven verify/Testcontainers en Docker;
- [x] código backend montado en solo lectura;
- [x] target Maven efímero;
- [x] caché Maven reutilizable;
- [x] generación package-lock-only;
- [x] lifecycle scripts npm deshabilitados;
- [x] verificación de ausencia de node_modules;
- [x] propiedad UID/GID del lockfile preservada en Unix;
- [x] npm ci automático con lockfile;
- [x] validador integral PowerShell;
- [x] validador integral Bash;
- [x] rama main y árbol limpio como precondiciones;
- [x] SHA-256 del lockfile;
- [x] repetición smoke después de npm ci;
- [x] evidencia JSON estructurada;
- [x] escaneo centralizado de seguridad Unix/PowerShell;
- [x] checker local `check-powershell-syntax.ps1`;
- [x] regresión CI contra `$LASTEXITCODE:`;
- [x] normalización de `mvnw.cmd`;
- [x] targets Make relevantes;
- [x] CI con sintaxis POSIX, Bash y PowerShell;
- [x] checker combinado de publicaciones Docker y enlace Windows;
- [x] diagnóstico de propietario Docker por ID/nombre/puertos;
- [x] PostgreSQL iniciado y validado antes de builds;
- [x] `stackKeptRunning` veraz en ambos validadores;
- [x] diagnóstico `docker ps` en fallos.

## SEG-001 — evidencia ejecutada real

### Aprobada

- [x] preflight Windows inicial;
- [x] guardas de envío;
- [x] descarga de imágenes base;
- [x] npm install;
- [x] fallo TypeScript reproducido;
- [x] correcciones aplicadas;
- [x] imágenes exportadas desde caché;
- [x] conflicto 5432 reproducido;
- [x] checkout actualizado por fast-forward;
- [x] fallo de parser PowerShell reproducido;
- [x] parser corregido y luego ejecutado;
- [x] PowerShell syntax PASS para 11 scripts sobre `f903a9e`;
- [x] preflight container-only PASS sobre `f903a9e`;
- [x] checker Windows PASS para 15432/8080/5173;
- [x] frontend clean build `--no-cache` PASS;
- [x] TypeScript strict PASS;
- [x] Vite production build PASS;
- [x] backend clean image build `--no-cache` PASS;
- [x] Maven package con tests omitidos PASS;
- [x] fallo Docker `port is already allocated` reproducido;
- [x] evidencia JSON/transcript del intento 5 generada.

### Estática o funcional aislada

- [x] YAML Compose/CI revisado;
- [x] scripts POSIX y Make revisados;
- [x] configurador Unix preservando secretos ficticios, UTF-8 y guardas;
- [x] backend verify Unix revisado;
- [x] lockfile seguro Unix revisado;
- [x] seguridad del repositorio revisada;
- [x] read-back remoto de validadores integrales;
- [x] paridad Windows/Unix documentada;
- [x] checker Docker/Windows nuevo revisado por código;
- [x] orden PostgreSQL-antes-de-builds revisado por código;
- [x] mensajes `KeepRunning` nuevos revisados por código.

### Pendiente de ejecución nueva

- [ ] actualizar al commit con hardening de propiedad Docker;
- [ ] ejecutar `check-powershell-syntax.ps1` sobre el nuevo `main`;
- [ ] inspeccionar `docker ps` y resolver propietario de `15432`;
- [ ] ejecutar checker Docker/Windows actualizado;
- [ ] confirmar que PostgreSQL inicia antes de builds;
- [ ] ejecutar funcionalmente el validador integral completo;
- [ ] ejecutar `bash -n scripts/validate-seg001.sh` mediante CI o checkout;
- [ ] ejecutar target Make integral mediante CI o checkout.

## SEG-001 — bloqueantes de cierre

- [ ] actualizar checkout local al último `main`;
- [ ] confirmar árbol rastreado limpio;
- [ ] confirmar sintaxis PowerShell tras el último hardening;
- [ ] resolver publicación Docker del puerto PostgreSQL;
- [ ] confirmar PostgreSQL publication/health;
- [ ] confirmar backend/frontend healthy;
- [ ] confirmar Flyway V1–V5;
- [ ] confirmar Hibernate validate;
- [ ] confirmar smoke host y contenedor;
- [ ] confirmar Maven verify;
- [ ] confirmar Spotless;
- [ ] confirmar unit tests;
- [ ] confirmar ArchUnit;
- [ ] confirmar Testcontainers;
- [ ] revisar JSON y transcript;
- [ ] revisar y versionar `frontend/package-lock.json`;
- [ ] repetir desde árbol limpio con lockfile versionado;
- [ ] confirmar npm ci desde el inicio;
- [ ] confirmar seguridad del repositorio;
- [ ] observar CI verde o documentar su indisponibilidad;
- [ ] registrar evidencia final;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

## Mejoras no bloqueantes

- [ ] resolución auditada de `DuplicateReview`;
- [ ] retry de `ImportJob`;
- [ ] filtros y exportación;
- [ ] pruebas HTTP adicionales;
- [ ] concurrencia de idempotencia;
- [ ] accesibilidad;
- [ ] actor y retención de auditoría.

## SEG-002 — preparado, no iniciado

Cuando SEG-001 cierre:

- organización/tenant;
- usuarios persistentes;
- roles `OWNER`, `ADMIN`, `SALES`, `REVIEWER`, `READ_ONLY`;
- hashing y ciclo de credenciales;
- actor de auditoría;
- autorización por endpoint y método;
- administración de usuarios;
- migración desde bootstrap;
- matriz de permisos.

## Priorización

1. seguridad;
2. datos;
3. idempotencia y exclusiones;
4. compilación y migraciones;
5. pruebas y reproducibilidad;
6. operación;
7. UX;
8. optimización.

No comenzar SEG-002 mientras SEG-001 conserve controles bloqueantes.
