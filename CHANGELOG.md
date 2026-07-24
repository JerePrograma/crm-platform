# Changelog

## 2026-07-23 — Experiencia integral de operador

- centraliza estados, canales, roles, etapas, acciones, motivos y eventos visibles en español mediante `frontend/src/uiLabels.ts`;
- humaniza navegación, dashboard, prospectos, contactos, importaciones, duplicados, pipeline, campañas, mensajes, outbox, inbound, exclusiones, auditoría, usuarios, configuración y cuenta;
- reemplaza `window.prompt` y `window.confirm` por diálogos accesibles con focus trap, Escape y retorno de foco;
- agrega foco visible, controles táctiles, responsive, tablas navegables, revelado progresivo, estados vacíos y errores orientativos;
- reorganiza la ficha de prospecto y agrega copia segura de canales;
- mejora importaciones con resumen, filtros, búsqueda, paginación visual y confirmación explícita;
- evita JSON crudo como vista principal de auditoría y oculta claves sensibles;
- conserva de forma controlada correo, teléfono/WhatsApp, ubicación, categoría, sitio, fuente, evidencia, prioridad y fecha al crear registros independientes desde duplicados;
- agrega `SanitizedDuplicateImportData` y regresiones para impedir persistencia arbitraria desde `raw_data`;
- valida frontend, Maven Verify, Docker fail-closed, Playwright, repository safety y `git diff --check` en el run `30034176306`;
- publica el cierre funcional en `8d12f8ff772d3445440e4419b22d5c81b102cb15`.

## 2026-07-23 — Contactabilidad y paginación

- corrige la clasificación de prospectos sin correo, teléfono ni WhatsApp utilizable, que quedan en `NEEDS_ENRICHMENT` en lugar de aparecer como contactables;
- mantiene las exclusiones como regla dominante y permite que el sitio participe en exclusiones sin convertir por sí solo al prospecto en contactable;
- recalcula contactabilidad al crear o eliminar contactos y al agregar, modificar o eliminar canales;
- aplica el mismo criterio en vista previa y ejecución de importaciones;
- conecta la lista de prospectos con la paginación real de la API y conserva búsqueda, estado y página en la URL;
- amplía pruebas de prospectos, importaciones, reportes, campañas, oportunidades y API frontend;
- valida Spotless, pruebas específicas, Maven Verify, frontend, Docker fail-closed, Playwright, repository safety y `git diff --check` en el run `30036648327`;
- publica el cierre funcional en `19732dec9638cd47fee4b39ac41c5968693b5b7a`.

## 2026-07-22 — SEG-011 complete CRM closure

- cierra SEG-010 y SEG-011 después de dos ejecuciones limpias consecutivas del
  validador integral sobre `986523a`;
- integra la historia completa mediante fast-forward en `main`, agrega la
  evidencia sanitizada final de SEG-011 y publica en `origin/main`;
- corrige el modo ejecutable Unix de `mvnw` y alinea las tags Docker aisladas
  por job; GitHub Actions run `29951586239` termina `success` con 22/22 jobs;
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
