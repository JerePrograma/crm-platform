# Automatización local

## Objetivo

Esta carpeta contiene controles reproducibles para preparar y comprobar un entorno local de Gestudio CRM. Los scripts no despliegan, no importan datos y no realizan comunicaciones.

## Preflight

### Linux y macOS

```bash
sh scripts/preflight.sh
```

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

El preflight comprueba:

- Git, Docker, Compose, Java, Node y npm;
- existencia de `.env`;
- variables de PostgreSQL;
- credenciales bootstrap locales;
- las cuatro guardas de envío cerradas;
- parseo del perfil completo de Docker Compose.

No imprime contraseñas ni inicia servicios.

## Smoke test

Requiere PostgreSQL, backend y frontend activos.

### Linux y macOS

```bash
sh scripts/smoke-test.sh
```

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba:

- `GET /actuator/health` con estado `UP`;
- acceso autenticado a la página de prospectos;
- entrega del documento raíz del frontend.

No crea prospectos, exclusiones ni importaciones.

## Variables opcionales del smoke test

Por defecto:

```text
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173
```

Pueden reemplazarse en la sesión sin modificar `.env`.

## Makefile

En sistemas con `make`:

```bash
make preflight
make db-up
make app-up
make app-logs
make smoke
make verify
make app-down
```

### Targets

| Target | Acción |
|---|---|
| `preflight` | valida herramientas, variables y Compose |
| `db-up` | inicia únicamente PostgreSQL |
| `db-down` | detiene PostgreSQL conservando el volumen |
| `app-up` | construye e inicia PostgreSQL, backend y frontend |
| `app-down` | retira el stack conservando el volumen |
| `app-logs` | sigue logs de todo el perfil `app` |
| `backend` | ejecuta Spring Boot desde Maven Wrapper |
| `frontend` | instala dependencias y ejecuta Vite |
| `verify` | ejecuta backend, frontend, Compose y builds de imágenes |
| `smoke` | ejecuta el smoke test contra servicios activos |
| `reset-db` | elimina contenedores y volumen local |

## Advertencia destructiva

```bash
make reset-db
```

o:

```bash
docker compose --profile app down -v
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
