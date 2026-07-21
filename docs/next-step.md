# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
HARDENING COMPLETO
SINTAXIS POWERSHELL PASS
PREFLIGHT PASS
FRONTEND CLEAN BUILD PASS
BACKEND CLEAN IMAGE BUILD PASS
ÚLTIMO INTENTO: FAIL_DOCKER_HOST_PORT_ALREADY_ALLOCATED
STACK/HEALTH/SMOKE PENDIENTES
MAVEN_VERIFY/TESTCONTAINERS PENDIENTES
LOCKFILE/NPM_CI PENDIENTES
CI_NOT_VISIBLE
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Evidencia más reciente

La ejecución Windows del 2026-07-21 se realizó sobre:

```text
commit ejecutado: f903a9e1278697af53e0bcbee3bd10b16e10b991
PostgreSQL host port: 15432
Backend host port: 8080
Frontend host port: 5173
```

Aprobó:

```text
checkout y working tree
PowerShell syntax, 11 scripts
preflight container-only
Docker daemon y Compose config
guardas de envío
TcpListener Windows para los tres puertos
frontend clean build --no-cache
TypeScript strict
Vite production build
backend clean image build --no-cache
Maven package con tests omitidos
```

El primer fallo real fue:

```text
Bind for 0.0.0.0:15432 failed: port is already allocated
```

El checker anterior comprobaba Windows, pero no detectaba publicaciones de otros contenedores Docker. Evidencia detallada:

```text
docs/validation/SEG-001-docker-port-owner-failure-2026-07-21.md
```

## Correcciones ya publicadas en `main`

- `scripts/check-host-ports.ps1` inspecciona `docker ps` y `TcpListener`;
- informa ID, nombre y puertos del contenedor propietario;
- `scripts/validate-docker-stack.ps1` inicia y valida PostgreSQL antes de los builds;
- los builds solo comienzan después de comprobar la publicación Docker real;
- ambos validadores imprimen publicaciones Docker al fallar;
- `stackKeptRunning` refleja contenedores realmente activos.

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
```

## 2. Identificar el propietario de `15432`

```powershell
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
docker ps --filter publish=15432 --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}"
```

Cuando aparezca un contenedor ajeno:

```powershell
docker stop <NOMBRE_O_ID>
```

No eliminar su volumen ni usar `docker system prune`.

Retirar también cualquier stack parcial de este repositorio, conservando datos:

```powershell
docker compose --profile app --profile smoke down --remove-orphans
```

## 3. Comprobar scripts y puertos

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1

powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

El checker actualizado debe imprimir controles Windows y Docker para cada puerto.

Si `15432` continúa ocupado o reservado, probar `25432`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-host-ports.ps1 `
  -PostgresPort 25432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

## 4. Ejecutar validación integral

Con `15432` libre:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 15432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

O con el puerto alternativo validado:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 25432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

No usar `-UseBuildCache` como evidencia de cierre.

## Orden automático actual

1. rama `main` y árbol limpio;
2. puertos y `DATABASE_URL`;
3. preflight fail-closed;
4. Compose config;
5. cleanup sin `-v`;
6. propiedad Docker y enlace Windows de puertos;
7. arranque y health de PostgreSQL;
8. frontend clean build;
9. backend clean image build;
10. arranque y health de backend/frontend;
11. smoke host y contenedor;
12. Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
13. generación segura de `package-lock.json`;
14. SHA-256;
15. frontend rebuild mediante `npm ci`;
16. health y smoke finales;
17. seguridad del repositorio;
18. JSON y transcript;
19. ningún commit automático.

## Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

## Después del primer PASS

```powershell
Get-ChildItem validation-output |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 8

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
- [x] preflight actualizado;
- [x] frontend clean build;
- [x] backend clean image build;
- [ ] publicación PostgreSQL Docker;
- [ ] tres servicios healthy;
- [ ] Flyway;
- [ ] Hibernate validate;
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
- no usar `docker compose down -v` salvo destrucción intencional;
- no usar `docker system prune` para resolver el conflicto;
- no ejecutar Testcontainers sobre código no confiable;
- no versionar `validation-output/`;
- no comenzar SEG-002 con bloqueantes.
