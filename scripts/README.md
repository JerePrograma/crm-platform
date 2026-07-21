# Automatización local

## Objetivo

Scripts reproducibles para configurar, construir, verificar y operar Gestudio CRM localmente.

Ningún script:

- despliega producción;
- habilita envíos;
- importa el XLSX real;
- realiza commits;
- elimina el volumen PostgreSQL salvo los comandos destructivos explícitos.

## Validación integral recomendada

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

### Make

```bash
make validate-seg001
```

El target Make ejecuta el validador Bash con cleanup final predeterminado.

Requisitos:

- rama `main`;
- working tree sin cambios inesperados;
- `.env` existente;
- Git;
- Docker con daemon accesible;
- Docker Compose v2;
- PowerShell o Bash según la plataforma.

No requiere Java, Maven, Node o npm instalados en el host.

Los validadores ejecutan:

1. validación de rama y working tree;
2. configuración segura de puertos;
3. preflight container-only;
4. Compose config;
5. builds frontend/backend sin caché;
6. arranque y health de tres servicios;
7. smoke host y contenedor;
8. Maven verify/Spotless/tests/ArchUnit/Testcontainers en Docker;
9. generación package-lock-only sin lifecycle scripts;
10. build frontend mediante npm ci;
11. recreación y health frontend;
12. smoke final;
13. seguridad del repositorio;
14. JSON y transcript;
15. cleanup opcional.

No usar `-UseBuildCache` ni `--use-build-cache` como evidencia de cierre.

Evidencia:

```text
validation-output/seg001-complete-*.log
validation-output/seg001-complete-*.json
validation-output/seg001-docker-*.json
frontend/package-lock.json
```

## Configurar puertos host

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Unix:

```bash
sh scripts/set-local-host-ports.sh 55432 8080 5173
```

Con Make:

```bash
make local-ports
```

Los scripts:

- actualizan `POSTGRES_HOST_PORT`;
- actualizan `BACKEND_HOST_PORT`;
- actualizan `FRONTEND_HOST_PORT`;
- coordinan `DATABASE_URL`;
- conservan nombre de base, usuarios y contraseñas;
- conservan `SENDING_*`;
- rechazan puertos inválidos o repetidos;
- no imprimen secretos;
- escriben UTF-8 sin BOM en PowerShell.

Los helpers `set-postgres-host-port.ps1` y `.sh` siguen disponibles como wrappers compatibles.

## Preflight

Herramientas locales:

```bash
sh scripts/preflight.sh --local
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

Solo contenedores:

```bash
sh scripts/preflight.sh --container-only
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Valida:

- Git;
- Docker instalado y daemon accesible;
- Docker Compose v2;
- Java/Node/npm en modo local;
- `.env`;
- tres puertos válidos y distintos;
- `DATABASE_URL` coordinada;
- credenciales DB y bootstrap;
- cuatro guardas de envío;
- perfiles `app` y `smoke`.

No imprime contraseñas ni inicia servicios.

## Validación Docker del stack

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Fases:

1. puertos;
2. preflight;
3. Compose config;
4. cleanup no destructivo;
5. builds limpios;
6. arranque;
7. health;
8. smoke host;
9. smoke contenedor;
10. JSON y transcript.

Parámetros:

```text
-PostgresPort
-BackendPort
-FrontendPort
-KeepRunning
-UseBuildCache
-NoTranscript
```

`-NoTranscript` existe para composición desde `validate-seg001.ps1`.

Evidencia:

```text
validation-output/seg001-docker-*.log
validation-output/seg001-docker-*.json
```

Este script no ejecuta Maven verify ni genera package-lock; el validador integral sí.

## Backend verify/Testcontainers sin Java local

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

Con Make:

```bash
make backend-verify-container
```

Comportamiento:

- usa `maven:3.9.16-eclipse-temurin-21`;
- ejecuta `mvn -B -f backend/pom.xml verify`;
- monta el repositorio en solo lectura;
- monta `backend/target` en volumen efímero;
- reutiliza `crm_maven_cache`;
- monta `/var/run/docker.sock`;
- configura `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`;
- elimina contenedor y volumen target.

Cobertura:

- compilación;
- Maven Enforcer;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers.

Advertencia: montar el socket Docker concede privilegios elevados. Ejecutar únicamente sobre código propio y revisado.

## Generar package-lock de forma segura

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Con Make:

```bash
make frontend-lock
```

Comando npm:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

Garantías comunes:

- solo genera o actualiza package-lock;
- no ejecuta lifecycle scripts;
- no debe crear node_modules;
- falla si node_modules aparece;
- no realiza commit.

Garantías adicionales Unix:

- ejecuta el contenedor con `id -u:id -g`;
- usa caché npm temporal dentro del contenedor;
- evita lockfiles propiedad de `root`;
- comprueba que el lockfile quede editable por el usuario actual.

Revisar:

```bash
git status --short
git diff -- frontend/package-lock.json
```

Dockerfile, Makefile y CI detectan automáticamente:

```text
package-lock presente -> npm ci
package-lock ausente  -> npm install
```

## Seguridad del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

Con Make:

```bash
make repository-safety
```

Comprueba:

- `git diff --check`;
- `.env` no rastreado;
- `validation-output/` no rastreado;
- datos privados no rastreados;
- lote `gestudio_lote_*_prospectos.xlsx` no rastreado;
- claves/certificados no rastreados;
- JSON de credenciales no rastreados.

No reemplaza un escáner de secretos por contenido.

## Smoke host

Unix:

```bash
sh scripts/smoke-test.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Variables:

```text
BACKEND_HOST_PORT
FRONTEND_HOST_PORT
BACKEND_URL opcional
FRONTEND_URL opcional
```

Comprueba:

- health;
- API autenticada;
- frontend.

No crea datos.

## Smoke contenedorizado

```bash
make smoke-container
```

Equivalente:

```bash
docker compose --profile app --profile smoke up \
  --build \
  --abort-on-container-exit \
  --exit-code-from smoke \
  smoke
```

Contra stack activo:

```bash
docker compose --profile app --profile smoke run --rm smoke
```

## Builds limpios manuales

```bash
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

Un build completamente `CACHED` no demuestra compilación limpia.

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
validate-seg001
smoke
smoke-container
reset-db
```

| Target | Acción |
|---|---|
| `preflight` | herramientas locales y configuración |
| `preflight-container` | modalidad Docker-only |
| `postgres-port` | wrapper compatible de puerto PostgreSQL |
| `local-ports` | configura tres puertos |
| `repository-safety` | escanea rutas/extensiones sensibles |
| `db-up` | inicia PostgreSQL |
| `db-down` | detiene PostgreSQL conservando volumen |
| `app-up` | construye e inicia stack |
| `app-down` | retira stack conservando volumen |
| `app-logs` | sigue logs |
| `backend` | ejecuta Spring Boot local |
| `backend-verify-container` | Maven verify/Testcontainers en Docker |
| `frontend` | npm ci/install y Vite |
| `frontend-lock` | genera package-lock-only |
| `verify` | validación con herramientas locales |
| `verify-container` | secuencia Docker-only Unix sin evidencia integral |
| `validate-seg001` | validador integral Bash con JSON/transcript |
| `smoke` | prueba stack activo |
| `smoke-container` | stack efímero y smoke |
| `reset-db` | elimina stack y volumen |

## Evidencia versionada

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
docs/validation/SEG-001-static-automation-2026-07-20.md
```

## Evidencia local

```text
validation-output/
```

Está ignorado por Git. Revisar transcripts antes de compartir y resumir resultados importantes en `docs/validation/SEG-001.md`.

## Advertencias destructivas

```bash
make reset-db
```

O:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

Eliminan la base local.

## Controles de envío

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

El preflight falla si alguno cambia.

## CI

CI valida:

- scripts POSIX;
- sintaxis Bash del validador integral;
- parser PowerShell;
- targets Make principales;
- puertos;
- seguridad del repositorio;
- preflight fail-closed;
- frontend typecheck/build;
- backend verify;
- Compose;
- imágenes;
- stack;
- smoke;
- logs y cleanup.

Hasta versionar package-lock, CI utiliza npm install. Después adopta npm ci automáticamente.
