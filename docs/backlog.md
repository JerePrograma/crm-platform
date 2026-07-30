# Backlog ejecutable

Actualizado: 2026-07-30

Rama canónica: `main`.

Un elemento solo puede declararse `COMPLETE` con evidencia ejecutada sobre el contenido remoto exacto. Código versionado, documentación o un tree local no publicado no equivalen a cierre.

## SEG-001-GMAIL — OAuth y campañas LIVE controladas

Estado: `LOCAL_FUNCTIONAL_PASS_PENDING_PUSH`

- [x] recuperación Git con snapshot externo e integridad 68/68;
- [x] V14 aditiva y compatibilidad V1/V11/V13;
- [x] OAuth offline, AES-256-GCM, reconnect, revoke y tenant isolation;
- [x] Gmail REST/MIME, resultados retry/permanent/ambiguous;
- [x] campaña LIVE por outbox, límites, ventanas, ledger y kill switch;
- [x] unsubscribe idempotente y cancelación de pendientes/seguimientos;
- [x] frontend conjunto y Playwright contra Google falso;
- [x] manual HTML/PDF/32 PNG/JSON/ZIP sintético;
- [x] backend 112/112 y frontend 13/13;
- [x] migraciones, backup/restore, perfil productivo y dependency/secret scans;
- [x] dos validaciones integrales limpias sobre `d724b80`;
- [x] secret scan, diff, stage explícito y commits locales revisados;
- [ ] repetir el gate sobre el commit documental definitivo;
- [ ] fetch final y push fast-forward.

Google real, credenciales reales, destinatarios reales y producción permanecen
fuera de esta ejecución.

## Baseline publicado

Los segmentos SEG-000–SEG-011, la experiencia de operador y la sincronización de contactabilidad/paginación mantienen sus cierres históricos publicados hasta `83e181ce614f145bbfe141cc7603c3042569be51`.

El commit `f25051884b7aadd5837286dedd9ae0eee899cb5a` añadió únicamente continuidad documental.

## VAL-001 — Parser exacto de `.Config.Env`

Estado: `COMPLETE_WITH_CI_NO_CHECKS_REPORTED`

Implementado y validado:

- [x] parser PowerShell fail-closed;
- [x] parser Node para Unix;
- [x] membresía exacta de siete guardas;
- [x] rechazo de JSON vacío, inválido o con raíz no-array;
- [x] cobertura de guardas completas, faltantes, inseguras, JSON inválido y líneas vacías;
- [x] integración en validadores Windows y Unix;
- [x] validadores alineados con rama `main`;
- [x] self-test PowerShell 5.1;
- [x] self-test Node 22;
- [x] sintaxis PowerShell y Bash;
- [x] `productionProfileSmoke`;
- [x] bloqueo efectivo de envíos;
- [x] cero estados enviados;
- [x] repository safety;
- [x] `git diff --check`;
- [x] `finalTreeClean`;
- [x] corrida integral 1;
- [x] corrida integral 2 sobre el mismo commit;
- [x] estado de CI consultado: `NO_CHECKS_REPORTED`.

Evidencia:

```text
commit: 0448c0e060311c284f4e4be4612982818a8480c4
run 1: complete-crm-20260724-201944.json
run 1 SHA-256: 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527
run 2: complete-crm-20260724-202955.json
run 2 SHA-256: 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
detalle: docs/validation/main-hardening-functional-closure-2026-07-24.md
```

## VAL-002 — Preflight de `ProductionFrontendPort`

Estado: `COMPLETE_WITH_FOCUSED_VALIDATION`

Implementado:

- [x] inspección de `scripts/check-host-ports.ps1`, validadores y consumidores;
- [x] parámetro opcional `ProductionFrontendPort` en Windows;
- [x] compatibilidad con consumidores existentes de tres puertos;
- [x] comprobación durante `tooling`, antes de suites costosas;
- [x] checker Node para Unix;
- [x] opción Unix `--production-frontend-port`;
- [x] puerto transmitido al smoke productivo Unix;
- [x] cobertura de puerto libre;
- [x] cobertura de puerto duplicado;
- [x] cobertura de listener ocupado;
- [x] cobertura de publicación Docker;
- [x] preservación de la demo autorizada;
- [x] sintaxis PowerShell, Node y Bash;
- [x] repository safety;
- [x] `git diff --check`;
- [x] validación focalizada sin repetir suites no afectadas.

Resultado observable:

```text
un ProductionFrontendPort ocupado falla durante tooling/preflight
no se inicia Maven, npm, Docker build, migraciones ni E2E
un puerto libre permite continuar
la demo remota no se detiene ni se modifica
```

Evidencia:

```text
docs/validation/production-frontend-port-preflight-2026-07-24.md
```

CI del commit final: `PENDING_POST_PUSH_VERIFICATION`.

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

Estado: `COMPLETE_WITH_FOCUSED_VALIDATION`

Inventario eliminado:

- [x] `.github/remote-ux-trigger`;
- [x] `.github/workflows/remote-ux-overhaul.yml`;
- [x] `scripts/remote-ux-preflight.py`;
- [x] `scripts/remote-ux-overhaul.py`;
- [x] `scripts/remote-ux-postfix.py`.

Verificaciones:

- [x] trigger exclusivo del workflow;
- [x] guarda histórica fija identificada;
- [x] guarda incompatible con la historia actual de `main`;
- [x] cero consumidores operativos externos;
- [x] cero dependencias desde otros workflows;
- [x] el workflow declaraba su propia eliminación;
- [x] repository safety;
- [x] `git diff --check`;
- [x] alcance limitado;
- [x] sin repetición de suites funcionales no afectadas.

Evidencia:

```text
docs/validation/ux-003-remote-automation-cleanup-2026-07-26.md
```

CI del commit final: `PENDING_POST_PUSH_VERIFICATION`.

## UX-004 — Métricas tenant-wide del dashboard

Estado: `COMPLETE_WITH_FUNCTIONAL_VALIDATION`

Implementado:

- [x] localizado el cálculo basado en la página frontend;
- [x] agregado tenant-scoped en PostgreSQL;
- [x] endpoint compatible protegido por `PROSPECT_READ`;
- [x] sin carga de páginas adicionales;
- [x] consumidor frontend actualizado;
- [x] prueba con 105 prospectos y página de 100;
- [x] semántica de cinco estados de interés preservada;
- [x] bloqueo por `NOT contact_eligible`;
- [x] acceso `VIEWER`;
- [x] tenant isolation;
- [x] pruebas backend focalizadas;
- [x] Maven Verify completo;
- [x] prueba API frontend;
- [x] typecheck, unit tests y build;
- [x] repository safety y `git diff --check`.

Evidencia:

```text
docs/validation/ux-004-tenant-wide-dashboard-metrics-2026-07-26.md
```

CI del commit final: `PENDING_POST_PUSH_VERIFICATION`.

## UX-006 — Importaciones de gran volumen

Estado: `COMPLETE_WITH_FUNCTIONAL_VALIDATION`

- [x] paginación backend de resultados;
- [x] filtros backend por hoja y resultado;
- [x] búsqueda backend y límites explícitos;
- [x] aislamiento e idempotencia;
- [x] consumidor frontend paginado;
- [x] evidencia versionada en `12421c53375deabebe8f48f17af3ae95af95893b`.

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
| EXT-004 | IMPLEMENTED_NOT_CONNECTED | Gmail de campañas implementado; Google real, SMTP y WhatsApp reales no conectados |

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
