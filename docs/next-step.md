# Continuidad después de SEG-001

## Estado

```text
SEG-000 COMPLETE
SEG-001 COMPLETE
SEG-002 PLANNED
VALIDACIÓN LOCAL INTEGRAL PASS
LOCKFILE VERSIONADO
SEGUNDA CORRIDA LIMPIA NPM_CI PASS
CI_VISIBLE_GREEN
PRODUCCIÓN NO DESPLEGADA
COMUNICACIONES DESHABILITADAS
```

SEG-001 quedó cerrado sobre `d8a5a449a72c660e2655f4be7144360cd1e719a4`.
SEG-002 no está implementado ni activado. No iniciar campañas, Gmail, Sheets,
workers, cloud o producción sin una instrucción explícita posterior.

## Evidencia de cierre

```text
validation-output/seg001-complete-20260721-133002.log
validation-output/seg001-complete-20260721-133002.json
validation-output/seg001-docker-20260721-133003.json
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
GitHub Actions run 29848718163: success
```

Matriz cerrada: Flyway V1–V5, Hibernate, tres servicios healthy, ambos smoke,
Maven verify, 29/29 tests, Spotless 55/55, ArchUnit, Testcontainers, lockfile,
dos builds mediante `npm ci`, seguridad del repositorio y CI visible están en
`PASS`.

## Evidencia histórica anterior al cierre

La sexta ejecución integral Windows del 2026-07-21 se realizó sobre:

```text
commit ejecutado: 39f5f9e635722a9a37e0c3abdb3ca452e8cd8bc5
PostgreSQL host port: 25432
Backend host port: 8080
Frontend host port: 5173
```

Aprobó:

```text
checkout y working tree
PowerShell syntax — 11 scripts
preflight container-only
Docker daemon y Compose config
guardas de envío
publicaciones Docker y bind Windows
PostgreSQL publication
PostgreSQL health
frontend clean build --no-cache
TypeScript strict
Vite production build
backend clean image build --no-cache
Maven package con tests omitidos
```

El primer fallo real fue:

```text
Schema validation: missing table [contact]
```

No se registró ninguna ejecución de Flyway antes de que Hibernate validara un esquema vacío.

Evidencia detallada:

```text
docs/validation/SEG-001-flyway-autoconfiguration-failure-2026-07-21.md
```

## Correcciones históricas ya publicadas en `main`

```text
50a6b1b7c6eedd45da9a8af1462b76e00ff64427
fix: enable Flyway auto-configuration on Spring Boot 4

a3782c42fdf6c84e83ad8adcebd8770d41438098
fix: fail fast when Flyway migrations are unavailable
```

Cambios:

- `spring-boot-starter-flyway` incorpora la auto-configuración requerida por Spring Boot 4.1;
- `flyway-database-postgresql` permanece como módulo de base específico;
- `spring.flyway.fail-on-missing-locations=true` falla explícitamente si los SQL no están empaquetados.

## Procedimiento histórico ejecutado

Los pasos siguientes se conservan como trazabilidad del recorrido que cerró
SEG-001; no son una tarea pendiente.

### 1. Actualizar checkout

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git status --short
git rev-parse HEAD
```

Esperado:

```text
git status --short: sin salida
HEAD: commit posterior a a3782c42
```

### 2. Detener el stack parcial sin borrar datos

La ejecución anterior usó `-KeepRunning`. Retirar contenedores del proyecto conservando el volumen PostgreSQL:

```powershell
docker compose `
  --profile app `
  --profile smoke `
  down `
  --remove-orphans
```

No usar `-v`.

### 3. Confirmar scripts y puerto

```powershell
powershell `
  -ExecutionPolicy Bypass `
  -File scripts/check-powershell-syntax.ps1

powershell `
  -ExecutionPolicy Bypass `
  -File scripts/check-host-ports.ps1 `
  -PostgresPort 25432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

El checker debe confirmar publicaciones Docker y enlace Windows disponibles.

### 4. Reejecutar validación integral

```powershell
powershell `
  -ExecutionPolicy Bypass `
  -File scripts/validate-seg001.ps1 `
  -PostgresPort 25432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

No usar `-UseBuildCache` como evidencia de cierre.

## Señal Flyway confirmada

Antes de los logs de Hibernate deben aparecer eventos equivalentes a:

```text
Flyway Community Edition
Database: jdbc:postgresql://postgres:5432/gestudio_crm
Creating Schema History table "public"."flyway_schema_history"
Migrating schema "public" to version "1 ..."
...
Successfully applied migrations
```

Luego Hibernate debe completar `ddl-auto=validate` sin `missing table`.

Los textos exactos pueden variar por versión; el criterio es que exista una instancia Flyway, se aplique el historial y las migraciones precedan a JPA.

## Orden automático confirmado

1. rama `main` y árbol limpio;
2. puertos y `DATABASE_URL`;
3. preflight fail-closed;
4. Compose config;
5. cleanup sin `-v`;
6. propiedad Docker y enlace Windows;
7. PostgreSQL start/health;
8. frontend clean build;
9. backend clean image build;
10. Flyway migrations;
11. Hibernate validation;
12. backend/frontend health;
13. smoke host y contenedor;
14. Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
15. generación segura de `package-lock.json`;
16. SHA-256;
17. frontend rebuild mediante `npm ci`;
18. health y smoke finales;
19. seguridad del repositorio;
20. JSON y transcript;
21. ningún commit automático.

## Resultado integral observado

```text
postgres health: healthy
backend health: healthy
frontend health: healthy
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

## Lockfile cerrado

La ejecución integral alcanzó la fase segura, generó el archivo sin lifecycle
scripts ni `node_modules` y permitió revisar y versionar exclusivamente
`frontend/package-lock.json`.

Después de ver `Complete SEG-001 validation passed.` se ejecutó:

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Se versionó únicamente el lockfile revisado:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

Después se repitió el validador desde árbol limpio y se demostró `npm ci` desde
el primer build.

## Criterios de cierre

- [x] PowerShell syntax;
- [x] preflight;
- [x] puertos Windows/propiedad Docker;
- [x] publicación y health PostgreSQL;
- [x] frontend clean build;
- [x] backend clean image build;
- [x] Flyway auto-configuración corregida ejecutada;
- [x] migraciones V1–V5 aplicadas;
- [x] Hibernate validate;
- [x] backend/frontend healthy;
- [x] smoke host;
- [x] smoke contenedor;
- [x] Maven verify;
- [x] Spotless;
- [x] unit tests;
- [x] ArchUnit;
- [x] Testcontainers;
- [x] package-lock versionado;
- [x] npm ci con lockfile versionado;
- [x] seguridad final;
- [x] evidencia final;
- [x] CI verde visible;
- [x] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Próxima decisión

SEG-002 permanece `PLANNED`. Antes de activarlo, actualizar `main`, leer su
alcance y solicitar autorización explícita. Comandos de continuidad:

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git status --short
git rev-parse HEAD

Get-Content AGENTS.md
Get-Content docs\status.md
Get-Content docs\next-step.md
Get-Content docs\backlog.md
```

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real a Git, CI o imágenes;
- no versionar `.env` o `validation-output/`;
- no usar `docker compose down -v` salvo destrucción intencional;
- no usar `docker system prune` como procedimiento normal;
- no ejecutar Testcontainers sobre código no confiable;
- no comenzar SEG-002 con bloqueantes.
