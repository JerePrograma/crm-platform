# Automatización local

## Objetivo

Esta carpeta contiene controles reproducibles para preparar, construir y comprobar un entorno local de Gestudio CRM. Los scripts no despliegan, no importan datos y no realizan comunicaciones.

## Preflight

### Modo de herramientas locales

Comprueba Git, Docker, Compose, Java, Node, npm, `.env`, variables y guardas.

Linux/macOS:

```bash
sh scripts/preflight.sh --local
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

### Modo completamente contenedorizado

Comprueba Git, Docker, Compose, `.env`, variables y guardas. No requiere Java, Node o npm en el host.

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

El preflight comprueba siempre:

- existencia de `.env`;
- `POSTGRES_DB`, URL, usuario y contraseña de base;
- credenciales bootstrap locales;
- las cuatro guardas de envío cerradas;
- parseo del perfil completo de Docker Compose.

No imprime contraseñas ni inicia servicios.

## Smoke test contra stack activo

Requiere PostgreSQL, backend y frontend ya activos.

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba:

- `GET /actuator/health` con estado `UP`;
- acceso autenticado a la página de prospectos;
- entrega del documento raíz del frontend.

No crea prospectos, exclusiones ni importaciones.

## Smoke test completamente contenedorizado

El perfil `smoke` usa un contenedor efímero y espera que frontend y backend estén saludables.

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

El target Make retira los contenedores al finalizar y conserva el volumen PostgreSQL. En CI se imprime el log completo si el smoke falla y luego se elimina también el volumen de CI.

## Generar `package-lock.json` mediante Docker

Este recorrido permite generar el lockfile sin instalar Node o npm en el host.

### Linux/macOS

```bash
sh scripts/generate-frontend-lock.sh
```

Con Make:

```bash
make frontend-lock
```

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Los scripts:

1. comprueban Docker;
2. localizan `frontend/package.json` desde la raíz real del repositorio;
3. ejecutan `node:22-alpine` con el directorio frontend montado;
4. ejecutan `npm install --no-audit --no-fund`;
5. verifican que exista `frontend/package-lock.json`;
6. no realizan commits automáticamente.

Después:

```bash
git status --short
git diff -- frontend/package-lock.json
```

Revisar y versionar el archivo. Una vez disponible, Dockerfile y CI deben migrarse de `npm install` a `npm ci` y repetir toda la matriz.

## Variables opcionales del smoke test de host

Por defecto:

```text
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173
```

Pueden reemplazarse en la sesión sin modificar `.env`.

## Makefile

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

### Targets

| Target | Acción |
|---|---|
| `preflight` | valida herramientas locales, variables y Compose |
| `preflight-container` | valida modalidad solo Docker |
| `db-up` | inicia únicamente PostgreSQL |
| `db-down` | detiene PostgreSQL conservando el volumen |
| `app-up` | construye e inicia PostgreSQL, backend y frontend |
| `app-down` | retira el stack conservando el volumen |
| `app-logs` | sigue logs de todo el perfil `app` |
| `backend` | ejecuta Spring Boot desde Maven Wrapper |
| `frontend` | instala dependencias y ejecuta Vite |
| `frontend-lock` | genera `package-lock.json` mediante Node en Docker |
| `verify` | ejecuta backend, frontend, Compose y builds de imágenes |
| `smoke` | prueba servicios ya activos desde el host |
| `smoke-container` | construye, levanta, prueba y retira stack efímero |
| `reset-db` | elimina contenedores y volumen local |

## Primer build real registrado

El 20 de julio de 2026, el preflight PowerShell en modo container-only pasó y el build frontend llegó a ejecutar TypeScript. Se reprodujeron y corrigieron:

- credenciales anulables dentro de callbacks;
- declaración ausente para imports CSS/Vite.

Evidencia:

```text
docs/validation/SEG-001-container-build-2026-07-20.md
```

La próxima operación debe reconstruir frontend desde `main`, después backend y finalmente smoke.

## Advertencia destructiva

```bash
make reset-db
```

o:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

elimina la base PostgreSQL local. No ejecutar si el volumen contiene información que deba conservarse.

## Controles de envío

Los scripts exigen:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

El preflight falla si cualquiera de estos valores cambia.

## Integración continua

CI valida:

- sintaxis `sh` de preflight, smoke y generación de lockfile;
- sintaxis PowerShell de preflight, smoke y generación de lockfile;
- preflight fail-closed con credenciales ficticias;
- frontend typecheck y build;
- Compose con perfiles `app` y `smoke`;
- imágenes backend y frontend;
- arranque de PostgreSQL, backend y frontend;
- smoke test contenedorizado;
- logs en fallo y limpieza obligatoria.

Hasta versionar `package-lock.json`, CI usa `npm install` sin caché npm.