# Automatización local

## Objetivo

Scripts reproducibles para preparar, construir y comprobar Gestudio CRM. No despliegan, no importan datos y no realizan comunicaciones.

## Preflight

### Herramientas locales

Linux/macOS:

```bash
sh scripts/preflight.sh --local
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

Comprueba Git, Docker, Compose, Java, Node, npm, `.env`, variables y guardas.

### Solo contenedores

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

No requiere Java, Node o npm en el host.

### Validaciones comunes

- `.env` presente;
- `POSTGRES_DB`;
- `POSTGRES_HOST_PORT` entero entre 1 y 65535;
- `DATABASE_URL` con el mismo puerto host;
- usuario y contraseña de base;
- credenciales bootstrap;
- cuatro guardas de envío cerradas;
- parseo de Compose.

Configuración recomendada:

```dotenv
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Compose publica el puerto host configurado hacia el `5432` interno. El backend contenedorizado utiliza `postgres:5432`.

Los scripts no imprimen contraseñas ni inician servicios.

## Builds limpios

Frontend:

```bash
docker compose --progress plain --profile app build --no-cache frontend
```

Backend:

```bash
docker compose --progress plain --profile app build --no-cache backend
```

`--progress` debe ir antes de `build`. Los builds completamente `CACHED` no sustituyen una validación limpia.

## Smoke contra stack activo

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba:

- `/actuator/health` con `UP`;
- API de prospectos autenticada;
- documento raíz del frontend.

No crea datos.

## Smoke contenedorizado

Con Make:

```bash
make smoke-container
```

Comando equivalente:

```bash
docker compose --profile app --profile smoke up \
  --build \
  --abort-on-container-exit \
  --exit-code-from smoke \
  smoke
```

Retirar después:

```bash
docker compose --profile app --profile smoke down --remove-orphans
```

El target Make conserva el volumen. CI elimina su volumen efímero.

## Generar package-lock mediante Docker

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

Los scripts:

1. comprueban Docker;
2. localizan `frontend/package.json`;
3. montan `frontend` en `node:22-alpine`;
4. ejecutan `npm install --no-audit --no-fund`;
5. verifican `frontend/package-lock.json`;
6. no realizan commits.

Revisar:

```bash
git status --short
git diff -- frontend/package-lock.json
```

Después se debe migrar Dockerfile/CI a `npm ci` y repetir la matriz.

## Variables del smoke host

Predeterminadas:

```text
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173
```

Pueden sobrescribirse en la sesión.

## Makefile

```bash
make preflight
make preflight-container
make db-up
make db-down
make app-up
make app-down
make app-logs
make backend
make frontend
make frontend-lock
make verify
make smoke
make smoke-container
make reset-db
```

| Target | Acción |
|---|---|
| `preflight` | valida herramientas locales y configuración |
| `preflight-container` | valida modalidad solo Docker |
| `db-up` | inicia PostgreSQL |
| `db-down` | detiene PostgreSQL conservando volumen |
| `app-up` | construye e inicia stack |
| `app-down` | retira stack conservando volumen |
| `app-logs` | sigue logs |
| `backend` | Spring Boot mediante Maven Wrapper |
| `frontend` | instala y ejecuta Vite |
| `frontend-lock` | genera lockfile con Node en Docker |
| `verify` | backend, frontend, Compose e imágenes |
| `smoke` | prueba stack activo desde host |
| `smoke-container` | construye, prueba y retira stack efímero |
| `reset-db` | elimina stack y volumen |

## Evidencia real

### Primer build

```text
docs/validation/SEG-001-container-build-2026-07-20.md
```

- preflight PASS;
- npm install PASS;
- tres errores TypeScript reproducidos y corregidos.

### Reejecución

```text
docs/validation/SEG-001-rerun-2026-07-20.md
```

- imágenes exportadas desde caché;
- build limpio no demostrado;
- stack bloqueado por puerto host 5432;
- puerto configurable corregido en `main`.

## Diagnóstico del puerto

Windows:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Puertos reservados:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

Usar `POSTGRES_HOST_PORT=55432` evita detener otro servicio local.

## Advertencia destructiva

```bash
make reset-db
```

o:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

elimina la base local.

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

- shell y PowerShell;
- preflight fail-closed;
- frontend typecheck/build;
- backend verify;
- Compose app/smoke;
- imágenes;
- stack;
- smoke;
- logs y cleanup.

Hasta versionar el lockfile, CI usa `npm install` sin caché npm.
