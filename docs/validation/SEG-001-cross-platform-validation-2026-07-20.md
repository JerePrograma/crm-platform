# Evidencia de paridad de validación — SEG-001

## Fecha

2026-07-20

## Estado

`IMPLEMENTED / STATIC_REVIEWED / FUNCTIONAL_EXECUTION_PENDING`

Este documento registra el hardening posterior a la automatización integral de Windows. No reemplaza una ejecución real con Docker.

## Objetivo

Disponer de recorridos equivalentes para cerrar SEG-001:

```text
Windows PowerShell -> scripts/validate-seg001.ps1
Linux/macOS Bash   -> scripts/validate-seg001.sh
```

Ambos recorridos deben comprobar:

1. rama `main`;
2. árbol de trabajo sin cambios inesperados;
3. configuración fail-closed;
4. puertos locales válidos y distintos;
5. Compose semánticamente válido;
6. builds frontend/backend sin caché;
7. PostgreSQL, backend y frontend saludables;
8. smoke desde el host;
9. smoke desde la red Compose;
10. Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
11. generación segura de `package-lock.json`;
12. reconstrucción frontend mediante `npm ci`;
13. smoke final;
14. escaneo de archivos sensibles;
15. evidencia estructurada y transcript;
16. ausencia de commits automáticos.

## Cambios implementados

### Validador integral Bash

Archivo:

```text
scripts/validate-seg001.sh
```

Características:

- Bash estricto: `set -Eeuo pipefail`;
- argumentos para los tres puertos;
- `--keep-running`;
- `--use-build-cache`, explícitamente no válido como evidencia de cierre;
- comprobación temprana del daemon Docker;
- rama `main` obligatoria;
- único archivo no rastreado permitido al inicio: `frontend/package-lock.json`;
- configuración de puertos mediante `set-local-host-ports.sh`;
- preflight container-only;
- cleanup sin `-v`;
- builds frontend/backend limpios por defecto;
- espera explícita de health;
- smoke host/contenedor;
- backend verify contenedorizado;
- generación de lockfile;
- SHA-256 mediante `sha256sum` o `shasum`;
- rebuild frontend que activa `npm ci`;
- smoke final;
- seguridad del repositorio;
- transcript y JSON en `validation-output/`;
- cleanup final salvo `--keep-running`.

### Propiedad del lockfile en Unix

`scripts/generate-frontend-lock.sh` ahora ejecuta Node con:

```text
--user $(id -u):$(id -g)
HOME=/tmp/npm-home
npm_config_cache=/tmp/npm-cache
```

Esto evita que `frontend/package-lock.json` quede propiedad de `root`.

Controles adicionales:

- rechaza `frontend/node_modules` preexistente;
- usa `--package-lock-only`;
- usa `--ignore-scripts`;
- no crea `node_modules`;
- comprueba que el lockfile sea editable por el usuario actual.

### Makefile

Nuevo target:

```bash
make validate-seg001
```

Equivale a:

```bash
bash scripts/validate-seg001.sh
```

### CI

El job `scripts` ahora incluye:

```text
bash -n scripts/validate-seg001.sh
make -n repository-safety backend-verify-container validate-seg001
```

El workflow mantiene además:

- parser de todos los scripts PowerShell;
- sintaxis de scripts POSIX;
- seguridad del repositorio;
- preflight fail-closed;
- backend verify;
- frontend typecheck/build;
- Compose y smoke E2E.

## Validación disponible en esta sesión

### Ejecutado

- read-back remoto del validador Bash completo;
- revisión de fases, cleanup y evidencia;
- verificación de que Dockerfile frontend copia `package*.json`;
- verificación de que `package-lock.json` no está excluido por `.dockerignore`;
- verificación de selección condicional `npm ci`/`npm install`;
- actualización de Make y CI;
- comparación de la rama histórica contra `main`.

### Resultado de ramas

```text
base: feat/seg-001-prospect-vertical-slice
head: main
main ahead: 162 antes de este hardening
main behind: 0
cambios exclusivos en la rama histórica: ninguno
```

Los commits adicionales de este documento y automatización aumentan la distancia de `main`; no alteran la conclusión.

### No ejecutado

El entorno de esta sesión no pudo resolver `raw.githubusercontent.com`, por lo que no fue posible descargar el archivo hacia el contenedor local para ejecutar `bash -n` fuera de GitHub.

Continúan pendientes:

- `bash -n` real mediante CI o checkout local;
- ejecución de `validate-seg001.sh`;
- ejecución de `validate-seg001.ps1`;
- builds limpios;
- stack saludable;
- Maven/Testcontainers;
- Flyway/Hibernate;
- generación real del lockfile;
- `npm ci` real;
- smoke final;
- CI visible.

## Seguridad

Sin cambios:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Además:

- no existe adaptador Gmail o SMTP;
- no se importan datos reales durante validación;
- `.env` y transcripts están ignorados;
- el lote operativo está bloqueado por el escaneo;
- los scripts no crean commits;
- los servicios continúan publicados solo en loopback.

## Condición de cierre

Este hardening elimina la diferencia operativa entre Windows y Unix, pero no cierra SEG-001.

SEG-001 solo puede marcarse `COMPLETE` cuando:

1. uno de los validadores integrales finalice en `PASS`;
2. `frontend/package-lock.json` sea revisado y versionado;
3. el validador se repita desde un árbol limpio con lockfile rastreado;
4. `npm ci` quede demostrado desde el primer build;
5. CI sea visible y verde o exista evidencia local equivalente revisada;
6. la matriz canónica sea actualizada con resultados reales.
