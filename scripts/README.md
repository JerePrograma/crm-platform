# Automatización local

## Objetivo

Scripts reproducibles para configurar, construir y comprobar Gestudio CRM. No despliegan, no importan datos y no realizan comunicaciones.

## Configurar puertos host

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Linux/macOS:

```bash
sh scripts/set-local-host-ports.sh 55432 8080 5173
```

Con Make:

```bash
make local-ports
```

Los scripts:

- añaden o actualizan `POSTGRES_HOST_PORT`;
- añaden o actualizan `BACKEND_HOST_PORT`;
- añaden o actualizan `FRONTEND_HOST_PORT`;
- actualizan `DATABASE_URL`;
- conservan nombre de base, usuarios y contraseñas;
- conservan las cuatro variables `SENDING_*`;
- rechazan puertos inválidos o repetidos;
- no imprimen secretos;
- PowerShell escribe UTF-8 sin BOM.

Los helpers históricos `set-postgres-host-port.ps1` y `.sh` siguen disponibles como wrappers compatibles.

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

- Git, Docker y Compose;
- Java/Node/npm en modo local;
- `.env`;
- tres puertos entre 1 y 65535;
- puertos distintos;
- `DATABASE_URL` coordinada;
- credenciales DB y bootstrap;
- cuatro guardas de envío cerradas;
- Compose con perfiles `app` y `smoke`.

No imprime contraseñas ni inicia servicios.

## Validación Docker automatizada — Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Flujo:

1. configura puertos;
2. ejecuta preflight container-only;
3. retira contenedores incompletos sin borrar volumen;
4. construye frontend sin caché;
5. construye backend sin caché;
6. inicia el perfil `app`;
7. espera health de PostgreSQL, backend y frontend;
8. ejecuta smoke PowerShell;
9. ejecuta smoke contenedorizado;
10. guarda transcript en `validation-output/`;
11. retira el stack, salvo `-KeepRunning`.

Parámetros:

```text
-PostgresPort
-BackendPort
-FrontendPort
-KeepRunning
-UseBuildCache
```

No utilizar `-UseBuildCache` como evidencia de cierre.

El validador no ejecuta Maven verify, Testcontainers ni genera el lockfile.

## Builds limpios manuales

```bash
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

Una salida completamente `CACHED` no demuestra compilación limpia.

## Smoke host

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Los URLs se derivan de:

```text
BACKEND_HOST_PORT
FRONTEND_HOST_PORT
```

Overrides opcionales:

```text
BACKEND_URL
FRONTEND_URL
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

Contra un stack ya activo:

```bash
docker compose --profile app --profile smoke run --rm smoke
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

Dockerfile, Makefile y CI detectan automáticamente:

```text
package-lock presente -> npm ci
package-lock ausente  -> npm install
```

## Makefile

```bash
make preflight
make preflight-container
make postgres-port
make local-ports
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
| `preflight-container` | valida modalidad Docker-only |
| `postgres-port` | compatibilidad: configura PostgreSQL y completa otros puertos |
| `local-ports` | configura 55432/8080/5173 coordinadamente |
| `db-up` | inicia PostgreSQL |
| `db-down` | detiene PostgreSQL conservando volumen |
| `app-up` | construye e inicia stack |
| `app-down` | retira stack conservando volumen |
| `app-logs` | sigue logs |
| `backend` | ejecuta Spring Boot |
| `frontend` | instala con npm ci/install y ejecuta Vite |
| `frontend-lock` | genera package-lock con Docker |
| `verify` | backend, frontend, Compose y clean builds |
| `smoke` | prueba stack activo |
| `smoke-container` | prueba stack efímero |
| `reset-db` | elimina stack y volumen |

## Evidencia

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-static-automation-2026-07-20.md
```

Estado demostrado:

- preflight inicial PASS;
- npm install PASS;
- fallo TypeScript reproducido y corregido;
- imágenes exportadas desde caché;
- conflicto 5432 reproducido;
- tres puertos configurables implementados;
- configurador Unix probado preservando secretos ficticios, UTF-8 y guardas;
- orquestador Windows pendiente de ejecución real.

## Evidencia local

```text
validation-output/
```

está ignorado por Git. Los transcripts deben revisarse y resumirse en `docs/validation/`, no versionarse automáticamente.

## Advertencia destructiva

```bash
make reset-db
```

O:

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
- configuradores de puertos;
- orquestador Docker a nivel de parser;
- preflight fail-closed;
- frontend typecheck/build;
- backend verify;
- Compose app/smoke;
- imágenes;
- stack;
- smoke;
- logs y cleanup.

Hasta versionar package-lock, CI utiliza npm install; después adoptará npm ci automáticamente.
