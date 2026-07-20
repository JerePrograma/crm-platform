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

Se cargó el contenido de `docker-compose.yml` con PyYAML.

Resultado:

```text
PASS
services presentes: postgres, backend, frontend
```

Este control confirma estructura YAML básica. No equivale a `docker compose --profile app config`.

### GitHub Actions — parseo YAML

Se cargó `.github/workflows/ci.yml` con PyYAML y se verificaron los jobs esperados.

Resultado:

```text
PASS
jobs: backend, frontend, scripts, compose-and-images
```

Este control no confirma que las actions externas o los comandos ejecuten correctamente.

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

### Makefile — parseo

Se utilizaron targets representativos en modo no ejecutable:

```bash
make -n preflight-container app-up verify smoke
```

Resultado:

```text
PASS
```

El control confirma que Make puede interpretar los targets y recetas. No ejecuta Docker, Maven o npm.

## Controles no ejecutados

- sintaxis PowerShell, porque `pwsh` no está instalado;
- `docker compose config`;
- builds de imágenes;
- Maven/Spotless/tests;
- frontend install/typecheck/build;
- preflight real;
- stack completo;
- smoke test real.

## Conclusión

Los archivos YAML y la automatización Unix superaron los controles estáticos disponibles. SEG-001 continúa `PENDING_EXECUTION` hasta completar la matriz real en un entorno con Docker, red y dependencias.
