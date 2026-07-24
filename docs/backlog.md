# Backlog ejecutable

Actualizado: 2026-07-23

Rama canónica: `main`.

Fuente de estado y contexto:

```text
docs/estado-integral-y-roadmap.md
```

Un elemento solo puede declararse `COMPLETE` con evidencia ejecutada. Código versionado sin pruebas no equivale a cierre.

## Resumen de segmentos cerrados

| ID | Segmento | Estado | Resultado verificable |
|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | fuente canónica, reglas y documentación |
| SEG-001 | Vertical slice persistente | COMPLETE | importación, exclusiones, UI, stack y CI |
| SEG-002 | Identidad, usuarios y RBAC | COMPLETE | sesión, permisos y tenant isolation |
| SEG-003 | Prospectos y contactos | COMPLETE | CRUD, búsqueda, paginación y ciclo comercial |
| SEG-004 | Actividades y tareas | COMPLETE | seguimiento y timeline trazable |
| SEG-005 | Duplicados | COMPLETE | revisión y merge transaccional |
| SEG-006 | Oportunidades | COMPLETE | pipeline, forecast y aging |
| SEG-007 | Campañas | COMPLETE | audiencias, plantillas y simulación |
| SEG-008 | Mensajería segura | COMPLETE | no-op, fake, manual y red bloqueada |
| SEG-009 | Outbox e inbound | COMPLETE | async idempotente e inbound firmado |
| SEG-010 | Reportes y producción local | COMPLETE | reportes, seguridad, observabilidad y perfil endurecido |
| SEG-011 | Validación integral | COMPLETE | dos recorridos reproducibles y evidencia |
| UX-001 | Experiencia de operador | COMPLETE | español, orientación, diálogos, accesibilidad y responsive |
| UX-002 | Contactabilidad y paginación | COMPLETE | reglas coherentes y paginación real |

## Capacidades completas

### Plataforma y seguridad

- [x] Java 21 y Spring Boot 4.1;
- [x] PostgreSQL 17 y Flyway V1–V13;
- [x] Hibernate validate;
- [x] sesión cookie HttpOnly same-origin y CSRF;
- [x] roles, permisos y aislamiento por organización;
- [x] auditoría JSONB y correlation ID;
- [x] métricas y health probes;
- [x] Docker Compose y perfil productivo local;
- [x] backup/restore sintético;
- [x] CI backend, frontend, migraciones, smoke y seguridad.

### CRM operativo

- [x] instituciones, prospectos, contactos y canales;
- [x] exclusiones dominantes;
- [x] búsqueda por institución, ubicación, contacto, correo, teléfono y etiquetas;
- [x] filtros, orden, paginación y CSV seguro;
- [x] notas, actividades, tareas y timeline;
- [x] ciclo comercial e historial;
- [x] oportunidades, pipeline, forecast y aging;
- [x] campañas, plantillas, audiencias y simulación;
- [x] outbox, retry, dead-letter y requeue;
- [x] inbound firmado, replay protection, cuarentena y asociación;
- [x] reportes, settings, usuarios y etiquetas.

### Importaciones y duplicados

- [x] CSV y XLSX;
- [x] vista previa obligatoria;
- [x] ejecución confirmada;
- [x] idempotencia y evidencia por fila;
- [x] resultados aceptados, excluidos, rechazados, duplicados y a revisión;
- [x] comparación de duplicados;
- [x] vinculación, creación separada, no duplicado, merge, descarte y defer;
- [x] conservación segura de correo, teléfono/WhatsApp, ubicación, sitio, fuente y evidencia;
- [x] extracción limitada a campos conocidos;
- [x] auditoría y aislamiento por organización.

### Experiencia de operador

- [x] interfaz principal en español;
- [x] diccionario centralizado de etiquetas;
- [x] errores y éxitos orientativos;
- [x] estados vacíos y datos faltantes explícitos;
- [x] formularios con labels y ayudas;
- [x] tablas con filtros, conteos y paginación;
- [x] ficha de prospecto con revelado progresivo;
- [x] auditoría sin JSON crudo como contenido principal;
- [x] importaciones resumidas y navegables;
- [x] duplicados con diferencias y consecuencias;
- [x] diálogos accesibles sin `window.prompt` ni `window.confirm`;
- [x] foco visible, teclado, Escape y retorno de foco;
- [x] responsive probado en móvil;
- [x] contactabilidad recalculada ante cambios de canales;
- [x] paginación real de prospectos con URL persistente.

## Backlog activo

No existe un segmento funcional activo. El backlog siguiente es post-cierre y debe ejecutarse de forma incremental.

### UX-003 — Limpieza de automatización remota

Estado: `READY`

Alcance:

- [ ] eliminar `.github/remote-ux-trigger`;
- [ ] eliminar `.github/workflows/remote-ux-overhaul.yml`;
- [ ] eliminar `scripts/remote-ux-preflight.py`;
- [ ] eliminar `scripts/remote-ux-overhaul.py`;
- [ ] eliminar `scripts/remote-ux-postfix.py`;
- [ ] confirmar que la CI canónica no depende de esos archivos;
- [ ] ejecutar repository safety;
- [ ] ejecutar `git diff --check`;
- [ ] documentar la limpieza.

Condición de aceptación:

```text
no quedan scripts de aplicación remota obsoletos
no cambia código funcional
CI canónica permanece intacta
```

### UX-004 — Métricas globales del dashboard

Estado: `READY`

Problema:

Las métricas de interés y bloqueo se calculan sobre la página de prospectos cargada en el frontend.

Alcance:

- [ ] localizar el reporte o endpoint agregado existente;
- [ ] exponer conteos tenant-scoped sin romper contratos;
- [ ] evitar cargar todas las páginas;
- [ ] actualizar dashboard;
- [ ] añadir pruebas con más de una página;
- [ ] validar permisos y aislamiento;
- [ ] ejecutar backend, frontend y E2E.

Criterio de aceptación:

```text
las métricas no cambian al navegar entre páginas
los conteos representan el total de la organización
```

### UX-005 — Auditoría manual WCAG 2.1 AA

Estado: `BLOCKED_MANUAL_VALIDATION`

- [ ] teclado completo;
- [ ] lector de pantalla;
- [ ] zoom 200 %;
- [ ] contraste medido;
- [ ] mensajes dinámicos;
- [ ] foco en diálogos;
- [ ] navegación móvil;
- [ ] registro de defectos y correcciones.

### PERF-001 — Escala representativa

Estado: `BLOCKED_AUTHORIZED_DATASET`

- [ ] definir volúmenes objetivo;
- [ ] generar datos sintéticos equivalentes;
- [ ] medir búsquedas y paginación;
- [ ] medir importaciones y duplicados;
- [ ] medir auditoría y reportes;
- [ ] revisar índices y consultas N+1;
- [ ] documentar umbrales.

### UX-006 — Importaciones de gran volumen

Estado: `PLANNED`

- [ ] paginación backend de filas importadas;
- [ ] filtros backend por hoja y resultado;
- [ ] búsqueda backend;
- [ ] conservar resumen y revelado progresivo;
- [ ] validar aislamiento e idempotencia.

### UX-007 — Navegadores y navegación móvil

Estado: `PLANNED`

- [ ] Playwright Firefox;
- [ ] Playwright WebKit;
- [ ] evaluar drawer móvil;
- [ ] comprobar scroll y modales;
- [ ] corregir únicamente defectos observables.

### TECH-001 — Modularización gradual del frontend

Estado: `PLANNED`

- [ ] inventariar componentes dentro de `frontend/src/App.tsx`;
- [ ] extraer por módulo sin cambiar comportamiento;
- [ ] evitar cambio de framework;
- [ ] mantener pruebas E2E;
- [ ] no realizar reformateo masivo.

## Pendientes externos

### EXT-001 — Validador Unix integral en host real

Estado: `IMPLEMENTED_NOT_RUN`

La sintaxis Bash, CI Unix y preflight contenedorizado pasaron. Falta una ejecución integral en un host Unix real.

### EXT-002 — XLSX real

Estado: `BLOCKED_EXTERNAL_FILE`

No incorporar el archivo a Git, CI, imágenes ni documentación. Solo podrá evaluarse desde un entorno autorizado y con datos tratados según la política definida.

### EXT-003 — Producción

Estado: `NOT_AUTHORIZED`

- [ ] infraestructura objetivo;
- [ ] secretos administrados;
- [ ] backup/restore y rollback;
- [ ] observabilidad y alertas;
- [ ] revisión de privacidad;
- [ ] pruebas de carga;
- [ ] autorización formal.

### EXT-004 — Proveedores reales

Estado: `IMPLEMENTED_NOT_CONNECTED`

- [ ] revisión separada de Gmail/SMTP;
- [ ] revisión separada de WhatsApp Cloud;
- [ ] límites y rate limiting;
- [ ] idempotencia con proveedor;
- [ ] kill switch probado;
- [ ] autorización de comunicaciones.

## Restricciones permanentes

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No versionar `.env`, `validation-output/`, secretos, credenciales o datos reales. No desplegar ni habilitar red real dentro de tareas de UX, mantenimiento o documentación.

## Evidencia

```text
docs/status.md
docs/estado-integral-y-roadmap.md
docs/next-step.md
docs/ux-operador.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
```
