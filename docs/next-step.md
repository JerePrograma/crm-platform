# SEG-001 — Build limpio, puerto PostgreSQL configurable y cierre desde `main`

## Estado

- producto y hardening: consolidados en `main`;
- preflight inicial: PASS;
- errores TypeScript: reproducidos y corregidos;
- imágenes frontend/backend: exportadas desde caché;
- build limpio: pendiente;
- stack: falló por puerto host 5432;
- puerto configurable: corregido;
- Flyway/Hibernate/tests/smoke: pendientes.

No iniciar SEG-002, campañas, Gmail, Sheets, workers o cloud.

## Fuentes

```text
estado: docs/status.md
matriz: docs/validation/SEG-001.md
primer build: docs/validation/SEG-001-container-build-2026-07-20.md
reejecución: docs/validation/SEG-001-rerun-2026-07-20.md
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

### 3. Configurar el puerto PostgreSQL sin tocar credenciales

No copiar nuevamente `.env.example`.

Ejecutar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-postgres-host-port.ps1 -Port 55432
```

El script:

- añade o reemplaza `POSTGRES_HOST_PORT`;
- actualiza `DATABASE_URL`;
- conserva usuario y contraseñas;
- conserva `SENDING_*`;
- escribe UTF-8 sin BOM.

Resultado esperado:

```text
PostgreSQL host port: 55432
Database URL: jdbc:postgresql://localhost:55432/gestudio_crm
Existing passwords and sending controls were preserved.
```

### 4. Preflight

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Esperado:

```text
Preflight passed.
PostgreSQL host port: 55432
Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true
```

### 5. Limpiar el intento incompleto

```powershell
docker compose --profile app --profile smoke down --remove-orphans
```

No usar `-v`; borraría la base local.

### 6. Build limpio frontend

```powershell
docker compose --progress plain --profile app build --no-cache frontend
```

Debe ejecutar realmente:

```text
npm install
npm run build
tsc -b
vite build
```

No aceptar una salida completamente `CACHED` como prueba limpia.

### 7. Build limpio backend

```powershell
docker compose --progress plain --profile app build --no-cache backend
```

Debe ejecutar:

```text
dependency:go-offline
mvn -B -DskipTests package
```

Este build no reemplaza `mvn verify`.

### 8. Levantar stack

```powershell
docker compose --profile app up -d
docker compose --profile app ps
```

Esperado:

```text
postgres   healthy   127.0.0.1:55432->5432
backend    healthy   127.0.0.1:8080->8080
frontend   healthy   127.0.0.1:5173->8080
```

### 9. Logs si falla

```powershell
docker compose logs postgres
docker compose logs backend
docker compose logs frontend
```

Seguimiento:

```powershell
docker compose --profile app logs -f
```

### 10. Smoke desde Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

### 11. Smoke contenedorizado

```powershell
docker compose --profile app --profile smoke up --abort-on-container-exit --exit-code-from smoke smoke
docker compose --profile app --profile smoke down --remove-orphans
```

### 12. Maven verify

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

### 13. Generar lockfile

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Verificar:

```powershell
Test-Path frontend\package-lock.json
git status --short
```

Después:

- versionar lockfile;
- migrar Dockerfile/CI a npm ci;
- habilitar caché npm;
- repetir builds y smoke.

## Diagnóstico opcional del 5432

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

No es necesario detener otro servicio al usar 55432.

## Linux/macOS

```bash
git switch main
git pull --ff-only
sh scripts/set-postgres-host-port.sh 55432
sh scripts/preflight.sh --container-only
docker compose --profile app --profile smoke down --remove-orphans
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
docker compose --profile app up -d
docker compose --profile app ps
sh scripts/smoke-test.sh
sh ./mvnw -B -f backend/pom.xml verify
sh scripts/generate-frontend-lock.sh
```

## Evidencia a registrar

- SHA;
- estado de `mvnw.cmd`;
- salida del actualizador de puerto;
- preflight;
- builds limpios;
- Compose config;
- health;
- Flyway/Hibernate;
- smoke host/container;
- Maven verify/Testcontainers;
- lockfile;
- nuevos errores y correcciones.

## Cierre

Requiere:

- frontend clean build PASS;
- backend clean build PASS;
- Maven/Spotless/tests PASS;
- Flyway/Hibernate/Testcontainers PASS;
- lockfile/npm ci PASS;
- stack healthy;
- smoke PASS;
- secretos/datos PASS;
- evidencia registrada;
- SEG-001 COMPLETE;
- SEG-002 ACTIVE.

## Restricciones

- no producción;
- no envíos;
- no XLSX real en Git/CI/imágenes;
- no declarar clean build desde caché;
- no usar `down -v` sin intención destructiva;
- no iniciar SEG-002 con bloqueantes;
- no desactivar TypeScript strict.
