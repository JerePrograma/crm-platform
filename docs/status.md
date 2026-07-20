# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica: `main`;
- todo SEG-001 fue consolidado por fast-forward, sin force push;
- correcciones posteriores realizadas directamente en `main` por autorización expresa;
- la rama histórica está detrás de `main` y no contiene cambios exclusivos;
- no existe pull request abierto para esta consolidación;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado;
- evidencia: `docs/main-consolidation.md`.

Toda sesión nueva debe partir de `main`.

## Segmentos

- `SEG-000` — repositorio y continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

SEG-001 está implementado, endurecido, consolidado y documentado. No puede cerrarse sin ejecución técnica verde.

## Alcance implementado

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper 3.9.16 con SHA-512;
- PostgreSQL 17, Flyway V1–V5 y Hibernate validate;
- instituciones, contactos, canales y prospectos;
- estados comerciales;
- exclusiones dominantes, retroactivas y equivalentes entre teléfono/WhatsApp;
- normalización y validación central;
- API paginada, OpenAPI y RFC 7807;
- auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, Prometheus y logging estructurado.

### Importación

- CSV con coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- parser por encabezados normalizados;
- comillas, saltos internos y rechazo de estructuras inválidas;
- fechas Excel UTC;
- SHA-256, basename seguro y 10 MB;
- HTTP 413 para exceso;
- ImportJob, ImportRow y DuplicateReview;
- preview y ejecución confirmada;
- transacción y recuperación por fila;
- idempotencia;
- orden por hoja/fila;
- métricas accepted/excluded/rejected/duplicate/review;
- duplicados exactos enlazados;
- ambigüedades persistidas en preview;
- preview con exclusiones sin escritura de dominio;
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
- tipos compatibles con `excludedRows`;
- diseño responsive.

Pendiente no bloqueante: visualizar `excludedRows` como control separado.

## Infraestructura local

### Procesos separados

- PostgreSQL mediante Compose;
- backend mediante Maven Wrapper;
- frontend mediante Vite;
- guía: `docs/local-development-and-usage.md`.

### Stack completamente contenedorizado

- perfil `app`: PostgreSQL, backend y frontend;
- perfil `smoke`: verificación E2E efímera;
- puertos solo en `127.0.0.1`;
- health chain PostgreSQL → backend → frontend → smoke;
- imagen backend multi-stage con curl/health;
- imagen frontend multi-stage con Nginx;
- proxy Nginx para `/api` y `/actuator`;
- contenedor smoke que verifica health, Basic Auth y frontend;
- guía: `docs/containerized-quickstart.md`.

### Automatización

- preflight Unix/PowerShell;
- modos local y container-only;
- smoke tests de host Unix/PowerShell;
- smoke test completamente contenedorizado;
- Makefile con `smoke-container` y resto de targets;
- `.gitattributes` multiplataforma;
- `.dockerignore` raíz y frontend;
- documentación: `scripts/README.md`.

## CI implementado

Jobs:

1. backend: Maven verify, Spotless, unit tests, ArchUnit y Testcontainers;
2. frontend: install, typecheck y build;
3. scripts: sintaxis Unix/PowerShell y preflight fail-closed;
4. compose-images-and-smoke:
   - configuración de perfiles `app` y `smoke`;
   - build backend;
   - build frontend;
   - arranque PostgreSQL/backend/frontend;
   - smoke E2E;
   - logs en fallo;
   - limpieza obligatoria.

Caché npm desactivada hasta disponer de lockfile.

## Trabajo finalizado

### Producto y hardening

- [x] dominio, persistencia, API y UI;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones y auditoría;
- [x] seguridad fail-closed;
- [x] parser endurecido;
- [x] recuperación por fila;
- [x] pruebas de regresión versionadas.

### Consolidación y documentación

- [x] unificar absolutamente todo en `main`;
- [x] conservar historia mediante fast-forward;
- [x] convertir `main` en única fuente canónica;
- [x] alinear entorno DB;
- [x] documentar Windows, Linux/macOS y Docker-only;
- [x] documentar flujo funcional, API y troubleshooting;
- [x] actualizar README, índice, estado, backlog, segmento, validación y changelog.

### Operación y automatización

- [x] stack Compose completo;
- [x] imágenes backend/frontend;
- [x] health checks encadenados;
- [x] preflight multiplataforma;
- [x] smoke de host multiplataforma;
- [x] smoke contenedorizado;
- [x] Makefile;
- [x] CI E2E preparado;
- [x] contextos Docker minimizados;
- [x] evidencia estática ejecutada.

## Seguridad vigente

- sin Gmail, SMTP o adaptador de envío;
- `SENDING_ENABLED=false`;
- `SENDING_DRY_RUN=true`;
- `SENDING_DAILY_LIMIT=0`;
- `SENDING_KILL_SWITCH=true`;
- kill switch persistente;
- API cerrada sin ambas credenciales;
- `.env`, XLSX real y datos operativos fuera de Git/CI/imágenes;
- auditoría de exclusión sin canal completo;
- contextos Docker excluyen planillas, claves y cachés;
- servicios publicados en loopback;
- smoke realiza solo lecturas.

## Validación

### Ejecutada

- consolidación y comparación de ramas;
- lectura remota posterior a escrituras;
- revisión estática backend/migraciones/frontend;
- configuración fail-closed;
- búsqueda remota de secretos y datos;
- parseo YAML de Compose con cuatro servicios;
- parseo YAML de CI con job E2E;
- sintaxis `sh` de preflight/smoke;
- parseo Makefile, incluido `smoke-container`.

Evidencia: `docs/validation/SEG-001-static-automation-2026-07-20.md`.

### Implementada pero no ejecutada

- Maven y Spotless;
- unit tests/ArchUnit/Testcontainers;
- Flyway/Hibernate reales;
- PowerShell syntax;
- frontend install/typecheck/build;
- preflight real;
- Docker Compose semántico;
- builds y pull de imágenes;
- stack y smoke real.

### Bloqueo del entorno disponible

- Maven ausente;
- Docker/Compose ausentes;
- PowerShell ausente;
- cachés vacías;
- registros externos inaccesibles;
- checks/runs del conector no visibles.

No se afirma que compile o arranque hasta registrar evidencia real.

## Tareas pendientes

### Bloqueantes

- [ ] clonar `main` en entorno con red y Docker;
- [ ] registrar SHA;
- [ ] ejecutar preflight;
- [ ] ejecutar Maven/Spotless/tests;
- [ ] validar Flyway/Hibernate/Testcontainers;
- [ ] ejecutar npm install y generar lockfile;
- [ ] typecheck/build frontend;
- [ ] migrar a npm ci;
- [ ] validar Compose semántico;
- [ ] construir ambas imágenes;
- [ ] ejecutar smoke contenedorizado;
- [ ] corregir fallos y repetir matriz;
- [ ] registrar evidencia;
- [ ] cerrar SEG-001 y activar SEG-002.

### No bloqueantes

- [ ] mostrar `excludedRows`;
- [ ] resolver DuplicateReview auditadamente;
- [ ] retry de ImportJob;
- [ ] filtros/exportación/accesibilidad;
- [ ] retención y actor de auditoría.

## Riesgos activos

1. compilación/esquema no ejecutados;
2. frontend sin lockfile;
3. imagen frontend usa npm install;
4. HTTP Basic temporal;
5. revisiones sin resolución;
6. trabajos fallidos sin retry;
7. institución–prospecto uno a uno;
8. auditoría sin actor/retención;
9. lote disponible 100, no 298;
10. contactos históricos requieren canales verificados;
11. stack Compose solo local, no producción.

## Próxima acción canónica

Leer `docs/next-step.md`: ejecutar la matriz desde `main`, corregir fallos reales y registrar evidencia. No iniciar SEG-002 mientras existan bloqueantes.
