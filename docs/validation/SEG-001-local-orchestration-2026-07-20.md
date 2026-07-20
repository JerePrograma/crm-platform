# SEG-001 — Evidencia de hardening de puertos y orquestación local

## Fecha

2026-07-20

## Estado

`IMPLEMENTED_AND_STATICALLY_REVIEWED — WINDOWS_DOCKER_EXECUTION_PENDING`

Este documento registra la automatización incorporada después de reproducir el conflicto del puerto host PostgreSQL `5432`.

No sustituye una ejecución limpia en Windows/Docker ni permite cerrar SEG-001 por sí solo.

## Problemas abordados

1. PostgreSQL ya era configurable, pero backend y frontend mantenían puertos host fijos.
2. El preflight solo validaba el puerto PostgreSQL.
3. Los smoke tests asumían siempre `8080` y `5173`.
4. El cambio de puertos requería editar `.env` manualmente.
5. La validación Docker exigía múltiples comandos manuales y no guardaba evidencia local.
6. Dockerfile y CI requerían una migración manual posterior de `npm install` a `npm ci`.
7. El helper histórico de PostgreSQL podía dejar incompleto un `.env` nuevo.

## Cambios implementados

### Puertos host coordinados

Variables:

```text
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
```

Compose publica únicamente en loopback:

```text
127.0.0.1:${POSTGRES_HOST_PORT}:5432
127.0.0.1:${BACKEND_HOST_PORT}:8080
127.0.0.1:${FRONTEND_HOST_PORT}:8080
```

La red interna no cambia:

```text
backend -> postgres:5432
frontend -> backend:8080
smoke -> backend:8080 y frontend:8080
```

### Preflight

Unix y PowerShell ahora:

- exigen los tres puertos;
- validan enteros entre 1 y 65535;
- exigen que sean distintos;
- comprueban que `DATABASE_URL` use `POSTGRES_HOST_PORT`;
- mantienen las cuatro guardas de envío cerradas;
- validan perfiles Compose `app` y `smoke`;
- muestran puertos sin imprimir contraseñas.

### Configuración segura de `.env`

Nuevos scripts:

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
```

Comportamiento:

- actualizan los tres puertos;
- actualizan `DATABASE_URL`;
- conservan `POSTGRES_DB`;
- conservan usuarios y contraseñas;
- conservan `SENDING_*`;
- PowerShell escribe UTF-8 sin BOM;
- rechazan puertos inválidos o repetidos.

Los helpers anteriores `set-postgres-host-port.*` permanecen como wrappers compatibles y completan los puertos faltantes.

### Smoke host

Los smoke tests derivan sus URLs desde:

```text
BACKEND_HOST_PORT
FRONTEND_HOST_PORT
```

`BACKEND_URL` y `FRONTEND_URL` continúan permitiendo override explícito.

### Validador Docker para Windows

Nuevo archivo:

```text
scripts/validate-docker-stack.ps1
```

Flujo:

1. configura puertos;
2. ejecuta preflight container-only;
3. retira contenedores incompletos sin borrar volúmenes;
4. construye frontend sin caché por defecto;
5. construye backend sin caché por defecto;
6. levanta el perfil `app`;
7. espera health de PostgreSQL, backend y frontend;
8. ejecuta smoke PowerShell;
9. ejecuta smoke dentro de Compose;
10. registra transcript en `validation-output/`;
11. retira el stack salvo que se use `-KeepRunning`.

No ejecuta Maven verify, Testcontainers ni genera el lockfile; estos siguen siendo controles separados.

### Reproducibilidad npm

Dockerfile frontend y CI ahora usan:

```text
package-lock.json presente -> npm ci
package-lock.json ausente  -> npm install
```

Esto permite que, al versionar el lockfile, Docker y CI adopten `npm ci` sin otro cambio funcional.

### Evidencia local ignorada

```text
validation-output/
```

se agregó a `.gitignore` para evitar versionar transcripts que puedan contener detalles locales.

## Controles estáticos ejecutados

### Unix shell

```text
set-local-host-ports.sh: PASS_SYNTAX
```

### Prueba funcional del actualizador Unix

Se ejecutó sobre un `.env` temporal con caracteres UTF-8 y secretos ficticios.

Resultado:

```text
puertos añadidos/actualizados: PASS
DATABASE_URL actualizada: PASS
contraseña DB preservada: PASS
contraseña bootstrap preservada: PASS
guardas de envío preservadas: PASS
UTF-8 preservado: PASS
```

### YAML

```text
Compose con postgres/backend/frontend/smoke: PASS_STRUCTURE
puerto PostgreSQL variable: PASS_STRUCTURE
puerto backend variable: PASS_STRUCTURE
puerto frontend variable: PASS_STRUCTURE
CI con frontend/scripts: PASS_STRUCTURE
```

### Read-back remoto

Se releyeron desde `main`:

```text
docker-compose.yml
scripts/set-local-host-ports.sh
scripts/validate-docker-stack.ps1
.github/workflows/ci.yml
```

Resultado: `PASS`.

## Controles todavía no ejecutados

- parser PowerShell de los scripts nuevos;
- `set-local-host-ports.ps1` sobre el `.env` real de Windows;
- `validate-docker-stack.ps1`;
- builds limpios frontend/backend;
- health real de los tres servicios;
- smoke PowerShell;
- smoke contenedorizado;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway;
- Hibernate validate;
- generación y revisión de `package-lock.json`;
- ejecución real con `npm ci`;
- GitHub Actions verde visible.

## Comando inmediato recomendado

```powershell
Set-Location C:\laburo\crm-platform
git switch main
git fetch origin
git pull --ff-only

git diff --ignore-space-at-eol -- mvnw.cmd
git restore -- mvnw.cmd

powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 -PostgresPort 55432 -BackendPort 8080 -FrontendPort 5173 -KeepRunning
```

Si `8080` o `5173` estuvieran ocupados:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 -PostgresPort 55432 -BackendPort 18080 -FrontendPort 15173 -KeepRunning
```

El smoke usará automáticamente esos puertos.

## Condición de cierre

Este hardening reduce bloqueos operativos, pero SEG-001 continúa abierto hasta obtener:

- build limpio frontend;
- build limpio backend;
- stack saludable;
- smoke host y contenedor;
- Maven verify y Testcontainers;
- Flyway/Hibernate;
- lockfile y ejecución `npm ci`;
- evidencia final registrada.
