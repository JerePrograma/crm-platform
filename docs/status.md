# Estado actual

Actualizado: 2026-07-21

## Repositorio y consolidación

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
```

- todo el trabajo vigente está consolidado en `main`;
- la rama `feat/seg-001-prospect-vertical-slice` está detrás y no contiene cambios exclusivos;
- no existe PR pendiente de consolidación;
- no se desplegó ningún ambiente;
- no se habilitó ningún envío;
- no se incorporaron datos reales a Git, CI o imágenes.

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
| SEG-001 — vertical slice persistente de prospectos | ACTIVE | implementación completa; validación funcional parcial |
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
LATEST_REAL_RUN=FAIL_WINDOWS_HOST_PORT_BIND
STACK_HEALTH_SMOKE_PENDING
MAVEN_VERIFY_TESTCONTAINERS_PENDING
LOCKFILE_NPM_CI_PENDING
CI_NOT_VISIBLE
```

SEG-001 no se marca `COMPLETE` hasta que toda la matriz bloqueante tenga evidencia ejecutada verde.

## Alcance funcional implementado

### Backend y plataforma

- Java 21 y Spring Boot;
- Maven 3.9.16 y Wrapper con SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate `validate`;
- Actuator, Prometheus y logging estructurado;
- OpenAPI y RFC 7807;
- autenticación bootstrap fail-closed;
- API de negocio cerrada cuando faltan credenciales.

### Dominio

- instituciones;
- contactos;
- canales de contacto;
- prospectos y estados comerciales;
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
- fechas Excel convertidas a UTC;
- nombre de archivo saneado;
- máximo 10 MB y HTTP 413;
- SHA-256 e idempotencia;
- `ImportJob`, `ImportRow` y `DuplicateReview`;
- preview persistente;
- ejecución confirmada mediante cabecera explícita;
- recuperación por fila;
- orden determinístico hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- coincidencias ambiguas persistidas;
- exclusiones importadas retroactivas y auditadas;
- fixture ficticia 100 prospectos/16 exclusiones.

### Frontend

- React 19, TypeScript estricto y Vite;
- Basic Auth UTF-8;
- credenciales solo en memoria;
- dashboard;
- prospectos paginados y ficha;
- importaciones y detalle por fila;
- cola de revisión ambigua;
- exclusiones;
- auditoría;
- diseño responsive;
- `excludedRows` visible como `Bloqueadas`;
- tipos Vite/CSS;
- credenciales no anulables después del guard.

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

Los servicios se publican solo en loopback. La red interna permanece estable:

```text
backend -> postgres:5432
frontend -> backend:8080
smoke -> backend:8080 y frontend:8080
```

### Imágenes

- backend multi-stage Maven/JRE;
- frontend multi-stage Node/Nginx;
- Nginx sirve la SPA y actúa como proxy de `/api` y `/actuator`;
- contextos Docker excluyen `.env`, datos, claves, logs y cachés;
- health checks encadenados.

## Automatización implementada

### Preflight

```text
scripts/preflight.ps1
scripts/preflight.sh
```

Validan Git, Docker, daemon, Compose, `.env`, puertos, `DATABASE_URL`, credenciales y cuatro guardas de envío.

### Configuración de puertos

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
scripts/set-postgres-host-port.ps1
scripts/set-postgres-host-port.sh
```

Preservan contraseñas, base de datos y `SENDING_*`.

### Enlace real de puertos Windows

```text
scripts/check-host-ports.ps1
```

Intenta enlazar PostgreSQL, backend y frontend sobre `127.0.0.1` mediante `TcpListener`. Detecta puertos ocupados o reservados antes de builds largos.

### Sintaxis PowerShell

```text
scripts/check-powershell-syntax.ps1
```

Parsea todos los `.ps1`. CI además bloquea la interpolación ambigua `$LASTEXITCODE:`.

### Smoke

```text
scripts/smoke-test.ps1
scripts/smoke-test.sh
servicio Compose smoke
```

Comprueban backend health, API autenticada y documento frontend sin crear datos ni enviar comunicaciones.

### Validadores integrales

```text
Windows: scripts/validate-seg001.ps1
Unix:    scripts/validate-seg001.sh
```

Recorren builds limpios, stack, health, smoke, Maven verify, Testcontainers, lockfile, npm ci, seguridad y evidencia.

### Backend verify sin Java local

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
```

Usan Maven/Java 21 dentro de Docker, código read-only, target efímero, caché Maven y socket Docker para Testcontainers.

### Lockfile seguro

```text
scripts/generate-frontend-lock.ps1
scripts/generate-frontend-lock.sh
```

Ejecutan:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No deben crear `node_modules`; Unix preserva UID/GID.

### Seguridad del repositorio

```text
scripts/check-repository-safety.ps1
scripts/check-repository-safety.sh
```

Bloquean `.env`, evidencia local, datos privados, lote operativo, claves, certificados, JSON de credenciales y errores de whitespace rastreados.

## CI implementado

Jobs:

1. backend: Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
2. frontend: npm install/ci, typecheck y build;
3. scripts: POSIX, Bash, PowerShell, checker de puertos, Make, seguridad y preflight;
4. Compose: imágenes, stack y smoke.

GitHub continúa sin mostrar estados o workflow runs visibles mediante el conector utilizado.

## Ejecución real acumulada

### Primer intento

```text
preflight: PASS
guardas: PASS
npm install frontend: PASS
TypeScript/build: FAIL
backend: CANCELED
stack/smoke: NOT_RUN
```

Se corrigieron dos usos de `Credentials | null` y el import CSS sin declaración.

### Segunda ejecución

```text
frontend image: PASS_FROM_CACHE
backend image: PASS_FROM_CACHE
stack: FAIL por puerto 5432
servicios/Flyway/Hibernate/smoke: NOT_RUN
```

Después se hicieron configurables los tres puertos.

### Tercer intento

```text
EXECUTED_FAIL — POWERSHELL_PARSE_ERROR
```

El patrón `$LASTEXITCODE:` impidió parsear el orquestador antes de Docker. Se corrigió en tres scripts, se añadió checker de sintaxis y regresión CI.

Evidencia:

```text
docs/validation/SEG-001-powershell-parser-failure-2026-07-21.md
```

### Cuarto intento — evidencia más reciente

Checkout ejecutado:

```text
main
65b64000a7e8f6abd71f2b118cebe904ee61f1d1
working tree: limpio
Docker 29.3.1
```

Resultados:

```text
PowerShell syntax: PASS — 10 scripts
preflight container-only: PASS
Compose config: PASS
frontend clean build: PASS
backend clean image build: PASS
stack: FAIL
causa: Windows no pudo enlazar 127.0.0.1:55432
working tree final: limpio
```

El frontend ejecutó TypeScript y Vite correctamente. El backend completó su build de imagen sin caché. Esto no sustituye `mvn verify`.

No se ejecutaron:

```text
PostgreSQL/backend/frontend health
Flyway
Hibernate validate
smoke host/container
Maven verify
Spotless
unit tests
ArchUnit
Testcontainers
package-lock
npm ci
seguridad final
```

Evidencia:

```text
docs/validation/SEG-001-port-bind-failure-2026-07-21.md
validation-output/seg001-complete-20260721-102334.log
validation-output/seg001-complete-20260721-102334.json
validation-output/seg001-docker-20260721-102335.json
```

## Correcciones posteriores al cuarto intento

- añadido `scripts/check-host-ports.ps1`;
- comprobación de enlace después del cleanup y antes de builds;
- `hostPorts=PASS` agregado a la evidencia Docker;
- mensaje `-KeepRunning` corregido cuando no hay stack activo;
- CI ejecuta el checker con puertos alternativos;
- siguiente paso actualizado para probar PostgreSQL en `15432`.

## Trabajo finalizado

- [x] producto backend/frontend;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones dominantes;
- [x] auditoría;
- [x] seguridad fail-closed;
- [x] hardening de importación;
- [x] correcciones TypeScript;
- [x] Compose e imágenes;
- [x] puertos configurables;
- [x] preflight multiplataforma;
- [x] smoke host/contenedor implementado;
- [x] backend verify contenedorizado;
- [x] lockfile seguro;
- [x] transición automática a npm ci;
- [x] validadores integrales Windows/Unix;
- [x] evidencia JSON/transcript;
- [x] seguridad centralizada;
- [x] checker de sintaxis PowerShell;
- [x] checker de enlace de puertos Windows;
- [x] frontend clean build ejecutado;
- [x] backend clean image build ejecutado;
- [x] documentación operativa y evidencias.

## Tareas pendientes bloqueantes

- [ ] actualizar checkout al último `main`;
- [ ] ejecutar checker de sintaxis;
- [ ] diagnosticar `55432` sin alterar rangos excluidos;
- [ ] confirmar puertos `15432`, `8080`, `5173` mediante `check-host-ports.ps1`;
- [ ] repetir validador integral con esos puertos;
- [ ] obtener los tres servicios healthy;
- [ ] confirmar Flyway y Hibernate;
- [ ] confirmar smoke host y contenedor;
- [ ] ejecutar Maven verify;
- [ ] confirmar Spotless, unit tests, ArchUnit y Testcontainers;
- [ ] generar y revisar `frontend/package-lock.json`;
- [ ] versionar el lockfile;
- [ ] repetir desde árbol limpio con npm ci desde el primer build;
- [ ] confirmar seguridad del repositorio;
- [ ] obtener CI visible verde o documentar excepción;
- [ ] actualizar matriz con evidencia final;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

## Deuda no bloqueante

- resolución auditada de `DuplicateReview` desde UI;
- retry explícito de `ImportJob`;
- filtros y exportación;
- accesibilidad;
- actor y retención de auditoría;
- evolución de la relación institución–prospecto.

## Riesgos

1. el siguiente arranque puede revelar fallos de Flyway, Hibernate o configuración;
2. Maven verify puede revelar errores no cubiertos por el package del Dockerfile;
3. Testcontainers depende del socket Docker y `host.docker.internal`;
4. montar el socket Docker concede privilegios elevados al contenedor Maven;
5. la caché Maven persiste localmente;
6. transcripts pueden contener logs técnicos y deben revisarse antes de compartir;
7. `package-lock.json` aún no está versionado;
8. npm ci todavía no tiene evidencia funcional;
9. CI no muestra runs visibles;
10. HTTP Basic es temporal;
11. Compose es exclusivamente local;
12. SEG-002 no debe comenzar con bloqueantes abiertos.

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
- servicios publicados solo en loopback;
- validaciones sin comunicaciones.

## Próximo paso

La única acción autorizada está en:

```text
docs/next-step.md
```

Debe probar primero el enlace de `15432`, `8080` y `5173`, y solo después repetir la validación integral sin caché.
