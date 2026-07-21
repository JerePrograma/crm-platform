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
- tipos Vite/CSS y visualización `Bloqueadas`;
- perfiles Compose `app` y `smoke`;
- imágenes backend/frontend multi-stage;
- Nginx, proxy y health checks encadenados;
- preflight Unix/PowerShell;
- smoke host y contenedorizado;
- Makefile y `.dockerignore` raíz/frontend;
- `.gitattributes` multiplataforma;
- configuradores coordinados de puertos Windows/Unix;
- validador Docker Windows con clean builds, health, smoke y evidencia JSON;
- backend Maven verify/Testcontainers contenedorizado para Windows/Unix;
- caché Maven local, target efímero y código montado en solo lectura;
- generadores Docker de `package-lock.json`;
- validadores integrales PowerShell y Bash;
- checker local `scripts/check-powershell-syntax.ps1`;
- checker Windows `scripts/check-host-ports.ps1` para publicaciones Docker y enlace real en loopback;
- diagnóstico de ID, nombre y puertos del contenedor propietario;
- evidencia integral JSON y transcript;
- escaneo centralizado del repositorio Windows/Unix;
- targets Make `repository-safety`, `backend-verify-container`, `verify-container` y `validate-seg001`;
- CI con backend, frontend, scripts y stack E2E;
- CI con sintaxis POSIX/Bash/PowerShell, parseo Make y prueba del checker de puertos;
- regresión CI contra `$LASTEXITCODE:`;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencias fechadas;
- evidencia de paridad `SEG-001-cross-platform-validation-2026-07-20.md`;
- evidencia `SEG-001-powershell-parser-failure-2026-07-21.md`;
- evidencia `SEG-001-port-bind-failure-2026-07-21.md`;
- evidencia `SEG-001-docker-port-owner-failure-2026-07-21.md`.

### Changed

- `main` es la única rama canónica;
- SEG-001 se consolidó por fast-forward sin force;
- la rama histórica está detrás y no tiene cambios exclusivos;
- PostgreSQL, backend y frontend publican puertos host configurables;
- valores predeterminados: 55432, 8080 y 5173;
- preflight valida puertos, URL DB, daemon Docker y guardas;
- smoke deriva URLs desde `.env`;
- README y quickstart usan validadores integrales como ruta principal;
- Dockerfile frontend usa `npm ci` cuando existe lockfile y `npm install` cuando falta;
- CI y Makefile aplican la misma selección npm;
- CI usa puertos alternativos para reducir conflictos;
- CI ejecuta el checker PowerShell compartido y el checker de enlace de puertos;
- CI ejecuta `bash -n` sobre el validador integral Unix;
- CI expande targets Make y ejecuta seguridad;
- builds de cierre exigen `--no-cache`;
- validadores integrales exigen `main` y working tree limpio;
- validadores integrales permiten únicamente el cambio esperado de package-lock;
- Maven verify puede ejecutarse sin Java instalado en el host;
- generador Unix de lockfile conserva UID/GID;
- `mvnw.cmd` fue renormalizado para `*.cmd text eol=crlf`;
- el checker de puertos consulta `docker ps` antes de probar `TcpListener`;
- el validador Docker ejecuta cleanup y verifica propiedad Docker/enlace Windows antes de builds;
- PostgreSQL se inicia y debe alcanzar health antes de construir frontend/backend;
- evidencia Docker registra `hostPorts` y `postgresBinding`;
- ambos validadores imprimen publicaciones Docker en fallos;
- `-KeepRunning` solo informa stack conservado cuando hay contenedores ejecutándose en ambos niveles;
- contextos Docker excluyen secretos, datos y cachés;
- puertos se declaran solo en loopback;
- resultados de importación se ordenan por hoja/fila;
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
- conflicto del puerto PostgreSQL 5432 mediante puerto configurable;
- riesgo de BOM en `.env` PowerShell;
- comandos Compose con `--progress` en posición correcta;
- splatting de parámetros del validador integral;
- escaneo del lote operativo en cualquier subdirectorio;
- generadores de lockfile no crean node_modules ni ejecutan lifecycle scripts;
- lockfile Unix no queda propiedad de `root`;
- rutas con espacios en el escaneo Unix;
- interpolación inválida `$LASTEXITCODE:` en tres scripts;
- detección tardía de puertos ocupados o reservados en Windows;
- falso negativo cuando Docker Desktop ya tenía publicado un puerto no visible para `TcpListener`;
- builds desperdiciados antes de descubrir que PostgreSQL no podía publicar el puerto;
- mensaje engañoso de `-KeepRunning` en el validador Docker;
- mensaje engañoso de `-KeepRunning` en el orquestador integral.

### Validation

Ejecución real acumulada:

- preflight PowerShell inicial: `PASS`;
- guardas de envío: `PASS`;
- npm install frontend: `PASS`;
- frontend TypeScript inicial: `FAIL` reproducido;
- tres errores frontend: corregidos;
- imágenes frontend/backend: `PASS_FROM_CACHE` en la segunda ejecución;
- arranque inicial: `FAIL` por 5432;
- checkout Windows actualizado por fast-forward el 2026-07-21: `PASS`;
- intento 3: `EXECUTED_FAIL — POWERSHELL_PARSE_ERROR`;
- parser corregido y `check-powershell-syntax.ps1`: `EXECUTED_PASS`;
- preflight container-only endurecido: `EXECUTED_PASS`;
- intento 4: frontend/backend clean builds `PASS`, arranque `FAIL` por rango excluido `55432`;
- checker Windows de puertos: implementado y luego ejecutado;
- intento 5 sobre `f903a9e1278697af53e0bcbee3bd10b16e10b991`;
- PowerShell syntax para 11 scripts: `EXECUTED_PASS`;
- preflight, Docker daemon y Compose config: `EXECUTED_PASS`;
- guardas de envío: `EXECUTED_PASS`;
- `TcpListener` para 15432/8080/5173: `EXECUTED_PASS`;
- frontend clean build sin caché: `EXECUTED_PASS`;
- TypeScript/Vite production build: `EXECUTED_PASS`;
- frontend image export: `EXECUTED_PASS`;
- backend clean image build sin caché: `EXECUTED_PASS`;
- Maven package con tests omitidos: `EXECUTED_PASS_PARTIAL`;
- intento 5: `EXECUTED_FAIL — DOCKER_HOST_PORT_ALREADY_ALLOCATED` en `15432`;
- PostgreSQL health, backend/frontend health, Flyway, Hibernate y smoke: `NOT_RUN`;
- Maven verify, Spotless, unit tests, ArchUnit y Testcontainers: `NOT_RUN`;
- package-lock y npm ci: `NOT_RUN`;
- JSON y transcript de fallo: `GENERATED`;
- checker de publicaciones Docker: implementado, ejecución local pendiente;
- PostgreSQL-antes-de-builds: implementado, ejecución local pendiente;
- mensajes `KeepRunning` veraces: implementados, ejecución local pendiente;
- validador integral Bash: implementado, ejecución pendiente;
- GitHub Actions: sin run visible desde el conector.

Evidencias:

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
```

### Documentation

- estado, backlog, siguiente paso, segmento y matriz sincronizados;
- instalación Windows/Linux/macOS/Docker-only;
- puertos variables, publicaciones Docker, enlace real y diagnóstico de rangos excluidos;
- validadores integrales Windows/Unix;
- checker de sintaxis y checker combinado de puertos;
- Maven/Testcontainers sin Java local;
- package-lock-only, propiedad Unix y npm ci;
- formato de evidencia JSON/transcript;
- seguridad, health, Swagger y autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- logs, cleanup, reset y troubleshooting;
- alcance, tareas finalizadas, pendientes y riesgos.

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas `SENDING_*` cerradas;
- Maven Wrapper verificado con SHA-512;
- datos reales fuera de Git, CI e imágenes;
- contextos Docker sin `.env`, planillas o claves;
- servicios declarados solo en localhost;
- smoke realiza lecturas;
- scripts de puertos preservan contraseñas;
- lockfile se genera con lifecycle scripts deshabilitados;
- scripts de seguridad bloquean entorno, evidencia, datos privados, lote, claves y credenciales;
- transcripts locales fuera de Git;
- CI usa credenciales ficticias;
- verificación backend con socket Docker documentada como operación privilegiada;
- ninguna validación habilita comunicaciones.

### Known limitations

- el hardening de propiedad Docker todavía no fue ejecutado en el checkout Windows;
- debe identificarse o detenerse el contenedor que publica `15432`, o elegirse otro puerto;
- stack, health, migraciones y smoke no están verdes;
- Maven verify y Testcontainers no están ejecutados;
- falta `package-lock.json` versionado;
- npm ci está preparado, pero pendiente de evidencia real;
- GitHub Actions no muestra runs visibles;
- HTTP Basic temporal;
- revisiones sin resolución y jobs sin retry;
- sin campañas, Gmail, Sheets o cloud;
- Compose es local, no producción.
