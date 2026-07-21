# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
VALIDACIÓN INTEGRAL WINDOWS/UNIX IMPLEMENTADA
EJECUCIÓN FUNCIONAL PENDIENTE
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Objetivo

Ejecutar un único recorrido integral que valide:

- builds limpios frontend/backend;
- Compose config;
- PostgreSQL, backend y frontend saludables;
- Flyway y Hibernate durante el arranque;
- smoke host y contenedor;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- generación segura de package-lock;
- reconstrucción frontend mediante npm ci;
- seguridad del repositorio;
- evidencia JSON y transcript.

## Fuentes

```text
estado: docs/status.md
matriz: docs/validation/SEG-001.md
automatización completa: docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
paridad Windows/Unix: docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
primer build: docs/validation/SEG-001-container-build-2026-07-20.md
segunda reejecución: docs/validation/SEG-001-rerun-2026-07-20.md
puertos/orquestación: docs/validation/SEG-001-local-orchestration-2026-07-20.md
Docker: docs/containerized-quickstart.md
scripts: scripts/README.md
```

## 1. Actualizar `main`

Windows PowerShell:

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git status
git rev-parse HEAD
```

Linux/macOS:

```bash
cd /ruta/a/crm-platform
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

No fusionar ni copiar nada desde `feat/seg-001-prospect-vertical-slice`: está detrás y no contiene trabajo exclusivo.

## 2. Resolver cambios locales

En Windows, inspeccionar especialmente `mvnw.cmd`:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git diff --numstat -- mvnw.cmd
```

Si no fue un cambio deliberado:

```powershell
git restore -- mvnw.cmd
git status --short
```

En cualquier plataforma, el validador integral exige un árbol limpio. No descarta cambios automáticamente.

Se permite únicamente un `frontend/package-lock.json` no rastreado generado por una ejecución anterior.

## 3. Confirmar `.env`

No copiar nuevamente `.env.example` cuando `.env` ya contiene credenciales locales.

Crear el archivo solo si no existe.

Windows:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Linux/macOS:

```bash
[ -f .env ] || cp .env.example .env
```

Configurar credenciales bootstrap locales y conservar:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Los validadores actualizan únicamente los puertos y `DATABASE_URL`.

## 4. Ejecutar validación integral

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

### Linux/macOS Bash

```bash
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 8080 \
  --frontend-port 5173 \
  --keep-running
```

También disponible mediante Make, con cleanup final predeterminado:

```bash
make validate-seg001
```

Ninguna variante requiere Java, Maven, Node o npm instalados en el host. Requieren Git, Docker y Docker Compose v2.

No usar `-UseBuildCache` ni `--use-build-cache` como evidencia de cierre.

## 5. Puertos alternativos

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

Los smoke tests toman las URLs desde `.env`.

## 6. Fases automáticas

### Fase A — precondiciones

- rama `main`;
- commit registrado;
- working tree sin cambios inesperados;
- daemon Docker accesible;
- Compose v2;
- `.env` presente;
- puertos válidos y distintos;
- guardas cerradas;
- Compose válido.

### Fase B — stack Docker inicial

- cleanup sin `-v`;
- frontend build sin caché;
- backend build sin caché;
- arranque del perfil `app`;
- health de PostgreSQL;
- health del backend;
- health del frontend;
- smoke desde el host;
- smoke contenedorizado.

### Fase C — backend completo

- Maven 3.9.16/Java 21 dentro de Docker;
- repositorio montado en solo lectura;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway/Hibernate cubiertos por arranque y suite.

### Fase D — lockfile y npm ci

- `npm install --package-lock-only --ignore-scripts`;
- ausencia de `node_modules`;
- UID/GID preservados en Unix;
- SHA-256 del lockfile;
- frontend build sin caché;
- selección automática de `npm ci`;
- recreación del frontend;
- health;
- smoke final host/contenedor.

### Fase E — seguridad

- `git diff --check`;
- `.env` no rastreado;
- transcripts no rastreados;
- datos privados no rastreados;
- lote operativo no rastreado;
- claves/certificados no rastreados;
- JSON de credenciales no rastreados;
- único cambio permitido: `frontend/package-lock.json`.

## 7. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

Servicios con puertos predeterminados:

```text
postgres   healthy   127.0.0.1:55432->5432
backend    healthy   127.0.0.1:8080->8080
frontend   healthy   127.0.0.1:5173->8080
```

## 8. Evidencia local

Archivos esperados:

```text
validation-output/seg001-docker-YYYYMMDD-HHMMSS.json
validation-output/seg001-complete-YYYYMMDD-HHMMSS.json
validation-output/seg001-complete-YYYYMMDD-HHMMSS.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git.

No compartir transcripts sin revisarlos. Pueden contener logs técnicos, aunque los scripts no imprimen contraseñas.

## 9. Si falla

Los validadores:

- marcan la fase incompleta;
- registran el error;
- imprimen estado y logs del stack;
- conservan evidencia local;
- retiran el stack salvo opción explícita de mantenerlo.

Conservar:

- SHA exacto;
- ruta del JSON;
- ruta del transcript;
- primera línea del error;
- fase afectada;
- servicio o test;
- stack trace completo;
- estado de los servicios.

No corregir desactivando:

- TypeScript strict;
- tests;
- health checks;
- Testcontainers;
- guardas;
- seguridad;
- builds limpios.

## 10. Revisar package-lock

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

Esperado en la primera ejecución:

```text
?? frontend/package-lock.json
```

O una modificación controlada cuando el archivo ya esté versionado.

## 11. Versionar lockfile

Solo después de revisar:

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

## 12. Repetir con lockfile versionado

Windows:

```powershell
git pull --ff-only
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Linux/macOS:

```bash
git pull --ff-only
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 8080 \
  --frontend-port 5173
```

La segunda ejecución debe comenzar con árbol limpio y usar `npm ci` desde el primer build.

## 13. CI

Después del push, verificar:

- backend;
- frontend con npm ci;
- scripts POSIX/Bash/PowerShell;
- Makefile;
- seguridad;
- imágenes/stack/smoke.

No marcar SEG-001 completo mientras no exista evidencia CI visible o una excepción explícita documentada sobre su indisponibilidad.

## 14. Criterios de cierre

- [ ] validador integral PASS;
- [ ] clean build frontend PASS;
- [ ] clean build backend PASS;
- [ ] Maven verify PASS;
- [ ] Spotless PASS;
- [ ] unit tests PASS;
- [ ] ArchUnit PASS;
- [ ] Testcontainers PASS;
- [ ] Flyway PASS;
- [ ] Hibernate validate PASS;
- [ ] tres servicios healthy;
- [ ] smoke host PASS;
- [ ] smoke container PASS;
- [ ] package-lock revisado y versionado;
- [ ] npm ci PASS con lockfile versionado;
- [ ] seguridad PASS;
- [ ] evidencia resumida en matriz;
- [ ] CI visible verde o excepción documentada;
- [ ] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar XLSX real a Git, CI o imágenes;
- no usar `docker compose down -v` salvo intención destructiva;
- no ejecutar backend verify contenedorizado sobre código no confiable porque monta el socket Docker;
- no versionar transcripts;
- no comenzar SEG-002 con bloqueantes.
