# Estado actual

Actualizado: 2026-07-23

La descripción exhaustiva del producto, la arquitectura, el enfoque UX, las validaciones y el roadmap se encuentra en:

```text
docs/estado-integral-y-roadmap.md
```

Este archivo conserva el resumen operativo que debe consultarse antes de iniciar cualquier cambio.

## Repositorio

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
último commit funcional: 19732dec9638cd47fee4b39ac41c5968693b5b7a
último cierre UX principal: 8d12f8ff772d3445440e4419b22d5c81b102cb15
producción: NO DESPLEGADA
comunicaciones reales: DESHABILITADAS
proveedores externos: IMPLEMENTADOS_NO_CONECTADOS
lote real: FUERA_DE_GIT_CI_E_IMÁGENES
```

`main` es la única fuente de verdad. No debe recuperarse trabajo desde ramas históricas sin demostrar que contiene cambios exclusivos y compatibles.

## Veredicto

El CRM está **funcionalmente completo y validado para demostración, evaluación interna y operación comercial segura sin envíos reales**.

No está autorizado ni validado para producción real. No existe endpoint de envío real y no deben conectarse Gmail, SMTP, WhatsApp Cloud, secretos o datos reales sin una fase separada y autorización explícita.

## Segmentos

| ID | Estado | Resumen |
|---|---|---|
| SEG-000 | COMPLETE | repositorio, continuidad y documentación canónica |
| SEG-001 | COMPLETE | vertical slice, persistencia, importación, seguridad y CI |
| SEG-002 | COMPLETE | identidad, organizaciones, usuarios, sesiones y RBAC |
| SEG-003 | COMPLETE | prospectos operativos, contactos, búsqueda y ciclo comercial |
| SEG-004 | COMPLETE | actividades, notas, tareas y timeline |
| SEG-005 | COMPLETE | resolución transaccional de duplicados |
| SEG-006 | COMPLETE | oportunidades, pipeline, forecast y aging |
| SEG-007 | COMPLETE | campañas, audiencias, plantillas y simulación |
| SEG-008 | COMPLETE | mensajería segura y adaptadores desconectados |
| SEG-009 | COMPLETE | outbox PostgreSQL, workers e inbound de prueba |
| SEG-010 | COMPLETE | reportes, seguridad, observabilidad y perfil productivo local |
| SEG-011 | COMPLETE | validación integral reproducible y cierre |
| UX-2026-07-23 | COMPLETE | experiencia de operador en español y flujos guiados |
| UX-CONTACTABILITY | COMPLETE | contactabilidad coherente y paginación real de prospectos |

## Arquitectura vigente

- Java 21 y Spring Boot 4.1.
- PostgreSQL 17.
- Flyway V1–V13.
- Hibernate validate.
- Spring Security, sesión HttpOnly same-origin, CSRF, RBAC y tenant isolation.
- React, TypeScript strict y Vite.
- Vitest y Playwright Chromium.
- Docker Compose, Testcontainers, ArchUnit y Maven Verify.
- OpenAPI, RFC 7807, auditoría JSONB, métricas y health probes.

## Funcionalidad disponible

- dashboard y reportes;
- prospectos, instituciones, contactos y canales;
- búsqueda por institución, ubicación, sitio, contacto, correo, teléfono y etiquetas;
- filtros, orden, paginación y URL persistente;
- notas, actividades, tareas y timeline;
- importación CSV/XLSX con vista previa y ejecución confirmada;
- exclusiones dominantes;
- duplicados exactos y ambiguos;
- vinculación, creación independiente, merge, descarte y postergación;
- oportunidades, pipeline, forecast y aging;
- campañas, plantillas, audiencias congeladas y simulación;
- borradores y enlaces manuales;
- outbox, worker, retry, dead-letter y requeue;
- inbound de prueba firmado, replay protection, cuarentena y asociación;
- auditoría, settings, usuarios, roles, cuenta y etiquetas.

## Experiencia de operador

Implementado y validado:

- textos principales en español claro;
- etiquetas técnicas centralizadas en `frontend/src/uiLabels.ts`;
- navegación y títulos humanizados;
- formularios con orientación y validación;
- errores y éxitos comprensibles;
- estados vacíos y datos faltantes explícitos;
- tablas con filtros, conteos, scroll y paginación;
- ficha de prospecto organizada mediante revelado progresivo;
- importaciones con resumen, filtros, búsqueda y paginación visual;
- auditoría con resumen legible y datos técnicos expandibles;
- diálogos accesibles en lugar de `window.prompt` y `window.confirm`;
- foco visible, teclado, Escape, focus trap y retorno de foco;
- responsive para escritorio y móvil;
- controles táctiles mínimos y reducción de movimiento.

## Correcciones funcionales de UX

### Duplicados

`CREATE_SEPARATE` y `MARK_NOT_DUPLICATE` conservan de forma validada institución, localidad, provincia, categoría, sitio, fuente, evidencia, prioridad, fecha de verificación, correo, teléfono/WhatsApp e identificador externo seguro.

La extracción se limita a campos conocidos mediante `SanitizedDuplicateImportData`. No se persiste JSON arbitrario.

### Contactabilidad

- sin correo, teléfono ni WhatsApp utilizable, el prospecto queda en `NEEDS_ENRICHMENT`;
- una web no convierte por sí sola al prospecto en contactable;
- las exclusiones siguen dominando;
- crear o eliminar contactos recalcula la condición;
- agregar, modificar o eliminar canales recalcula la condición;
- vista previa y ejecución de importación usan la misma regla.

### Paginación

La pantalla de prospectos consume la paginación real de la API y conserva `q`, `status` y `page` en la URL.

## Seguridad vigente

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

Permanece prohibido:

- desplegar producción sin autorización;
- habilitar o probar envíos reales;
- agregar un endpoint de envío;
- incluir secretos o `.env` en Git;
- usar datos reales en código, pruebas o documentación;
- importar el XLSX real en Git, CI o imágenes;
- debilitar exclusiones, idempotencia, CSRF, permisos o tenant isolation.

## Evidencia ejecutada

### Cierre integral de plataforma

```text
GitHub Actions run: 29951586239
commit: b904ff37e506f058dab351c2b941e13ee4ed9981
resultado: 22/22 jobs success
```

Incluyó backend, frontend, migraciones, smoke, perfil productivo, backup/restore, mensajería, outbox, inbound y dependency scan.

### Experiencia de operador

```text
commit final: 8d12f8ff772d3445440e4419b22d5c81b102cb15
workflow base: 9b1d6483864291a35cce3e3ad9a2932268353fdd
run: 30034176306
resultado: PASS
```

### Contactabilidad y paginación

```text
commit final: 19732dec9638cd47fee4b39ac41c5968693b5b7a
workflow base: 42d1574e7558f544e5894aef1a69a72a7e08da6f
run: 30036648327
resultado: PASS
```

Validaciones confirmadas en los cierres UX:

- Spotless;
- pruebas específicas backend;
- Maven Verify completo;
- npm ci;
- TypeScript typecheck;
- Vitest;
- build frontend;
- Docker Compose fail-closed;
- Playwright Chromium;
- repository safety;
- `git diff --check`;
- ausencia de diálogos nativos en `frontend/src`.

## Pendientes prioritarios

1. Corregir las métricas del dashboard que hoy calculan interés y bloqueo sobre la página cargada, no sobre el total tenant-scoped.
2. Eliminar `.github/remote-ux-trigger`, `.github/workflows/remote-ux-overhaul.yml` y los scripts `scripts/remote-ux-*.py`; están inertes por su guarda fija, pero son deuda técnica.
3. Ejecutar auditoría manual WCAG 2.1 AA con lector de pantalla, zoom, contraste y teclado completo.
4. Ejecutar pruebas de escala con volúmenes representativos y datos sintéticos autorizados.
5. Validar Firefox y WebKit.

Pendientes de prioridad media:

- paginación y filtros backend para importaciones de varios miles de filas;
- modularización gradual de `frontend/src/App.tsx`;
- posible drawer móvil si la navegación horizontal resulta insuficiente;
- skeletons más completos para operaciones lentas;
- revisión visual manual de contraste y estados disabled.

## Estado externo

```text
Gmail: IMPLEMENTED_NOT_CONNECTED
WhatsApp Cloud: IMPLEMENTED_NOT_CONNECTED
XLSX real: BLOCKED_EXTERNAL_FILE
validador Unix integral en host real: IMPLEMENTED_NOT_RUN
producción: NOT_AUTHORIZED / NOT_DEPLOYED
```

La sintaxis Bash, los jobs Unix de CI y los preflight contenedorizados sí pasaron. La ausencia de una corrida integral en un host Unix real no invalida las validaciones Windows y CI, pero sigue siendo evidencia pendiente.

## Fuentes

```text
docs/estado-integral-y-roadmap.md
docs/next-step.md
docs/backlog.md
docs/ux-operador.md
docs/execution/complete-crm-platform-plan.md
docs/execution/complete-crm-platform-progress.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
```
