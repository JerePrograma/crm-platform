# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

`main` es la fuente canónica. La implementación completa fue integrada
localmente mediante fast-forward desde `feat/complete-crm-platform`.
`origin/main` permanece pendiente de publicación directa y validación remota
dentro de la misión actual; no se creó PR ni se desplegó producción.

```text
SEG-000: COMPLETE
SEG-001: COMPLETE — validación local integral y CI verdes
SEG-002: COMPLETE — identidad, usuarios, sesión, RBAC y tenant
SEG-003–SEG-007: COMPLETE — CRM operativo, pipeline y campañas simuladas
SEG-008: COMPLETE — mensajería no-op/fake/manual y adapters desconectados
SEG-009: COMPLETE — outbox, workers e inbound fake durable
SEG-010: COMPLETE — reportes, seguridad, observabilidad y perfil productivo validado localmente
SEG-011: COMPLETE — dos validaciones integrales limpias reproducibles
```

Evidencia real disponible:

- segunda validación Windows limpia sobre `d8a5a449…`: `PASS`;
- Flyway V1–V5 antes de Hibernate: `PASS`;
- PostgreSQL/backend/frontend: `healthy`;
- smoke host y contenedor: `PASS`;
- Maven verify, 29/29 tests, Spotless, ArchUnit y Testcontainers: `PASS`;
- `frontend/package-lock.json` versionado y primer build mediante `npm ci`:
  `PASS`;
- repository safety: `PASS`;
- GitHub Actions run `29848718163`: `success`.
- Flyway V1–V6, sesión cookie/CSRF, RBAC y tenant isolation: `PASS`;
- Maven verify SEG-002, 36/36 tests: `PASS`;
- frontend sin credenciales persistidas y smoke cookie/CSRF host/contenedor:
  `PASS`.
- Flyway V1–V11, mensajería safe-by-default y contract tests loopback: `PASS`;
- Docker V10→V11, health completo y simulación visual fake: `PASS`;
- Gmail/WhatsApp reales: `IMPLEMENTED_NOT_CONNECTED`.
- Flyway V1–V12, Hibernate validate, outbox/worker/inbound: `PASS`;
- Maven verify SEG-009, 69/69, Spotless 148/148 y ArchUnit: `PASS`;
- E2E fake inbound HMAC/replay/quarantine/REPLIED y cero estados prohibidos:
  `PASS`.
- Flyway V1–V13, reporting/search/settings/tags y tenant isolation: `PASS`;
- Maven verify SEG-010, 79/79, Spotless 159/159 y ArchUnit: `PASS`;
- Vitest 2/2, Playwright integral 2/2, TypeScript/Vite: `PASS`;
- backup/restore sintético y perfil productivo validado localmente: `PASS`;
- imagen backend no-root/read-only compatible, Grype High/Critical y npm audit:
  `PASS`;
- validador completo Windows sobre `986523a`: `FUNCTIONAL_PASS` dos veces,
  713,870 s y 734,162 s, con 21 fases verdes por corrida;
- ambas corridas: E2E 2/2, migraciones vacío/V11→V13, cuatro bloqueos
  efectivos, cero `SENT|DELIVERED|READ`, restore y perfil productivo
  validado localmente: `PASS`;
- XLSX real: `BLOCKED_EXTERNAL_FILE`; producción: `NOT_AUTHORIZED` /
  `NOT_DEPLOYED`; publicación directa de `main`: pendiente; PR: no creado.

Fuentes:

```text
docs/status.md
docs/next-step.md
docs/validation/SEG-001.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
docs/validation/SEG-002-identity-rbac-2026-07-21.md
docs/validation/SEG-008-safe-messaging-2026-07-21.md
docs/validation/SEG-009-transactional-outbox-inbound-2026-07-22.md
docs/validation/SEG-010-operations-production-2026-07-22.md
docs/validation/SEG-010-performance-accessibility-2026-07-22.md
docs/validation/SEG-011-complete-crm-closure-2026-07-22.md
docs/execution/complete-crm-platform-plan.md
docs/execution/complete-crm-platform-progress.md
docs/validation/COMPLETE-CRM-matrix.md
```

## Alcance

- Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V13 y Hibernate validate;
- organizaciones, usuarios persistentes, roles y permisos;
- sesión cookie HttpOnly same-origin, CSRF, bloqueo e invalidación;
- tenant isolation y auditoría de identidad;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- prospectos/contactos operativos, timeline, tareas y ciclo comercial;
- merge trazable de duplicados, oportunidades y pipeline;
- campañas, audiencias congeladas, plantillas y secuencias limitadas;
- borradores, simulaciones fake y enlaces manuales;
- adapters Gmail/WhatsApp aislados y desconectados;
- outbox PostgreSQL, workers con lease/retry/dead-letter e idempotencia;
- inbound fake firmado, replay protection, quarantine, asociación y timeline;
- reporting agregado, búsqueda PostgreSQL, settings y etiquetas tenant-scoped;
- correlation ID, métricas, health/readiness/liveness y logs sanitizados;
- backup/restore verificado y perfil productivo local no-root/read-only;
- Vitest, Playwright y validadores CRM Windows/Unix;
- preview, ejecución confirmada y evidencia por fila;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- React, TypeScript strict y Vite;
- Docker Compose para PostgreSQL, backend, frontend y smoke;
- Maven verify, ArchUnit y Testcontainers;
- CI, preflight, smoke y evidencia estructurada.

## Seguridad de envío

Existen adapters Gmail draft-only y WhatsApp Cloud para contract testing y
conexión futura, pero no están conectados ni pueden inicializarse con la
configuración canónica. La API no expone endpoint de envío.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

PostgreSQL contiene además un kill switch persistente. La operación disponible
solo crea borradores, simulaciones `FAKE` y enlaces manuales; ninguna ruta puede
persistir `SENT`.

## Requisitos recomendados

Para levantar y validar todo en contenedores:

- Git;
- Docker Desktop o Docker Engine;
- Docker Compose v2;
- PowerShell en Windows o Bash en Linux/macOS.

El validador integral canónico verifica Git, Docker, Maven, Java 21, Node y npm
en el host. Las verificaciones backend también pueden ejecutarse completamente
en contenedor con `scripts/verify-backend-container.*`.

Docker Desktop debe utilizar contenedores Linux.

## 1. Obtener o actualizar `main`

Checkout nuevo:

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
```

Checkout existente:

```bash
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

La rama `feat/seg-001-prospect-vertical-slice` está detrás y no contiene trabajo exclusivo. No copiar ni fusionar nada desde ella.

En Windows, si `mvnw.cmd` aparece modificado sin intención:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git restore -- mvnw.cmd
git status --short
```

## 2. Crear `.env`

Crear solo si no existe.

Windows:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Linux/macOS:

```bash
[ -f .env ] || cp .env.example .env
```

Editar como mínimo:

```dotenv
POSTGRES_DB=gestudio_crm
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No volver a copiar `.env.example` sobre un `.env` que ya contiene credenciales elegidas.

## 3. Levantar y validar todo — Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

## 4. Levantar y validar todo — Linux/macOS

```bash
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 8080 \
  --frontend-port 5173 \
  --keep-running
```

Con Make, dejando cleanup final:

```bash
make validate-seg001
```

## 5. Qué ejecutan los validadores

1. exigen rama `main`;
2. rechazan cambios locales inesperados;
3. coordinan los tres puertos y `DATABASE_URL`;
4. ejecutan preflight fail-closed;
5. validan Compose;
6. retiran contenedores incompletos sin borrar el volumen;
7. construyen frontend/backend sin caché;
8. levantan PostgreSQL, backend y frontend;
9. esperan los tres health checks;
10. ejecutan smoke host y contenedor;
11. ejecutan Maven verify, Spotless, unit tests, ArchUnit y Testcontainers en Docker;
12. generan `package-lock.json` sin lifecycle scripts ni `node_modules`;
13. calculan SHA-256 del lockfile;
14. reconstruyen frontend mediante `npm ci`;
15. recrean frontend y repiten health/smoke;
16. escanean archivos sensibles;
17. producen transcript y JSON;
18. no realizan commits.

No usar `-UseBuildCache` o `--use-build-cache` como evidencia de cierre.

## 6. Puertos alternativos

Si `8080` o `5173` están ocupados:

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Linux/macOS:

```bash
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 18080 \
  --frontend-port 15173 \
  --keep-running
```

Los smoke tests derivan las URLs desde `.env`.

## 7. Resultado esperado

```text
postgres health: healthy
backend health: healthy
frontend health: healthy
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

## 8. Abrir el sistema

Con puertos predeterminados:

```text
Frontend: http://localhost:5173
Health:   http://localhost:8080/actuator/health
Swagger:  http://localhost:8080/swagger-ui/index.html
```

Ingresar con:

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

## 9. Evidencia local

```text
validation-output/seg001-docker-*.json
validation-output/seg001-complete-*.json
validation-output/seg001-complete-*.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git. Revisar transcripts antes de compartirlos.

## 10. Revisar y versionar package-lock

Windows:

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Linux/macOS:

```bash
test -f frontend/package-lock.json
sha256sum frontend/package-lock.json 2>/dev/null || shasum -a 256 frontend/package-lock.json
git status --short
git diff -- frontend/package-lock.json
```

Después de revisarlo:

```bash
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar:

```text
.env
validation-output/
gestudio_lote_100_prospectos.xlsx
```

Repetir luego el validador desde un árbol limpio. Esa ejecución debe usar `npm ci` desde el primer build.

## 11. Flujo para usar Gestudio CRM

### Paso 1 — Ingresar

Abrir el frontend e iniciar sesión con las credenciales bootstrap.

Las credenciales permanecen solo en memoria del navegador.

### Paso 2 — Revisar el Dashboard

Confirmar:

- prospectos visibles;
- cantidad de exclusiones;
- contactos bloqueados;
- revisiones ambiguas;
- envíos bloqueados.

### Paso 3 — Registrar exclusiones antes de importar

Canales admitidos:

```text
EMAIL
PHONE
WHATSAPP
WEBSITE
SOCIAL
```

Una exclusión es dominante. Si coincide con un prospecto existente, lo vuelve no elegible y lo lleva a `DO_NOT_CONTACT`.

### Paso 4 — Preparar el archivo

Formatos:

- CSV UTF-8 con coma o punto y coma;
- XLSX con hoja `Prospectos`;
- hoja opcional `Exclusiones`;
- máximo 10 MB;
- CSV requiere la columna `Institución`;
- encabezados se interpretan por nombre normalizado.

No utilizar el lote operativo real durante validaciones técnicas.

### Paso 5 — Ejecutar Preview

El preview persiste evidencia de importación, pero no crea entidades de dominio procedentes del archivo.

Revisar estados:

```text
ACCEPTED
EXCLUDED
REJECTED
DUPLICATE
REVIEW_REQUIRED
```

Métricas visibles:

```text
Aceptadas
Bloqueadas
Rechazadas
Duplicadas
A revisión
```

### Paso 6 — Corregir problemas

Antes de ejecutar definitivamente:

- corregir filas rechazadas;
- revisar canales inválidos;
- resolver coincidencias ambiguas modificando el archivo;
- confirmar que las exclusiones sean esperadas;
- evitar fusiones automáticas.

La UI actual muestra revisiones pendientes, pero todavía no permite resolver `DuplicateReview` directamente.

### Paso 7 — Ejecutar importación confirmada

Usar el botón `Importar con confirmación`.

La API exige:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

La ejecución procesa por fila, aplica exclusiones, crea prospectos elegibles y registra auditoría.

### Paso 8 — Revisar Prospectos

Comprobar:

- institución;
- localidad;
- estado comercial;
- elegibilidad;
- prioridad;
- puntuación;
- alumnos estimados;
- fuente;
- propietario.

### Paso 9 — Revisar Auditoría

Eventos principales:

```text
PROSPECT_CREATED
EXCLUSION_CREATED
IMPORT_STARTED
IMPORT_COMPLETED
IMPORT_FAILED
```

### Paso 10 — Mantener envíos bloqueados

No modificar las cuatro variables `SENDING_*`. El sistema no contiene adaptador de envío.

## 12. Comandos separados

### Validar solo el stack Docker — Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

### Maven verify/Testcontainers sin Java local

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

Este control monta el socket Docker. Ejecutarlo únicamente sobre código propio y revisado.

### Generar package-lock

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

### Seguridad del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

## 13. Desarrollo con procesos separados

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

## 14. API principal

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

## 15. Makefile

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
validate-seg001
validate-complete-crm
smoke
smoke-container
reset-db
```

## 16. Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base local:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

La segunda operación es destructiva.

## Limitaciones actuales

- recorrido integral Bash/Linux/macOS no ejecutado localmente; sintaxis Bash y
  preflight container-only sí fueron ejecutados desde WSL;
- advertencias Hikari de contextos Testcontainers ya cerrados, sin fallos;
- Mockito deberá configurarse como agente antes de una futura restricción de Java;
- Gmail y WhatsApp reales están implementados pero no conectados;
- XLSX real no disponible: preview bloqueado externamente;
- perfil productivo validado localmente; despliegue no autorizado;
- CI ampliado está versionado y parseado localmente, pero permanece
  `IMPLEMENTED_NOT_RUN` hasta publicar `origin/main` y observar el workflow de
  la misión actual.

## Documentación

- `docs/README.md` — índice;
- `docs/status.md` — alcance, progreso, tareas y riesgos;
- `docs/next-step.md` — siguiente acción;
- `docs/containerized-quickstart.md` — Docker;
- `docs/local-development-and-usage.md` — procesos separados y flujo;
- `scripts/README.md` — automatización;
- `docs/validation/SEG-001.md` — matriz;
- `docs/validation/SEG-001-complete-validation-automation-2026-07-20.md` — contrato integral;
- `docs/validation/SEG-001-cross-platform-validation-2026-07-20.md` — paridad Windows/Unix.
- `docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md` — causa raíz Jackson y cierre integral.
- `docs/segments/SEG-010.md` y `docs/segments/SEG-011.md` — cierre operativo e integral.
- `docs/validation/SEG-011-complete-crm-closure-2026-07-22.md` — dos corridas
  Windows reproducibles y frontera externa final.
- `docs/production/` y `docs/runbooks/` — perfil y procedimientos operativos.
