# Estado actual

Actualizado: 2026-07-21

## Repositorio y consolidación

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
```

- todo el trabajo vigente está consolidado en `main`;
- la rama histórica `feat/seg-001-prospect-vertical-slice` está detrás y no contiene cambios exclusivos;
- no existe PR pendiente para esta consolidación;
- las correcciones autorizadas se aplican directamente en `main`;
- no se desplegó ningún ambiente;
- no se habilitó ningún envío;
- el XLSX real permanece fuera de Git, CI e imágenes.

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
| SEG-001 — vertical slice persistente de prospectos | ACTIVE | implementación completa; validación integral todavía bloqueada |
| SEG-002 — identidad, usuarios y RBAC | PLANNED | bloqueado hasta cierre verde de SEG-001 |

Estado operativo de SEG-001:

```text
IMPLEMENTATION_COMPLETE
HARDENING_COMPLETE
MAIN_CONSOLIDATED
CROSS_PLATFORM_VALIDATION_IMPLEMENTED
POWERSHELL_SYNTAX_PASS
PREFLIGHT_PASS
FRONTEND_CLEAN_BUILD_PASS
BACKEND_CLEAN_IMAGE_BUILD_PASS
LATEST_REAL_RUN=FAIL_DOCKER_HOST_PORT_ALREADY_ALLOCATED
DOCKER_PORT_OWNER_HARDENING_COMMITTED
STACK_HEALTH_SMOKE_PENDING
MAVEN_VERIFY_TESTCONTAINERS_PENDING
LOCKFILE_NPM_CI_PENDING
CI_NOT_VISIBLE
```

SEG-001 no se marca `COMPLETE` por la existencia de automatización o builds parciales. Requiere evidencia ejecutada verde del recorrido completo.

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

Puertos host configurables:

```text
POSTGRES_HOST_PORT
BACKEND_HOST_PORT
FRONTEND_HOST_PORT
```

Mapeos declarados:

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
- servicios publicados solo en loopback en la definición Compose;
- health checks encadenados;
- PostgreSQL ahora se inicia y valida antes de los builds de aplicación.

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
scripts/check-host-ports.ps1
```

Los configuradores preservan contraseñas y `SENDING_*`.

El checker Windows actualizado:

1. inspecciona publicaciones de contenedores activos mediante `docker ps`;
2. informa ID, nombre y puertos del propietario;
3. prueba cada puerto mediante `TcpListener` sobre `127.0.0.1`;
4. falla antes de builds;
5. distingue ocupación Docker de reserva/ocupación Windows.

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
scripts/validate-docker-stack.ps1
```

Linux/macOS:

```text
scripts/validate-seg001.sh
```

El orden Windows endurecido es:

1. rama y working tree;
2. configuración segura de puertos;
3. preflight;
4. Compose config;
5. cleanup sin `-v`;
6. propiedad Docker y enlace Windows de puertos;
7. arranque y health real de PostgreSQL;
8. builds frontend/backend;
9. arranque y health backend/frontend;
10. smoke;
11. Maven verify/Testcontainers;
12. lockfile;
13. npm ci;
14. smoke final;
15. seguridad;
16. evidencia.

### Sintaxis PowerShell

```text
scripts/check-powershell-syntax.ps1
```

Parsea todos los scripts `.ps1`. La última ejecución real aprobó 11 scripts antes del hardening más reciente; la nueva versión requiere reejecución tras pull.

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

### Intento 1 — frontend

```text
preflight: PASS
guardas: PASS
npm install: PASS
frontend TypeScript/build: FAIL
backend: CANCELED
stack: NOT_RUN
smoke: NOT_RUN
```

Errores reproducidos y corregidos:

- `Credentials | null` en `getProspect`;
- `Credentials | null` en `refresh`;
- import CSS sin declaración.

### Intento 2 — Compose con puerto 5432

```text
frontend image: PASS_FROM_CACHE
backend image: PASS_FROM_CACHE
clean builds: NO DEMOSTRADOS
stack: FAIL por puerto host 5432
servicios: NOT_STARTED
Flyway/Hibernate/smoke: NOT_RUN
```

Resultado: tres puertos configurables y coordinados.

### Intento 3 — parser PowerShell

```text
InvalidVariableReferenceWithDrive
$LASTEXITCODE:
```

Estado:

```text
EXECUTED_FAIL — POWERSHELL_PARSE_ERROR
```

Resultado: tres scripts corregidos, checker local, regresión CI y normalización de `mvnw.cmd`.

### Intento 4 — puerto Windows reservado

Sobre un commit posterior:

```text
PowerShell syntax: PASS
preflight: PASS
frontend clean build: PASS
backend clean image build: PASS
stack: FAIL al enlazar 127.0.0.1:55432
```

Windows informó que `55432` pertenecía a un rango excluido.

Resultado: `check-host-ports.ps1` con `TcpListener` y diagnóstico de rangos excluidos.

### Intento 5 — puerto 15432 publicado por Docker

Commit ejecutado:

```text
f903a9e1278697af53e0bcbee3bd10b16e10b991
```

Aprobó:

```text
checkout main y working tree limpio
PowerShell syntax, 11 scripts
preflight container-only
Docker daemon
Compose config
guardas de envío
TcpListener Windows para 15432, 8080 y 5173
frontend clean build --no-cache
TypeScript strict
Vite production build
backend clean image build --no-cache
Maven package con tests omitidos
```

Primer fallo real:

```text
Bind for 0.0.0.0:15432 failed: port is already allocated
```

Estado:

```text
EXECUTED_FAIL — DOCKER_HOST_PORT_ALREADY_ALLOCATED
```

No se alcanzaron health, migraciones, smoke, Maven verify, Testcontainers, lockfile o npm ci.

Evidencia:

```text
docs/validation/SEG-001-docker-port-owner-failure-2026-07-21.md
```

### Correcciones posteriores al intento 5

- `check-host-ports.ps1` detecta publicaciones Docker activas;
- muestra contenedor propietario;
- el stack inicia PostgreSQL antes de builds;
- el fallo de publicación ocurre antes de reconstruir imágenes;
- ambos validadores imprimen `docker ps` en fallos;
- `stackKeptRunning` refleja estado real en ambos niveles;
- `docs/next-step.md` usa esta evidencia como próxima acción canónica.

Estas correcciones todavía requieren ejecución real en Windows.

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
- comparación de ramas;
- read-back del hardening de propiedad Docker y orden de arranque.

No se afirma que el hardening nuevo haya pasado PowerShell o Docker hasta la próxima ejecución real.

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
- [x] checker de propiedad Docker/Windows;
- [x] PostgreSQL antes de builds;
- [x] mensajes `KeepRunning` veraces;
- [x] documentación operativa y evidencias fechadas.

## Tareas pendientes bloqueantes

- [ ] actualizar checkout al último `main`;
- [ ] confirmar árbol limpio;
- [ ] ejecutar checker PowerShell tras el nuevo hardening;
- [ ] identificar o detener el contenedor que publica `15432`, o elegir otro puerto;
- [ ] ejecutar checker actualizado Windows/Docker;
- [ ] obtener publicación y health de PostgreSQL;
- [ ] obtener stack completo healthy;
- [ ] obtener Flyway/Hibernate verdes;
- [ ] obtener smoke host y contenedor verdes;
- [ ] obtener Maven verify/Spotless/tests/ArchUnit/Testcontainers verdes;
- [ ] generar y revisar `package-lock.json`;
- [ ] versionar lockfile;
- [ ] repetir desde árbol limpio con npm ci desde el inicio;
- [ ] obtener seguridad final verde;
- [ ] obtener CI visible verde o excepción documentada;
- [ ] actualizar matriz con evidencia final;
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

1. otro contenedor puede volver a publicar un puerto elegido;
2. Docker Desktop puede mantener asignaciones no visibles para `TcpListener`;
3. Testcontainers depende del socket Docker y `host.docker.internal`;
4. el socket Docker concede privilegios elevados;
5. la caché Maven persiste localmente;
6. `package-lock.json` todavía no está versionado;
7. CI no muestra runs visibles;
8. HTTP Basic es temporal;
9. Compose es local, no productivo.

## Próximo paso

Fuente canónica:

```text
docs/next-step.md
```

Resumen:

1. actualizar `main`;
2. inspeccionar `docker ps` y el propietario de `15432`;
3. detener solo el contenedor conflictivo o elegir un puerto alternativo;
4. ejecutar sintaxis y checker actualizado;
5. repetir `scripts/validate-seg001.ps1` sin caché;
6. corregir el siguiente fallo real sin desactivar controles.

## Seguridad vigente

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No desplegar, no habilitar envíos, no incorporar el XLSX real y no iniciar SEG-002 mientras existan bloqueantes.
