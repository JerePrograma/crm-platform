# SEG-001 — Próxima acción canónica

## Estado

```text
IMPLEMENTACIÓN COMPLETA
VALIDACIÓN INTEGRAL AUTOMATIZADA
EJECUCIÓN REAL PENDIENTE
```

No iniciar SEG-002, campañas, Gmail, Sheets, workers, cloud o producción.

## Objetivo

Ejecutar desde Windows/Docker el recorrido completo que valida:

- builds limpios frontend/backend;
- Compose config;
- PostgreSQL, backend y frontend saludables;
- Flyway y Hibernate durante el arranque;
- smoke host y contenedor;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- generación segura de package-lock;
- reconstrucción frontend mediante npm ci;
- seguridad del repositorio;
- evidencia JSON y transcript.

## Fuentes

```text
estado: docs/status.md
matriz: docs/validation/SEG-001.md
automatización completa: docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
primer build: docs/validation/SEG-001-container-build-2026-07-20.md
segunda reejecución: docs/validation/SEG-001-rerun-2026-07-20.md
puertos/orquestación: docs/validation/SEG-001-local-orchestration-2026-07-20.md
Docker: docs/containerized-quickstart.md
scripts: scripts/README.md
```

## 1. Actualizar `main`

```powershell
Set-Location C:\laburo\crm-platform

git switch main
git fetch origin
git pull --ff-only

git status
git rev-parse HEAD
```

## 2. Resolver el cambio local de `mvnw.cmd`

Inspeccionar:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git diff --numstat -- mvnw.cmd
```

Si no fue un cambio deliberado:

```powershell
git restore -- mvnw.cmd
git status --short
```

El validador integral exige archivos rastreados limpios. No restaura ni descarta cambios automáticamente.

## 3. Confirmar `.env`

No copiar nuevamente `.env.example` cuando `.env` ya contiene credenciales locales.

El script actualizará únicamente puertos y `DATABASE_URL`.

Guardas obligatorias:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

## 4. Ejecutar validación integral

Comando recomendado:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Este comando no requiere Java, Maven, Node o npm instalados en el host.

No usar `-UseBuildCache` como evidencia de cierre.

## 5. Puertos alternativos

Si `8080` o `5173` están ocupados:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

Los smoke tests toman las URLs desde `.env`.

## 6. Fases ejecutadas automáticamente

### Fase A — precondiciones

- rama `main`;
- commit registrado;
- archivos rastreados limpios;
- Docker disponible;
- `.env` presente;
- puertos válidos y distintos;
- guardas cerradas;
- Compose válido.

### Fase B — stack Docker inicial

- cleanup sin `-v`;
- build frontend sin caché;
- build backend sin caché;
- arranque del perfil `app`;
- health de PostgreSQL;
- health del backend;
- health del frontend;
- smoke PowerShell;
- smoke contenedorizado.

### Fase C — backend completo

- Maven 3.9.16/Java 21 dentro de Docker;
- repositorio de solo lectura;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway/Hibernate cubiertos por arranque y suite.

### Fase D — lockfile y npm ci

- `npm install --package-lock-only --ignore-scripts`;
- verificación de ausencia de `node_modules`;
- SHA-256 del lockfile;
- build frontend sin caché;
- selección automática de `npm ci`;
- recreación del frontend;
- health;
- smoke final host/contenedor.

### Fase E — seguridad

- `git diff --check`;
- `.env` no rastreado;
- transcripts no rastreados;
- datos privados no rastreados;
- lote operativo no rastreado;
- claves/certificados no rastreados;
- JSON de credenciales no rastreados;
- único cambio permitido: `frontend/package-lock.json`.

## 7. Resultado esperado

```text
SEG-001 Docker validation passed.
Containerized backend verification passed.
Frontend lockfile generated.
Repository safety scan passed.
Complete SEG-001 validation passed.
```

Servicios con puertos predeterminados:

```text
postgres   healthy   127.0.0.1:55432->5432
backend    healthy   127.0.0.1:8080->8080
frontend   healthy   127.0.0.1:5173->8080
```

## 8. Evidencia local

Archivos esperados:

```text
validation-output/seg001-docker-YYYYMMDD-HHMMSS.json
validation-output/seg001-complete-YYYYMMDD-HHMMSS.json
validation-output/seg001-complete-YYYYMMDD-HHMMSS.log
frontend/package-lock.json
```

`validation-output/` está ignorado por Git.

No compartir el transcript sin revisarlo. Puede contener logs de aplicación, aunque los scripts no imprimen contraseñas.

## 9. Si falla

El script:

- marca la fase incompleta;
- registra el error en JSON;
- imprime `docker compose ps`;
- imprime logs del stack;
- conserva el transcript;
- retira el stack salvo `-KeepRunning`.

Conservar:

- SHA exacto;
- ruta del JSON;
- ruta del transcript;
- primera línea del error;
- fase afectada;
- servicio o test;
- stack trace completo;
- estado de los servicios.

No corregir mediante:

- desactivar TypeScript strict;
- omitir tests;
- desactivar health checks;
- desactivar Testcontainers;
- relajar guardas;
- habilitar envíos;
- usar builds cacheados como evidencia.

## 10. Revisar package-lock

Después de PASS:

```powershell
Test-Path frontend\package-lock.json
Get-FileHash frontend\package-lock.json -Algorithm SHA256
git status --short
git diff -- frontend\package-lock.json
```

Esperado:

```text
?? frontend/package-lock.json
```

O una modificación controlada si el archivo ya estaba versionado.

## 11. Versionar lockfile

Solo después de revisar:

```powershell
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

No agregar `.env` ni `validation-output/`.

## 12. Repetir sobre lockfile versionado

Actualizar y repetir:

```powershell
git pull --ff-only
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Esta segunda ejecución debe comenzar con un árbol rastreado limpio y utilizar `npm ci` desde el inicio.

## 13. CI

Después del push:

- verificar job backend;
- verificar frontend con npm ci;
- verificar scripts y seguridad;
- verificar imágenes/stack/smoke;
- no marcar SEG-001 completo mientras no exista evidencia CI visible o una decisión explícita documentada sobre su indisponibilidad.

## 14. Criterios de cierre

- [ ] validador integral PASS;
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
- [ ] evidencia resumida en matriz;
- [ ] CI visible verde o excepción documentada;
- [ ] SEG-001 COMPLETE;
- [ ] SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar XLSX real a Git, CI o imágenes;
- no usar `docker compose down -v` salvo intención destructiva;
- no ejecutar verificación backend contenedorizada sobre código no confiable porque monta el socket Docker;
- no versionar transcripts;
- no comenzar SEG-002 con bloqueantes.
