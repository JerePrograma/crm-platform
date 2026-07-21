# SEG-001 — clean builds y fallo de enlace de puerto Windows

Fecha: 2026-07-21

## Clasificación

```text
EXECUTED_FAIL — WINDOWS_HOST_PORT_BIND
```

Esta evidencia corresponde al primer intento que superó el parser PowerShell y alcanzó builds limpios reales del frontend y backend.

## Checkout

```text
rama: main
commit ejecutado: 65b64000a7e8f6abd71f2b118cebe904ee61f1d1
working tree inicial: limpio
Docker: 29.3.1
```

## Controles previos

```text
check-powershell-syntax.ps1: PASS — 10 scripts
preflight.ps1 -ContainerOnly: PASS
Docker daemon: reachable
Compose config: PASS
guardas de envío: PASS
```

Guardas observadas:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

## Frontend clean build

Comando ejecutado por el validador:

```text
docker compose --progress plain --profile app build --no-cache frontend
```

Resultado:

```text
EXECUTED_PASS — CLEAN_BUILD
npm install: PASS
TypeScript: PASS
Vite production build: PASS
image export: PASS
```

Salida relevante:

```text
17 modules transformed
✓ built
Image crm-platform-frontend Built
```

El build se realizó sin `package-lock.json`; por eso utilizó `npm install`. La validación posterior mediante `npm ci` no se alcanzó.

## Backend clean build

Comando ejecutado por el validador:

```text
docker compose --progress plain --profile app build --no-cache backend
```

Resultado:

```text
EXECUTED_PASS — CLEAN_IMAGE_BUILD
Maven dependency resolution: PASS
package con tests omitidos según Dockerfile: PASS
runtime image export: PASS
Image crm-platform-backend Built
```

Este resultado no sustituye `mvn verify`: Spotless, unit tests, ArchUnit y Testcontainers continuaron pendientes.

## Primer error real

El fallo ocurrió al levantar el perfil `app`:

```text
docker compose --profile app up -d
```

Docker no pudo enlazar PostgreSQL:

```text
ports are not available
127.0.0.1:55432
bind: An attempt was made to access a socket in a way forbidden by its access permissions
```

Interpretación:

- `55432` estaba ocupado o dentro de un rango excluido/reservado por Windows, Hyper-V o Docker Desktop;
- la red Compose y los contenedores fueron creados parcialmente;
- PostgreSQL no inició;
- backend y frontend no llegaron a ejecutarse;
- no hubo health checks ni migraciones.

## Fases no ejecutadas

```text
PostgreSQL health: NOT_RUN
backend health: NOT_RUN
frontend health: NOT_RUN
Flyway V1–V5: NOT_RUN
Hibernate validate: NOT_RUN
smoke host: NOT_RUN
smoke container: NOT_RUN
Maven verify: NOT_RUN
Spotless: NOT_RUN
unit tests: NOT_RUN
ArchUnit: NOT_RUN
Testcontainers: NOT_RUN
package-lock generation: NOT_RUN
npm ci rebuild: NOT_RUN
repository safety final: NOT_RUN
```

## Evidencia local generada

```text
validation-output/seg001-complete-20260721-102334.log
validation-output/seg001-complete-20260721-102334.json
validation-output/seg001-docker-20260721-102335.json
```

El working tree quedó limpio después del fallo. `validation-output/` continuó fuera de Git.

## Problemas de automatización detectados

La ejecución reveló dos mejoras necesarias:

1. el validador comprobaba formato y rango de puertos, pero no intentaba enlazarlos después del cleanup;
2. `-KeepRunning` informaba que el stack quedaba activo aunque ningún contenedor estuviera realmente ejecutándose.

## Correcciones publicadas

Se añadió:

```text
scripts/check-host-ports.ps1
```

Este script intenta enlazar cada puerto sobre `127.0.0.1` mediante `TcpListener` y falla con diagnóstico antes de los builds.

`validate-docker-stack.ps1` ahora:

1. ejecuta cleanup sin `-v`;
2. comprueba PostgreSQL, backend y frontend mediante enlace real;
3. inicia builds solo cuando los tres puertos están disponibles;
4. registra `hostPorts=PASS` en el JSON;
5. informa correctamente si no existe un stack en ejecución para conservar.

CI ejecuta el checker con tres puertos alternativos y el parser PowerShell continúa cubriendo todos los scripts.

## Próximo intento

Antes de repetir el recorrido completo:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Cuando devuelva PASS, ejecutar el validador integral con los mismos valores.

Para diagnosticar `55432`:

```powershell
Get-NetTCPConnection -LocalPort 55432 -ErrorAction SilentlyContinue
netsh interface ipv4 show excludedportrange protocol=tcp
```

No modificar rangos excluidos de Windows como parte de la validación. Elegir un puerto disponible es la acción conservadora.
