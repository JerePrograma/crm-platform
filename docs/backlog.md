# Backlog ejecutable

Actualizado: 2026-07-24

Rama canónica: `main`.

Un elemento solo puede declararse `COMPLETE` con evidencia ejecutada sobre el contenido remoto exacto. Código versionado, documentación o un tree local no publicado no equivalen a cierre.

## Baseline publicado

Los segmentos SEG-000–SEG-011, la experiencia de operador y la sincronización de contactabilidad/paginación mantienen sus cierres históricos publicados hasta `83e181ce614f145bbfe141cc7603c3042569be51`.

El commit `f25051884b7aadd5837286dedd9ae0eee899cb5a` añadió únicamente continuidad documental.

## VAL-001 — Parser exacto de `.Config.Env`

Estado: `IMPLEMENTED_NOT_FULLY_VALIDATED`

Implementado:

- [x] parser PowerShell fail-closed;
- [x] parser Node para Unix;
- [x] membresía exacta de siete guardas;
- [x] rechazo de JSON vacío, inválido o con raíz no-array;
- [x] prueba con guardas completas;
- [x] prueba con `SENDING_ENABLED=false` ausente;
- [x] prueba con `SENDING_ENABLED=true`;
- [x] prueba con JSON inválido;
- [x] prueba con líneas vacías;
- [x] integración en validadores Windows y Unix;
- [x] validadores alineados con rama `main`;
- [x] self-test Node ejecutado en Node 22;
- [ ] self-test PowerShell 5.1 ejecutado;
- [ ] `productionProfileSmoke` ejecutado;
- [ ] `finalTreeClean` ejecutado;
- [ ] corrida integral 1;
- [ ] corrida integral 2 sobre el mismo commit;
- [ ] CI del SHA exacto comprobado.

Criterio de aceptación:

```text
productionProfileSmoke=FUNCTIONAL_PASS
finalTreeClean=FUNCTIONAL_PASS
summary.status=FUNCTIONAL_PASS
dos corridas consecutivas sobre el mismo commit
```

Evidencia:

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## REC-001 — Recuperación del candidato histórico

Estado: `BLOCKED_EXTERNAL_PATCHES`

Referencia:

```text
tree final histórico: 9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Bloqueo:

- el tree no está publicado como commit o rama accesible;
- los cuatro patches y su manifiesto SHA-256 no están disponibles remotamente;
- no puede comprobarse equivalencia de contenido.

Tareas:

- [ ] localizar patches fuera de GitHub;
- [ ] verificar hashes contra `SHA256SUMS.txt` o manifiesto equivalente;
- [ ] aplicar `git apply --check` sobre el `main` actual en un clon temporal;
- [ ] revisar cada diff por ruta y símbolo;
- [ ] integrar únicamente cambios verificables;
- [ ] preservar `docs/continuity/`;
- [ ] validar dos veces el commit final.

No reconstruir el candidato por descripción documental.

## UX-003 — Limpieza de automatización remota obsoleta

Estado: `READY_AFTER_VAL_001`

- [ ] verificar que `.github/remote-ux-trigger`, el workflow remoto y los scripts `remote-ux-*` siguen presentes;
- [ ] confirmar que ningún workflow canónico depende de ellos;
- [ ] eliminar solo esos archivos;
- [ ] ejecutar repository safety y `git diff --check`;
- [ ] documentar el resultado.

## UX-004 — Métricas tenant-wide del dashboard

Estado: `READY_AFTER_VAL_001`

Problema publicado: los conteos de interés y bloqueo pueden depender de la página de prospectos cargada.

- [ ] localizar endpoint/reporte agregado real;
- [ ] exponer conteos tenant-scoped compatibles;
- [ ] no cargar todas las páginas;
- [ ] actualizar consumidor frontend;
- [ ] añadir pruebas backend y frontend con más de una página;
- [ ] validar RBAC y tenant isolation.

## UX-006 — Importaciones de gran volumen

Estado: `PLANNED`

- [ ] paginación backend de resultados;
- [ ] filtros backend por hoja y resultado;
- [ ] búsqueda backend;
- [ ] límites explícitos;
- [ ] pruebas de aislamiento e idempotencia.

## OPS-001 — Outbox e inbound paginados

Estado: `PLANNED`

- [ ] confirmar APIs actuales;
- [ ] agregar paginación/filtros compatibles donde falten;
- [ ] preservar at-least-once, replay protection y tenant isolation;
- [ ] actualizar frontend y pruebas.

## UX-007 — Navegadores, foco y móvil

Estado: `PLANNED`

- [ ] ejecutar Chromium, Firefox y WebKit;
- [ ] validar retorno de foco mediante disparador explícito;
- [ ] corregir solo defectos reproducibles;
- [ ] evaluar drawer móvil con evidencia responsive;
- [ ] ejecutar auditoría manual WCAG 2.1 AA.

## TECH-001 — Modularización gradual

Estado: `PLANNED`

- [ ] inventariar responsabilidades reales de `frontend/src/App.tsx`;
- [ ] extraer por módulo sin reescritura;
- [ ] conservar contratos, rutas y pruebas;
- [ ] evitar dependencias nuevas y reformateos masivos.

## PERF-001 — Escala representativa

Estado: `BLOCKED_AUTHORIZED_DATASET`

- [ ] definir volúmenes objetivo;
- [ ] generar datos sintéticos equivalentes;
- [ ] medir búsquedas, reportes, importaciones y duplicados;
- [ ] detectar N+1 y revisar índices;
- [ ] documentar umbrales.

## Pendientes externos

| ID | Estado | Descripción |
|---|---|---|
| EXT-001 | IMPLEMENTED_NOT_RUN | validador integral Unix en host real |
| EXT-002 | BLOCKED_EXTERNAL_FILE | evaluación autorizada del XLSX real fuera de Git/CI |
| EXT-003 | NOT_AUTHORIZED | infraestructura y despliegue productivo |
| EXT-004 | IMPLEMENTED_NOT_CONNECTED | Gmail/SMTP/WhatsApp reales |

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

No versionar `.env`, XLSX, ZIP, logs, `validation-output/`, secretos ni datos reales. No desplegar ni habilitar red real desde tareas de validación, UX, mantenimiento o documentación.
