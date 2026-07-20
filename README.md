# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

Todo el código y la documentación vigentes están consolidados en `main`.

```text
SEG-000: COMPLETE
SEG-001: ACTIVE — implementación completa, validación verde pendiente
SEG-002: PLANNED
```

Evidencia real disponible:

- preflight Docker: `PASS`;
- primer build frontend: `FAIL` con tres errores TypeScript;
- errores TypeScript: corregidos;
- imágenes frontend/backend: exportadas desde caché;
- primer arranque: bloqueado por puerto 5432;
- tres puertos host configurables: implementados;
- validación integral Docker/Maven/Testcontainers/npm ci: automatizada y pendiente de ejecución.

Fuentes:

```text
docs/status.md
docs/next-step.md
docs/validation/SEG-001.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
```

## Alcance

- Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V5 y Hibernate validate;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview, ejecución confirmada y evidencia por fila;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- React, TypeScript strict y Vite;
- Docker Compose para PostgreSQL, backend, frontend y smoke;
- Maven verify, ArchUnit y Testcontainers;
- CI, preflight, smoke y evidencia estructurada.

## Seguridad de envío

No existe adaptador Gmail, SMTP o de correo.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL contiene además un kill switch persistente. Ninguna operación disponible puede enviar mensajes.

## Validación completa recomendada en Windows

### Requisitos

- Git;
- Docker Desktop con contenedores Linux;
- Docker Compose v2;
- Windows PowerShell.

No requiere Java, Maven, Node o npm instalados en el host.

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
git status --short
```

### 2. Crear `.env` solamente cuando no exista

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Editar `CRM_BOOTSTRAP_PASSWORD` y conservar las cuatro guardas de envío.

Puertos predeterminados:

```dotenv
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

### 3. Ejecutar el recorrido integral

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

El script:

1. exige `main` y archivos rastreados limpios;
2. actualiza puertos sin tocar contraseñas;
3. ejecuta preflight fail-closed;
4. construye frontend/backend sin caché;
5. levanta PostgreSQL, backend y frontend;
6. espera los health checks;
7. ejecuta smoke host y contenedor;
8. ejecuta Maven verify/Spotless/tests/ArchUnit/Testcontainers en Docker;
9. genera package-lock sin lifecycle scripts ni node_modules;
10. reconstruye frontend mediante npm ci;
11. repite health y smoke;
12. escanea archivos sensibles rastreados;
13. produce transcript y JSON;
14. deja `frontend/package-lock.json` sin commit para revisión.

No usar `-UseBuildCache` como evidencia de cierre.

### 4. Puertos alternativos

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Los smoke tests derivan las URLs desde `.env`.

### 5. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

### 6. Evidencia

```text
validation-output/seg001-docker-*.json
validation-output/seg001-complete-*.json
validation-output/seg001-complete-*.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git. Revisar transcripts antes de compartirlos.

### 7. Abrir el sistema

Con puertos predeterminados:

```text
Frontend: http://localhost:5173
Health:   http://localhost:8080/actuator/health
Swagger:  http://localhost:8080/swagger-ui/index.html
```

Ingresar con `CRM_BOOTSTRAP_USERNAME` y `CRM_BOOTSTRAP_PASSWORD`.

### 8. Revisar y versionar package-lock

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Después de revisarlo:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar `.env` ni `validation-output/`.

### 9. Repetir desde árbol limpio

```powershell
git pull --ff-only
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

La segunda ejecución debe usar npm ci desde el primer build.

## Comandos separados

### Validar solo Docker stack

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

### Maven verify/Testcontainers sin Java local

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

Este control monta el socket Docker. Ejecutarlo solamente sobre código propio y revisado.

### Generar package-lock de forma segura

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Usa:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

### Seguridad del repositorio

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

## Linux/macOS

Recorrido contenedorizado equivalente mediante Make:

```bash
git switch main
git pull --ff-only
sh scripts/set-local-host-ports.sh 55432 8080 5173
make verify-container
```

Targets individuales:

```bash
make preflight-container
make repository-safety
make backend-verify-container
make frontend-lock
make smoke-container
```

## Desarrollo con procesos separados

Requiere Java 21, Docker, Node 22 y npm.

```bash
sh scripts/preflight.sh --local
docker compose up -d postgres
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Otra terminal:

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

## Makefile

```text
preflight
preflight-container
postgres-port
local-ports
repository-safety
db-up
db-down
app-up
app-down
app-logs
backend
backend-verify-container
frontend
frontend-lock
verify
verify-container
smoke
smoke-container
reset-db
```

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

- validador integral pendiente de ejecución real;
- clean builds pendientes;
- Maven/Testcontainers/Flyway/Hibernate pendientes;
- package-lock pendiente de generación y versión;
- npm ci pendiente de evidencia real;
- CI no muestra runs visibles;
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
- `docs/validation/SEG-001.md` — matriz;
- `docs/validation/SEG-001-complete-validation-automation-2026-07-20.md` — contrato del validador integral.
