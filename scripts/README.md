# Automatización local

## Objetivo

Scripts reproducibles para configurar, construir, verificar y operar Gestudio CRM localmente.

Ningún script:

- despliega producción;
- habilita envíos;
- importa el XLSX real;
- realiza commits;
- elimina el volumen PostgreSQL salvo comandos destructivos explícitos.

## Estado de validación actual

Evidencia real disponible:

```text
PowerShell syntax: PASS
preflight container-only: PASS
frontend clean build: PASS
backend clean image build: PASS
último fallo: 127.0.0.1:55432 no enlazable en Windows
```

La próxima ejecución debe comprobar primero puertos alternativos mediante `check-host-ports.ps1`.

## Sintaxis PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
```

Parsea todos los archivos `.ps1` y reporta archivo, línea, columna y mensaje. No continuar con Docker si falla.

## Comprobar puertos Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

El checker intenta enlazar cada valor sobre `127.0.0.1` mediante `TcpListener`.

Cuando falla, diagnosticar:

```powershell
Get-NetTCPConnection -LocalPort 55432 -ErrorAction SilentlyContinue
netsh interface ipv4 show excludedportrange protocol=tcp
```

No modificar rangos excluidos de Windows como parte de la validación. Elegir otro puerto.

## Validación integral recomendada

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
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

Requisitos:

- rama `main`;
- working tree sin cambios inesperados;
- `.env` existente;
- Git;
- Docker con daemon accesible;
- Docker Compose v2;
- PowerShell o Bash según plataforma.

No requiere Java, Maven, Node o npm instalados en el host.

Los validadores ejecutan:

1. rama y working tree;
2. configuración segura de puertos;
3. preflight container-only;
4. Compose config;
5. cleanup sin `-v`;
6. comprobación real de enlace Windows antes de builds;
7. builds frontend/backend sin caché;
8. arranque y health de tres servicios;
9. smoke host y contenedor;
10. Maven verify/Spotless/tests/ArchUnit/Testcontainers en Docker;
11. generación package-lock-only;
12. SHA-256;
13. build frontend mediante npm ci;
14. recreación y health frontend;
15. smoke final;
16. seguridad del repositorio;
17. JSON y transcript;
18. cleanup opcional.

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
  -PostgresPort 15432 `
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

Los configuradores:

- actualizan `POSTGRES_HOST_PORT`;
- actualizan `BACKEND_HOST_PORT`;
- actualizan `FRONTEND_HOST_PORT`;
- coordinan `DATABASE_URL`;
- conservan nombre de base, usuarios y contraseñas;
- conservan `SENDING_*`;
- rechazan puertos inválidos o repetidos;
- no imprimen secretos;
- escriben UTF-8 sin BOM en PowerShell.

Los wrappers `set-postgres-host-port.ps1` y `.sh` conservan compatibilidad.

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
- Docker y daemon;
- Docker Compose v2;
- Java/Node/npm en modo local;
- `.env`;
- tres puertos válidos y distintos;
- `DATABASE_URL` coordinada;
- credenciales DB y bootstrap;
- cuatro guardas de envío;
- perfiles `app` y `smoke`.

No inicia servicios. La disponibilidad real de puertos Windows se comprueba después del cleanup mediante `check-host-ports.ps1`.

## Validación Docker del stack

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Fases:

1. configuración de puertos;
2. preflight;
3. Compose config;
4. cleanup no destructivo;
5. comprobación de enlace de puertos;
6. builds limpios;
7. arranque;
8. health;
9. smoke host;
10. smoke contenedor;
11. JSON y transcript.

Parámetros:

```text
-PostgresPort
-BackendPort
-FrontendPort
-KeepRunning
-UseBuildCache
-NoTranscript
```

`-NoTranscript` se usa desde `validate-seg001.ps1`.

El JSON incluye:

```text
ports
hostPorts
cleanBuilds
services
smokeHost
smokeContainer
stackKeptRunning
error
```

`stackKeptRunning=true` solo cuando existen contenedores ejecutándose.

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

Ejecuta:

```text
mvn -B -f backend/pom.xml verify
```

Cubre:

- compilación;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- migraciones e integración cubiertas por la suite.

Características:

- Maven 3.9.16 y Java 21 dentro de Docker;
- código montado read-only;
- `target` efímero;
- caché Maven persistente;
- socket Docker;
- `TESTCONTAINERS_HOST_OVERRIDE`;
- cleanup del contenedor y target.

El socket Docker es una operación privilegiada. Ejecutar solo sobre código propio y revisado.

## Generar package-lock sin Node local

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Ejecuta:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

Garantías:

- no ejecuta lifecycle scripts;
- no debe crear `node_modules`;
- Unix conserva UID/GID;
- no hace commit;
- el lockfile queda para revisión.

## Smoke

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Unix:

```bash
sh scripts/smoke-test.sh
```

Contenedor:

```bash
docker compose --profile app --profile smoke run --rm smoke
```

Comprueba:

- `/actuator/health`;
- API autenticada de prospectos;
- documento raíz frontend;
- sin creación de datos;
- sin comunicaciones.

Las URLs se derivan de `.env`, con overrides opcionales `BACKEND_URL` y `FRONTEND_URL`.

## Seguridad del repositorio

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
```

Unix:

```bash
sh scripts/check-repository-safety.sh
```

Bloquea que Git rastree:

- `.env`;
- `validation-output/`;
- datos privados;
- `gestudio_lote_*_prospectos.xlsx`;
- claves y certificados;
- JSON de credenciales;
- errores de `git diff --check`.

## Docker Compose manual

Levantar app:

```bash
docker compose --profile app up -d --build
```

Ver estado:

```bash
docker compose --profile app ps
```

Logs:

```bash
docker compose --profile app logs -f
```

Detener sin borrar base:

```bash
docker compose --profile app down --remove-orphans
```

Borrar también volumen PostgreSQL, solo con intención destructiva:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

## Makefile

Targets:

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

## Guardas obligatorias

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Los scripts fallan cuando alguna guarda se abre.
