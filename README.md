# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

Todo el código y la documentación vigentes están consolidados en `main`.

`SEG-001 — Vertical slice persistente de prospectos` está implementado y endurecido. La validación real produjo:

- preflight Docker: `PASS`;
- primer build frontend: `FAIL` con tres errores TypeScript;
- errores frontend: corregidos en `main`;
- imágenes frontend/backend: exportadas desde caché;
- primer arranque: bloqueado por el puerto host PostgreSQL 5432;
- PostgreSQL, backend y frontend con puertos configurables: implementado;
- validador Docker Windows: implementado;
- build limpio, stack, migraciones, tests, smoke y lockfile: pendientes.

Evidencias:

```text
docs/validation/SEG-001.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
```

Estado detallado: `docs/status.md`.

## Alcance

- backend Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V5 y Hibernate validate;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview, ejecución confirmada y evidencia por fila;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- React, TypeScript y Vite;
- Docker Compose para PostgreSQL, backend, frontend y smoke;
- GitHub Actions, Testcontainers, preflight y smoke tests.

## Seguridad de envío

No existe adaptador Gmail, SMTP o de correo.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL contiene además un kill switch persistente. Ninguna operación disponible puede enviar mensajes.

## Inicio recomendado en Windows: un comando

### Requisitos

- Git;
- Docker Desktop;
- Docker Compose v2.

Java, Node y Maven no son necesarios para validar el stack Docker. Java 21 sí es necesario después para ejecutar Maven verify desde el host.

### 1. Actualizar `main`

```powershell
Set-Location C:\laburo\crm-platform
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

Si `mvnw.cmd` aparece modificado sin intención:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git restore -- mvnw.cmd
```

### 2. Crear `.env` solo si no existe

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Editar las credenciales bootstrap y conservar las guardas de envío.

Variables de puertos:

```dotenv
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

### 3. Ejecutar validación Docker automatizada

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

El script:

1. actualiza puertos sin tocar contraseñas;
2. ejecuta preflight;
3. limpia contenedores incompletos sin borrar volumen;
4. construye frontend y backend sin caché;
5. levanta los tres servicios;
6. espera health checks;
7. ejecuta smoke PowerShell;
8. ejecuta smoke contenedorizado;
9. guarda evidencia en `validation-output/`;
10. deja el stack activo.

Si 8080 o 5173 están ocupados:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Los smoke tests usan automáticamente los puertos configurados.

### 4. Abrir

Con puertos predeterminados:

```text
Frontend: http://localhost:5173
Health:   http://localhost:8080/actuator/health
Swagger:  http://localhost:8080/swagger-ui/index.html
```

Ingresar con `CRM_BOOTSTRAP_USERNAME` y `CRM_BOOTSTRAP_PASSWORD`.

## Validaciones posteriores

### Maven verify

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Debe cubrir compilación, Spotless, unit tests, ArchUnit, Testcontainers, Flyway y Hibernate.

### Generar package-lock

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Revisar:

```powershell
Test-Path frontend\package-lock.json
git status --short
git diff -- frontend\package-lock.json
```

Dockerfile, Makefile y CI ya seleccionan automáticamente:

```text
package-lock presente -> npm ci
package-lock ausente  -> npm install
```

## Inicio manual con Docker

Configurar puertos:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Preflight:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Builds limpios:

```powershell
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

Levantar:

```powershell
docker compose --profile app up -d
docker compose --profile app ps
```

Smoke:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
docker compose --profile app --profile smoke run --rm smoke
```

No utilizar `docker compose down -v` salvo que se pretenda eliminar la base local.

## Linux/macOS

```bash
git switch main
git pull --ff-only
sh scripts/set-local-host-ports.sh 55432 8080 5173
sh scripts/preflight.sh --container-only
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
docker compose --profile app up -d
sh scripts/smoke-test.sh
docker compose --profile app --profile smoke run --rm smoke
```

Maven y lockfile:

```bash
sh ./mvnw -B -f backend/pom.xml verify
sh scripts/generate-frontend-lock.sh
```

## Desarrollo con procesos separados

Requiere Java 21, Docker, Node 22 y npm.

```bash
sh scripts/preflight.sh --local
docker compose up -d postgres
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

En otra terminal:

```bash
cd frontend
if [ -f package-lock.json ]; then npm ci; else npm install; fi
npm run dev
```

Guía completa: `docs/local-development-and-usage.md`.

## Flujo operativo

1. ingresar al Dashboard y comprobar que los envíos figuran bloqueados;
2. registrar canales en `Exclusiones`;
3. preparar CSV o XLSX de hasta 10 MB;
4. ejecutar `Preview`;
5. revisar `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
6. corregir el archivo;
7. ejecutar `Importar con confirmación`;
8. revisar prospectos, elegibilidad y métrica `Bloqueadas`;
9. revisar `Auditoría`.

La ejecución exige:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

## Formatos de importación

- CSV UTF-8 con coma o punto y coma;
- XLSX con hoja `Prospectos` y hoja opcional `Exclusiones`;
- máximo 10 MB;
- CSV requiere `Institución`;
- parser por encabezados normalizados.

Los datos reales no se versionan.

## API principal

```text
GET  /actuator/health
GET  /api/v1/prospects
POST /api/v1/prospects
GET  /api/v1/prospects/{id}
POST /api/v1/imports/prospects/preview
POST /api/v1/imports/prospects/execute
GET  /api/v1/imports/prospects/{jobId}
GET  /api/v1/imports/prospects/{jobId}/rows
GET  /api/v1/imports/prospects/duplicate-reviews/pending
GET  /api/v1/exclusions
POST /api/v1/exclusions
GET  /api/v1/exclusions/{id}
GET  /api/v1/audit
```

## Automatización Make

```bash
make preflight
make preflight-container
make postgres-port
make local-ports
make db-up
make app-up
make app-logs
make frontend-lock
make smoke
make smoke-container
make verify
make app-down
```

Detalles: `scripts/README.md`.

## Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar base local:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

La segunda operación es destructiva.

## Limitaciones actuales

- clean builds posteriores a las correcciones pendientes;
- PowerShell nuevo pendiente de ejecución real;
- Flyway, Hibernate, tests y smoke pendientes;
- falta package-lock versionado;
- HTTP Basic temporal;
- sin usuarios persistentes/RBAC;
- sin resolución UI de DuplicateReview;
- sin retry de trabajos fallidos;
- sin campañas, Gmail, Sheets, workers o cloud;
- Compose es local, no producción.

## Documentación

- `docs/README.md` — índice;
- `docs/status.md` — alcance, progreso, tareas y riesgos;
- `docs/next-step.md` — siguiente acción;
- `docs/containerized-quickstart.md` — Docker;
- `docs/local-development-and-usage.md` — procesos separados y flujo;
- `scripts/README.md` — automatización;
- `docs/validation/SEG-001.md` — matriz.
