# SEG-001 — Validación Docker automatizada y cierre desde `main`

## Estado

- producto y hardening: consolidados en `main`;
- primer preflight Windows: `PASS`;
- primer frontend build: `FAIL` con tres errores TypeScript;
- errores TypeScript: corregidos;
- segunda exportación de imágenes: `PASS_FROM_CACHE`;
- build limpio: pendiente;
- primer arranque: `FAIL` por puerto host 5432;
- tres puertos configurables: implementados;
- validador Docker Windows: implementado;
- Maven/Testcontainers/Flyway/Hibernate/lockfile: pendientes.

No iniciar SEG-002, campañas, Gmail, Sheets, workers o cloud.

## Fuentes

```text
estado: docs/status.md
matriz: docs/validation/SEG-001.md
primer build: docs/validation/SEG-001-container-build-2026-07-20.md
segunda reejecución: docs/validation/SEG-001-rerun-2026-07-20.md
orquestación: docs/validation/SEG-001-local-orchestration-2026-07-20.md
Docker: docs/containerized-quickstart.md
scripts: scripts/README.md
```

## Orden inmediato — Windows PowerShell

### 1. Actualizar `main`

```powershell
Set-Location C:\laburo\crm-platform
git switch main
git fetch origin
git pull --ff-only
git status
git rev-parse HEAD
```

### 2. Revisar `mvnw.cmd`

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git diff --numstat -- mvnw.cmd
```

Si no fue un cambio intencional:

```powershell
git restore -- mvnw.cmd
git status --short
```

No versionar cambios de finales de línea accidentales.

### 3. Ejecutar validación Docker automatizada

Comando recomendado:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

El script:

1. actualiza `.env` sin tocar contraseñas;
2. valida los tres puertos y las guardas;
3. retira contenedores incompletos sin borrar volumen;
4. construye frontend sin caché;
5. construye backend sin caché;
6. levanta PostgreSQL, backend y frontend;
7. espera los tres health checks;
8. ejecuta smoke PowerShell;
9. ejecuta smoke dentro de Compose;
10. guarda transcript en `validation-output/`;
11. deja el stack activo por `-KeepRunning`.

No usar `-UseBuildCache` para evidencia de cierre.

### 4. Alternativa si 8080 o 5173 están ocupados

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Los smoke tests toman automáticamente esos puertos desde `.env`.

### 5. Resultado esperado

```text
postgres   healthy
backend    healthy
frontend   healthy
Smoke test passed.
Container smoke test passed.
SEG-001 Docker validation passed.
```

Mapeos predeterminados:

```text
127.0.0.1:55432 -> PostgreSQL 5432
127.0.0.1:8080  -> backend 8080
127.0.0.1:5173  -> frontend 8080
```

### 6. Evidencia generada

```text
validation-output/seg001-docker-YYYYMMDD-HHMMSS.log
```

El directorio está ignorado por Git. No versionar el transcript completo sin revisarlo; resumir comandos, resultados y errores en `docs/validation/SEG-001.md`.

### 7. Si el validador falla

El script imprime automáticamente:

```powershell
docker compose --profile app --profile smoke ps
docker compose --profile app --profile smoke logs --no-color
```

Conservar:

- primera línea del error;
- servicio afectado;
- health observado;
- stack trace o error de compilación completo;
- ruta del transcript;
- SHA de `main`.

No corregir desactivando TypeScript strict, tests, guardas o health checks.

## Controles posteriores al stack Docker

### Maven verify completo

Con Docker activo:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Debe cubrir:

- compilación;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway V1–V5;
- Hibernate validate.

Si Java no está instalado en el host, instalar Java 21 antes de este control. La imagen backend usa `-DskipTests` y no sustituye Maven verify.

### Generar package-lock

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Verificar:

```powershell
Test-Path frontend\package-lock.json
git status --short
git diff -- frontend\package-lock.json
```

Dockerfile frontend, Makefile y CI ya detectan automáticamente el lockfile:

```text
presente -> npm ci
ausente  -> npm install
```

Después de generar el lockfile, repetir:

```powershell
docker compose --progress plain --profile app build --no-cache frontend
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

### Smoke manual opcional

Stack activo:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Contenedor:

```powershell
docker compose --profile app --profile smoke run --rm smoke
```

## Ejecución manual equivalente

Solo para diagnóstico cuando el orquestador falle:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173

powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly

docker compose --profile app --profile smoke down --remove-orphans

docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend

docker compose --profile app up -d
docker compose --profile app ps

powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
docker compose --profile app --profile smoke run --rm smoke
```

No usar `down -v`; eliminaría la base local.

## Linux/macOS

```bash
git switch main
git pull --ff-only
sh scripts/set-local-host-ports.sh 55432 8080 5173
sh scripts/preflight.sh --container-only
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
docker compose --profile app up -d
docker compose --profile app ps
sh scripts/smoke-test.sh
docker compose --profile app --profile smoke run --rm smoke
sh ./mvnw -B -f backend/pom.xml verify
sh scripts/generate-frontend-lock.sh
```

## Evidencia a registrar

- fecha y SHA;
- estado de `mvnw.cmd`;
- Docker y Compose;
- puertos seleccionados;
- preflight;
- build frontend limpio;
- build backend limpio;
- Compose config;
- health de tres servicios;
- Flyway/Hibernate;
- smoke host;
- smoke contenedor;
- Maven verify/Testcontainers;
- lockfile y npm ci;
- nuevos errores y correcciones;
- escaneo de secretos/datos;
- repetición final verde.

## Cierre

SEG-001 requiere:

- frontend clean build `PASS`;
- backend clean build `PASS`;
- Maven/Spotless/tests `PASS`;
- Flyway/Hibernate/Testcontainers `PASS`;
- package-lock/npm ci `PASS`;
- stack healthy;
- smoke host/container `PASS`;
- secretos/datos `PASS`;
- evidencia registrada;
- SEG-001 `COMPLETE`;
- SEG-002 `ACTIVE`.

## Restricciones

- no producción;
- no envíos;
- no XLSX real en Git, CI o imágenes;
- no declarar build limpio desde caché;
- no usar `down -v` sin intención destructiva;
- no versionar transcripts sin revisión;
- no iniciar SEG-002 con bloqueantes;
- no desactivar TypeScript strict, tests o guardas.
