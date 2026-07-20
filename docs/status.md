# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica: `main`;
- todo el trabajo vigente está consolidado en `main`;
- consolidación original por fast-forward, sin force push;
- correcciones posteriores realizadas directamente en `main` por autorización expresa;
- rama histórica detrás y sin cambios exclusivos;
- sin pull request pendiente de consolidación;
- sin despliegue;
- sin envío habilitado.

Toda sesión debe comenzar con:

```bash
git switch main
git fetch origin
git pull --ff-only
```

## Segmentos

| Segmento | Estado | Condición |
|---|---|---|
| SEG-000 — repositorio y continuidad | COMPLETE | fuentes canónicas y reglas versionadas |
| SEG-001 — vertical slice persistente de prospectos | ACTIVE | producto completo, validación funcional incompleta |
| SEG-002 — identidad, usuarios y RBAC | PLANNED | bloqueado hasta matriz verde de SEG-001 |

Estado operativo de SEG-001:

```text
PENDING_COMPLETE_LOCAL_VALIDATION
CLEAN_BUILDS_PENDING
TESTCONTAINERS_PENDING
LOCKFILE_PENDING
CI_NOT_VISIBLE
```

## Alcance funcional implementado

### Backend

- Java 21;
- Spring Boot 4.1;
- Maven 3.9.16;
- Maven Wrapper validado mediante SHA-512;
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
- exclusiones;
- equivalencia teléfono/WhatsApp;
- normalización central;
- elegibilidad dominante;
- aplicación retroactiva de exclusiones;
- auditoría JSONB.

### Importaciones

- CSV UTF-8 con coma o punto y coma;
- comillas y saltos internos;
- rechazo de comillas sin cerrar;
- rechazo de headers duplicados después de normalización;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- encabezados normalizados;
- fechas Excel en UTC;
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
- confirmación HTTP explícita;
- procesamiento y recuperación por fila;
- orden determinístico por hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- coincidencias ambiguas persistidas también en preview;
- preview con exclusiones sin escritura de dominio;
- exclusiones importadas mediante caso de uso, retroactivas y auditadas;
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

### Stack Compose

Servicios:

```text
postgres
backend
frontend
smoke
```

Perfiles:

```text
app
smoke
```

Health chain:

```text
PostgreSQL -> backend -> frontend -> smoke
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
- Nginx sirve SPA y proxy `/api`/`actuator`;
- contextos Docker excluyen `.env`, datos, claves, logs y cachés;
- puertos publicados solamente en loopback.

## Automatización implementada

### Preflight

```text
scripts/preflight.ps1
scripts/preflight.sh
```

Valida:

- Git;
- Docker;
- Compose;
- Java/Node/npm en modo local;
- `.env`;
- tres puertos válidos y distintos;
- `DATABASE_URL` coordinada con PostgreSQL;
- credenciales DB y bootstrap;
- guardas de envío;
- perfiles Compose.

### Configuración de puertos

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
scripts/set-postgres-host-port.ps1
scripts/set-postgres-host-port.sh
```

Los helpers coordinan puertos y URL sin reemplazar contraseñas ni guardas.

### Smoke

```text
scripts/smoke-test.ps1
scripts/smoke-test.sh
servicio Compose smoke
```

Comprueban health, API autenticada y frontend sin crear datos.

### Docker stack

```text
scripts/validate-docker-stack.ps1
```

Ejecuta:

1. configuración de puertos;
2. preflight;
3. Compose config;
4. limpieza no destructiva;
5. builds limpios por defecto;
6. arranque;
7. espera de health;
8. smoke host;
9. smoke contenedorizado;
10. transcript y JSON estructurado;
11. cleanup opcional.

### Backend verify sin Java local

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
```

Características:

- imagen Maven/Java 21;
- repositorio montado en solo lectura;
- `target` en volumen efímero;
- caché Maven persistente;
- socket Docker para Testcontainers;
- `TESTCONTAINERS_HOST_OVERRIDE`;
- limpieza de contenedor y volumen target.

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

### Validador integral

```text
scripts/validate-seg001.ps1
```

Recorrido:

1. exige rama `main`;
2. exige archivos rastreados limpios;
3. ejecuta validación Docker completa;
4. ejecuta Maven verify/Testcontainers en Docker;
5. genera package-lock;
6. calcula SHA-256;
7. reconstruye frontend con npm ci;
8. recrea frontend;
9. repite smoke host y contenedor;
10. ejecuta seguridad del repositorio;
11. permite solamente el cambio esperado del lockfile;
12. produce transcript y JSON;
13. no realiza commits.

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
- JSON de credenciales o service account;
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

Los transcripts deben revisarse antes de compartirse. La evidencia canónica continúa siendo el resumen versionado en `docs/validation/`.

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
   - sintaxis shell;
   - parser PowerShell;
   - seguridad del repositorio;
   - preflight fail-closed.
4. compose-images-and-smoke:
   - entorno ficticio seguro;
   - Compose config;
   - imágenes;
   - stack;
   - smoke;
   - logs en fallo;
   - cleanup destructivo solo del volumen efímero de CI.

GitHub continúa sin mostrar estados o workflow runs visibles mediante el conector.

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

Errores reproducidos:

- `Credentials | null` en getProspect;
- `Credentials | null` en refresh;
- import CSS sin declaración.

Los tres fueron corregidos en `main`.

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

## Validaciones ejecutadas en automatización

### PASS real previo

- preflight PowerShell;
- guardas de envío;
- descarga de imágenes base;
- npm install;
- reproducción del fallo TypeScript;
- presencia de correcciones;
- reproducción del conflicto 5432.

### PASS estático o funcional aislado

- YAML Compose;
- YAML CI;
- sintaxis shell disponible;
- parseo Makefile;
- configurador Unix de tres puertos;
- preservación de contraseñas ficticias;
- preservación de UTF-8;
- preservación de guardas;
- sintaxis de backend verify Unix;
- sintaxis del generador de lockfile seguro;
- targets `backend-verify-container` y `verify-container`;
- read-back remoto de archivos críticos.

### Pendiente de ejecución

- parser PowerShell de scripts nuevos;
- `check-repository-safety.ps1`;
- `validate-docker-stack.ps1` actualizado;
- `verify-backend-container.ps1`;
- `validate-seg001.ps1`;
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
- [x] hardening parser/importación;
- [x] correcciones TypeScript;
- [x] stack Compose;
- [x] puertos configurables;
- [x] imágenes y health checks;
- [x] smoke host y contenedor;
- [x] preflight multiplataforma;
- [x] validador Docker;
- [x] backend verify contenedorizado;
- [x] generación segura de lockfile;
- [x] transición automática a npm ci;
- [x] validador integral;
- [x] evidencia JSON y transcript;
- [x] seguridad centralizada del repositorio;
- [x] CI implementado;
- [x] documentación operativa.

## Tareas pendientes bloqueantes

- [ ] actualizar checkout local al último main;
- [ ] restaurar modificación accidental de `mvnw.cmd`;
- [ ] ejecutar `scripts/validate-seg001.ps1`;
- [ ] revisar JSON y transcript;
- [ ] corregir cualquier fallo real;
- [ ] repetir hasta PASS;
- [ ] revisar `frontend/package-lock.json`;
- [ ] versionar lockfile;
- [ ] repetir validación sobre lockfile versionado;
- [ ] observar CI verde;
- [ ] registrar evidencia final;
- [ ] marcar SEG-001 COMPLETE;
- [ ] activar SEG-002.

## Deuda no bloqueante

- resolución auditada de `DuplicateReview`;
- retry explícito de `ImportJob`;
- filtros/exportación;
- accesibilidad;
- actor y retención de auditoría;
- evolución de institución–prospecto uno a uno.

## Riesgos

1. pueden aparecer errores nuevos en builds limpios;
2. Testcontainers dentro de un contenedor depende del socket Docker y `host.docker.internal`;
3. montar Docker socket concede privilegios elevados al contenedor Maven;
4. Maven cache persiste localmente;
5. transcripts pueden incluir logs y deben revisarse antes de compartir;
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
- sin Gmail/SMTP/adaptador de envío;
- sin datos reales en Git/CI/imágenes;
- sin producción;
- smoke de solo lectura.

## Próxima acción canónica

Consultar `docs/next-step.md` y ejecutar el validador integral desde un `main` limpio.
