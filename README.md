# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

Todo el código y la documentación vigentes están consolidados en `main`.

`SEG-001 — Vertical slice persistente de prospectos` está implementado. El primer preflight Docker real pasó y el build frontend reprodujo tres errores TypeScript, ya corregidos en `main`. Continúa abierto hasta reconstruir frontend, compilar backend, ejecutar migraciones/tests y completar smoke E2E.

Evidencias:

```text
docs/validation/SEG-001.md
docs/validation/SEG-001-container-build-2026-07-20.md
```

El sistema incluye:

- backend Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V5 y JPA `validate`;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview con paridad de validación y ejecución confirmada;
- evidencia por fila y cola de revisión humana;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- interfaz React, TypeScript y Vite;
- Docker Compose para PostgreSQL, backend, frontend y smoke;
- GitHub Actions, Testcontainers, preflight y smoke tests.

Estado detallado: `docs/status.md`.

## Seguridad de envío

No existe ningún adaptador de correo, Gmail o SMTP. Los valores obligatorios son:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL inicializa además un kill switch persistente. Ninguna operación disponible puede enviar mensajes.

## Inicio rápido recomendado: todo con Docker

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
git switch main
git fetch origin
git pull --ff-only
```

### 2. Crear configuración local

Linux/macOS:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

No sobrescribir `.env` si ya contiene credenciales locales válidas.

Editar `.env`:

```dotenv
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:5432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

No modificar las cuatro guardas de envío.

### 3. Ejecutar preflight

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Con Make:

```bash
make preflight-container
```

### 4. Reconstruir después de las correcciones frontend

Para obtener errores aislados y claros:

```bash
docker compose --profile app build frontend --progress=plain
docker compose --profile app build backend --progress=plain
```

Windows PowerShell usa los mismos comandos.

### 5. Construir e iniciar el stack

```bash
docker compose --profile app up -d --build
docker compose --profile app ps
```

Con Make:

```bash
make app-up
```

Servicios publicados solo en localhost:

```text
PostgreSQL  127.0.0.1:5432
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

### 7. Ejecutar smoke test

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Con Make:

```bash
make smoke
```

Smoke completamente contenedorizado:

```bash
make smoke-container
```

Guía específica: `docs/containerized-quickstart.md`.

## Generar `package-lock.json` sin Node local

Linux/macOS:

```bash
sh scripts/generate-frontend-lock.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Con Make:

```bash
make frontend-lock
```

Después revisar:

```bash
git status --short
git diff -- frontend/package-lock.json
```

El lockfile debe versionarse antes de cambiar Dockerfile y CI a `npm ci`.

## Desarrollo con procesos separados

Requiere:

- Java 21;
- Docker con Compose v2;
- Node.js 22 y npm;
- `curl` o `wget`, `unzip` y SHA-512 para Maven Wrapper.

Preflight:

```bash
sh scripts/preflight.sh --local
```

### PostgreSQL

```bash
docker compose up -d postgres
docker compose ps
```

### Backend

Linux/macOS:

```bash
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Windows PowerShell:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`.

Guía completa: `docs/local-development-and-usage.md`.

## Flujo operativo recomendado

1. ingresar al Dashboard y comprobar que los envíos figuran bloqueados;
2. registrar en `Exclusiones` los canales que no deben contactarse;
3. preparar un CSV o XLSX de hasta 10 MB;
4. ejecutar `Preview` desde `Importaciones`;
5. revisar filas `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
6. corregir el archivo cuando corresponda;
7. ejecutar `Importar con confirmación`;
8. revisar prospectos, elegibilidad y resumen, incluida la métrica `Bloqueadas`;
9. comprobar eventos en `Auditoría`.

La ejecución requiere confirmación en la UI y la cabecera:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

## Formatos de importación

- CSV UTF-8 separado por coma o punto y coma;
- XLSX con hoja `Prospectos` y hoja opcional `Exclusiones`;
- tamaño máximo: 10 MB;
- CSV requiere al menos `Institución`;
- parser por encabezados normalizados.

Contratos:

- `docs/import-existing-data.md`;
- `docs/import-hardening.md`.

Los datos operativos reales no se versionan.

## API implementada

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

## Automatización local

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

Backend:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Frontend:

```bash
cd frontend
npm install
npm run typecheck
npm run build
cd ..
```

Infraestructura:

```bash
docker compose --profile app --profile smoke config
docker build -t gestudio-crm:local .
docker build -f frontend/Dockerfile -t gestudio-crm-frontend:local frontend
make smoke-container
```

Las pruebas de integración requieren Docker porque utilizan PostgreSQL mediante Testcontainers.

La evidencia real se registra en `docs/validation/SEG-001.md`.

## Detener el entorno

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base local:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

`down -v` destruye los datos del volumen.

## Limitaciones actuales

- frontend corregido pendiente de reejecución;
- backend, Flyway, Hibernate, tests y smoke aún no alcanzados por el primer build;
- falta `package-lock.json`;
- Dockerfile/CI usan `npm install` hasta versionar el lockfile;
- HTTP Basic es temporal;
- no existen usuarios persistentes ni RBAC;
- no existe resolución desde UI de `DuplicateReview`;
- no existe retry explícito de trabajos fallidos;
- no existen campañas, Gmail, Sheets, workers ni infraestructura cloud;
- el stack Compose es local, no producción.

## Documentación

- `docs/README.md` — índice completo;
- `docs/containerized-quickstart.md` — stack completo con Docker;
- `docs/local-development-and-usage.md` — procesos separados y flujo funcional;
- `scripts/README.md` — preflight, smoke, lockfile y Makefile;
- `docs/status.md` — estado real;
- `docs/next-step.md` — reejecución inmediata;
- `docs/backlog.md` — tareas y segmentos;
- `docs/validation/SEG-001.md` — matriz;
- `docs/validation/SEG-001-container-build-2026-07-20.md` — primer build real.

## Continuidad

Antes de modificar el proyecto, leer:

1. `AGENTS.md`;
2. `docs/status.md`;
3. `docs/next-step.md`;
4. `docs/backlog.md`;
5. `docs/segments/SEG-001.md`;
6. `docs/validation/SEG-001.md`;
7. ADR y documentación del módulo afectado.

La instrucción `continuar` ejecuta el trabajo indicado en `docs/next-step.md` y actualiza toda la documentación canónica al finalizar.