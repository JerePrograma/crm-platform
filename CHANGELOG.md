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
- configuradores coordinados de puertos Windows/Unix;
- wrappers compatibles de puerto PostgreSQL;
- validador Docker Windows con clean builds, health y smoke;
- evidencia Docker JSON y transcript;
- backend Maven verify/Testcontainers contenedorizado para Windows/Unix;
- caché Maven local en volumen Docker;
- target Maven efímero y código montado en solo lectura;
- generadores Docker de `package-lock.json`;
- validador integral PowerShell `scripts/validate-seg001.ps1`;
- validador integral Bash `scripts/validate-seg001.sh`;
- evidencia integral JSON y transcript;
- escaneo centralizado del repositorio Windows/Unix;
- targets Make `repository-safety`, `backend-verify-container`, `verify-container` y `validate-seg001`;
- `validation-output/` ignorado por Git;
- CI con backend, frontend, scripts y stack E2E;
- CI con sintaxis POSIX, Bash, parser PowerShell y parseo Make;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencias fechadas;
- evidencia de paridad `SEG-001-cross-platform-validation-2026-07-20.md`.

### Changed

- `main` es la única rama canónica;
- SEG-001 se consolidó por fast-forward sin force;
- la rama histórica está detrás y no tiene cambios exclusivos;
- PostgreSQL, backend y frontend publican puertos host configurables;
- valores predeterminados: 55432, 8080 y 5173;
- preflight valida tres puertos válidos, distintos y coherentes;
- preflight exige que el daemon Docker responda;
- smoke deriva URLs desde `.env`;
- helpers de PostgreSQL delegan al configurador conjunto;
- README y quickstart usan validadores integrales como ruta principal;
- Dockerfile frontend usa `npm ci` cuando existe lockfile y `npm install` cuando falta;
- CI y Makefile aplican la misma selección npm;
- CI usa puertos alternativos para reducir conflictos;
- CI parsea todos los scripts PowerShell nuevos;
- CI ejecuta `bash -n` sobre el validador integral Unix;
- CI expande targets Make relevantes;
- CI ejecuta el escaneo centralizado de seguridad;
- builds de cierre exigen `--no-cache`;
- validadores integrales exigen `main` y working tree limpio;
- validadores integrales permiten únicamente el cambio esperado de package-lock;
- Maven verify puede ejecutarse sin Java instalado en el host;
- generador Unix de lockfile conserva UID/GID del usuario;
- generador Unix usa caché npm temporal dentro del contenedor;
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
- comandos Compose con `--progress` en posición correcta;
- splatting de parámetros del validador integral PowerShell;
- escaneo del lote operativo ampliado a cualquier subdirectorio;
- generadores de lockfile ya no crean node_modules ni ejecutan lifecycle scripts;
- lockfile Unix ya no debe quedar propiedad de `root`;
- rutas con espacios conservadas por el escaneo Unix.

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
- backend verify Unix: revisión/sintaxis previa `PASS`;
- generador lockfile seguro Unix: revisión/sintaxis previa `PASS`;
- Make `backend-verify-container`: `PASS_PARSE`;
- Make `verify-container`: `PASS_PARSE`;
- CI YAML: revisión/parseo previo `PASS`;
- validador integral PowerShell: pendiente de ejecución;
- validador integral Bash: implementado, pendiente de `bash -n` por CI/checkout y ejecución funcional;
- generador Unix con UID/GID: pendiente de sintaxis/ejecución sobre checkout actualizado;
- Maven/Testcontainers/Flyway/Hibernate/smoke: pendientes;
- GitHub Actions: sin run visible desde el conector.

Evidencias:

```text
docs/validation/SEG-001-static-automation-2026-07-20.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
```

### Documentation

- estado, backlog, siguiente paso, segmento y matriz sincronizados;
- instalación Windows/Linux/macOS/Docker-only;
- puertos variables y diagnóstico;
- validador Docker y validadores integrales Windows/Unix;
- Maven/Testcontainers sin Java local;
- package-lock-only, propiedad Unix y npm ci;
- formato de evidencia JSON/transcript;
- escaneo de seguridad;
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
- lockfile se genera con lifecycle scripts deshabilitados;
- `node_modules` inesperado produce fallo;
- scripts de seguridad bloquean `.env`, evidencia, datos privados, lote, claves y credenciales rastreados;
- transcripts locales fuera de Git;
- CI usa credenciales ficticias y elimina su volumen;
- verificación backend con socket Docker documentada como operación privilegiada sobre código confiable.

### Known limitations

- frontend/backend clean builds pendientes;
- validadores integrales pendientes de ejecución real;
- Maven, tests, migraciones y smoke no están verdes;
- falta `package-lock.json` versionado;
- npm ci está preparado, pero pendiente de evidencia real;
- GitHub Actions no muestra runs visibles;
- HTTP Basic temporal;
- revisiones sin resolución y jobs sin retry;
- sin campañas, Gmail, Sheets o cloud;
- Compose es local, no producción.
