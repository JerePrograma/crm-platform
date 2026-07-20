# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica y activa: `main`;
- todo SEG-001 fue consolidado por fast-forward, sin force push;
- las correcciones posteriores se realizan directamente en `main` por autorización expresa;
- no existe pull request abierto para esta consolidación;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado;
- evidencia de consolidación: `docs/main-consolidation.md`.

Toda sesión nueva debe partir de `main`. Ninguna rama temática anterior constituye fuente de verdad.

## Segmentos

- `SEG-000` — repositorio y continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

`SEG-001` está implementado, endurecido y documentado. No puede marcarse `COMPLETE` hasta registrar una ejecución técnica verde.

## Alcance implementado

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper fijado a Maven 3.9.16 con SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate `ddl-auto=validate`;
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

- CSV UTF-8 con coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- parser por encabezados normalizados;
- rechazo de encabezados duplicados;
- comillas, delimitadores y saltos internos;
- rechazo de comillas sin cerrar;
- fechas Excel en UTC;
- SHA-256, basename seguro y límite de 10 MB;
- límite multipart alineado y HTTP 413;
- `ImportJob`, `ImportRow` y `DuplicateReview`;
- preview y ejecución confirmada;
- transacción y recuperación por fila;
- idempotencia por contenido y modo;
- orden determinístico por hoja/fila;
- métricas `acceptedRows`, `excludedRows`, `rejectedRows`, `duplicateRows` y `reviewRows`;
- duplicados exactos enlazados al existente;
- ambigüedades persistidas durante preview;
- preview con exclusiones sin escritura de dominio;
- exclusiones importadas retroactivas y auditadas;
- fixture ficticia 100/16.

### Frontend

- React, TypeScript y Vite;
- credenciales solo en memoria;
- Basic Auth UTF-8;
- Dashboard;
- listado y ficha de prospectos;
- preview y ejecución de importaciones;
- resultado por fila;
- revisiones ambiguas;
- exclusiones y auditoría;
- tipos compatibles con `excludedRows`;
- diseño responsive.

Pendiente no bloqueante: mostrar `excludedRows` como control separado en la vista.

## Infraestructura y operación local

### Modalidad por procesos

- PostgreSQL mediante Compose;
- backend mediante Maven Wrapper;
- frontend mediante Vite;
- guía: `docs/local-development-and-usage.md`.

### Modalidad completamente contenedorizada

- perfil Compose `app` con PostgreSQL, backend y frontend;
- puertos publicados solo en `127.0.0.1`;
- dependencia por health checks: PostgreSQL → backend → frontend;
- imagen backend multi-stage con health probe;
- imagen frontend multi-stage con Nginx y proxy `/api`/`/actuator`;
- guía: `docs/containerized-quickstart.md`.

### Automatización

- `scripts/preflight.sh` y `scripts/preflight.ps1`;
- modo local con Java/Node y modo `container-only`;
- `scripts/smoke-test.sh` y `scripts/smoke-test.ps1`;
- Makefile con preflight, DB, stack, logs, verificación, smoke y reset;
- documentación en `scripts/README.md`;
- `.gitattributes` para finales de línea multiplataforma;
- `.dockerignore` raíz y frontend para excluir secretos, datos y cachés.

### CI

El workflow contiene trabajos separados para:

- Maven, Spotless, unit tests y Testcontainers;
- instalación, typecheck y build frontend;
- sintaxis de scripts Unix y PowerShell;
- preflight fail-closed;
- validación del perfil completo de Compose;
- build de imagen backend;
- build de imagen frontend.

No se habilitó caché npm porque todavía no existe `package-lock.json`.

## Trabajo finalizado

### Implementación y hardening

- [x] dominio y persistencia;
- [x] importaciones persistentes;
- [x] deduplicación y revisión humana;
- [x] exclusiones dominantes;
- [x] auditoría;
- [x] API y UI;
- [x] configuración fail-closed;
- [x] parser endurecido;
- [x] recuperación por fila;
- [x] métricas de bloqueados separadas;
- [x] pruebas de regresión versionadas.

### Consolidación y documentación

- [x] consolidar absolutamente todo en `main`;
- [x] conservar historia mediante fast-forward;
- [x] convertir `main` en única fuente canónica;
- [x] alinear `.env.example`, Compose y backend;
- [x] documentar Linux/macOS y Windows;
- [x] documentar flujo funcional completo;
- [x] documentar detención, reinicio y troubleshooting;
- [x] añadir inicio rápido contenedorizado;
- [x] añadir preflight, smoke tests y Makefile;
- [x] ampliar CI para scripts y ambas imágenes;
- [x] minimizar contextos Docker;
- [x] actualizar README e índice documental.

## Seguridad vigente

- no existe adaptador Gmail, SMTP o de correo;
- `SENDING_ENABLED=false`;
- `SENDING_DRY_RUN=true`;
- `SENDING_DAILY_LIMIT=0`;
- `SENDING_KILL_SWITCH=true`;
- kill switch persistente activo;
- API cerrada sin ambas credenciales bootstrap;
- `.env` y datos operativos fuera de Git e imágenes;
- XLSX real fuera del repositorio y CI;
- auditoría de exclusión sin canal completo;
- contextos Docker excluyen planillas, secretos, claves y cachés;
- puertos locales ligados a loopback.

## Validación

### Evidencia disponible

- consolidación de `main` verificada;
- archivos releídos después de escritura;
- revisión estática de backend, migraciones, controladores y frontend;
- configuración fail-closed comprobada;
- búsqueda remota sin datos reales ni secretos evidentes;
- Compose, Dockerfiles, scripts y CI revisados estáticamente;
- checks y workflow runs consultados para el último commit observado: sin resultados visibles.

### Implementada pero no ejecutada

- Maven y Spotless;
- unit tests y ArchUnit;
- Testcontainers, Flyway y Hibernate reales;
- frontend typecheck/build;
- preflight en entorno real;
- Compose completo;
- imágenes backend/frontend;
- smoke test.

### Bloqueo del entorno disponible

- Maven ausente;
- Docker/Compose ausentes;
- cachés Maven/npm vacías;
- acceso a registros externos no disponible;
- commits del conector sin checks visibles.

No se afirma que el proyecto compile o arranque hasta registrar evidencia real en `docs/validation/SEG-001.md`.

## Tareas pendientes

### Bloqueantes de SEG-001

- [ ] clonar `main` en un entorno con red y Docker;
- [ ] registrar el SHA exacto;
- [ ] ejecutar preflight;
- [ ] ejecutar Maven, Spotless y pruebas;
- [ ] validar Flyway V1–V5, Hibernate y Testcontainers;
- [ ] ejecutar `npm install` y generar `package-lock.json`;
- [ ] ejecutar typecheck y build frontend;
- [ ] validar perfil Compose completo;
- [ ] construir ambas imágenes;
- [ ] levantar stack y ejecutar smoke test;
- [ ] corregir todos los fallos reales;
- [ ] repetir la matriz completa;
- [ ] documentar fecha, SHA, comandos y resultados;
- [ ] cerrar SEG-001 y activar SEG-002.

### No bloqueantes

- [ ] visualizar `excludedRows` en UI;
- [ ] resolver `DuplicateReview` de forma auditada;
- [ ] retry explícito de `ImportJob` fallido;
- [ ] filtros combinables;
- [ ] exportación de resultados;
- [ ] accesibilidad básica;
- [ ] política de retención;
- [ ] actor persistente en auditoría.

## Riesgos activos

1. pueden existir fallos de compilación o formato no detectables estáticamente;
2. puede existir divergencia JPA/Flyway no observada;
3. el frontend no tiene lockfile;
4. HTTP Basic es temporal y no implementa RBAC;
5. la imagen frontend usa `npm install` hasta generar lockfile;
6. no existe resolución auditada de revisiones;
7. no existe retry explícito;
8. la relación institución–prospecto sigue siendo uno a uno;
9. la auditoría no tiene actor ni retención final;
10. el lote disponible cubre 100 prospectos, no 298;
11. contactos históricos deben excluirse solo con canales exactos verificados;
12. el stack Compose es local, no producción.

## Próxima acción canónica

Leer `docs/next-step.md`: ejecutar toda la matriz desde `main`, corregir fallos reales y registrar evidencia. No iniciar SEG-002 mientras los controles principales permanezcan pendientes.
