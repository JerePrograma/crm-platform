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
red hacia GitHub: no disponible
```

El intento de clonar el repositorio mediante Git falló por resolución DNS. Los archivos críticos se verificaron mediante read-back del conector GitHub y reproducciones exactas en un directorio temporal.

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

No equivale a `docker compose config` real.

### GitHub Actions

Estructura comprobada:

```text
jobs: backend, frontend, scripts, compose-images-and-smoke
install condicional npm ci/npm install: presente
scripts de puertos: presentes
backend verify container: presente en shell/parser
validador integral: presente en parser PowerShell
seguridad centralizada: presente
preflight container-only en E2E: presente
smoke contenedorizado: presente
cleanup: presente
```

Resultado: `PASS_PARSE / PASS_STRUCTURE`.

PyYAML cargó el workflow y se verificaron los cuatro jobs y el paso de seguridad.

### Scripts shell iniciales

```bash
sh -n scripts/preflight.sh
sh -n scripts/smoke-test.sh
sh -n scripts/set-postgres-host-port.sh
sh -n scripts/set-local-host-ports.sh
```

Resultado: `PASS_SYNTAX`.

### Backend verify contenedorizado Unix

```bash
sh -n scripts/verify-backend-container.sh
```

Resultado: `PASS_SYNTAX`.

Validado estructuralmente:

- imagen Maven 3.9.16/Java 21;
- repositorio read-only;
- volumen Maven cache;
- volumen target efímero;
- socket Docker;
- Testcontainers host override;
- cleanup.

No se ejecutó Docker.

### Generador lockfile seguro Unix

```bash
sh -n scripts/generate-frontend-lock.sh
```

Resultado: `PASS_SYNTAX`.

Validado estructuralmente:

```text
--package-lock-only
--ignore-scripts
--no-audit
--no-fund
fallo si node_modules existe
```

No se ejecutó npm/Docker.

### Seguridad del repositorio Unix

```bash
sh -n scripts/check-repository-safety.sh
```

Resultado: `PASS_SYNTAX`.

Cobertura estructural:

- `.env`;
- validation-output;
- import/export private;
- lote operativo en cualquier subdirectorio;
- claves/certificados;
- JSON de credenciales;
- `git diff --check`.

No se ejecutó contra un checkout completo por falta de clon de red.

### Configurador conjunto Unix

```bash
sh -n scripts/set-local-host-ports.sh
```

Resultado: `PASS_SYNTAX`.

### Prueba funcional aislada del configurador Unix

Se ejecutó sobre `.env` temporal con:

- contraseña DB ficticia UTF-8;
- contraseña bootstrap ficticia;
- guardas cerradas;
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

Controles:

```bash
make -n backend-verify-container
make -n verify-container
```

Primer intento:

```text
FAIL_HARNESS
```

Causa: el comando se lanzó con `-f /tmp/.../Makefile` desde otro directorio y la invocación recursiva `$(MAKE) smoke-container` no encontró el Makefile.

Repetición desde la raíz temporal correcta:

```text
backend-verify-container: PASS_PARSE
verify-container: PASS_PARSE
smoke-container recursion: PASS_PARSE
```

Recetas expandidas:

- backend verify contenedorizado;
- preflight container-only;
- lockfile;
- Compose config;
- builds limpios;
- smoke contenedorizado;
- cleanup.

El primer fallo fue del harness estático, no del Makefile versionado.

### Read-back remoto

Se releyeron desde `main`:

```text
docker-compose.yml
.github/workflows/ci.yml
Makefile
scripts/set-local-host-ports.sh
scripts/validate-docker-stack.ps1
scripts/verify-backend-container.sh
scripts/generate-frontend-lock.sh
scripts/check-repository-safety.sh
scripts/validate-seg001.ps1
frontend/Dockerfile
```

Resultado: `PASS`.

## Hardening observado

- tres puertos host configurables;
- preflight exige enteros válidos y distintos;
- `DATABASE_URL` coordinada;
- smoke deriva URLs desde `.env`;
- configuradores preservan credenciales;
- PowerShell escribe UTF-8 sin BOM por diseño;
- wrappers históricos compatibles;
- Dockerfile/CI/Makefile adoptan npm ci con lockfile;
- lockfile-only sin lifecycle scripts;
- evidencia JSON y transcript;
- backend verify sin Java local;
- repositorio backend read-only;
- socket Docker documentado como privilegiado;
- seguridad centralizada;
- orquestador integral exige main y árbol limpio;
- único cambio esperado: package-lock.

## Controles no ejecutados

- parser PowerShell;
- configuradores PowerShell;
- seguridad PowerShell;
- validador Docker PowerShell;
- backend verify PowerShell;
- validador integral PowerShell;
- Docker Compose config semántico;
- builds frontend/backend;
- stack completo;
- smoke host/contenedor;
- Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- package-lock real;
- npm ci real;
- seguridad contra checkout completo;
- CI real.

## Evidencias relacionadas

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
```

## Conclusión

Compose, CI, shell, Makefile, configuración de puertos, backend verify, lockfile seguro y seguridad del repositorio superaron los controles estáticos disponibles.

SEG-001 continúa pendiente hasta ejecutar `scripts/validate-seg001.ps1`, generar/versionar package-lock, repetir npm ci y observar evidencia funcional verde.
