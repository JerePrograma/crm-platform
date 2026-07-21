# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
HARDENING COMPLETO
SINTAXIS POWERSHELL PASS
PREFLIGHT PASS
PUERTOS WINDOWS/DOCKER PASS
POSTGRES PUBLICATION PASS
POSTGRES HEALTH PASS
FRONTEND CLEAN BUILD PASS
BACKEND CLEAN IMAGE BUILD PASS
ÚLTIMO INTENTO: FAIL_FLYWAY_AUTOCONFIGURATION_MISSING
FIX FLYWAY: IMPLEMENTED_NOT_RUN
STACK/HEALTH/SMOKE PENDIENTES
MAVEN_VERIFY/TESTCONTAINERS PENDIENTES
LOCKFILE/NPM_CI PENDIENTES
CI_NOT_VISIBLE
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Evidencia más reciente

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

## Correcciones ya publicadas en `main`

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

## 1. Actualizar checkout

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

## 2. Detener el stack parcial sin borrar datos

La ejecución anterior usó `-KeepRunning`. Retirar contenedores del proyecto conservando el volumen PostgreSQL:

```powershell
docker compose `
  --profile app `
  --profile smoke `
  down `
  --remove-orphans
```

No usar `-v`.

## 3. Confirmar scripts y puerto

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

## 4. Reejecutar validación integral

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

## Señal esperada del fix Flyway

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

## Orden automático esperado

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

## Resultado integral esperado

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

## No ejecutar todavía los comandos del lockfile

La ejecución anterior falló antes de esa fase. Por eso `frontend/package-lock.json` no existía y `git add` no tenía nada que agregar.

Solo después de ver `Complete SEG-001 validation passed.` ejecutar:

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Versionar únicamente el lockfile revisado:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

Después repetir el validador desde árbol limpio para demostrar `npm ci` desde el primer build.

## Criterios de cierre pendientes

- [x] PowerShell syntax;
- [x] preflight;
- [x] puertos Windows/propiedad Docker;
- [x] publicación y health PostgreSQL;
- [x] frontend clean build;
- [x] backend clean image build;
- [ ] Flyway auto-configuración corregida ejecutada;
- [ ] migraciones V1–V5 aplicadas;
- [ ] Hibernate validate;
- [ ] backend/frontend healthy;
- [ ] smoke host;
- [ ] smoke contenedor;
- [ ] Maven verify;
- [ ] Spotless;
- [ ] unit tests;
- [ ] ArchUnit;
- [ ] Testcontainers;
- [ ] package-lock versionado;
- [ ] npm ci con lockfile versionado;
- [ ] seguridad final;
- [ ] evidencia final;
- [ ] CI verde visible o excepción explícita;
- [ ] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real a Git, CI o imágenes;
- no versionar `.env` o `validation-output/`;
- no usar `docker compose down -v` salvo destrucción intencional;
- no usar `docker system prune` como procedimiento normal;
- no ejecutar Testcontainers sobre código no confiable;
- no comenzar SEG-002 con bloqueantes.
