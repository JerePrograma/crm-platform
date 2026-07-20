# Evidencia estática de automatización — SEG-001

## Fecha

2026-07-20

## Alcance

Controles ejecutados sobre representaciones exactas o releídas de la automatización y configuración versionadas.

No sustituyen Docker, compilación, tests, migraciones o smoke real.

## Entorno disponible

```text
sh: disponible
make: disponible
Python + PyYAML: disponible
PowerShell/pwsh: no disponible
Docker/Compose: no disponible
Maven: no disponible
```

## Controles ejecutados

### Docker Compose

Estructura comprobada:

```text
services: postgres, backend, frontend, smoke
postgres host port variable: presente
backend host port variable: presente
frontend host port variable: presente
smoke -> frontend healthy: presente
```

Resultado: `PASS_STRUCTURE`.

Esto no equivale a `docker compose config` real.

### GitHub Actions

Estructura comprobada:

```text
jobs: backend, frontend, scripts, compose-images-and-smoke
install condicional npm ci/npm install: presente
scripts de puertos en validación Unix: presentes
scripts de puertos/orquestador en parser PowerShell: presentes
preflight container-only en job E2E: presente
smoke contenedorizado: presente
cleanup: presente
```

Resultado: `PASS_STRUCTURE`.

### Shell inicial

```bash
sh -n scripts/preflight.sh
sh -n scripts/smoke-test.sh
sh -n scripts/generate-frontend-lock.sh
sh -n scripts/set-postgres-host-port.sh
```

Resultado registrado previamente: `PASS_SYNTAX`.

### Configurador conjunto Unix

Archivo:

```text
scripts/set-local-host-ports.sh
```

Control:

```bash
sh -n scripts/set-local-host-ports.sh
```

Resultado: `PASS_SYNTAX`.

### Prueba funcional aislada del configurador Unix

Se ejecutó sobre un `.env` temporal con:

- contraseña DB ficticia con carácter UTF-8;
- contraseña bootstrap ficticia;
- cuatro guardas de envío cerradas;
- URL PostgreSQL inicial en 5432;
- puertos backend/frontend ausentes.

Comando equivalente:

```bash
sh scripts/set-local-host-ports.sh 55432 18080 15173
```

Resultado:

```text
POSTGRES_HOST_PORT=55432: PASS
BACKEND_HOST_PORT=18080: PASS
FRONTEND_HOST_PORT=15173: PASS
DATABASE_URL coordinada: PASS
contraseña DB preservada: PASS
contraseña bootstrap preservada: PASS
guardas SENDING_* preservadas: PASS
UTF-8 preservado: PASS
```

Estado: `PASS_FUNCTIONAL_ISOLATED`.

### Makefile

Controles acumulados:

```bash
make -n preflight-container
make -n frontend-lock
make -n smoke-container
make -n postgres-port
make -n local-ports
make -n verify
```

Resultado: `PASS_SYNTAX`.

### Read-back remoto

Se releyeron desde `main`:

```text
docker-compose.yml
.github/workflows/ci.yml
scripts/set-local-host-ports.sh
scripts/validate-docker-stack.ps1
frontend/Dockerfile
```

Resultado: `PASS`.

## Hardening observado

- tres puertos host configurables;
- preflight exige enteros válidos y distintos;
- `DATABASE_URL` coordinada con PostgreSQL;
- smoke deriva URLs desde `.env`;
- configuradores preservan credenciales;
- PowerShell escribe UTF-8 sin BOM por diseño;
- wrappers históricos conservan compatibilidad;
- Dockerfile/CI/Makefile adoptan npm ci cuando exista lockfile;
- transcript local fuera de Git;
- orquestador Docker Windows usa clean builds por defecto.

## Controles no ejecutados en este entorno

- parser PowerShell;
- configurador conjunto PowerShell;
- orquestador Docker PowerShell;
- `docker compose config` semántico;
- builds frontend/backend;
- stack completo;
- smoke host/contenedor;
- Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- generación real de package-lock;
- npm ci real.

## Evidencia real separada

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
```

## Conclusión

Compose, CI, shell, Makefile y el configurador Unix coordinado superaron los controles disponibles. La automatización está preparada para una reejecución Windows reproducible, pero SEG-001 continúa pendiente hasta completar builds limpios, health, smoke, Maven, Testcontainers, Flyway, Hibernate y lockfile.
