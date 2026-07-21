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

Evidencia real acumulada:

```text
PowerShell syntax: PASS, 11 scripts
preflight container-only: PASS
frontend clean build --no-cache: PASS
TypeScript strict/Vite: PASS
backend clean image build --no-cache: PASS
Maven package con tests omitidos: PASS_PARTIAL
Flyway V1-V5/Hibernate: PASS
PostgreSQL/backend/frontend: healthy
smoke host/contenedor: PASS
Maven verify/Testcontainers: PASS
tests: PASS, 29/29
Spotless: PASS, 55/55
package-lock/npm ci: PASS
repository safety: PASS
CI run 29848718163: success
```

La evidencia final se ejecutó sobre `d8a5a449…` con puertos
`25432/8080/5173`, lockfile versionado y un primer build frontend mediante
`npm ci`.

Como antecedente, el checker anterior comprobó que Windows podía enlazar
`127.0.0.1:15432`, pero Docker respondió:

```text
Bind for 0.0.0.0:15432 failed: port is already allocated
```

El checker actualizado inspecciona publicaciones Docker y la pila Windows. El validador inicia PostgreSQL antes de los builds para probar la publicación real sin desperdiciar una reconstrucción completa.

Evidencia:

```text
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
```

## Sintaxis PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
```

Parsea todos los archivos `.ps1` y reporta archivo, línea, columna y mensaje. No continuar con Docker si falla.

## Comprobar puertos Windows y Docker

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Por cada puerto, el checker:

1. ejecuta `docker ps`;
2. detecta publicaciones activas `:<puerto>->`;
3. informa ID, nombre y puertos del contenedor propietario;
4. prueba el enlace exclusivo sobre `127.0.0.1` mediante `TcpListener`;
5. falla antes de builds.

Diagnóstico manual:

```powershell
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
docker ps --filter publish=15432 --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
Get-NetTCPConnection -LocalPort 15432 -ErrorAction SilentlyContinue
netsh interface ipv4 show excludedportrange protocol=tcp
```

Interpretación:

- aparece un contenedor Docker: detener o reconfigurar únicamente ese contenedor;
- aparece un listener Windows: detener el proceso propietario o elegir otro puerto;
- el puerto pertenece a un rango excluido: elegir otro puerto;
- no modificar rangos excluidos ni usar `docker system prune` como parte de la validación.

Un `TcpListener` verde por sí solo no demuestra que Docker Desktop pueda publicar el puerto.

## Validación integral recomendada

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Cuando `15432` esté ocupado, validar primero otro valor, por ejemplo `25432`, y usarlo en el validador.

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

El validador Windows ejecuta:

1. rama y working tree;
2. configuración segura de puertos;
3. preflight container-only;
4. Compose config;
5. cleanup sin `-v`;
6. publicaciones Docker y enlace Windows;
7. arranque real de PostgreSQL;
8. health de PostgreSQL;
9. builds frontend/backend sin caché;
10. arranque y health backend/frontend;
11. smoke host y contenedor;
12. Maven verify/Spotless/tests/ArchUnit/Testcontainers en Docker;
13. generación package-lock-only;
14. SHA-256;
15. build frontend mediante npm ci;
16. recreación y health frontend;
17. smoke final;
18. seguridad del repositorio;
19. JSON y transcript;
20. cleanup opcional.

No usar `-UseBuildCache` ni `--use-build-cache` como evidencia de cierre.

Evidencia local:

```text
validation-output/seg001-complete-*.log
validation-output/seg001-complete-*.json
validation-output/seg001-docker-*.json
frontend/package-lock.json
```

`validation-output/` permanece fuera de Git.

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

No inicia servicios. Tampoco sustituye la comprobación real de publicaciones Docker.

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
5. checker Docker/Windows;
6. `docker compose up -d postgres`;
7. health PostgreSQL;
8. builds limpios;
9. `docker compose up -d backend frontend`;
10. health backend/frontend;
11. smoke host;
12. smoke contenedor;
13. JSON y transcript.

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
postgresBinding
cleanBuilds
services
smokeHost
smokeContainer
stackKeptRunning
error
```

`stackKeptRunning=true` solo cuando Compose reporta contenedores del proyecto. La presencia de `-KeepRunning` no basta.

Ante un fallo, el script imprime:

```text
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
docker compose ... ps
docker compose ... logs
```

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
- detección de la API del daemon y propagación mediante
  `JAVA_TOOL_OPTIONS=-Dapi.version=...`;
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

Levantar solo PostgreSQL:

```bash
docker compose --profile app up -d postgres
```

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

No usar `docker system prune` como procedimiento normal de diagnóstico.

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
