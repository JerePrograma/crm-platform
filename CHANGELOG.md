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
- preflight Unix/PowerShell;
- validación de `POSTGRES_HOST_PORT`;
- smoke host y contenedorizado;
- scripts Docker para generar `package-lock.json`;
- Makefile;
- `.dockerignore` y `.gitattributes`;
- CI backend/frontend/scripts/E2E;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencia fechada.

### Changed

- `main` es la única rama canónica;
- SEG-001 se consolidó por fast-forward;
- PostgreSQL host utiliza puerto configurable;
- puerto recomendado cambiado a `55432`;
- `DATABASE_URL` local debe coincidir con `POSTGRES_HOST_PORT`;
- backend contenedorizado conserva `postgres:5432`;
- README y guías usan builds `--no-cache` para evidencia limpia;
- opción `--progress` documentada como flag global de Compose;
- preflight PowerShell tolera UTF-8 BOM;
- CI mantiene caché npm desactivada hasta lockfile;
- validación distingue PASS_FROM_CACHE, clean build, FAIL_FIXED y NOT_RUN.

### Fixed — hardening 2026-07-20

- exclusiones importadas suprimen y auditan;
- preview conserva revisiones ambiguas y aplica exclusiones;
- duplicados exactos enlazan prospecto;
- EXCLUDED separado de accepted;
- correo inválido se rechaza por fila;
- recuperación de filas inválidas;
- límite 10 MB y HTTP 413;
- CSV/headers inválidos rechazados;
- filenames saneados;
- backend/PostgreSQL comparten credenciales;
- smoke PowerShell;
- CI sin caché npm prematura;
- credenciales frontend no anulables;
- imports CSS reconocidos;
- conflicto de puerto host 5432 resuelto mediante `POSTGRES_HOST_PORT`;
- parser `.env` PowerShell tolera BOM.

### Documentation

- estado, backlog, puntero, segmento y validación sincronizados;
- instalación Linux/macOS/Windows/Docker-only;
- health, Swagger y autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- formatos y estados de importación;
- Make, preflight, smoke y lockfile;
- builds limpios y diferencia con caché;
- troubleshooting de puertos Windows;
- evidencia del primer build;
- evidencia de reejecución con imágenes cacheadas y conflicto 5432.

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas SENDING cerradas;
- Maven verificado con SHA-512;
- datos reales fuera de Git/CI/imágenes;
- servicios solo en localhost;
- smoke solo lectura;
- scripts no imprimen secretos ni realizan commits.

### Validation

- Compose/CI YAML: PASS_SYNTAX;
- scripts Unix y Make: PASS_SYNTAX;
- preflight PowerShell real: PASS;
- guardas de envío: PASS;
- npm install frontend: PASS;
- TypeScript inicial: FAIL reproducido;
- errores TypeScript: corregidos;
- frontend image: PASS_FROM_CACHE;
- backend image: PASS_FROM_CACHE;
- stack: FAIL por 5432;
- corrección de puerto: versionada;
- clean builds, stack, Flyway, Hibernate, tests y smoke: pendientes.

Evidencias:

```text
docs/validation/SEG-001-static-automation-2026-07-20.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
```

### Known limitations

- builds limpios pendientes;
- stack pendiente de reejecución con puerto 55432;
- Maven/Spotless/tests/Testcontainers pendientes;
- Flyway/Hibernate pendientes;
- falta package-lock y migración a npm ci;
- CI real no observado verde;
- HTTP Basic temporal;
- RBAC no implementado;
- revisiones sin resolución y jobs sin retry;
- sin campañas/Gmail/Sheets/cloud;
- Compose es local, no producción.
