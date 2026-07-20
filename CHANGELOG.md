# Changelog

Todos los cambios relevantes se documentan aquí. El proyecto todavía no tiene una versión publicada.

## Unreleased

### Added

- Java 21, Spring Boot, Maven Wrapper verificado, PostgreSQL y Flyway;
- configuración de envío fail-closed y kill switch persistente;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes, idempotentes y auditadas;
- preview, ejecución confirmada, estados y métricas por fila;
- cola de revisión de duplicados;
- API REST, OpenAPI, RFC 7807 y auditoría JSONB;
- autenticación bootstrap fail-closed;
- frontend React/TypeScript/Vite;
- `frontend/src/vite-env.d.ts` para tipos Vite e imports CSS;
- visualización `Bloqueadas` para `excludedRows`;
- perfil Compose `app` con PostgreSQL/backend/frontend;
- perfil Compose `smoke` con verificación E2E efímera;
- imagen backend multi-stage con health;
- imagen frontend multi-stage con Nginx/proxy;
- preflight Unix/PowerShell local y container-only;
- smoke de host Unix/PowerShell;
- smoke E2E contenedorizado;
- scripts Docker para generar `package-lock.json` en Windows y Unix;
- target Make `frontend-lock`;
- Makefile incluido `smoke-container`;
- `.dockerignore` raíz/frontend;
- `.gitattributes` multiplataforma;
- CI con backend, frontend, scripts y compose-images-and-smoke;
- validación sintáctica CI de scripts de lockfile;
- fixtures/Testcontainers/ArchUnit;
- documentación técnica y operativa;
- quickstarts Docker y procesos separados;
- evidencia estática de automatización;
- evidencia del primer build contenedorizado real;
- evidencia de consolidación en main.

### Changed

- `main` es la única rama canónica;
- SEG-001 se consolidó por fast-forward sin force;
- README presenta dos modalidades;
- DB local consume variables coherentes desde `.env`;
- puertos publicados solo en loopback;
- CI valida perfiles app/smoke y ejecuta smoke E2E;
- CI imprime logs en fallo y limpia siempre;
- caché npm desactivada hasta lockfile;
- contextos Docker excluyen secretos/datos/cachés;
- resultados de importación ordenados por hoja/fila;
- Basic Auth frontend usa UTF-8;
- CSV admite coma/punto y coma;
- fechas Excel en UTC;
- el puntero canónico ahora exige reconstruir frontend corregido antes de continuar con backend;
- validación diferencia `FAIL`, `FIXED_PENDING_RERUN`, `CANCELED` y `NOT_RUN`.

### Fixed — hardening 2026-07-20

- exclusiones importadas usan el caso de uso manual, suprimen y auditan;
- preview conserva revisiones ambiguas y aplica exclusiones;
- duplicados exactos enlazan prospecto existente;
- `EXCLUDED` separado de accepted;
- correo inválido se rechaza por fila;
- recuperación no vuelve a fallar;
- límites multipart/funcional 10 MB y HTTP 413;
- CSV/headers inválidos se rechazan;
- filenames se saneán;
- backend/PostgreSQL comparten credenciales;
- smoke PowerShell corregido;
- CI evita configuración npm incompatible sin lockfile;
- callbacks autenticados ya no reciben `Credentials | null`;
- TypeScript reconoce imports CSS mediante tipos Vite;
- la corrección conserva `strict: true` y no desactiva `strictNullChecks`.

### Documentation

- estado/backlog/puntero/segmento/validación sincronizados;
- instalación Linux/macOS/Windows/Docker-only;
- health/Swagger/autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- formatos/estados de importación;
- Make/preflight/smoke host y container;
- generación de lockfile sin Node en el host;
- logs, cleanup, reset y troubleshooting;
- tareas finalizadas, pendientes y riesgos;
- primer build real documentado en `docs/validation/SEG-001-container-build-2026-07-20.md`.

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas `SENDING_*` cerradas;
- Maven verificado con SHA-512;
- datos reales fuera de Git/CI/imágenes;
- contextos Docker sin `.env`, planillas o claves;
- servicios solo en localhost;
- smoke realiza lecturas;
- CI usa credenciales ficticias y elimina volumen;
- scripts de lockfile no realizan commits ni imprimen secretos.

### Validation

- Compose YAML con cuatro servicios: `PASS_SYNTAX`;
- CI YAML con job E2E: `PASS_SYNTAX`;
- scripts Unix `sh -n`: `PASS`;
- Makefile `make -n`, incluido smoke-container: `PASS`;
- preflight PowerShell container-only real: `PASS`;
- guardas de envío reales: `PASS`;
- pull de PostgreSQL y capas base: `PASS/PARTIAL_PASS`;
- npm install frontend real: `PASS`, 24 paquetes;
- frontend TypeScript inicial: `FAIL` reproducido;
- tres errores frontend: corregidos y pendientes de reejecución;
- backend: `CANCELED`, todavía sin resultado;
- evidencia estática: `docs/validation/SEG-001-static-automation-2026-07-20.md`;
- evidencia real: `docs/validation/SEG-001-container-build-2026-07-20.md`.

### Known limitations

- frontend corregido todavía debe reconstruirse;
- backend, Flyway, Hibernate, tests y smoke aún no fueron alcanzados por el primer build;
- CI real todavía no fue observado verde;
- falta `package-lock.json`;
- imagen frontend usa `npm install` hasta lockfile;
- PowerShell smoke todavía no fue ejecutado;
- RBAC no implementado;
- revisiones sin resolución y jobs sin retry;
- sin campañas/Gmail/Sheets/cloud;
- Compose es local, no producción.