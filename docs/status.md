# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica: `main`;
- todo SEG-001 consolidado por fast-forward, sin force push;
- correcciones posteriores realizadas directamente en `main` por autorización expresa;
- la rama histórica está detrás de `main` y no contiene cambios exclusivos;
- no existe pull request abierto para esta consolidación;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado;
- evidencia de consolidación: `docs/main-consolidation.md`.

Toda sesión nueva debe partir de `main`.

## Segmentos

- `SEG-000` — repositorio y continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

SEG-001 está implementado, endurecido, consolidado y documentado. No puede cerrarse hasta obtener una ejecución técnica verde después de las correcciones del primer build real.

## Alcance implementado

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper 3.9.16 con SHA-512;
- PostgreSQL 17, Flyway V1–V5 y Hibernate validate;
- instituciones, contactos, canales y prospectos;
- estados comerciales;
- exclusiones dominantes y retroactivas;
- equivalencia teléfono/WhatsApp;
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
- SHA-256, basename seguro y límite de 10 MB;
- HTTP 413 para exceso;
- `ImportJob`, `ImportRow` y `DuplicateReview`;
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
- diseño responsive;
- resumen de importación con `acceptedRows`, `excludedRows`, `rejectedRows`, `duplicateRows` y `reviewRows` visibles;
- declaración `vite-env.d.ts` para tipos Vite e imports CSS;
- referencia no anulable `activeCredentials` después del guard de autenticación.

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
- imagen backend multi-stage con health HTTP;
- imagen frontend multi-stage con Nginx;
- proxy Nginx para `/api` y `/actuator`;
- contenedor smoke que verifica health, Basic Auth y frontend;
- guía: `docs/containerized-quickstart.md`.

### Automatización

- preflight Unix/PowerShell;
- modos local y container-only;
- smoke tests de host Unix/PowerShell;
- smoke test completamente contenedorizado;
- Makefile con `smoke-container`;
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

## Primera ejecución real aportada

Entorno:

- Windows PowerShell;
- Docker `29.3.1`, build `c2be9cc`;
- ejecución `docker compose --profile app up -d --build`.

Resultado:

- preflight `-ContainerOnly`: `PASS`;
- guardas de envío: `PASS`;
- imagen PostgreSQL descargada;
- metadata/capas Maven, Temurin, Node y Nginx descargadas;
- `npm install` frontend: `PASS`, 24 paquetes instalados;
- frontend TypeScript/build: `FAIL`;
- backend build: `CANCELED` al fallar frontend;
- Flyway/Hibernate/stack/smoke: `NOT_RUN`.

Errores reproducidos:

1. `credentials | null` enviado a `getProspect`;
2. `credentials | null` enviado a `refresh`;
3. import CSS sin declaración Vite/TypeScript.

Evidencia completa:

```text
docs/validation/SEG-001-container-build-2026-07-20.md
```

## Correcciones aplicadas después del intento

### Commit `72f0421caaf4898ce04fd97724ecbad9a4ed6390`

- fija `activeCredentials` después del guard de autenticación;
- utiliza la referencia no anulable en callbacks y paneles;
- conserva `strict: true`;
- muestra `excludedRows` como `Bloqueadas`.

### Commit `31960db073d6df0cae683b02267a752b9538e08f`

- crea `frontend/src/vite-env.d.ts`;
- referencia `vite/client`;
- declara imports `*.css`.

Ambos archivos fueron releídos desde `main`. La compilación posterior todavía debe ejecutarse.

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
- [x] visualización de filas bloqueadas en UI.

### Consolidación y documentación

- [x] unificar absolutamente todo en `main`;
- [x] conservar historia mediante fast-forward;
- [x] convertir `main` en única fuente canónica;
- [x] alinear entorno DB;
- [x] documentar Windows, Linux/macOS y Docker-only;
- [x] documentar flujo funcional, API y troubleshooting;
- [x] actualizar README, índice, estado, backlog, segmento, validación y changelog;
- [x] documentar el primer build real y sus correcciones.

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
- [x] evidencia estática ejecutada;
- [x] preflight container-only ejecutado en Windows.

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
- parseo Makefile, incluido `smoke-container`;
- preflight PowerShell container-only real;
- descarga de imágenes/capas reales;
- instalación npm real dentro del build;
- compilación TypeScript real que reprodujo tres errores.

### Falló y fue corregida, pendiente de reejecución

- frontend typecheck/build.

### Implementada pero aún no ejecutada con éxito

- Maven y Spotless;
- unit tests/ArchUnit/Testcontainers;
- Flyway/Hibernate reales;
- PowerShell smoke;
- frontend build posterior a las correcciones;
- `docker compose config` semántico registrado;
- build completo de ambas imágenes;
- stack saludable;
- smoke E2E real.

No se afirma que compile o arranque hasta registrar la reejecución.

## Tareas pendientes

### Bloqueantes inmediatos

- [ ] actualizar checkout local a `main` con los commits correctivos;
- [ ] reconstruir primero la imagen frontend;
- [ ] reconstruir el stack completo;
- [ ] registrar cualquier error posterior;
- [ ] validar backend, Flyway e Hibernate;
- [ ] ejecutar smoke PowerShell o contenedorizado;
- [ ] generar `frontend/package-lock.json`;
- [ ] migrar Docker/CI a `npm ci`;
- [ ] ejecutar Maven/Spotless/tests/Testcontainers;
- [ ] registrar evidencia final;
- [ ] cerrar SEG-001 y activar SEG-002.

### No bloqueantes

- [ ] resolver `DuplicateReview` auditadamente;
- [ ] retry de `ImportJob`;
- [ ] filtros/exportación/accesibilidad;
- [ ] retención y actor de auditoría.

## Riesgos activos

1. pueden aparecer errores adicionales al continuar el build frontend;
2. backend, migraciones y tests aún no llegaron a ejecutarse en el intento real;
3. frontend sin lockfile;
4. imagen frontend usa `npm install`;
5. HTTP Basic temporal;
6. revisiones sin resolución;
7. trabajos fallidos sin retry;
8. institución–prospecto uno a uno;
9. auditoría sin actor/retención;
10. lote disponible 100, no 298;
11. contactos históricos requieren canales verificados;
12. stack Compose solo local, no producción.

## Próxima acción canónica

Leer `docs/next-step.md`. Actualizar `main`, reconstruir frontend y stack, ejecutar smoke y documentar el siguiente resultado real.