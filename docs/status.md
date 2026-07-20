# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica: `main`;
- todo SEG-001 consolidado por fast-forward, sin force push;
- correcciones posteriores realizadas directamente en `main` por autorización expresa;
- la rama histórica está detrás y no contiene cambios exclusivos;
- no existe pull request abierto;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado.

Toda sesión debe partir de `main`.

## Segmentos

- `SEG-000` — repositorio y continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

SEG-001 está implementado, endurecido, consolidado y documentado. Permanece abierto hasta completar builds limpios, arranque, migraciones, pruebas y smoke.

## Alcance implementado

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper 3.9.16 verificado;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate validate;
- instituciones, contactos, canales y prospectos;
- estados comerciales;
- exclusiones dominantes y retroactivas;
- equivalencia teléfono/WhatsApp;
- normalización y validación;
- API paginada, OpenAPI y RFC 7807;
- auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, Prometheus y logs estructurados.

### Importación

- CSV con coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- parser por encabezados normalizados;
- comillas y saltos internos;
- fechas Excel UTC;
- SHA-256, basename seguro y 10 MB;
- HTTP 413 para exceso;
- ImportJob, ImportRow y DuplicateReview;
- preview y ejecución confirmada;
- recuperación por fila;
- idempotencia;
- orden por hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- ambigüedades persistidas;
- preview con exclusiones;
- exclusiones importadas retroactivas y auditadas;
- fixture ficticia 100/16.

### Frontend

- React, TypeScript y Vite;
- credenciales solo en memoria;
- Basic Auth UTF-8;
- dashboard;
- prospectos y ficha;
- importaciones y resultados;
- revisiones ambiguas;
- exclusiones y auditoría;
- diseño responsive;
- `excludedRows` visible como `Bloqueadas`;
- `vite-env.d.ts` para Vite/CSS;
- referencia no anulable `activeCredentials`.

## Infraestructura local

### Procesos separados

- PostgreSQL mediante Compose;
- backend mediante Maven Wrapper;
- frontend mediante Vite.

### Stack contenedorizado

- perfil `app`: PostgreSQL, backend y frontend;
- perfil `smoke`: validación E2E efímera;
- puertos en loopback;
- health chain PostgreSQL → backend → frontend → smoke;
- imagen backend multi-stage;
- imagen frontend multi-stage con Nginx;
- proxy `/api` y `/actuator`;
- smoke de solo lectura.

### Puerto PostgreSQL host

El puerto publicado dejó de estar fijo.

Configuración recomendada:

```text
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Compose mantiene internamente:

```text
postgres:5432
```

El cambio evita conflictos con PostgreSQL local, puertos reservados de Windows u otros contenedores.

### Automatización

- preflight Unix/PowerShell;
- modos local y container-only;
- validación de `POSTGRES_HOST_PORT`;
- smoke host Unix/PowerShell;
- smoke contenedorizado;
- generación de lockfile mediante Docker;
- Makefile;
- `.gitattributes`;
- `.dockerignore` raíz/frontend.

## CI implementado

Jobs:

1. backend: Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
2. frontend: install, typecheck y build;
3. scripts: sintaxis Unix/PowerShell y preflight fail-closed;
4. compose-images-and-smoke: imágenes, stack, smoke, logs y cleanup.

Caché npm permanece desactivada hasta disponer de lockfile.

## Ejecución real acumulada

### Primer intento

- preflight container-only: `PASS`;
- guardas de envío: `PASS`;
- imágenes base descargadas;
- npm install frontend: `PASS`;
- frontend TypeScript/build: `FAIL`;
- backend: cancelado por fallo paralelo;
- stack y smoke: no ejecutados.

Errores frontend:

1. credenciales anulables en `getProspect`;
2. credenciales anulables en `refresh`;
3. import CSS sin declaración.

Los tres errores fueron corregidos en `main`.

### Segunda reejecución

- correcciones frontend presentes localmente: `PASS`;
- preflight: `PASS`;
- imagen frontend exportada: `PASS_FROM_CACHE`;
- imagen backend exportada: `PASS_FROM_CACHE`;
- build limpio frontend: no demostrado;
- build limpio backend: no demostrado;
- arranque: `FAIL` por puerto host `5432`;
- `docker compose ps`: vacío;
- Flyway/Hibernate/smoke: no ejecutados.

Evidencia:

```text
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
```

## Correcciones posteriores

- puerto host PostgreSQL configurable;
- valor recomendado 55432;
- preflight exige puerto y URL coherentes;
- documentación usa sintaxis correcta `docker compose --progress plain ...`;
- builds definitivos deben usar `--no-cache`;
- generadores de lockfile Unix/PowerShell versionados.

## Trabajo finalizado

### Producto y hardening

- [x] dominio, persistencia, API y UI;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones y auditoría;
- [x] seguridad fail-closed;
- [x] parser endurecido;
- [x] recuperación por fila;
- [x] pruebas de regresión versionadas;
- [x] filas bloqueadas visibles.

### Consolidación y documentación

- [x] todo unificado en `main`;
- [x] historia conservada;
- [x] documentación Windows/Linux/Docker-only;
- [x] flujo funcional y troubleshooting;
- [x] evidencias reales fechadas;
- [x] puerto host configurable documentado.

### Operación y automatización

- [x] stack Compose;
- [x] imágenes backend/frontend;
- [x] health checks;
- [x] preflight multiplataforma;
- [x] smoke host/container;
- [x] Makefile;
- [x] CI E2E preparado;
- [x] contextos Docker seguros;
- [x] generación de lockfile mediante Docker.

## Seguridad vigente

- sin Gmail, SMTP o adaptador de envío;
- `SENDING_ENABLED=false`;
- `SENDING_DRY_RUN=true`;
- `SENDING_DAILY_LIMIT=0`;
- `SENDING_KILL_SWITCH=true`;
- kill switch persistente;
- API cerrada sin credenciales;
- `.env`, XLSX real y datos operativos fuera de Git/CI/imágenes;
- servicios publicados en loopback;
- smoke solo lectura.

## Validación

### Ejecutada

- consolidación y comparación de ramas;
- revisión estática backend/migraciones/frontend;
- configuración fail-closed;
- escaneo de secretos/datos;
- YAML Compose/CI;
- sintaxis shell y Make;
- preflight PowerShell real;
- descarga de imágenes base;
- npm install real;
- fallo TypeScript real y corrección;
- exportación cacheada de imágenes;
- reproducción del conflicto de puerto 5432.

### Pendiente

- preflight con puerto 55432;
- frontend build limpio `--no-cache`;
- backend build limpio `--no-cache`;
- Maven verify/Spotless/tests;
- Testcontainers;
- Flyway/Hibernate;
- stack saludable;
- smoke host y container;
- package-lock;
- migración a npm ci;
- evidencia final verde.

## Tareas pendientes

### Bloqueantes inmediatos

- [ ] actualizar checkout a último `main`;
- [ ] inspeccionar/restaurar modificación local de `mvnw.cmd`;
- [ ] añadir `POSTGRES_HOST_PORT=55432` a `.env`;
- [ ] actualizar DATABASE_URL a 55432;
- [ ] repetir preflight;
- [ ] limpiar contenedores incompletos sin borrar volumen;
- [ ] build limpio frontend;
- [ ] build limpio backend;
- [ ] levantar stack;
- [ ] validar health/Flyway/Hibernate;
- [ ] ejecutar smoke;
- [ ] ejecutar Maven verify/Testcontainers;
- [ ] generar/versionar lockfile;
- [ ] migrar a npm ci;
- [ ] registrar evidencia y cerrar SEG-001.

### No bloqueantes

- [ ] resolución auditada de DuplicateReview;
- [ ] retry de ImportJob;
- [ ] filtros/exportación/accesibilidad;
- [ ] actor y retención de auditoría.

## Riesgos activos

1. builds exportados solo desde caché;
2. pueden aparecer errores nuevos en builds limpios;
3. Maven/tests/migraciones aún no ejecutados;
4. frontend sin lockfile;
5. HTTP Basic temporal;
6. revisiones sin resolución;
7. trabajos fallidos sin retry;
8. institución–prospecto uno a uno;
9. auditoría sin actor/retención;
10. stack Compose solo local.

## Próxima acción canónica

Leer `docs/next-step.md`. Actualizar `main`, configurar puerto 55432, ejecutar builds sin caché, levantar stack y completar smoke/pruebas.
