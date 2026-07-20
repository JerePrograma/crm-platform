# SEG-001 — Build limpio, puerto PostgreSQL configurable y cierre desde `main`

## Estado

Producto, hardening, documentación y automatización están consolidados en `main`.

Evidencia real disponible:

- preflight PowerShell container-only: `PASS`;
- primer frontend build: `FAIL` por tres errores TypeScript;
- errores TypeScript: corregidos en `main`;
- segunda exportación frontend: `PASS_FROM_CACHE`;
- segunda exportación backend: `PASS_FROM_CACHE`;
- arranque del stack: `FAIL` por conflicto del puerto host `5432`;
- puerto configurable: corregido en `main`;
- build limpio, stack, Flyway, Hibernate, pruebas y smoke: pendientes.

No iniciar SEG-002, campañas, Gmail, Sheets, workers o cloud antes de estabilizar SEG-001.

## Fuentes canónicas

```text
branch: main
estado: docs/status.md
validación principal: docs/validation/SEG-001.md
primer build: docs/validation/SEG-001-container-build-2026-07-20.md
segunda reejecución: docs/validation/SEG-001-rerun-2026-07-20.md
Docker-only: docs/containerized-quickstart.md
automatización: scripts/README.md
backlog: docs/backlog.md
```

## Correcciones ya aplicadas

### Frontend

- referencia no anulable `activeCredentials`;
- tipos Vite y módulos CSS;
- `excludedRows` visible como `Bloqueadas`;
- `strict: true` conservado.

### PostgreSQL local

- nuevo `POSTGRES_HOST_PORT`;
- puerto recomendado `55432`;
- Compose publica `${POSTGRES_HOST_PORT}:5432`;
- backend contenedorizado mantiene `postgres:5432`;
- preflight valida puerto y `DATABASE_URL` coherentes.

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

### 2. Revisar la modificación local de `mvnw.cmd`

```powershell
git diff -- mvnw.cmd
```

Si no fue una modificación intencional:

```powershell
git restore -- mvnw.cmd
git status --short
```

No incluir `mvnw.cmd` en commits salvo que exista un cambio deliberado y revisado.

### 3. Ajustar `.env` sin sobrescribir credenciales

No copiar nuevamente `.env.example`.

Añadir o reemplazar:

```dotenv
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Con PowerShell:

```powershell
$envPath = '.env'
$content = Get-Content $envPath

if ($content -match '^POSTGRES_HOST_PORT=') {
  $content = $content -replace '^POSTGRES_HOST_PORT=.*$', 'POSTGRES_HOST_PORT=55432'
} else {
  $dbLine = [Array]::IndexOf($content, 'POSTGRES_DB=gestudio_crm')
  if ($dbLine -ge 0) {
    $before = $content[0..$dbLine]
    $after = if ($dbLine + 1 -lt $content.Count) { $content[($dbLine + 1)..($content.Count - 1)] } else { @() }
    $content = @($before + 'POSTGRES_HOST_PORT=55432' + $after)
  } else {
    $content += 'POSTGRES_HOST_PORT=55432'
  }
}

$content = $content -replace '^DATABASE_URL=.*$', 'DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm'
Set-Content -Path $envPath -Value $content -Encoding utf8
```

No imprimir ni reemplazar `CRM_BOOTSTRAP_PASSWORD`.

### 4. Ejecutar preflight

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Esperado:

```text
Preflight passed.
PostgreSQL host port: 55432
Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true
```

### 5. Limpiar contenedores incompletos

Conservar el volumen:

```powershell
docker compose --profile app --profile smoke down --remove-orphans
```

No usar `-v` salvo que se quiera eliminar la base local.

### 6. Reconstruir frontend sin caché

La sintaxis correcta coloca `--progress` antes del subcomando:

```powershell
docker compose --progress plain --profile app build --no-cache frontend
```

Debe verse una ejecución real de:

```text
npm install
npm run build
tsc -b
vite build
```

No aceptar como evidencia definitiva una salida donde todos esos stages aparezcan `CACHED`.

### 7. Reconstruir backend sin caché

```powershell
docker compose --progress plain --profile app build --no-cache backend
```

Debe verse una ejecución real de:

```text
dependency:go-offline
mvn -B -DskipTests package
```

El Dockerfile genera la imagen, pero no sustituye `mvn verify`.

### 8. Levantar el stack

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

### 9. Logs ante fallo

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

### 12. Maven verify completo

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

- versionar el lockfile;
- migrar Dockerfile y CI a `npm ci`;
- habilitar caché npm por lockfile;
- repetir frontend, imágenes y smoke.

## Diagnóstico opcional del puerto 5432

Para identificar quién lo utiliza:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Luego:

```powershell
Get-Process -Id <OwningProcess>
```

Puertos reservados por Windows:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

No es necesario detener otro PostgreSQL si se utiliza `POSTGRES_HOST_PORT=55432`.

## Ruta Linux/macOS

```bash
git switch main
git pull --ff-only

# editar .env:
# POSTGRES_HOST_PORT=55432
# DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm

sh scripts/preflight.sh --container-only

docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
docker compose --profile app up -d
docker compose --profile app ps

sh scripts/smoke-test.sh
sh ./mvnw -B -f backend/pom.xml verify
sh scripts/generate-frontend-lock.sh
```

## Evidencia a registrar

- SHA exacto;
- diff o restauración de `mvnw.cmd`;
- puerto host PostgreSQL;
- preflight;
- build frontend sin caché;
- build backend sin caché;
- Compose config;
- health de los tres servicios;
- Flyway/Hibernate;
- smoke host;
- smoke contenedorizado;
- Maven verify/Testcontainers;
- lockfile;
- cualquier nuevo fallo y corrección.

## Criterios de cierre

- frontend build limpio: `PASS`;
- backend build limpio: `PASS`;
- Maven verify/Spotless/tests: `PASS`;
- Flyway/Hibernate/Testcontainers: `PASS`;
- lockfile y `npm ci`: `PASS`;
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
- no declarar build limpio usando solo caché;
- no usar `docker compose down -v` sin intención de borrar la base;
- no iniciar SEG-002 con bloqueantes;
- no desactivar controles TypeScript.
