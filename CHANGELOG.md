# Changelog

## 2026-07-22 — SEG-011 complete CRM closure

- cierra SEG-010 y SEG-011 después de dos ejecuciones limpias consecutivas del
  validador integral sobre `986523a`;
- integra la historia completa mediante fast-forward en `main` local y agrega la
  evidencia sanitizada final de SEG-011;
- valida dos veces 79/79 backend, Spotless 159/159, ArchUnit, Vitest 2/2,
  Playwright 2/2, imágenes sin caché, npm audit, Grype, migraciones
  vacío/V11→V13, backup/restore y perfil productivo local;
- confirma en ambas corridas los cuatro bloqueos efectivos, providers reales no
  conectados y cero `SENT|DELIVERED|READ`;
- corrige la precedencia entorno/`.env` de preflight/smoke y elimina una carrera
  ADMIN→VIEWER esperando el estado observable de logout, sin sleeps;
- mantiene Unix integral y CI remoto sin ejecutar, XLSX real bloqueado por
  archivo externo, producción/push/PR no autorizados y comunicaciones reales
  deshabilitadas.

## 2026-07-22 — SEG-010 operations candidate

- agrega V13 con configuración, etiquetas e índices de búsqueda/reporting;
- incorpora reporting tenant-scoped, monedas separadas, búsqueda PostgreSQL y
  exportación formula-safe;
- completa UI operativa, Vitest y Playwright integral para CRM/outbox/inbound;
- agrega correlation ID, Micrometer/probes, límites de importación y threat
  model/PII;
- agrega backup/restore probado, perfil productivo local, runbooks, CI y
  validadores CRM Windows/Unix;
- endurece backend con JRE mínima no-root fijada, healthcheck Java, JDBC/Jackson
  parchados y Grype sin High/Critical;
- mantiene Gmail/WhatsApp no conectados, XLSX real bloqueado externamente,
  envío real deshabilitado y producción no desplegada;
- valida focalmente 79/79 backend, Spotless 159/159, ArchUnit, Vitest 2/2,
  Playwright 2/2, backup/restore y production profile; el cierre integral
  repetido pertenece a SEG-011.

## 2026-07-22 — SEG-009 outbox e inbound durable

- agrega V12 con outbox PostgreSQL, leases, retry/dead-letter, idempotencia e inbound;
- agrega worker transaccional, administración, métricas y UI operativa;
- agrega webhook fake HMAC/replay, quarantine, asociación y efectos de dominio;
- mantiene cuatro bloqueos de envío, providers reales no conectados y cero estados prohibidos;
- valida 69/69 backend, TypeScript/Vite, Docker V11→V12 y E2E real con datos sintéticos.

## 2026-07-21 — SEG-008

- agregó V11 con conexiones externas, mensajes e intentos tenant-scoped;
- implementó contratos, policy fail-closed, providers no-op/fake y enlaces
  manuales;
- agregó adapters Gmail draft-only y WhatsApp Cloud detrás de red real
  deshabilitada y configuración completa obligatoria;
- incorporó UI de mensajes/integraciones, contract tests loopback y regresión de
  merge;
- migró el volumen sintético V10→V11, ejecutó frontend/Docker/health/Playwright
  y demostró cero estados `SENT`.

## 2026-07-21 — SEG-007

- agregó V10 con campañas, audiencia congelada, secuencias y resultados de
  simulación;
- implementó plantillas versionadas y renderer de variables allow-listed;
- incorporó aprobación RBAC, idempotencia y bloqueo persistente/ambiental;
- conectó UI de preview, audiencia, secuencia y simulación sin botón de envío;
- validó Flyway V1–V10, Maven 50/50, frontend, Docker, smoke y Playwright.

## 2026-07-21 — SEG-006

- agregó V9 con oportunidades, historial de etapas y unicidad de oportunidad
  activa principal;
- implementó transiciones, control optimista, reglas de cierre y sincronización
  con prospectos y tareas;
- incorporó pipeline kanban, tabla, forecast, aging y oportunidades estancadas;
- validó Flyway V1–V9, Maven 45/45, frontend, Docker, smoke y Playwright.

## 2026-07-21 — SEG-005

- agregó V8 con resolución trazable, mapas de merge y redirect del absorbido;
- implementó seis acciones tenant-scoped, merge transaccional e idempotente y
  preservación de referencias;
- convirtió la bandeja de duplicados en una comparación accionable;
- validó Flyway V1–V8, Maven 42/42, frontend, Docker, smoke y Playwright.

## 2026-07-21 — SEG-003 y SEG-004

- agregó el modelo operativo V7 de prospectos, contactos, estados, notas,
  actividades, tareas y timeline;
- agregó CRUD tenant-scoped, control optimista, CSV seguro y ciclo comercial;
- conectó formularios React para alta, edición, contacto, nota, tarea y transición;
- validó Flyway V1–V7, Maven 39/39, frontend, Docker, smoke y Playwright.

Todos los cambios relevantes se documentan aquí. El proyecto todavía no tiene una versión publicada.

## Unreleased

### Added

- plan vivo, progreso y matriz ejecutable para completar SEG-002–SEG-011;
- activación formal de SEG-002 en la rama de integración
  `feat/complete-crm-platform` después de repetir el baseline SEG-001;

- Java 21, Spring Boot 4.1, Maven Wrapper, PostgreSQL y Flyway;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes, idempotentes y auditadas;
- preview, ejecución confirmada y métricas por fila;
- API REST, OpenAPI, RFC 7807 y auditoría JSONB;
- autenticación bootstrap fail-closed;
- frontend React/TypeScript/Vite;
- perfiles Compose `app` y `smoke`;
- imágenes backend/frontend multi-stage;
- Nginx, proxy y health checks encadenados;
- preflight Unix/PowerShell;
- smoke host y contenedorizado;
- Makefile y `.dockerignore` raíz/frontend;
- `.gitattributes` multiplataforma;
- configuradores coordinados de puertos Windows/Unix;
- checker PowerShell de sintaxis;
- checker Windows de enlace y propiedad Docker de puertos;
- validador Docker Windows con clean builds, health, smoke y evidencia JSON;
- validadores integrales PowerShell y Bash;
- backend Maven verify/Testcontainers contenedorizado;
- generadores Docker de `package-lock.json`;
- escaneo centralizado del repositorio Windows/Unix;
- CI con backend, frontend, scripts, regresiones de puertos y stack E2E;
- fixtures, Testcontainers y ArchUnit;
- documentación técnica, operativa y evidencias fechadas.

### Changed

- `main` es la única rama canónica;
- PostgreSQL, backend y frontend publican puertos host configurables solo en loopback;
- valores predeterminados: 55432, 8080 y 5173;
- preflight valida daemon Docker, puertos, URL DB, credenciales y guardas;
- validadores integrales exigen `main` y working tree limpio;
- builds de cierre exigen `--no-cache`;
- Maven verify puede ejecutarse sin Java instalado en el host;
- Dockerfile frontend usa `npm ci` cuando existe lockfile y `npm install` cuando falta;
- el validador inicia PostgreSQL y espera health antes de reconstruir imágenes;
- `stackKeptRunning` refleja contenedores realmente activos;
- resultados de importación se ordenan por hoja/fila;
- Basic Auth frontend usa UTF-8;
- CSV admite coma y punto y coma;
- fechas Excel usan UTC;
- documentación diferencia PASS, FAIL, PASS_PARTIAL, NOT_RUN y PENDING;
- `org.flywaydb:flyway-core` directo fue reemplazado por `org.springframework.boot:spring-boot-starter-flyway` para habilitar la auto-configuración de Spring Boot 4;
- se conserva `org.flywaydb:flyway-database-postgresql`;
- `spring.flyway.fail-on-missing-locations=true` activa fail-fast cuando faltan recursos de migración.
- código propio de serialización JSON usa Jackson 3 administrado por Spring Boot
  4.1; Jackson 2 queda restringido a dependencias transitivas de springdoc;
- wrappers Maven/Testcontainers detectan la API Docker del daemon y la propagan
  a docker-java;
- healthcheck frontend usa loopback IPv4 explícito;
- `frontend/package-lock.json` está versionado y los builds usan `npm ci`.

### Fixed

- exclusiones importadas usan el caso de uso manual, suprimen y auditan;
- preview conserva revisiones ambiguas y aplica exclusiones;
- duplicados exactos enlazan prospecto existente;
- `EXCLUDED` está separado de accepted;
- correo inválido se rechaza por fila;
- límites multipart/funcional 10 MB y HTTP 413;
- CSV y headers inválidos se rechazan;
- filenames se saneán;
- backend y PostgreSQL comparten credenciales;
- callbacks autenticados no reciben `Credentials | null`;
- imports CSS reconocidos por TypeScript;
- conflicto inicial del puerto PostgreSQL 5432 mediante puerto configurable;
- riesgo de BOM en `.env` PowerShell;
- comandos Compose con `--progress` en posición correcta;
- splatting de parámetros del validador integral;
- escaneo del lote operativo en cualquier subdirectorio;
- generadores de lockfile no crean `node_modules` ni ejecutan lifecycle scripts;
- propiedad Unix del lockfile;
- rutas con espacios en el escaneo Unix;
- interpolación inválida `$LASTEXITCODE:` en scripts PowerShell;
- detección tardía de puertos reservados u ocupados en Windows;
- detección de puertos publicados por otros contenedores Docker;
- mensaje engañoso de `-KeepRunning` cuando no existía stack activo;
- ausencia de auto-configuración Flyway en Spring Boot 4 que dejaba el esquema vacío y provocaba `Schema validation: missing table [contact]`.
- incompatibilidad entre `com.fasterxml.jackson.databind.ObjectMapper` solicitado
  por código propio y `tools.jackson.databind.ObjectMapper` registrado por Spring
  Boot 4;
- timestamps de auditoría enviados como `Instant` sin tipo SQL inferible por el
  driver PostgreSQL;
- flag Docker inválido `--environment` en el verificador backend;
- API docker-java predeterminada 1.32 rechazada por Docker Engine 29;
- fixture transaccional de deduplicación incompatible con el límite real
  `REQUIRES_NEW`;
- healthcheck Nginx que resolvía `localhost` por IPv6 aunque Nginx escuchaba IPv4.

### Validation

Ejecución real acumulada:

- preflight PowerShell inicial: PASS;
- guardas de envío: PASS;
- npm install frontend: PASS;
- frontend TypeScript inicial: FAIL reproducido y corregido;
- imágenes frontend/backend desde caché: PASS_FROM_CACHE en una ejecución temprana;
- arranque inicial: FAIL por puerto 5432;
- PowerShell parser: FAIL reproducido, corregido y luego PASS sobre 11 scripts;
- frontend clean build sin caché: PASS;
- backend clean image build sin caché: PASS;
- intento con `55432`: FAIL por bind Windows;
- intento con `15432`: FAIL porque Docker tenía el puerto asignado;
- checker Docker/Windows actualizado: PASS sobre 25432/8080/5173;
- publicación PostgreSQL en 25432: PASS;
- PostgreSQL health: PASS;
- frontend clean build `--no-cache`: PASS;
- TypeScript strict/Vite production build: PASS;
- backend clean image build `--no-cache`: PASS;
- Maven package con tests omitidos: PASS_PARTIAL;
- conexión backend → PostgreSQL 17.10: PASS;
- sexta ejecución integral: FAIL por ausencia de auto-configuración Flyway;
- migraciones V1–V5: NOT_RUN en esa ejecución;
- Hibernate validate: FAIL por tabla `contact` ausente;
- backend/frontend health y smoke: NOT_RUN;
- Maven verify, Spotless, unit tests, ArchUnit y Testcontainers: NOT_RUN;
- package-lock y npm ci: NOT_RUN;
- working tree posterior: PASS, limpio;
- corrección `spring-boot-starter-flyway`: EXECUTED_PASS;
- ejecución sobre `a9e2c44`: Flyway V1–V5 e Hibernate PASS; FAIL al crear
  `AuditEventWriter` por mapper Jackson 2 inexistente;
- regresión `ExclusionIntegrationTest`: PASS para contexto Spring, mapper Jackson
  3 y persistencia JSONB con mapa, UUID, fechas, enum y null;
- primera validación integral completa sobre `951d19b`: PASS;
- lockfile SHA-256:
  `1936217c0598825ef43519069a3ba89a974e2b30e3b9f2619d4e62dd10810c98`;
- segunda validación limpia sobre `d8a5a44`: PASS con `npm ci` desde el primer
  build;
- PostgreSQL/backend/frontend: healthy;
- smoke host/contenedor inicial y final: PASS;
- Maven verify: PASS, 29/29 tests;
- Spotless: PASS, 55/55;
- ArchUnit/Testcontainers: PASS;
- repository safety: PASS;
- GitHub Actions run `29848718163`: success en backend, frontend, scripts y
  compose-images-and-smoke.

Evidencias principales:

```text
docs/validation/SEG-001-static-automation-2026-07-20.md
docs/validation/SEG-001-container-build-2026-07-20.md
docs/validation/SEG-001-rerun-2026-07-20.md
docs/validation/SEG-001-local-orchestration-2026-07-20.md
docs/validation/SEG-001-complete-validation-automation-2026-07-20.md
docs/validation/SEG-001-cross-platform-validation-2026-07-20.md
docs/validation/SEG-001-powershell-parser-failure-2026-07-21.md
docs/validation/SEG-001-port-bind-failure-2026-07-21.md
docs/validation/SEG-001-docker-port-owner-failure-2026-07-21.md
docs/validation/SEG-001-flyway-autoconfiguration-failure-2026-07-21.md
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
```

### Security

- sin credenciales bootstrap no hay API de negocio;
- sin adaptador de envío;
- cuatro guardas `SENDING_*` cerradas;
- Maven Wrapper verificado;
- datos reales fuera de Git, CI e imágenes;
- contextos Docker sin `.env`, planillas o claves;
- servicios solo en localhost;
- smoke realiza lecturas;
- lockfile se genera con lifecycle scripts deshabilitados;
- scripts de seguridad bloquean entorno, evidencia, datos privados, lote, claves y credenciales;
- transcripts locales fuera de Git;
- CI usa credenciales ficticias;
- ninguna validación habilita comunicaciones.

### Known limitations

- recorrido integral funcional Bash/Linux/macOS no ejecutado localmente;
- advertencias Hikari al cerrar bases Testcontainers efímeras, sin fallos;
- Mockito todavía se auto-adjunta como agente y requerirá configuración explícita
  ante futuras restricciones del JDK;
- HTTP Basic es temporal;
- revisiones ambiguas no tienen resolución UI;
- jobs sin retry;
- sin campañas, Gmail, Sheets o cloud;
- Compose es local, no producción.
