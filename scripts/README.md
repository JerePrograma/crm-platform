# Automatización local

## Objetivo

Scripts reproducibles para preparar, construir y comprobar Gestudio CRM. No despliegan, no importan datos y no realizan comunicaciones.

## Configurar el puerto PostgreSQL host

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-postgres-host-port.ps1 -Port 55432
```

Linux/macOS:

```bash
sh scripts/set-postgres-host-port.sh 55432
```

Con Make:

```bash
make postgres-port
```

Los scripts:

- añaden o actualizan `POSTGRES_HOST_PORT`;
- actualizan `DATABASE_URL` con el mismo puerto;
- conservan `POSTGRES_DB`;
- conservan usuarios y contraseñas;
- conservan las cuatro variables `SENDING_*`;
- no imprimen secretos;
- PowerShell escribe UTF-8 sin BOM.

Configuración resultante recomendada:

```dotenv
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Compose publica el puerto host hacia el `5432` interno. El backend contenedorizado usa `postgres:5432`.

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

- `.env`;
- `POSTGRES_DB`;
- `POSTGRES_HOST_PORT` entre 1 y 65535;
- `DATABASE_URL` con el mismo puerto;
- credenciales DB y bootstrap;
- guardas de envío;
- Compose.

No imprime contraseñas ni inicia servicios.

## Builds limpios

```bash
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

`--progress` debe ir antes de `build`. Una salida completamente `CACHED` no demuestra compilación limpia.

## Smoke host

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba health, API autenticada y frontend. No crea datos.

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

Retirar:

```bash
docker compose --profile app --profile smoke down --remove-orphans
```

## Generar package-lock

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Linux/macOS:

```bash
sh scripts/generate-frontend-lock.sh
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

Después se debe migrar Dockerfile/CI a `npm ci` y repetir la matriz.

## Variables smoke host

```text
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173
```

Pueden sobrescribirse en la sesión.

## Makefile

```bash
make preflight
make preflight-container
make postgres-port
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
| `preflight` | valida herramientas locales |
| `preflight-container` | valida modalidad Docker-only |
| `postgres-port` | configura 55432 y DATABASE_URL |
| `db-up` | inicia PostgreSQL |
| `db-down` | detiene PostgreSQL conservando volumen |
| `app-up` | construye e inicia stack |
| `app-down` | retira stack conservando volumen |
| `app-logs` | sigue logs |
| `backend` | ejecuta Spring Boot |
| `frontend` | ejecuta Vite |
| `frontend-lock` | genera package-lock con Docker |
| `verify` | backend, frontend, Compose y clean builds |
| `smoke` | prueba stack activo |
| `smoke-container` | prueba stack efímero |
| `reset-db` | elimina stack y volumen |

## Evidencias

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-static-automation-2026-07-20.md
```

Estado demostrado:

- preflight real PASS;
- npm install PASS;
- fallo TypeScript reproducido y corregido;
- imágenes exportadas desde caché;
- conflicto 5432 reproducido;
- puerto configurable corregido;
- actualizador Unix probado preservando secretos y UTF-8.

## Diagnóstico 5432

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

Usar 55432 evita detener otro servicio.

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

Valida scripts Unix/PowerShell, actualizador de puerto, preflight, frontend, backend, Compose, imágenes, stack, smoke, logs y cleanup.

Hasta versionar package-lock, CI usa `npm install` sin caché npm.
