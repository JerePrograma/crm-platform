# Automatización local

## Objetivo

Esta carpeta contiene controles reproducibles para preparar y comprobar un entorno local de Gestudio CRM. Los scripts no despliegan, no importan datos y no realizan comunicaciones.

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
make preflight-container
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
| `preflight` | valida herramientas locales, variables y Compose |
| `preflight-container` | valida modalidad solo Docker |
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

## Integración continua

CI valida:

- sintaxis `sh` de ambos scripts Unix;
- sintaxis PowerShell de ambos scripts Windows;
- preflight fail-closed con credenciales ficticias;
- Compose con perfil `app`;
- imágenes backend y frontend.

Los smoke tests no se ejecutan en CI todavía porque requieren levantar y esperar el stack completo; siguen siendo un control de cierre manual de SEG-001.
