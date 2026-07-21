# SEG-001 — fallo de puerto publicado por Docker — 2026-07-21

## Estado

```text
EXECUTED_FAIL — DOCKER_HOST_PORT_ALREADY_ALLOCATED
```

La ejecución se realizó en Windows sobre:

```text
commit: f903a9e1278697af53e0bcbee3bd10b16e10b991
rama: main
Docker: 29.3.1
PostgreSQL host port: 15432
Backend host port: 8080
Frontend host port: 5173
```

## Comando

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

## Controles aprobados

```text
checkout main actualizado: PASS
working tree inicial: PASS
PowerShell syntax, 11 scripts: PASS
preflight container-only: PASS
Docker daemon: PASS
Compose config: PASS
guardas de envío: PASS
Windows TcpListener 127.0.0.1:15432: PASS
Windows TcpListener 127.0.0.1:8080: PASS
Windows TcpListener 127.0.0.1:5173: PASS
frontend clean build --no-cache: PASS
TypeScript strict build: PASS
Vite production build: PASS
backend clean image build --no-cache: PASS
Maven package con tests omitidos: PASS
```

## Primer fallo real

Después de construir ambas imágenes, Compose intentó iniciar el stack:

```text
docker compose --profile app up -d
```

Docker respondió:

```text
Bind for 0.0.0.0:15432 failed: port is already allocated
```

El checker anterior solamente probaba el enlace mediante `TcpListener` en la pila de red de Windows. Docker Desktop puede conservar o detectar una publicación perteneciente a otro contenedor sin que ese control de Windows la vea de la misma manera.

El problema no corresponde a:

- TypeScript;
- Vite;
- compilación Java;
- resolución Maven;
- PostgreSQL dentro del contenedor;
- Flyway;
- Hibernate.

El contenedor PostgreSQL no llegó a iniciar.

## Alcance no ejecutado

```text
PostgreSQL health: NOT_RUN
backend startup/health: NOT_RUN
frontend startup/health: NOT_RUN
Flyway startup: NOT_RUN
Hibernate validate startup: NOT_RUN
smoke host: NOT_RUN
smoke container: NOT_RUN
Maven verify: NOT_RUN
Spotless: NOT_RUN
unit tests: NOT_RUN
ArchUnit: NOT_RUN
Testcontainers: NOT_RUN
package-lock generation: NOT_RUN
npm ci rebuild: NOT_RUN
final smoke: NOT_RUN
repository safety final: NOT_RUN
```

## Correcciones aplicadas en main

### Propiedad de puertos Docker

`scripts/check-host-ports.ps1` ahora:

1. consulta contenedores activos mediante `docker ps`;
2. detecta publicaciones `:<puerto>->`;
3. informa ID, nombre y puertos del contenedor propietario;
4. falla antes de cualquier build;
5. conserva la comprobación `TcpListener` de Windows.

### PostgreSQL antes de builds

`scripts/validate-docker-stack.ps1` ahora ejecuta este orden:

1. preflight;
2. Compose config;
3. cleanup sin `-v`;
4. comprobación Windows y Docker de puertos;
5. `docker compose up -d postgres`;
6. health de PostgreSQL;
7. builds frontend/backend;
8. arranque backend/frontend;
9. health y smoke.

El puerto se prueba así con la publicación real de Compose antes de invertir tiempo en builds limpios.

### Diagnóstico en fallos

Los validadores ahora imprimen:

```text
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
```

### KeepRunning

El orquestador integral ya no marca `stackKeptRunning=true` solamente porque se especificó `-KeepRunning`. Comprueba que existan contenedores del proyecto realmente activos.

## Seguridad

Durante la ejecución se conservaron:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No hubo comunicaciones, despliegues ni importación del XLSX real.

## Próxima ejecución

Actualizar al último `main` y comprobar primero quién publica `15432`:

```powershell
git switch main
git fetch origin
git pull --ff-only

docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
docker ps --filter publish=15432 --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
```

Detener únicamente el contenedor ajeno identificado o elegir un puerto nuevo. No eliminar volúmenes sin intención explícita.

Después:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173

powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Si `15432` continúa ocupado, usar por ejemplo `25432` después de validarlo con el checker actualizado.
