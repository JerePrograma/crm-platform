# Evidencia estática de automatización — SEG-001

## Fecha

2026-07-20

## Alcance

Controles ejecutados localmente sobre representaciones exactas de los archivos versionados de automatización y configuración.

No sustituyen Docker, compilación, tests o smoke test real.

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

### Docker Compose — parseo YAML

Se cargó el contenido actualizado de `docker-compose.yml` con PyYAML.

Resultado:

```text
PASS
services presentes: postgres, backend, frontend, smoke
dependencia smoke -> frontend healthy: presente
```

Este control confirma estructura YAML básica y relaciones declaradas. No equivale a `docker compose --profile app --profile smoke config`.

### GitHub Actions — parseo YAML

Se cargó `.github/workflows/ci.yml` con PyYAML y se verificaron los jobs esperados.

Resultado:

```text
PASS
jobs: backend, frontend, scripts, compose-images-and-smoke
paso Run containerized smoke test: presente
validación de scripts de lockfile: presente
```

Este control no confirma que las actions externas, imágenes o comandos ejecuten correctamente.

### Scripts Unix — sintaxis inicial

```bash
sh -n scripts/preflight.sh
sh -n scripts/smoke-test.sh
```

Resultado:

```text
preflight.sh: PASS
smoke-test.sh: PASS
```

### Generación de lockfile — sintaxis Unix

Se reprodujo el contenido exacto de `scripts/generate-frontend-lock.sh` y se ejecutó:

```bash
sh -n scripts/generate-frontend-lock.sh
```

Resultado:

```text
generate-frontend-lock.sh: PASS
```

El control confirma sintaxis shell. No ejecutó Docker ni npm.

### Makefile — parseo inicial

```bash
make -n preflight-container app-up verify smoke
```

Resultado:

```text
PASS
```

### Makefile — smoke y lockfile

```bash
make -n frontend-lock smoke-container
```

Resultado:

```text
PASS
```

Recetas expandidas:

```text
sh scripts/generate-frontend-lock.sh
set -eu; trap cleanup; docker compose ... smoke
```

Los controles Make confirman que se interpretan targets y recetas. No ejecutan Docker, Maven o npm.

## Evidencia real separada

El preflight PowerShell container-only y el primer build Docker sí se ejecutaron en un entorno Windows externo. Su evidencia está en:

```text
docs/validation/SEG-001-container-build-2026-07-20.md
```

Esta evidencia estática no duplica ni reemplaza esa ejecución.

## Controles no ejecutados en este entorno

- sintaxis PowerShell, porque `pwsh` no está instalado;
- ejecución real de `generate-frontend-lock.ps1`;
- `docker compose config` semántico;
- build frontend después de los commits correctivos;
- build backend;
- ejecución del servicio `smoke`;
- Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- stack completo.

## Conclusión

Compose, CI, scripts Unix y Makefile superaron los controles estáticos disponibles. Esto incluye el nuevo generador de lockfile y el target `frontend-lock`. SEG-001 continúa pendiente de reejecución funcional después de las correcciones frontend.