# Evidencia estática de automatización — SEG-001

## Fecha

2026-07-20

## Alcance

Controles ejecutados sobre representaciones exactas de archivos versionados. No sustituyen Docker, compilación, tests o smoke real.

## Entorno disponible

```text
sh: disponible
make: disponible
Python + PyYAML: disponible
PowerShell/pwsh: no disponible
Docker/Compose: no disponible
Maven: no disponible
```

## Docker Compose

Se cargó el contenido actualizado con PyYAML.

```text
PASS
services: postgres, backend, frontend, smoke
postgres port: 127.0.0.1:${POSTGRES_HOST_PORT:-55432}:5432
backend database: postgres:5432
smoke depende de frontend healthy
```

Confirma estructura YAML, no `docker compose config` semántico.

## GitHub Actions

Se cargó el workflow y se verificaron jobs y pasos.

```text
PASS
jobs: backend, frontend, scripts, compose-images-and-smoke
smoke E2E: presente
scripts lockfile: presentes
scripts PostgreSQL port: presentes
```

No confirma ejecución de actions externas.

## Scripts Unix

Sintaxis ejecutada:

```bash
sh -n scripts/preflight.sh
sh -n scripts/smoke-test.sh
sh -n scripts/generate-frontend-lock.sh
sh -n scripts/set-postgres-host-port.sh
```

Resultado:

```text
PASS
```

## Prueba funcional del actualizador Unix

Se ejecutó `set-postgres-host-port.sh 55432` sobre un `.env` temporal con:

- contraseña de base ficticia;
- contraseña bootstrap con caracteres no ASCII;
- cuatro guardas de envío.

Resultado:

```text
POSTGRES_HOST_PORT añadido: PASS
DATABASE_URL actualizado: PASS
POSTGRES_DB preservado: PASS
DATABASE_PASSWORD preservada: PASS
CRM_BOOTSTRAP_PASSWORD preservada: PASS
SENDING_* preservadas: PASS
UTF-8 preservado: PASS
```

## Makefile

Comando:

```bash
make -n postgres-port verify smoke-container
```

Resultado:

```text
PASS
```

Recetas expandidas:

```text
sh scripts/set-postgres-host-port.sh 55432
mvn verify
frontend install/typecheck/build
Compose config
frontend clean build
backend clean build
smoke contenedorizado con cleanup
```

## Evidencia real separada

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
```

La evidencia real confirma preflight, npm install, fallo TypeScript, imágenes cacheadas y conflicto del puerto 5432.

## Controles no ejecutados en este entorno

- parser PowerShell de los scripts nuevos;
- ejecución real de `set-postgres-host-port.ps1`;
- preflight con puerto 55432 en Windows;
- Docker Compose config semántico;
- builds sin caché;
- Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- stack y smoke.

## Conclusión

La estructura Compose/CI, shell, Makefile y el actualizador Unix superaron los controles disponibles. El puerto PostgreSQL configurable y la preservación de secretos/guardas están demostrados en Unix. Windows y la matriz funcional completa continúan pendientes de reejecución.
