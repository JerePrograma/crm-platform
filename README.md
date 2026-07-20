# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

Todo el código y la documentación vigentes están consolidados en `main`.

`SEG-001 — Vertical slice persistente de prospectos` está implementado. La validación real produjo:

- preflight Docker: `PASS`;
- primer build frontend: `FAIL` con tres errores TypeScript;
- errores frontend: corregidos en `main`;
- imágenes frontend/backend: exportadas desde caché;
- stack: bloqueado por el puerto host PostgreSQL 5432;
- puerto PostgreSQL configurable: corregido en `main`;
- build limpio, stack, migraciones, tests y smoke: pendientes.

Evidencias:

```text
docs/validation/SEG-001.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
```

El sistema incluye:

- backend Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V5 y JPA validate;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview, ejecución confirmada y evidencia por fila;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- React, TypeScript y Vite;
- Compose para PostgreSQL, backend, frontend y smoke;
- GitHub Actions, Testcontainers, preflight y smoke tests.

Estado detallado: `docs/status.md`.

## Seguridad de envío

No existe adaptador de correo, Gmail o SMTP. Valores obligatorios:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL contiene además un kill switch persistente. Ninguna operación disponible puede enviar mensajes.

## Inicio rápido recomendado: Docker

### Requisitos

- Git;
- Docker Desktop o Docker Engine;
- Docker Compose v2.

Java, Node y Maven no son necesarios en el host para esta modalidad.

### 1. Clonar o actualizar `main`

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD
```

En un checkout existente:

```bash
git status
git diff -- mvnw.cmd
git pull --ff-only
```

Restaurar `mvnw.cmd` únicamente si su modificación local no fue intencional:

```bash
git restore -- mvnw.cmd
```

### 2. Crear o actualizar `.env`

Linux/macOS:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

No sobrescribir `.env` si ya contiene credenciales válidas.

Configuración recomendada:

```dotenv
POSTGRES_DB=gestudio_crm
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
DATABASE_POOL_SIZE=10
DATABASE_MIN_IDLE=1
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

`POSTGRES_HOST_PORT` afecta únicamente al host. El backend contenedorizado conecta internamente a `postgres:5432`.

### 3. Ejecutar preflight

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Con Make:

```bash
make preflight-container
```

### 4. Build limpio de validación

Frontend:

```bash
docker compose --progress plain --profile app build --no-cache frontend
```

Backend:

```bash
docker compose --progress plain --profile app build --no-cache backend
```

`--progress` es una opción global y debe ir antes de `build`.

### 5. Levantar

```bash
docker compose --profile app up -d
docker compose --profile app ps
```

Con Make:

```bash
make app-up
```

Puertos:

```text
PostgreSQL  127.0.0.1:55432
Backend     127.0.0.1:8080
Frontend    127.0.0.1:5173
```

Logs:

```bash
docker compose --profile app logs -f
```

### 6. Abrir

```text
Frontend: http://localhost:5173
Health:   http://localhost:8080/actuator/health
Swagger:  http://localhost:8080/swagger-ui/index.html
```

Ingresar con `CRM_BOOTSTRAP_USERNAME` y `CRM_BOOTSTRAP_PASSWORD`.

### 7. Smoke

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Con Make:

```bash
make smoke
make smoke-container
```

Guía completa: `docs/containerized-quickstart.md`.

## Generar package-lock sin Node local

Linux/macOS:

```bash
sh scripts/generate-frontend-lock.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Con Make:

```bash
make frontend-lock
```

Revisar:

```bash
git status --short
git diff -- frontend/package-lock.json
```

El lockfile debe versionarse antes de cambiar Dockerfile y CI a `npm ci`.

## Desarrollo con procesos separados

Requiere Java 21, Docker, Node 22 y npm.

Preflight:

```bash
sh scripts/preflight.sh --local
```

PostgreSQL:

```bash
docker compose up -d postgres
docker compose ps
```

Backend Linux/macOS:

```bash
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Backend Windows:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`.

## Flujo operativo

1. ingresar al Dashboard y comprobar envíos bloqueados;
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

## Formatos

- CSV UTF-8 con coma o punto y coma;
- XLSX con hoja `Prospectos` y hoja opcional `Exclusiones`;
- máximo 10 MB;
- CSV requiere `Institución`;
- parser por encabezados normalizados.

Los datos reales no se versionan.

## API

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

## Automatización

```bash
make preflight
make preflight-container
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

## Validación completa

Backend Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Backend Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Infraestructura:

```bash
docker compose --profile app --profile smoke config
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
make smoke-container
```

Testcontainers requiere Docker.

## Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

`down -v` destruye el volumen.

## Limitaciones actuales

- builds limpios posteriores a las correcciones pendientes;
- stack bloqueado previamente por 5432, pendiente de reejecución con 55432;
- Flyway, Hibernate, tests y smoke pendientes;
- falta package-lock;
- Dockerfile/CI usan npm install hasta versionarlo;
- HTTP Basic temporal;
- sin usuarios persistentes/RBAC;
- sin resolución UI de DuplicateReview;
- sin retry de trabajos fallidos;
- sin campañas, Gmail, Sheets, workers o cloud;
- Compose es local, no producción.

## Documentación

- `docs/README.md` — índice;
- `docs/status.md` — estado real;
- `docs/next-step.md` — siguiente acción;
- `docs/containerized-quickstart.md` — Docker;
- `docs/local-development-and-usage.md` — procesos separados;
- `scripts/README.md` — automatización;
- `docs/validation/SEG-001.md` — matriz.
