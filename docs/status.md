# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica: `main`;
- todo SEG-001 está consolidado en `main`;
- la consolidación original fue fast-forward, sin force push;
- las correcciones posteriores se realizaron directamente en `main` por autorización expresa;
- la rama histórica está detrás y no contiene cambios exclusivos;
- no existe pull request abierto para esta consolidación;
- no se desplegó ningún ambiente;
- no se habilitó ningún envío.

Toda sesión debe comenzar desde `main` actualizado mediante `git pull --ff-only`.

## Segmentos

| Segmento | Estado | Condición |
|---|---|---|
| SEG-000 — repositorio y continuidad | COMPLETE | fuente canónica y reglas versionadas |
| SEG-001 — vertical slice de prospectos | ACTIVE | implementación completa, validación ejecutada parcialmente |
| SEG-002 — identidad, usuarios y RBAC | PLANNED | bloqueado hasta cierre verde de SEG-001 |

SEG-001 no puede marcarse `COMPLETE` hasta obtener builds limpios, migraciones, pruebas, stack saludable, smoke y lockfile reproducible.

## Alcance implementado

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper 3.9.16 verificado mediante SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate `validate`;
- instituciones, contactos, canales y prospectos;
- estados comerciales;
- exclusiones dominantes y retroactivas;
- equivalencia teléfono/WhatsApp;
- normalización y validación central;
- API paginada;
- OpenAPI;
- RFC 7807;
- auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, Prometheus y logging estructurado.

### Importación

- CSV separado por coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- encabezados normalizados;
- soporte de comillas, delimitadores y saltos internos;
- rechazo de CSV o headers inválidos;
- fechas Excel en UTC;
- SHA-256 e idempotencia;
- basename seguro;
- máximo 10 MB;
- respuesta HTTP 413 por exceso;
- `ImportJob`, `ImportRow` y `DuplicateReview`;
- preview persistente;
- ejecución confirmada;
- transacción y recuperación por fila;
- orden determinístico por hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- coincidencias ambiguas persistidas en preview;
- preview con exclusiones sin escritura de dominio;
- exclusiones importadas retroactivas y auditadas;
- fixture ficticia de 100 prospectos y 16 exclusiones.

### Frontend

- React, TypeScript y Vite;
- credenciales solo en memoria;
- Basic Auth UTF-8;
- dashboard;
- prospectos y ficha;
- importaciones y detalle por fila;
- revisiones ambiguas;
- exclusiones;
- auditoría;
- diseño responsive;
- `excludedRows` visible como `Bloqueadas`;
- `vite-env.d.ts` para tipos Vite e imports CSS;
- referencia no anulable `activeCredentials` después del guard de autenticación;
- TypeScript `strict` conservado.

## Infraestructura local

### Procesos separados

- PostgreSQL mediante Docker Compose;
- backend mediante Maven Wrapper;
- frontend mediante Vite;
- documentación: `docs/local-development-and-usage.md`.

### Stack contenedorizado

- perfil `app`: PostgreSQL, backend y frontend;
- perfil `smoke`: validación E2E efímera;
- puertos publicados solo en `127.0.0.1`;
- health chain PostgreSQL → backend → frontend → smoke;
- imagen backend multi-stage;
- imagen frontend multi-stage con Nginx;
- proxy Nginx para `/api` y `/actuator`;
- smoke interno de solo lectura.

### Puertos host configurables

Variables canónicas:

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

La red interna permanece estable:

```text
backend -> postgres:5432
frontend -> backend:8080
smoke -> backend:8080 y frontend:8080
```

`DATABASE_URL` del host debe usar `POSTGRES_HOST_PORT`. Compose reemplaza internamente esa URL por `postgres:5432` para el backend contenedorizado.

## Automatización implementada

### Preflight

Unix y PowerShell:

- modos local y container-only;
- validación de Git, Docker y Compose;
- validación opcional de Java, Node y npm;
- `.env` obligatorio;
- tres puertos obligatorios, válidos y distintos;
- coherencia de `DATABASE_URL`;
- credenciales DB y bootstrap obligatorias;
- cuatro guardas de envío cerradas;
- parseo de perfiles Compose `app` y `smoke`;
- sin impresión de contraseñas.

### Configuración segura de puertos

Archivos:

```text
scripts/set-local-host-ports.ps1
scripts/set-local-host-ports.sh
```

Actualizan:

- `POSTGRES_HOST_PORT`;
- `BACKEND_HOST_PORT`;
- `FRONTEND_HOST_PORT`;
- `DATABASE_URL`.

Conservan:

- nombre de base;
- usuarios;
- contraseñas;
- guardas `SENDING_*`;
- resto de `.env`.

PowerShell escribe UTF-8 sin BOM. Los helpers históricos `set-postgres-host-port.*` funcionan como wrappers compatibles.

### Smoke tests

- Unix y PowerShell contra stack activo;
- URLs derivadas de `BACKEND_HOST_PORT` y `FRONTEND_HOST_PORT`;
- overrides opcionales `BACKEND_URL` y `FRONTEND_URL`;
- health público;
- API autenticada de prospectos;
- documento raíz frontend;
- sin creación de datos ni comunicaciones.

### Orquestador Docker Windows

Archivo:

```text
scripts/validate-docker-stack.ps1
```

Ejecuta:

1. configuración coordinada de puertos;
2. preflight container-only;
3. cleanup sin eliminar volumen;
4. build frontend sin caché por defecto;
5. build backend sin caché por defecto;
6. arranque del perfil `app`;
7. espera de health para tres servicios;
8. smoke PowerShell;
9. smoke contenedorizado;
10. transcript en `validation-output/`;
11. cleanup final salvo `-KeepRunning`.

No ejecuta Maven verify, Testcontainers ni genera el lockfile; esos controles permanecen separados.

### Lockfile npm

Archivos:

```text
scripts/generate-frontend-lock.ps1
scripts/generate-frontend-lock.sh
```

Dockerfile frontend, Makefile y CI seleccionan automáticamente:

```text
package-lock.json presente -> npm ci
package-lock.json ausente  -> npm install
```

La caché npm de CI permanece desactivada hasta versionar el lockfile.

### Makefile

Targets relevantes:

```text
preflight
preflight-container
postgres-port
local-ports
db-up
db-down
app-up
app-down
app-logs
backend
frontend
frontend-lock
verify
smoke
smoke-container
reset-db
```

### Evidencia local

`validation-output/` está ignorado por Git. Puede contener transcripts de validación, pero no debe convertirse en fuente canónica; los resultados relevantes deben resumirse en `docs/validation/`.

## CI implementado

Jobs:

1. backend:
   - Maven verify;
   - Spotless;
   - unit tests;
   - ArchUnit;
   - Testcontainers.
2. frontend:
   - instalación condicional npm ci/npm install;
   - typecheck;
   - build.
3. scripts:
   - sintaxis Unix;
   - parser PowerShell;
   - preflight fail-closed;
   - puertos alternativos para evitar conflictos.
4. compose-images-and-smoke:
   - `.env` ficticio seguro;
   - preflight container-only;
   - Compose app/smoke;
   - build backend/frontend;
   - stack;
   - smoke E2E;
   - logs en fallo;
   - cleanup con volumen efímero.

GitHub continúa sin exponer checks visibles para los commits consultados desde el conector.

## Ejecución real acumulada

### Primer intento Windows/Docker

- preflight container-only: `PASS`;
- guardas de envío: `PASS`;
- imágenes base descargadas;
- npm install frontend: `PASS`;
- frontend TypeScript/build: `FAIL`;
- backend: cancelado por fallo paralelo;
- stack y smoke: no ejecutados.

Errores reproducidos:

1. credenciales anulables en `getProspect`;
2. credenciales anulables en `refresh`;
3. import CSS sin declaración Vite/TypeScript.

Los tres errores fueron corregidos en `main`.

### Segunda reejecución Windows/Docker

- correcciones frontend presentes: `PASS`;
- preflight: `PASS`;
- imagen frontend exportada: `PASS_FROM_CACHE`;
- imagen backend exportada: `PASS_FROM_CACHE`;
- build limpio frontend: no demostrado;
- build limpio backend: no demostrado;
- arranque: `FAIL` por conflicto del puerto host 5432;
- `docker compose ps`: vacío;
- Flyway, Hibernate y smoke: no ejecutados.

Evidencias:

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
```

### Hardening posterior

- los tres puertos host son configurables;
- valor PostgreSQL recomendado: 55432;
- preflight valida puertos y URL;
- smoke deriva URLs desde `.env`;
- builds definitivos usan `--no-cache`;
- orquestación Docker Windows automatizada;
- Docker/CI preparados para `npm ci`;
- evidencia: `docs/validation/SEG-001-local-orchestration-2026-07-20.md`.

## Validaciones ejecutadas

### PASS real

- preflight PowerShell container-only antes del hardening de tres puertos;
- guardas de envío;
- descarga de PostgreSQL y bases de imágenes;
- npm install frontend;
- ejecución TypeScript que reprodujo errores;
- presencia local de las correcciones;
- reproducción del conflicto 5432.

### PASS estático o funcional aislado

- estructura YAML de Compose;
- estructura YAML de CI;
- sintaxis shell disponible;
- parseo Makefile;
- actualizador Unix de tres puertos;
- preservación de contraseñas ficticias;
- preservación de UTF-8;
- preservación de guardas de envío;
- read-back remoto de archivos críticos.

### Pendiente de ejecución

- parser PowerShell de scripts nuevos;
- `set-local-host-ports.ps1` sobre `.env` real;
- `validate-docker-stack.ps1`;
- preflight con tres puertos;
- build frontend limpio;
- build backend limpio;
- Maven verify;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- Flyway;
- Hibernate validate;
- stack saludable;
- smoke host;
- smoke contenedorizado;
- generación/revisión de package-lock;
- ejecución real con npm ci;
- evidencia final verde.

## Trabajo finalizado

### Producto y hardening

- [x] dominio, persistencia, API y UI;
- [x] importaciones persistentes e idempotentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones y auditoría;
- [x] seguridad fail-closed;
- [x] parser endurecido;
- [x] recuperación por fila;
- [x] pruebas de regresión versionadas;
- [x] filas bloqueadas visibles;
- [x] correcciones TypeScript reales.

### Consolidación y documentación

- [x] todo unificado en `main`;
- [x] historia conservada;
- [x] documentación Windows/Linux/Docker-only;
- [x] flujo funcional y troubleshooting;
- [x] evidencias reales fechadas;
- [x] puertos host configurables documentados;
- [x] orquestación local documentada.

### Operación y automatización

- [x] stack Compose;
- [x] imágenes backend/frontend;
- [x] health checks;
- [x] preflight multiplataforma;
- [x] configuración segura de puertos;
- [x] smoke host/container;
- [x] orquestador Docker Windows;
- [x] Makefile;
- [x] CI E2E preparado;
- [x] contextos Docker seguros;
- [x] generación de lockfile mediante Docker;
- [x] adopción automática de npm ci cuando exista lockfile.

## Tareas pendientes

### Bloqueantes inmediatos

- [ ] actualizar checkout local al último `main`;
- [ ] inspeccionar/restaurar modificación local de `mvnw.cmd`;
- [ ] ejecutar `validate-docker-stack.ps1`;
- [ ] registrar transcript y cualquier fallo;
- [ ] confirmar build frontend limpio;
- [ ] confirmar build backend limpio;
- [ ] confirmar los tres servicios saludables;
- [ ] confirmar Flyway/Hibernate;
- [ ] confirmar smoke host y contenedor;
- [ ] ejecutar Maven verify/Testcontainers;
- [ ] generar y revisar package-lock;
- [ ] repetir frontend/CI con npm ci;
- [ ] escanear secretos y datos reales;
- [ ] registrar evidencia final;
- [ ] cerrar SEG-001;
- [ ] activar SEG-002.

### No bloqueantes de SEG-001

- [ ] resolución auditada de `DuplicateReview`;
- [ ] retry de `ImportJob`;
- [ ] filtros y exportación;
- [ ] accesibilidad;
- [ ] actor y retención de auditoría;
- [ ] concurrencia adicional de idempotencia.

## Seguridad vigente

- no existe Gmail, SMTP ni adaptador de envío;
- `SENDING_ENABLED=false`;
- `SENDING_DRY_RUN=true`;
- `SENDING_DAILY_LIMIT=0`;
- `SENDING_KILL_SWITCH=true`;
- kill switch persistente;
- API cerrada sin credenciales bootstrap;
- `.env`, XLSX real y datos operativos fuera de Git, CI e imágenes;
- servicios publicados solo en loopback;
- smoke solo lectura;
- ningún script despliega o envía comunicaciones.

## Riesgos activos

1. los builds demostrados después del fix fueron cacheados;
2. pueden aparecer errores nuevos en builds limpios;
3. PowerShell nuevo todavía no fue parseado/ejecutado en Windows;
4. Maven, tests y migraciones aún no están verdes;
5. frontend todavía no tiene lockfile versionado;
6. HTTP Basic es temporal;
7. revisiones ambiguas no tienen resolución UI;
8. trabajos fallidos no tienen retry;
9. relación institución–prospecto sigue uno a uno;
10. auditoría no tiene actor persistente ni retención definitiva;
11. stack Compose es local y no productivo.

## Próxima acción canónica

Leer `docs/next-step.md` y ejecutar el validador Docker Windows desde el último `main`. No iniciar SEG-002 mientras los controles bloqueantes permanezcan pendientes.
