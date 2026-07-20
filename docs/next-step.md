# SEG-001 — Reejecución del build corregido y cierre desde `main`

## Estado

Producto, hardening, documentación y automatización están consolidados en `main`.

Ya existe una ejecución real en Windows/Docker:

- preflight container-only: `PASS`;
- npm install frontend: `PASS`;
- frontend typecheck/build: `FAIL` con tres errores;
- errores corregidos en `main`;
- reejecución: pendiente;
- backend, Flyway, Hibernate y smoke: aún no alcanzados.

## Objetivo del próximo `continuar`

Trabajar solo desde `main`, reconstruir el frontend corregido, continuar con backend y stack, ejecutar smoke y registrar todos los resultados.

No iniciar identidad/RBAC, campañas, Gmail, Sheets, workers o cloud antes de estabilizar SEG-001.

## Fuentes canónicas

```text
branch: main
Docker-only: docs/containerized-quickstart.md
procesos separados: docs/local-development-and-usage.md
automatización: scripts/README.md
estado: docs/status.md
validación: docs/validation/SEG-001.md
primer build real: docs/validation/SEG-001-container-build-2026-07-20.md
backlog: docs/backlog.md
```

## Fallo real ya corregido

Errores observados:

```text
Credentials | null en getProspect
Credentials | null en refresh
import CSS sin declaración Vite/TypeScript
```

Correcciones:

```text
72f0421caaf4898ce04fd97724ecbad9a4ed6390
31960db073d6df0cae683b02267a752b9538e08f
```

Además, `excludedRows` ya se muestra como `Bloqueadas` en la UI.

No desactivar `strict`, `strictNullChecks` ni el typecheck para eludir errores.

## Orden inmediato obligatorio — Windows PowerShell

### 1. Actualizar el checkout

```powershell
Set-Location C:\laburo\crm-platform
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

Confirmar que existen:

```powershell
Test-Path frontend\src\vite-env.d.ts
Select-String -Path frontend\src\App.tsx -Pattern 'activeCredentials'
Select-String -Path frontend\src\App.tsx -Pattern 'Bloqueadas'
```

### 2. Conservar `.env`

No volver a copiar `.env.example` si `.env` ya contiene la contraseña local elegida.

Comprobar únicamente su existencia:

```powershell
Test-Path .env
```

No imprimir la contraseña en consola o logs.

### 3. Repetir preflight

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Resultado esperado:

```text
Preflight passed.
Mode: container-only
Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true
```

### 4. Reconstruir solo frontend primero

```powershell
docker compose --profile app build frontend --progress=plain
```

Objetivo:

```text
npm run build: PASS
TypeScript: PASS
Vite build: PASS
imagen frontend: PASS
```

Si falla, conservar desde la primera línea `error TS...` hasta el final del stage.

### 5. Construir backend de forma aislada

```powershell
docker compose --profile app build backend --progress=plain
```

Esto evita que un fallo paralelo oculte el resultado backend.

Registrar:

- compilación Maven;
- formato Spotless;
- generación del JAR;
- cualquier error Java/Spring/JPA.

El Dockerfile utiliza `-DskipTests`; las pruebas completas se ejecutan después.

### 6. Levantar el stack

```powershell
docker compose --profile app up -d
docker compose --profile app ps
```

Esperado:

```text
postgres   healthy
backend    healthy
frontend   healthy
```

Si un servicio falla:

```powershell
docker compose logs postgres
docker compose logs backend
docker compose logs frontend
```

### 7. Ejecutar smoke PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Esperado:

```text
Smoke test passed.
Backend health: UP
Authenticated API: reachable
Frontend: reachable
```

### 8. Ejecutar smoke contenedorizado

```powershell
docker compose --profile app --profile smoke up --abort-on-container-exit --exit-code-from smoke smoke
docker compose --profile app --profile smoke down --remove-orphans
```

O con Make en un entorno compatible:

```bash
make smoke-container
```

### 9. Ejecutar pruebas backend completas

Con Docker activo:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Debe validar:

- compilación;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway V1–V5;
- Hibernate validate.

### 10. Generar lockfile sin Node local

```powershell
docker run --rm `
  -v "${PWD}\frontend:/workspace/frontend" `
  -w /workspace/frontend `
  node:22-alpine `
  npm install --no-audit --no-fund
```

Comprobar:

```powershell
Test-Path frontend\package-lock.json
git status --short
```

### 11. Validar frontend con lockfile

Mientras Dockerfile/CI sigan en `npm install`:

```powershell
docker compose --profile app build frontend --no-cache --progress=plain
```

Después de versionar el lockfile:

- cambiar Dockerfile a `npm ci`;
- cambiar CI a `npm ci`;
- habilitar caché npm por lockfile;
- repetir build y smoke.

## Ruta equivalente Linux/macOS

```bash
git switch main
git fetch origin
git pull --ff-only
git rev-parse HEAD

sh scripts/preflight.sh --container-only

docker compose --profile app build frontend --progress=plain
docker compose --profile app build backend --progress=plain
docker compose --profile app up -d
docker compose --profile app ps

sh scripts/smoke-test.sh
sh ./mvnw -B -f backend/pom.xml verify
```

Lockfile:

```bash
docker run --rm \
  -v "$PWD/frontend:/workspace/frontend" \
  -w /workspace/frontend \
  node:22-alpine \
  npm install --no-audit --no-fund
```

## Evidencia a registrar

En `docs/validation/SEG-001.md` y, cuando corresponda, un archivo fechado separado:

- fecha y SHA;
- Docker/Compose;
- preflight;
- build frontend corregido;
- build backend;
- Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- package-lock;
- Compose app/smoke;
- estado de los servicios;
- smoke host;
- smoke contenedorizado;
- errores nuevos;
- commits correctivos;
- repetición final verde.

## Criterios de cierre

- frontend typecheck/build: `PASS`;
- imagen frontend: `PASS`;
- backend Maven/package: `PASS`;
- Maven verify/Spotless/tests: `PASS`;
- Flyway/Hibernate/Testcontainers: `PASS`;
- package-lock versionado y `npm ci`: `PASS`;
- Compose app/smoke: `PASS`;
- stack saludable: `PASS`;
- smoke E2E: `PASS`;
- secretos/datos: `PASS`;
- evidencia registrada;
- SEG-001 `COMPLETE`;
- SEG-002 `ACTIVE`.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar XLSX real a Git, CI o imágenes;
- no declarar éxito sin salida ejecutada;
- no iniciar SEG-002 con bloqueantes;
- no tratar Compose local como producción;
- no desactivar controles TypeScript para ocultar fallos.