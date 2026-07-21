# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
HARDENING COMPLETO
VALIDACIÓN INTEGRAL WINDOWS/UNIX IMPLEMENTADA
PRIMER INTENTO DEL ORQUESTADOR: FAIL_POWERSHELL_PARSE
PARSER CORREGIDO EN MAIN
EJECUCIÓN FUNCIONAL PENDIENTE
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Evidencia más reciente

El 2026-07-21 se ejecutó desde Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

La ejecución falló durante el parseo de PowerShell antes de preflight o Docker:

```text
InvalidVariableReferenceWithDrive
$LASTEXITCODE:
```

El mismo patrón existía en tres scripts y fue corregido en `main`:

```text
scripts/validate-seg001.ps1
scripts/validate-docker-stack.ps1
scripts/verify-backend-container.ps1
```

También se añadió:

```text
scripts/check-powershell-syntax.ps1
```

y una regresión CI que rechaza `$LASTEXITCODE:`.

Evidencia detallada:

```text
docs/validation/SEG-001-powershell-parser-failure-2026-07-21.md
```

## Objetivo inmediato

Actualizar el checkout, dejar el árbol limpio, comprobar la sintaxis de todos los scripts PowerShell y repetir la validación integral.

La próxima evidencia funcional comienza recién después del parser:

- preflight;
- Compose config;
- builds limpios;
- stack y health;
- smoke;
- Maven verify/Testcontainers;
- lockfile;
- npm ci;
- seguridad.

## 1. Actualizar `main`

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git rev-parse HEAD
git status --short
```

No fusionar ni copiar nada desde `feat/seg-001-prospect-vertical-slice`.

## 2. Normalizar `mvnw.cmd`

El checkout anterior mostró `mvnw.cmd` modificado aunque la diferencia ignorando finales de línea estaba vacía.

El blob remoto fue renormalizado sin cambios funcionales.

Después del pull ejecutar:

```powershell
git restore --source=origin/main --staged --worktree -- mvnw.cmd
git update-index --refresh
git status --short
```

Esperado:

```text
sin salida para mvnw.cmd
```

Si todavía aparece modificado, conservar estas salidas:

```powershell
git diff --raw -- mvnw.cmd
git diff --numstat -- mvnw.cmd
git diff --ignore-space-at-eol -- mvnw.cmd
git check-attr -a -- mvnw.cmd
git config --show-origin --get core.autocrlf
git rev-parse HEAD:mvnw.cmd
git hash-object --path=mvnw.cmd mvnw.cmd
```

No ejecutar el validador integral con cambios inesperados.

## 3. Confirmar `.env`

Crear únicamente cuando no exista:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Conservar:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Puertos recomendados:

```text
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
```

Los configuradores actualizan puertos y `DATABASE_URL` sin reemplazar contraseñas.

## 4. Comprobar sintaxis PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
```

Esperado:

```text
PowerShell syntax validation passed for <N> scripts.
```

No continuar si este control falla.

## 5. Ejecutar preflight separado

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Esto permite distinguir un problema de configuración o Docker de un fallo posterior de build.

Esperado:

```text
Preflight passed.
Docker daemon: reachable
Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true
```

## 6. Ejecutar validación integral

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

No usar `-UseBuildCache` como evidencia de cierre.

Puertos alternativos:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

## 7. Fases automáticas

1. rama `main` y árbol limpio;
2. configuración segura de puertos;
3. preflight fail-closed;
4. Compose config;
5. cleanup sin `-v`;
6. frontend build sin caché;
7. backend build sin caché;
8. arranque de PostgreSQL, backend y frontend;
9. tres health checks;
10. smoke host y contenedor;
11. Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
12. Flyway/Hibernate durante arranque y suite;
13. generación segura de `package-lock.json`;
14. SHA-256 del lockfile;
15. frontend rebuild mediante `npm ci`;
16. health y smoke finales;
17. seguridad del repositorio;
18. JSON y transcript;
19. ningún commit automático.

## 8. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

Servicios:

```text
postgres   healthy   127.0.0.1:55432->5432
backend    healthy   127.0.0.1:8080->8080
frontend   healthy   127.0.0.1:5173->8080
```

## 9. Si falla

Conservar:

- commit exacto;
- comando ejecutado;
- primer error real;
- fase;
- JSON;
- transcript;
- `docker compose ps`;
- logs del servicio afectado;
- `git status --short`.

No corregir desactivando tests, TypeScript strict, health checks, Testcontainers, guardas o builds limpios.

## 10. Después del primer PASS

```powershell
Get-ChildItem validation-output |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 8

Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Versionar únicamente el lockfile:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar:

```text
.env
validation-output/
gestudio_lote_100_prospectos.xlsx
```

## 11. Segunda ejecución

Después de versionar el lockfile:

```powershell
git pull --ff-only
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Debe empezar con árbol limpio y utilizar `npm ci` desde el primer build.

## 12. Criterios de cierre

- [ ] sintaxis PowerShell PASS en checkout real;
- [ ] preflight actualizado PASS;
- [ ] clean build frontend PASS;
- [ ] clean build backend PASS;
- [ ] Maven verify PASS;
- [ ] Spotless PASS;
- [ ] unit tests PASS;
- [ ] ArchUnit PASS;
- [ ] Testcontainers PASS;
- [ ] Flyway PASS;
- [ ] Hibernate validate PASS;
- [ ] tres servicios healthy;
- [ ] smoke host PASS;
- [ ] smoke container PASS;
- [ ] package-lock revisado y versionado;
- [ ] npm ci PASS con lockfile versionado;
- [ ] seguridad PASS;
- [ ] evidencia registrada;
- [ ] CI visible verde o excepción explícita documentada;
- [ ] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real a Git, CI o imágenes;
- no usar `docker compose down -v` salvo destrucción intencional;
- no ejecutar Testcontainers contenedorizado sobre código no confiable;
- no versionar transcripts;
- no comenzar SEG-002 con bloqueantes.
