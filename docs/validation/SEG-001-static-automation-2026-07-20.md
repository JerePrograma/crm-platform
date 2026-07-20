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
```

Este control no confirma que las actions externas, imágenes o comandos ejecuten correctamente.

### Scripts Unix — sintaxis

Comandos equivalentes:

```bash
sh -n scripts/preflight.sh
sh -n scripts/smoke-test.sh
```

Resultado:

```text
preflight.sh: PASS
smoke-test.sh: PASS
```

### Makefile — parseo inicial

```bash
make -n preflight-container app-up verify smoke
```

Resultado:

```text
PASS
```

### Makefile — smoke contenedorizado

```bash
make -n smoke-container verify
```

Resultado:

```text
PASS
```

Los controles Make confirman que se interpretan targets y recetas. No ejecutan Docker, Maven o npm.

## Controles no ejecutados

- sintaxis PowerShell, porque `pwsh` no está instalado;
- `docker compose config` semántico;
- pull de `curlimages/curl`;
- builds de imágenes;
- ejecución del servicio `smoke`;
- Maven/Spotless/tests;
- frontend install/typecheck/build;
- preflight real;
- stack completo.

## Conclusión

Compose, CI, scripts Unix y Makefile superaron los controles estáticos disponibles, incluido el nuevo smoke E2E contenedorizado a nivel de sintaxis y estructura. SEG-001 continúa `PENDING_EXECUTION` hasta completar la matriz real en un entorno con Docker, red y dependencias.
