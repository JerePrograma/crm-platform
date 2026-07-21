# Inicio rápido completamente contenedorizado

## Objetivo

Levantar y validar PostgreSQL, backend y frontend con Docker, sin instalar Java, Maven, Node o npm en el host.

El recorrido recomendado incluye:

- builds limpios;
- Flyway/Hibernate;
- health checks;
- smoke host/contenedor;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- package-lock;
- npm ci;
- seguridad del repositorio;
- evidencia JSON y transcript.

## Seguridad

No existe adaptador Gmail, SMTP o de correo.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No modificar estos valores.

## 1. Obtener el repositorio

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

Checkout existente:

```bash
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

Restaurar `mvnw.cmd` solo cuando su modificación no haya sido intencional:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git restore -- mvnw.cmd
```

El validador integral exige un working tree sin cambios inesperados.

La rama histórica está detrás y no contiene trabajo exclusivo.

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

Valores recomendados:

```dotenv
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Mapeos:

| Servicio | Host | Interno |
|---|---:|---:|
| PostgreSQL | `127.0.0.1:55432` | `5432` |
| Backend | `127.0.0.1:8080` | `8080` |
| Frontend | `127.0.0.1:5173` | `8080` |

La red interna usa `postgres:5432`, `backend:8080` y `frontend:8080`.

## 3. Validación integral Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

## 4. Validación integral Linux/macOS

```bash
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 8080 \
  --frontend-port 5173 \
  --keep-running
```

Con Make y cleanup final:

```bash
make validate-seg001
```

No usar `-UseBuildCache` ni `--use-build-cache` como evidencia de cierre.

## 5. Fases del recorrido integral

1. confirma `main` y working tree limpio;
2. coordina puertos y `DATABASE_URL`;
3. comprueba el daemon Docker;
4. ejecuta preflight;
5. valida Compose;
6. limpia contenedores sin borrar volumen;
7. construye frontend/backend sin caché;
8. levanta el perfil `app`;
9. espera health de PostgreSQL, backend y frontend;
10. ejecuta smoke host;
11. ejecuta smoke contenedorizado;
12. ejecuta Maven verify/Testcontainers dentro de Docker;
13. genera package-lock-only sin scripts npm;
14. preserva UID/GID del lockfile en Unix;
15. calcula SHA-256;
16. reconstruye frontend mediante npm ci;
17. recrea frontend y espera health;
18. repite smoke;
19. ejecuta seguridad del repositorio;
20. genera JSON y transcript;
21. deja package-lock sin commit para revisión.

## 6. Puertos alternativos

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

## 8. Evidencia

```text
validation-output/seg001-docker-*.json
validation-output/seg001-complete-*.json
validation-output/seg001-complete-*.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git.

Revisar transcripts antes de compartirlos. Los scripts no imprimen contraseñas, pero los logs de la aplicación podrían contener contexto operativo.

## 9. Abrir el sistema

Puertos predeterminados:

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

Después de revisar:

```bash
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar `.env`, `validation-output/` ni el XLSX operativo.

## 11. Repetir sobre lockfile versionado

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

La segunda ejecución debe utilizar npm ci desde el primer build.

## 12. Validar solamente el stack Docker

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Este script valida imágenes, Compose, health y smoke, pero no Maven verify ni package-lock.

## 13. Backend verify separado

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

Ejecuta Maven verify dentro de Docker con el repositorio en solo lectura y Testcontainers mediante el socket Docker.

Advertencia: el socket Docker concede privilegios elevados. Ejecutar únicamente sobre código propio y revisado.

## 14. Lockfile separado

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Comando npm:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No crea node_modules ni ejecuta lifecycle scripts. En Unix preserva UID/GID y comprueba que el archivo quede editable.

## 15. Seguridad del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

Bloquea `.env`, evidencia, datos privados, lote operativo, claves, certificados y JSON de credenciales rastreados.

## 16. Ruta manual

### Configurar puertos

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

### Preflight

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Unix:

```bash
sh scripts/preflight.sh --container-only
```

### Builds limpios

```bash
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

### Levantar

```bash
docker compose --profile app up -d
docker compose --profile app ps
```

### Smoke

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
docker compose --profile app --profile smoke run --rm smoke
```

Unix:

```bash
sh scripts/smoke-test.sh
docker compose --profile app --profile smoke run --rm smoke
```

## 17. Flujo funcional

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

## 18. Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

La segunda operación es destructiva.

## 19. Diagnóstico

### Puerto ocupado

Utilizar puertos alternativos en el validador integral.

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

### Testcontainers no conecta

Revisar:

- Docker activo;
- contenedores Linux;
- socket `/var/run/docker.sock`;
- `host.docker.internal`;
- permisos del daemon.

### Lockfile propiedad de root

Actualizar `main` y volver a ejecutar `scripts/generate-frontend-lock.sh`. La versión actual utiliza UID/GID del host.

### Contraseña PostgreSQL cambiada

Solo para entorno descartable:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
docker compose --profile app up -d --build
```

## Limitaciones actuales

- validadores integrales pendientes de ejecución real;
- clean builds pendientes;
- Maven/Testcontainers/Flyway/Hibernate pendientes;
- package-lock pendiente;
- npm ci pendiente de evidencia real;
- CI no muestra runs visibles;
- HTTP Basic temporal;
- stack local, no productivo.
