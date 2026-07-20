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
git status
git diff --ignore-space-at-eol -- mvnw.cmd
git pull --ff-only
```

Restaurar `mvnw.cmd` solo cuando su modificación no haya sido intencional:

```bash
git restore -- mvnw.cmd
```

El validador integral exige archivos rastreados limpios.

## 2. Crear `.env`

Unix:

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

Fases:

1. confirma `main` y árbol rastreado limpio;
2. coordina puertos y `DATABASE_URL`;
3. ejecuta preflight;
4. valida Compose;
5. limpia contenedores sin borrar volumen;
6. construye frontend/backend sin caché;
7. levanta el perfil `app`;
8. espera health de los tres servicios;
9. ejecuta smoke PowerShell;
10. ejecuta smoke contenedorizado;
11. ejecuta Maven verify/Testcontainers dentro de Docker;
12. genera package-lock-only sin scripts npm;
13. reconstruye frontend mediante npm ci;
14. recrea frontend y espera health;
15. repite smoke;
16. ejecuta seguridad del repositorio;
17. genera JSON y transcript;
18. deja package-lock sin commit para revisión.

No usar `-UseBuildCache` como evidencia de cierre.

Puertos alternativos:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

## 4. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

Servicios:

```text
postgres   healthy
backend    healthy
frontend   healthy
```

## 5. Evidencia

```text
validation-output/seg001-docker-*.json
validation-output/seg001-complete-*.json
validation-output/seg001-complete-*.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git.

Revisar transcripts antes de compartirlos. Los scripts no imprimen contraseñas, pero los logs de la aplicación podrían contener contexto operativo.

## 6. Abrir el sistema

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

## 7. Revisar package-lock

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Después de revisar:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar `.env` ni `validation-output/`.

## 8. Repetición sobre lockfile versionado

```powershell
git pull --ff-only
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

La segunda ejecución debe utilizar npm ci desde el primer build.

## 9. Ruta Unix Docker-only

```bash
git switch main
git pull --ff-only
sh scripts/set-local-host-ports.sh 55432 8080 5173
make verify-container
```

`make verify-container` ejecuta:

- preflight container-only;
- backend Maven verify/Testcontainers en Docker;
- package-lock-only;
- Compose config;
- builds limpios;
- smoke contenedorizado;
- seguridad del repositorio.

Targets individuales:

```bash
make repository-safety
make backend-verify-container
make frontend-lock
make smoke-container
```

## 10. Validar solamente el stack Docker

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Este script valida imágenes, Compose, health y smoke, pero no Maven verify ni package-lock.

## 11. Backend verify separado

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

## 12. Lockfile separado

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Comando npm utilizado:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No crea node_modules ni ejecuta lifecycle scripts.

## 13. Seguridad del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

Bloquea `.env`, evidencia, datos privados, lote operativo, claves, certificados y JSON de credenciales rastreados.

## 14. Ruta manual

### Configurar puertos

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

### Preflight

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

### Builds limpios

```powershell
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

### Levantar

```powershell
docker compose --profile app up -d
docker compose --profile app ps
```

### Smoke

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
docker compose --profile app --profile smoke run --rm smoke
```

## 15. Flujo funcional

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

## 16. Detener

Conservar datos:

```bash
docker compose --profile app down
```

Eliminar también la base:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

La segunda operación es destructiva.

## 17. Diagnóstico

### Puerto ocupado

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55433 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Identificar proceso:

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

- Docker Desktop activo;
- contenedores Linux;
- socket `/var/run/docker.sock`;
- `host.docker.internal`;
- permisos del daemon.

### Contraseña PostgreSQL cambiada

Solo para entorno descartable:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
docker compose --profile app up -d --build
```

## Limitaciones actuales

- validador integral pendiente de ejecución real;
- clean builds pendientes;
- Maven/Testcontainers/Flyway/Hibernate pendientes;
- package-lock pendiente;
- npm ci pendiente de evidencia real;
- CI no muestra runs visibles;
- HTTP Basic temporal;
- stack local, no productivo.
