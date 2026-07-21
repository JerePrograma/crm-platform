# Estado actual

Actualizado: 2026-07-20

## Repositorio y consolidación

- repositorio: `JerePrograma/crm-platform`;
- rama predeterminada y canónica: `main`;
- todo el trabajo vigente está en `main`;
- la consolidación original fue fast-forward, sin force push;
- las correcciones posteriores se realizaron directamente en `main` por autorización expresa;
- `feat/seg-001-prospect-vertical-slice` está detrás de `main` y no contiene cambios exclusivos;
- no existe pull request pendiente para esta consolidación;
- no se desplegó ningún ambiente;
- no se habilitó ningún envío.

Toda sesión debe comenzar con:

```bash
git switch main
git fetch origin
git pull --ff-only
```

No es necesario fusionar ni copiar contenido desde la rama histórica.

## Segmentos

| Segmento | Estado | Condición |
|---|---|---|
| SEG-000 — repositorio y continuidad | COMPLETE | fuente canónica, reglas y documentación |
| SEG-001 — vertical slice persistente de prospectos | ACTIVE | implementación completa; validación funcional pendiente |
| SEG-002 — identidad, usuarios y RBAC | PLANNED | bloqueado hasta cierre verde de SEG-001 |

Estado operativo de SEG-001:

```text
IMPLEMENTATION_COMPLETE
HARDENING_COMPLETE
MAIN_CONSOLIDATED
CROSS_PLATFORM_VALIDATION_IMPLEMENTED
FUNCTIONAL_VALIDATION_PENDING
LOCKFILE_PENDING
CI_NOT_VISIBLE
```

SEG-001 no puede marcarse `COMPLETE` solo porque exista código de validación. Requiere evidencia ejecutada.

## Alcance funcional implementado

### Backend

- Java 21;
- Spring Boot 4.1;
- Maven 3.9.16;
- Maven Wrapper con validación SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate `validate`;
- Actuator;
- Prometheus;
- logging estructurado;
- OpenAPI;
- RFC 7807;
- autenticación bootstrap fail-closed;
- API cerrada cuando faltan credenciales.

### Dominio

- instituciones;
- contactos;
- canales de contacto;
- prospectos;
- estados comerciales;
- exclusiones dominantes;
- equivalencia teléfono/WhatsApp;
- normalización central;
- elegibilidad;
- aplicación retroactiva de exclusiones;
- auditoría JSONB.

### Importaciones

- CSV UTF-8 con coma o punto y coma;
- comillas, delimitadores y saltos internos;
- rechazo de comillas sin cerrar;
- rechazo de encabezados duplicados después de normalización;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- fechas Excel convertidas a UTC;
- basename saneado;
- máximo 10 MB;
- HTTP 413 por exceso;
- SHA-256;
- idempotencia por archivo y modo;
- `ImportJob`;
- `ImportRow`;
- `DuplicateReview`;
- preview persistente;
- ejecución confirmada;
- cabecera HTTP explícita para ejecutar;
- procesamiento y recuperación por fila;
- orden determinístico por hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- coincidencias ambiguas persistidas también en preview;
- preview con exclusiones sin escritura de dominio;
- exclusiones importadas mediante el caso de uso manual, retroactivas y auditadas;
- fixture ficticia de 100 prospectos y 16 exclusiones.

### Frontend

- React 19;
- TypeScript estricto;
- Vite;
- credenciales solo en memoria;
- Basic Auth UTF-8;
- dashboard;
- prospectos paginados y ficha;
- importaciones y detalle por fila;
- cola de revisión ambigua;
- exclusiones;
- auditoría;
- diseño responsive;
- `excludedRows` visible como `Bloqueadas`;
- tipos Vite/CSS;
- credenciales no anulables después del guard de autenticación.

## Infraestructura local

### Docker Compose

Perfiles:

```text
app   -> postgres, backend, frontend
smoke -> comprobación E2E efímera
```

Puertos host configurables:

```text
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
```

Mapeos:

```text
127.0.0.1:${POSTGRES_HOST_PORT}:5432
127.0.0.1:${BACKEND_HOST_PORT}:8080
127.0.0.1:${FRONTEND_HOST_PORT}:8080
```

Red interna estable:

```text
backend -> postgres:5432
frontend -> backend:8080
smoke -> backend:8080 y frontend:8080
```

### Imágenes

- backend multi-stage Maven/JRE;
- frontend multi-stage Node/Nginx;
- Nginx sirve la SPA;
- Nginx actúa como proxy de `/api` y `/actuator`;
- contextos Docker excluyen `.env`, datos, claves, logs y cachés;
- servicios publicados solamente en loopback.

## Automatización implementada

### Preflight

Archivos:

```text
scripts/preflight.ps1
scripts/preflight.sh
```

Validan:

- Git;
- Docker instalado y daemon accesible;
- Docker Compose v2;
- Java/Node/npm en modo local;
- `.env`;
- tres puertos válidos y distintos;
- coherencia de `DATABASE_URL`;
- credenciales DB y bootstrap;
- cuatro guardas de envío;
- perfiles Compose `app` y `smoke`.

### Configuración segura de puertos

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
scripts/set-postgres-host-port.ps1
scripts/set-postgres-host-port.sh
```

Coordinan puertos y `DATABASE_URL` sin reemplazar contraseñas ni guardas.

### Smoke

```text
scripts/smoke-test.ps1
scripts/smoke-test.sh
servicio Compose smoke
```

Comprueban:

- backend health;
- API autenticada de prospectos;
- documento raíz frontend;
- sin creación de datos;
- sin comunicaciones.

### Validación Docker Windows

```text
scripts/validate-docker-stack.ps1
```

Ejecuta configuración, preflight, Compose config, cleanup no destructivo, builds, arranque, health, smoke y evidencia JSON/transcript.

### Backend verify sin Java local

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
```

Características:

- Maven 3.9.16 y Java 21 dentro de Docker;
- repositorio montado en solo lectura;
- `target` en volumen efímero;
- caché Maven persistente;
- socket Docker para Testcontainers;
- `TESTCONTAINERS_HOST_OVERRIDE`;
- cleanup del contenedor y volumen temporal.

### Lockfile seguro

```text
scripts/generate-frontend-lock.ps1
scripts/generate-frontend-lock.sh
```

Ejecutan:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No deben crear `node_modules` ni ejecutar lifecycle scripts.

En Unix, el contenedor utiliza el UID/GID del usuario para evitar que el lockfile quede propiedad de `root`.

### Validación integral Windows

```text
scripts/validate-seg001.ps1
```

### Validación integral Linux/macOS

```text
scripts/validate-seg001.sh
```

Ambos recorridos:

1. exigen rama `main`;
2. exigen árbol limpio, salvo lockfile no rastreado de una ejecución anterior;
3. configuran puertos;
4. ejecutan preflight;
5. construyen frontend/backend sin caché por defecto;
6. levantan el stack;
7. esperan health;
8. ejecutan smoke host y contenedor;
9. ejecutan Maven verify/Testcontainers en Docker;
10. generan `package-lock.json`;
11. calculan SHA-256;
12. reconstruyen frontend mediante `npm ci`;
13. repiten health y smoke;
14. ejecutan seguridad del repositorio;
15. permiten únicamente el cambio esperado del lockfile;
16. producen transcript y JSON;
17. no realizan commits.

### Seguridad del repositorio

```text
scripts/check-repository-safety.ps1
scripts/check-repository-safety.sh
```

Bloquean:

- `.env` rastreado;
- `validation-output/` rastreado;
- datos privados de importación/exportación;
- lote `gestudio_lote_*_prospectos.xlsx` en cualquier subdirectorio;
- claves y certificados;
- JSON de credenciales o service accounts;
- errores de `git diff --check`.

### Makefile

Targets:

```text
preflight
preflight-container
postgres-port
local-ports
repository-safety
db-up
db-down
app-up
app-down
app-logs
backend
backend-verify-container
frontend
frontend-lock
verify
verify-container
validate-seg001
smoke
smoke-container
reset-db
```

### npm ci

Dockerfile frontend, CI y Makefile seleccionan automáticamente:

```text
package-lock presente -> npm ci
package-lock ausente  -> npm install
```

La caché npm de CI sigue deshabilitada hasta versionar el lockfile.

## Evidencia local

Directorio ignorado:

```text
validation-output/
```

Formatos:

```text
seg001-docker-*.log
seg001-docker-*.json
seg001-complete-*.log
seg001-complete-*.json
```

Los transcripts deben revisarse antes de compartirse. La evidencia canónica es el resumen versionado en `docs/validation/`.

## CI implementado

Jobs:

1. backend:
   - Maven verify;
   - Spotless;
   - unit tests;
   - ArchUnit;
   - Testcontainers.
2. frontend:
   - npm ci o npm install;
   - typecheck;
   - build.
3. scripts:
   - sintaxis POSIX;
   - sintaxis Bash del validador integral;
   - parser PowerShell;
   - parseo de targets Make;
   - seguridad del repositorio;
   - preflight fail-closed.
4. compose-images-and-smoke:
   - entorno ficticio seguro;
   - Compose config;
   - imágenes;
   - stack;
   - smoke;
   - logs en fallo;
   - cleanup del volumen efímero de CI.

GitHub continúa sin mostrar estados o workflow runs visibles mediante el conector utilizado.

## Ejecución real acumulada

### Primer intento Windows/Docker

```text
preflight container-only: PASS
guardas: PASS
npm install frontend: PASS
TypeScript/build: FAIL
backend: CANCELED
stack: NOT_RUN
smoke: NOT_RUN
```

Errores reproducidos y corregidos:

- `Credentials | null` en `getProspect`;
- `Credentials | null` en `refresh`;
- import CSS sin declaración.

### Segunda reejecución

```text
frontend image: PASS_FROM_CACHE
backend image: PASS_FROM_CACHE
clean builds: NO DEMOSTRADOS
stack: FAIL por puerto host 5432
servicios: NOT_STARTED
Flyway/Hibernate/smoke: NOT_RUN
```

El puerto PostgreSQL y posteriormente los tres puertos quedaron configurables.

### Validación estática o funcional aislada

- estructura Compose;
- estructura CI;
- sintaxis de scripts POSIX disponibles;
- parseo Makefile;
- configurador Unix de tres puertos;
- preservación de contraseñas ficticias;
- preservación de UTF-8;
- preservación de guardas;
- backend verify Unix revisado;
- generador de lockfile seguro revisado;
- seguridad del repositorio revisada;
- validadores integrales revisados por código;
- paridad Windows/Unix documentada.

### Pendiente de ejecución

- parser real de todos los scripts PowerShell nuevos;
- `bash -n scripts/validate-seg001.sh` mediante checkout o CI;
- validador integral Windows;
- validador integral Unix;
- builds frontend/backend sin caché;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway;
- Hibernate validate;
- tres servicios healthy;
- smoke host;
- smoke contenedorizado;
- package-lock real;
- npm ci real;
- CI visible verde.

## Trabajo finalizado

- [x] producto backend/frontend;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones dominantes;
- [x] auditoría;
- [x] seguridad fail-closed;
- [x] hardening del parser/importación;
- [x] correcciones TypeScript;
- [x] stack Compose;
- [x] puertos configurables;
- [x] imágenes y health checks;
- [x] smoke host y contenedor;
- [x] preflight multiplataforma;
- [x] validador Docker Windows;
- [x] backend verify contenedorizado;
- [x] generación segura de lockfile;
- [x] preservación de propiedad del lockfile en Unix;
- [x] transición automática a npm ci;
- [x] validador integral Windows;
- [x] validador integral Unix;
- [x] evidencia JSON y transcript;
- [x] seguridad centralizada;
- [x] CI implementado;
- [x] Makefile actualizado;
- [x] documentación operativa y evidencias.

## Tareas pendientes bloqueantes

- [ ] actualizar checkout local al último `main`;
- [ ] restaurar la modificación accidental de `mvnw.cmd` si continúa presente;
- [ ] ejecutar uno de los validadores integrales;
- [ ] revisar JSON y transcript;
- [ ] corregir cualquier fallo real;
- [ ] repetir hasta `PASS`;
- [ ] revisar `frontend/package-lock.json`;
- [ ] versionar el lockfile;
- [ ] repetir validación desde árbol limpio con lockfile rastreado;
- [ ] demostrar `npm ci` desde el primer build;
- [ ] observar CI verde o registrar evidencia local equivalente;
- [ ] actualizar la matriz con salidas reales;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

## Deuda no bloqueante

- resolución auditada de `DuplicateReview`;
- retry explícito de `ImportJob`;
- filtros y exportación;
- accesibilidad;
- actor y retención de auditoría;
- evolución de la relación institución–prospecto uno a uno.

## Riesgos

1. pueden aparecer errores nuevos en builds limpios;
2. Testcontainers dentro de un contenedor depende del socket Docker y `host.docker.internal`;
3. montar el socket Docker concede privilegios elevados al contenedor Maven;
4. la caché Maven persiste localmente;
5. transcripts pueden contener logs técnicos y deben revisarse antes de compartir;
6. package-lock aún no fue generado;
7. CI no presenta runs visibles;
8. HTTP Basic es temporal;
9. Compose es solo local;
10. SEG-002 no debe comenzar con bloqueantes abiertos.

## Seguridad vigente

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Además:

- kill switch persistente;
- sin Gmail, SMTP o adaptador de envío;
- sin datos reales en Git, CI o imágenes;
- servicios solo en loopback;
- smoke de solo lectura;
- validadores sin commits automáticos.

## Próxima acción canónica

Leer `docs/next-step.md` y ejecutar:

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Linux/macOS:

```bash
bash scripts/validate-seg001.sh \
  --postgres-port 55432 \
  --backend-port 8080 \
  --frontend-port 5173 \
  --keep-running
```
