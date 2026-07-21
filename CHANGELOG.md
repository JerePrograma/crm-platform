# Changelog

Todos los cambios relevantes se documentan aquí. El proyecto todavía no tiene una versión publicada.

## Unreleased

### Added

- Java 21, Spring Boot 4.1, Maven Wrapper, PostgreSQL y Flyway;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes, idempotentes y auditadas;
- preview, ejecución confirmada y métricas por fila;
- API REST, OpenAPI, RFC 7807 y auditoría JSONB;
- autenticación bootstrap fail-closed;
- frontend React/TypeScript/Vite;
- perfiles Compose `app` y `smoke`;
- imágenes backend/frontend multi-stage;
- Nginx, proxy y health checks encadenados;
- preflight Unix/PowerShell;
- smoke host y contenedorizado;
- Makefile y `.dockerignore` raíz/frontend;
- `.gitattributes` multiplataforma;
- configuradores coordinados de puertos Windows/Unix;
- checker PowerShell de sintaxis;
- checker Windows de enlace y propiedad Docker de puertos;
- validador Docker Windows con clean builds, health, smoke y evidencia JSON;
- validadores integrales PowerShell y Bash;
- backend Maven verify/Testcontainers contenedorizado;
- generadores Docker de `package-lock.json`;
- escaneo centralizado del repositorio Windows/Unix;
- CI con backend, frontend, scripts, regresiones de puertos y stack E2E;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencias fechadas.

### Changed

- `main` es la única rama canónica;
- PostgreSQL, backend y frontend publican puertos host configurables solo en loopback;
- valores predeterminados: 55432, 8080 y 5173;
- preflight valida daemon Docker, puertos, URL DB, credenciales y guardas;
- validadores integrales exigen `main` y working tree limpio;
- builds de cierre exigen `--no-cache`;
- Maven verify puede ejecutarse sin Java instalado en el host;
- Dockerfile frontend usa `npm ci` cuando existe lockfile y `npm install` cuando falta;
- el validador inicia PostgreSQL y espera health antes de reconstruir imágenes;
- `stackKeptRunning` refleja contenedores realmente activos;
- resultados de importación se ordenan por hoja/fila;
- Basic Auth frontend usa UTF-8;
- CSV admite coma y punto y coma;
- fechas Excel usan UTC;
- documentación diferencia PASS, FAIL, PASS_PARTIAL, NOT_RUN y PENDING;
- `org.flywaydb:flyway-core` directo fue reemplazado por `org.springframework.boot:spring-boot-starter-flyway` para habilitar la auto-configuración de Spring Boot 4;
- se conserva `org.flywaydb:flyway-database-postgresql`;
- `spring.flyway.fail-on-missing-locations=true` activa fail-fast cuando faltan recursos de migración.

### Fixed

- exclusiones importadas usan el caso de uso manual, suprimen y auditan;
- preview conserva revisiones ambiguas y aplica exclusiones;
- duplicados exactos enlazan prospecto existente;
- `EXCLUDED` está separado de accepted;
- correo inválido se rechaza por fila;
- límites multipart/funcional 10 MB y HTTP 413;
- CSV y headers inválidos se rechazan;
- filenames se saneán;
- backend y PostgreSQL comparten credenciales;
- callbacks autenticados no reciben `Credentials | null`;
- imports CSS reconocidos por TypeScript;
- conflicto inicial del puerto PostgreSQL 5432 mediante puerto configurable;
- riesgo de BOM en `.env` PowerShell;
- comandos Compose con `--progress` en posición correcta;
- splatting de parámetros del validador integral;
- escaneo del lote operativo en cualquier subdirectorio;
- generadores de lockfile no crean `node_modules` ni ejecutan lifecycle scripts;
- propiedad Unix del lockfile;
- rutas con espacios en el escaneo Unix;
- interpolación inválida `$LASTEXITCODE:` en scripts PowerShell;
- detección tardía de puertos reservados u ocupados en Windows;
- detección de puertos publicados por otros contenedores Docker;
- mensaje engañoso de `-KeepRunning` cuando no existía stack activo;
- ausencia de auto-configuración Flyway en Spring Boot 4 que dejaba el esquema vacío y provocaba `Schema validation: missing table [contact]`.

### Validation

Ejecución real acumulada:

- preflight PowerShell inicial: PASS;
- guardas de envío: PASS;
- npm install frontend: PASS;
- frontend TypeScript inicial: FAIL reproducido y corregido;
- imágenes frontend/backend desde caché: PASS_FROM_CACHE en una ejecución temprana;
- arranque inicial: FAIL por puerto 5432;
- PowerShell parser: FAIL reproducido, corregido y luego PASS sobre 11 scripts;
- frontend clean build sin caché: PASS;
- backend clean image build sin caché: PASS;
- intento con `55432`: FAIL por bind Windows;
- intento con `15432`: FAIL porque Docker tenía el puerto asignado;
- checker Docker/Windows actualizado: PASS sobre 25432/8080/5173;
- publicación PostgreSQL en 25432: PASS;
- PostgreSQL health: PASS;
- frontend clean build `--no-cache`: PASS;
- TypeScript strict/Vite production build: PASS;
- backend clean image build `--no-cache`: PASS;
- Maven package con tests omitidos: PASS_PARTIAL;
- conexión backend → PostgreSQL 17.10: PASS;
- sexta ejecución integral: FAIL por ausencia de auto-configuración Flyway;
- migraciones V1–V5: NOT_RUN en esa ejecución;
- Hibernate validate: FAIL por tabla `contact` ausente;
- backend/frontend health y smoke: NOT_RUN;
- Maven verify, Spotless, unit tests, ArchUnit y Testcontainers: NOT_RUN;
- package-lock y npm ci: NOT_RUN;
- working tree posterior: PASS, limpio;
- corrección `spring-boot-starter-flyway`: IMPLEMENTED_NOT_RUN;
- GitHub Actions: sin estado visible desde el conector.

Evidencias principales:

```text
docs/validation/SEG-001-static-automation-2026-07-20.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
docs/validation/SEG-001-powershell-parser-failure-2026-07-21.md
docs/validation/SEG-001-port-bind-failure-2026-07-21.md
docs/validation/SEG-001-docker-port-owner-failure-2026-07-21.md
docs/validation/SEG-001-flyway-autoconfiguration-failure-2026-07-21.md
```

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas `SENDING_*` cerradas;
- Maven Wrapper verificado;
- datos reales fuera de Git, CI e imágenes;
- contextos Docker sin `.env`, planillas o claves;
- servicios solo en localhost;
- smoke realiza lecturas;
- lockfile se genera con lifecycle scripts deshabilitados;
- scripts de seguridad bloquean entorno, evidencia, datos privados, lote, claves y credenciales;
- transcripts locales fuera de Git;
- CI usa credenciales ficticias;
- ninguna validación habilita comunicaciones.

### Known limitations

- la corrección Flyway todavía no fue ejecutada localmente;
- Flyway V1–V5, Hibernate validate y tres servicios healthy aún no están verdes;
- smoke host/contenedor no está ejecutado después del fix;
- Maven verify y Testcontainers continúan pendientes;
- falta `frontend/package-lock.json` versionado;
- npm ci está preparado, pero pendiente de evidencia real;
- GitHub Actions no muestra runs visibles desde el conector;
- HTTP Basic es temporal;
- revisiones ambiguas no tienen resolución UI;
- jobs sin retry;
- sin campañas, Gmail, Sheets o cloud;
- Compose es local, no producción.
