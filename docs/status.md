# Estado actual

Actualizado: 2026-07-21

## Repositorio y consolidación

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
```

- todo el trabajo vigente está consolidado en `main`;
- la consolidación original fue fast-forward y sin force push;
- las correcciones posteriores se realizan directamente en `main` por autorización expresa;
- `feat/seg-001-prospect-vertical-slice` está detrás y no contiene cambios exclusivos;
- no existe PR pendiente para esta consolidación;
- no se desplegó ningún ambiente;
- no se habilitó ningún envío.

Toda sesión comienza con:

```bash
git switch main
git fetch origin
git pull --ff-only
```

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
LATEST_REAL_RUN=FAIL_POWERSHELL_PARSE
PARSER_FIX_COMMITTED
FUNCTIONAL_VALIDATION_PENDING
LOCKFILE_PENDING
CI_NOT_VISIBLE
```

SEG-001 no se marca `COMPLETE` por la existencia de automatización. Requiere evidencia ejecutada verde.

## Alcance funcional implementado

### Backend y plataforma

- Java 21;
- Spring Boot;
- Maven 3.9.16 y Wrapper con SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate `validate`;
- Actuator y Prometheus;
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
- exclusiones dominantes y retroactivas;
- equivalencia teléfono/WhatsApp;
- normalización central;
- elegibilidad;
- auditoría JSONB.

### Importaciones

- CSV UTF-8 con coma o punto y coma;
- comillas, delimitadores y saltos internos;
- rechazo de estructura inválida;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- fechas Excel en UTC;
- basename saneado;
- máximo 10 MB y HTTP 413;
- SHA-256;
- idempotencia;
- `ImportJob`, `ImportRow` y `DuplicateReview`;
- preview persistente;
- ejecución confirmada;
- recuperación por fila;
- orden hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- coincidencias ambiguas persistidas;
- exclusiones importadas retroactivas y auditadas;
- fixture ficticia 100/16.

### Frontend

- React 19;
- TypeScript estricto;
- Vite;
- Basic Auth UTF-8;
- credenciales solo en memoria;
- dashboard;
- prospectos paginados y ficha;
- importaciones y detalle por fila;
- cola de revisión;
- exclusiones;
- auditoría;
- diseño responsive;
- `excludedRows` visible como `Bloqueadas`;
- tipos Vite/CSS;
- credenciales no anulables después del guard.

## Infraestructura local implementada

### Docker Compose

Perfiles:

```text
app   -> postgres, backend, frontend
smoke -> comprobación E2E efímera
```

Puertos host:

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

Red interna:

```text
backend -> postgres:5432
frontend -> backend:8080
smoke -> backend:8080 y frontend:8080
```

### Imágenes y health

- backend multi-stage Maven/JRE;
- frontend multi-stage Node/Nginx;
- proxy `/api` y `/actuator`;
- contextos sin secretos o datos reales;
- servicios publicados solo en loopback;
- health checks encadenados.

## Automatización implementada

### Preflight

```text
scripts/preflight.ps1
scripts/preflight.sh
```

Validan Git, Docker, daemon, Compose, `.env`, puertos, URL DB, credenciales y guardas.

### Puertos

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
scripts/set-postgres-host-port.ps1
scripts/set-postgres-host-port.sh
```

Preservan contraseñas y `SENDING_*`.

### Smoke

```text
scripts/smoke-test.ps1
scripts/smoke-test.sh
servicio Compose smoke
```

Comprueban health, API autenticada y frontend sin crear datos.

### Validación integral

Windows:

```text
scripts/validate-seg001.ps1
```

Linux/macOS:

```text
scripts/validate-seg001.sh
```

Ambos recorren builds limpios, stack, health, smoke, Maven verify, Testcontainers, lockfile, npm ci, seguridad y evidencia.

### Sintaxis PowerShell

```text
scripts/check-powershell-syntax.ps1
```

Parsea todos los scripts `.ps1` y reporta archivo, línea, columna y mensaje.

CI además rechaza expresamente:

```text
$LASTEXITCODE:
```

### Backend verify sin Java local

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
```

Usan Maven/Java 21 en Docker, código read-only, target efímero, caché Maven y socket Docker para Testcontainers.

### Lockfile seguro

```text
scripts/generate-frontend-lock.ps1
scripts/generate-frontend-lock.sh
```

Ejecutan:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No deben crear `node_modules`. Unix preserva UID/GID.

### Seguridad del repositorio

```text
scripts/check-repository-safety.ps1
scripts/check-repository-safety.sh
```

Bloquean `.env`, evidencia local, datos privados, lote operativo, claves, certificados, JSON de credenciales y errores de whitespace rastreados.

## CI implementado

Jobs:

1. backend Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
2. frontend npm install/ci, typecheck y build;
3. scripts POSIX, Bash, PowerShell, Make, seguridad y preflight;
4. Compose, imágenes, stack y smoke.

GitHub todavía no muestra estados o workflow runs visibles mediante el conector utilizado.

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

Los tres puertos quedaron configurables.

### Tercer intento — 2026-07-21

El checkout se actualizó correctamente por fast-forward hasta el `main` disponible en ese momento.

El validador integral se ejecutó dos veces y falló durante el parseo:

```text
InvalidVariableReferenceWithDrive
scripts/validate-seg001.ps1:21
$LASTEXITCODE:
```

Estado:

```text
EXECUTED_FAIL — POWERSHELL_PARSE_ERROR
```

El fallo ocurrió antes de `Start-Transcript`, preflight y Docker.

Por lo tanto:

```text
preflight: NOT_RUN
Compose: NOT_RUN
builds: NOT_RUN
stack: NOT_RUN
health: NOT_RUN
smoke: NOT_RUN
Maven/Testcontainers: NOT_RUN
lockfile/npm ci: NOT_RUN
```

Correcciones:

- formato explícito en `validate-seg001.ps1`;
- mismo arreglo en `validate-docker-stack.ps1`;
- mismo arreglo en `verify-backend-container.ps1`;
- checker local de sintaxis PowerShell;
- regresión CI específica;
- renormalización de `mvnw.cmd`.

Evidencia:

```text
docs/validation/SEG-001-powershell-parser-failure-2026-07-21.md
```

## Validación estática o aislada acumulada

- estructura Compose;
- estructura CI;
- sintaxis POSIX disponible;
- parseo Makefile;
- configurador Unix de tres puertos;
- preservación de secretos ficticios, UTF-8 y guardas;
- revisión de backend verify;
- revisión de generador de lockfile;
- revisión de seguridad;
- revisión de validadores Windows/Unix;
- comparación de ramas.

La corrección PowerShell nueva todavía debe ejecutarse en el checkout Windows.

## Trabajo finalizado

- [x] producto backend/frontend;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones dominantes;
- [x] auditoría;
- [x] seguridad fail-closed;
- [x] hardening de importación;
- [x] correcciones TypeScript;
- [x] stack Compose;
- [x] puertos configurables;
- [x] imágenes y health checks;
- [x] smoke host/contenedor;
- [x] preflight multiplataforma;
- [x] backend verify contenedorizado;
- [x] lockfile seguro;
- [x] transición automática a npm ci;
- [x] validadores integrales Windows/Unix;
- [x] evidencia JSON/transcript;
- [x] seguridad centralizada;
- [x] checker PowerShell local;
- [x] regresión del parser en CI;
- [x] normalización de `mvnw.cmd`;
- [x] documentación operativa.

## Tareas pendientes bloqueantes

- [ ] actualizar checkout al último `main`;
- [ ] confirmar `mvnw.cmd` limpio;
- [ ] ejecutar checker PowerShell;
- [ ] ejecutar preflight actualizado;
- [ ] ejecutar validador integral;
- [ ] corregir el siguiente fallo real, si existe;
- [ ] obtener builds limpios;
- [ ] obtener stack healthy;
- [ ] obtener Maven/tests/Testcontainers verdes;
- [ ] generar y revisar `package-lock.json`;
- [ ] versionar lockfile;
- [ ] repetir desde árbol limpio con npm ci desde el inicio;
- [ ] obtener CI visible verde o excepción documentada;
- [ ] actualizar matriz con evidencia;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

## Deuda no bloqueante

- resolución auditada de `DuplicateReview`;
- retry explícito de `ImportJob`;
- filtros y exportación;
- accesibilidad;
- actor y retención de auditoría;
- evolución de institución–prospecto.

## Riesgos

1. pueden aparecer nuevos errores en builds limpios;
2. Testcontainers depende del socket Docker y `host.docker.internal`;
3. el socket Docker concede privilegios elevados;
4. la caché Maven persiste localmente;
5. transcripts deben revisarse antes de compartir;
6. package-lock aún no fue generado;
7. CI no presenta runs visibles;
8. HTTP Basic es temporal;
9. Compose es solo local;
10. SEG-002 no debe comenzar con bloqueantes.

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
- el tercer intento no llegó a ejecutar Docker ni modificar datos.
