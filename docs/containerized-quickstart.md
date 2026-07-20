# Inicio rápido completamente contenedorizado

## Objetivo

Levantar PostgreSQL, backend y frontend con Docker Compose, validar health y smoke, y conservar todas las guardas de envío cerradas.

En el host solo requiere Git y Docker con Compose v2. Java 21 se necesita después para Maven verify desde el host.

## Seguridad

No existe adaptador Gmail, SMTP o de correo.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No modificar esos valores.

## 1. Obtener el repositorio

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

Checkout existente:

```bash
git status
git diff --ignore-space-at-eol -- mvnw.cmd
git pull --ff-only
```

Restaurar `mvnw.cmd` solo si su modificación no fue intencional:

```bash
git restore -- mvnw.cmd
```

## 2. Crear `.env`

Linux/macOS:

```bash
[ -f .env ] || cp .env.example .env
```

Windows:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Configurar credenciales bootstrap locales.

Puertos predeterminados:

```dotenv
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Compose publica únicamente en loopback:

| Servicio | Host | Interno |
|---|---:|---:|
| PostgreSQL | `127.0.0.1:55432` | `5432` |
| Backend | `127.0.0.1:8080` | `8080` |
| Frontend | `127.0.0.1:5173` | `8080` |

La red interna usa `postgres:5432`, `backend:8080` y `frontend:8080`.

## 3. Ruta recomendada Windows — validador automático

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

El script:

1. actualiza los tres puertos y `DATABASE_URL`;
2. conserva contraseñas y `SENDING_*`;
3. ejecuta preflight container-only;
4. retira contenedores incompletos sin borrar volumen;
5. construye frontend sin caché;
6. construye backend sin caché;
7. levanta el perfil `app`;
8. espera health de PostgreSQL, backend y frontend;
9. ejecuta smoke PowerShell;
10. ejecuta smoke dentro de Compose;
11. guarda transcript en `validation-output/`;
12. deja el stack activo por `-KeepRunning`.

No usar `-UseBuildCache` como evidencia de cierre.

Puertos alternativos:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

## 4. Ruta manual

### Configurar puertos

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

### Preflight

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Comprueba:

- Git, Docker y Compose;
- `.env`;
- tres puertos válidos y distintos;
- coherencia de `DATABASE_URL`;
- credenciales DB/bootstrap;
- guardas de envío;
- perfiles `app` y `smoke`.

### Build limpio

```bash
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

Una salida completamente `CACHED` no demuestra clean build.

### Levantar

```bash
docker compose --profile app up -d
docker compose --profile app ps
```

Esperado:

```text
postgres   healthy
backend    healthy
frontend   healthy
```

Orden:

1. PostgreSQL;
2. backend aplica Flyway y responde health;
3. frontend inicia después del backend saludable.

### Logs

```bash
docker compose --profile app logs -f
docker compose logs postgres
docker compose logs backend
docker compose logs frontend
```

## 5. Abrir el sistema

Con puertos predeterminados:

```text
Frontend: http://localhost:5173
Health:   http://localhost:8080/actuator/health
Swagger:  http://localhost:8080/swagger-ui/index.html
```

Con puertos alternativos, reemplazar 8080/5173 por `BACKEND_HOST_PORT` y `FRONTEND_HOST_PORT`.

Ingresar con:

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

## 6. Smoke

Host Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Host Unix:

```bash
sh scripts/smoke-test.sh
```

Contenedor contra stack activo:

```bash
docker compose --profile app --profile smoke run --rm smoke
```

Smoke efímero con Make:

```bash
make smoke-container
```

Comprueba:

- backend health;
- Basic Auth contra prospectos;
- documento raíz frontend;
- sin crear datos.

## 7. Pruebas backend

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Debe cubrir compilación, Spotless, unit tests, ArchUnit, Testcontainers, Flyway y Hibernate.

El Dockerfile backend usa `-DskipTests`; el build de imagen no sustituye este control.

## 8. Generar package-lock

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

Dockerfile, CI y Makefile detectan automáticamente:

```text
package-lock presente -> npm ci
package-lock ausente  -> npm install
```

Después de generar el lockfile:

```bash
git status --short
git diff -- frontend/package-lock.json
docker compose --progress plain --profile app build --no-cache frontend
```

## 9. Flujo funcional

1. ingresar al Dashboard;
2. comprobar envíos bloqueados;
3. registrar exclusiones conocidas;
4. preparar CSV o XLSX de hasta 10 MB;
5. ejecutar preview;
6. revisar `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
7. corregir el archivo;
8. ejecutar importación confirmada;
9. revisar prospectos y elegibilidad;
10. revisar auditoría.

Guía completa: `docs/local-development-and-usage.md`.

## 10. Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

La segunda operación es destructiva.

## 11. Diagnóstico

### Puerto ocupado

Cambiar los tres puertos coordinadamente:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55433 `
  -BackendPort 18080 `
  -FrontendPort 15173
```

Windows, identificar proceso:

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Puertos reservados:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

### Backend no saludable

```bash
docker compose logs backend
```

Revisar PostgreSQL, credenciales, Flyway, Hibernate y memoria Docker.

### Frontend 502

```bash
docker compose logs backend
docker compose logs frontend
```

Nginx necesita resolver `backend:8080` dentro de Compose.

### Smoke falla

```bash
docker compose --profile app --profile smoke logs --no-color
```

### Contraseña PostgreSQL cambiada

La contraseña se fija al crear el volumen. Solo para un entorno descartable:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
docker compose --profile app up -d --build
```

## 12. Evidencia

El validador Windows escribe:

```text
validation-output/seg001-docker-YYYYMMDD-HHMMSS.log
```

El directorio está ignorado por Git. Resumir el resultado en `docs/validation/SEG-001.md`.

## Limitaciones actuales

- clean builds y orquestador pendientes de ejecución real;
- Maven/Testcontainers/Flyway/Hibernate pendientes;
- falta versionar package-lock;
- npm ci preparado, pero pendiente de lockfile;
- HTTP Basic temporal;
- stack local, no productivo.
