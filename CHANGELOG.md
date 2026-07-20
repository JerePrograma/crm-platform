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
- tipos Vite/CSS;
- visualización `Bloqueadas` para `excludedRows`;
- perfiles Compose `app` y `smoke`;
- imágenes backend/frontend multi-stage;
- Nginx y proxy;
- health checks encadenados;
- preflight Unix/PowerShell;
- smoke host Unix/PowerShell;
- smoke contenedorizado;
- Makefile;
- `.dockerignore` raíz/frontend;
- `.gitattributes` multiplataforma;
- generadores Docker de `package-lock.json`;
- configuradores coordinados de puertos Windows/Unix;
- orquestador Docker Windows con clean builds, health, smoke y transcript;
- `validation-output/` ignorado por Git;
- CI con backend, frontend, scripts y stack E2E;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencias fechadas.

### Changed

- `main` es la única rama canónica;
- SEG-001 se consolidó por fast-forward sin force;
- PostgreSQL, backend y frontend publican puertos host configurables;
- valores predeterminados: 55432, 8080 y 5173;
- preflight valida tres puertos válidos, distintos y coherentes;
- smoke deriva URLs desde `.env`;
- helpers de PostgreSQL delegan al configurador conjunto;
- README presenta el validador Docker Windows como ruta principal;
- Dockerfile frontend usa `npm ci` cuando existe lockfile y `npm install` cuando falta;
- CI y Makefile aplican la misma selección npm;
- CI usa puertos alternativos para reducir conflictos;
- builds de cierre exigen `--no-cache`;
- contextos Docker excluyen secretos, datos y cachés;
- puertos publicados solo en loopback;
- resultados de importación ordenados por hoja/fila;
- Basic Auth frontend usa UTF-8;
- CSV admite coma/punto y coma;
- fechas Excel usan UTC;
- documentación diferencia PASS, FAIL, CACHED, NOT_RUN y PENDING.

### Fixed

- exclusiones importadas usan el caso de uso manual, suprimen y auditan;
- preview conserva revisiones ambiguas y aplica exclusiones;
- duplicados exactos enlazan prospecto existente;
- `EXCLUDED` está separado de accepted;
- correo inválido se rechaza por fila;
- recuperación no vuelve a fallar;
- límites multipart/funcional 10 MB y HTTP 413;
- CSV y headers inválidos se rechazan;
- filenames se saneán;
- backend y PostgreSQL comparten credenciales;
- smoke PowerShell;
- CI sin caché npm prematura;
- callbacks autenticados no reciben `Credentials | null`;
- imports CSS reconocidos por TypeScript;
- `strict: true` permanece activo;
- conflicto del puerto PostgreSQL 5432 mediante puerto host configurable;
- riesgo de BOM en `.env` PowerShell;
- transición incompleta de helper PostgreSQL a preflight de tres puertos;
- comandos Compose con `--progress` en posición correcta.

### Validation

Ejecución real acumulada:

- preflight PowerShell inicial: `PASS`;
- guardas de envío: `PASS`;
- npm install frontend: `PASS`;
- frontend TypeScript inicial: `FAIL` reproducido;
- tres errores frontend: corregidos;
- imágenes frontend/backend: `PASS_FROM_CACHE`;
- clean builds: pendientes;
- arranque: `FAIL` por 5432;
- puertos coordinados: implementados;
- configurador Unix: `PASS_FUNCTIONAL_ISOLATED` preservando secretos ficticios, UTF-8 y guardas;
- orquestador Windows: pendiente de ejecución;
- Maven/Testcontainers/Flyway/Hibernate/smoke: pendientes;
- GitHub Actions: sin run visible desde el conector.

Evidencias:

```text
docs/validation/SEG-001-static-automation-2026-07-20.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
```

### Documentation

- estado, backlog, siguiente paso, segmento y matriz sincronizados;
- instalación Windows/Linux/macOS/Docker-only;
- puertos variables y diagnóstico;
- validador Docker de un comando;
- health, Swagger y autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- formatos y estados de importación;
- Make, preflight, smoke y lockfile;
- logs, cleanup, reset y troubleshooting;
- alcance, tareas finalizadas, pendientes y riesgos.

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas `SENDING_*` cerradas;
- Maven Wrapper verificado con SHA-512;
- datos reales fuera de Git, CI e imágenes;
- contextos Docker sin `.env`, planillas o claves;
- servicios solo en localhost;
- smoke realiza lecturas;
- scripts de puertos preservan contraseñas;
- transcripts locales fuera de Git;
- CI usa credenciales ficticias y elimina su volumen.

### Known limitations

- frontend/backend clean builds pendientes;
- PowerShell nuevo pendiente de ejecución real;
- Maven, tests, migraciones y smoke no están verdes;
- falta `package-lock.json` versionado;
- npm ci está preparado, pero depende del lockfile;
- HTTP Basic temporal;
- revisiones sin resolución y jobs sin retry;
- sin campañas, Gmail, Sheets o cloud;
- Compose es local, no producción.
