# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
HARDENING COMPLETO
SINTAXIS POWERSHELL PASS
PREFLIGHT PASS
FRONTEND CLEAN BUILD PASS
BACKEND CLEAN IMAGE BUILD PASS
ÚLTIMO INTENTO: FAIL_WINDOWS_HOST_PORT_BIND
STACK/HEALTH/SMOKE PENDIENTES
MAVEN_VERIFY/TESTCONTAINERS PENDIENTES
LOCKFILE/NPM_CI PENDIENTES
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Evidencia más reciente

El 2026-07-21 se ejecutó desde Windows sobre:

```text
main
65b64000a7e8f6abd71f2b118cebe904ee61f1d1
```

Pasaron:

```text
PowerShell syntax: PASS — 10 scripts
preflight container-only: PASS
Compose config: PASS
frontend clean build: PASS
backend clean image build: PASS
working tree final: limpio
```

El primer error real ocurrió al levantar PostgreSQL:

```text
127.0.0.1:55432
bind: An attempt was made to access a socket in a way forbidden by its access permissions
```

Clasificación:

```text
EXECUTED_FAIL — WINDOWS_HOST_PORT_BIND
```

No se ejecutaron health, Flyway, Hibernate, smoke, Maven verify, Testcontainers, package-lock o npm ci.

Evidencia:

```text
docs/validation/SEG-001-port-bind-failure-2026-07-21.md
validation-output/seg001-complete-20260721-102334.log
validation-output/seg001-complete-20260721-102334.json
validation-output/seg001-docker-20260721-102335.json
```

## Corrección publicada

Se añadió:

```text
scripts/check-host-ports.ps1
```

El validador Docker ahora:

1. ejecuta cleanup sin borrar volúmenes;
2. intenta enlazar los tres puertos en `127.0.0.1`;
3. falla antes de reconstruir imágenes cuando un puerto está ocupado o reservado;
4. registra `hostPorts=PASS` en la evidencia JSON;
5. no informa falsamente que dejó un stack activo cuando no hay contenedores ejecutándose.

CI parsea todos los scripts y ejecuta el checker con puertos alternativos.

## 1. Actualizar `main`

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git rev-parse HEAD
git status --short
```

El árbol debe estar limpio. No fusionar ni copiar nada desde `feat/seg-001-prospect-vertical-slice`.

## 2. Comprobar sintaxis PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
```

Esperado:

```text
PowerShell syntax validation passed for <N> scripts.
```

## 3. Diagnosticar el puerto rechazado

```powershell
Get-NetTCPConnection -LocalPort 55432 -ErrorAction SilentlyContinue
netsh interface ipv4 show excludedportrange protocol=tcp
```

No modificar los rangos excluidos de Windows. Elegir un puerto alternativo es la solución conservadora.

## 4. Comprobar puertos alternativos

Usar inicialmente:

```text
PostgreSQL: 15432
Backend:    8080
Frontend:   5173
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Esperado:

```text
PostgreSQL host port available: 127.0.0.1:15432
Backend host port available: 127.0.0.1:8080
Frontend host port available: 127.0.0.1:5173
All requested loopback host ports are available.
```

Si alguno falla, elegir otro valor y repetir únicamente este checker.

## 5. Confirmar `.env`

Crear únicamente si no existe:

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

El validador actualizará puertos y `DATABASE_URL` sin reemplazar contraseñas.

## 6. Ejecutar preflight

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

Esperado:

```text
Preflight passed.
Docker daemon: reachable
Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true
```

## 7. Repetir validación integral

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

No usar `-UseBuildCache` como evidencia de cierre.

## 8. Fases esperadas

1. rama `main` y árbol limpio;
2. configuración segura de puertos;
3. preflight;
4. Compose config;
5. cleanup sin `-v`;
6. enlace real de los tres puertos;
7. frontend build sin caché;
8. backend build sin caché;
9. arranque de PostgreSQL, backend y frontend;
10. health checks;
11. smoke host y contenedor;
12. Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
13. Flyway y Hibernate;
14. generación de `package-lock.json`;
15. SHA-256;
16. rebuild mediante `npm ci`;
17. health y smoke finales;
18. seguridad del repositorio;
19. JSON y transcript.

## 9. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

Servicios esperados:

```text
postgres   healthy   127.0.0.1:15432->5432
backend    healthy   127.0.0.1:8080->8080
frontend   healthy   127.0.0.1:5173->8080
```

## 10. Si falla

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

No desactivar tests, TypeScript strict, health checks, Testcontainers, guardas o builds limpios.

## 11. Después del primer PASS

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

## 12. Segunda ejecución

Después de versionar el lockfile, repetir el validador desde un árbol limpio con los mismos puertos. Debe utilizar `npm ci` desde el primer build.

## Criterios de cierre

- [x] sintaxis PowerShell PASS;
- [x] preflight actualizado PASS;
- [x] clean build frontend PASS;
- [x] clean image build backend PASS;
- [ ] host ports alternativos PASS;
- [ ] PostgreSQL/backend/frontend healthy;
- [ ] Flyway PASS;
- [ ] Hibernate validate PASS;
- [ ] smoke host PASS;
- [ ] smoke container PASS;
- [ ] Maven verify PASS;
- [ ] Spotless PASS;
- [ ] unit tests PASS;
- [ ] ArchUnit PASS;
- [ ] Testcontainers PASS;
- [ ] package-lock revisado y versionado;
- [ ] npm ci PASS con lockfile versionado;
- [ ] seguridad PASS;
- [ ] evidencia registrada;
- [ ] CI visible verde o excepción documentada;
- [ ] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real a Git, CI o imágenes;
- no usar `docker compose down -v` salvo destrucción intencional;
- no modificar rangos excluidos de Windows para forzar un puerto;
- no ejecutar Testcontainers contenedorizado sobre código no confiable;
- no versionar transcripts;
- no comenzar SEG-002 con bloqueantes.
