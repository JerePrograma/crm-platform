# Backlog ejecutable

Actualizado: 2026-10-14

Rama canónica: `main`.

Un elemento solo puede declararse `COMPLETE` con evidencia ejecutada sobre el contenido remoto exacto. Código versionado, documentación o un tree local no publicado no equivalen a cierre.

## SCALARIS-001 — Marketing Scalaris y entrega local Windows

Estado: `LOCAL_OPERATIONAL_PASS`; implementación `PUBLISHED_FAST_FORWARD` en `331b3e7be5ad52b93b285a0ff9654abedce79b7d`, CI `FAILED_DEPENDENCY_SCAN`. Paquete local previo revisado e instalación de uso preparada; no actualizados automáticamente al nuevo SHA.

- [x] Perfil y organización Scalaris separados de la compatibilidad Gestudio.
- [x] Cuenta Marketing con permisos comerciales y administración separada.
- [x] Referencias comerciales, etiquetas y plantillas para servicio técnico, desarrollo y venta a pedido.
- [x] Agenda, lenguaje de personas/negocios y ayuda para Marketing implementados.
- [x] Iniciadores Docker por doble clic, configuración privada, datos persistentes y respaldo implementados.
- [x] Regresión launcher PowerShell 5.1: 55 aserciones; respaldo con Docker simulado.
- [x] Pruebas unitarias backend con Java 21: 62 aprobadas.
- [x] Iniciador y empaquetador portable Windows implementados con Java 21/PostgreSQL 18/Node 24.
- [x] Backend: 62 unitarias y 6 de integración focalizada; frontend: 16 unitarias, typecheck/build/audit sin vulnerabilidades.
- [x] Corrida E2E conjunta 4/4 aprobada, sin omitidos, inesperados ni flaky; fuente local congelada.
- [x] Apertura, roles, loopback, etiquetas/plantillas y persistencia portable real: 18 comprobaciones PowerShell 5.1.
- [x] Respaldo con checksum y recuperación en base nueva, con la anterior conservada y cero estados enviados.
- [x] Manual revisado visualmente a 1440/390/320 px, sin desborde tras corregir nombres de archivos en móvil estrecho.
- [x] ZIP final, manifiesto y checksum revisados: 2.243 entradas, cero archivos privados.
- [x] Validación y límites de la entrega local reconciliados.
- [x] Instalación de uso activa en loopback 8787, cero prospectos y catálogo Scalaris; QA detenido.
- [x] Add, revisión de diff, commit y push autorizados explícitamente por el usuario.
- [x] Verificar commit/push y coincidencia HEAD/origin/main/ls-remote; `331b3e7`, divergencia `0 0`.
- [x] Registrar fallo real de [dependency-scan](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532/job/115977926787): Grype exit 2, high/critical Java/runtime.
- [x] Registrar resultado final de [CI 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532); snapshot 21/22 jobs success; solo dependency-scan failure, sin PASS integral.
- [x] Registrar resultados post-push en este cierre documental.
- [ ] Ejecutar Docker/Compose/health/respaldo reales cuando haya Docker disponible (`BLOCKED_EXTERNAL` en este host).
- [ ] Ejecutar cierre integral repetido sobre un commit definitivo si se autoriza esa misión; no realizado en esta edición.

El siguiente paso de negocio es uso/piloto local, con publicación y resultado final de CI registrados. La publicación de implementación ya está confirmada; la autoridad es [publicación GitHub Scalaris](validation/scalaris-github-publication-2026-10-14.md). Carga de contactos reales, conexiones, PR y producción permanecen fuera de alcance. La guía es `docs/scalaris-marketing-local.md` y el segmento es `docs/segments/SCALARIS-001.md`.

## SCALARIS-SEC-001 — gate Grype de Java e imagen runtime

Estado: `EXECUTED_FAIL_REMEDIATION_PENDING`. Fallo real de [dependency-scan 115977926787](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532/job/115977926787), sobre `331b3e7`: Grype exit 2 por hallazgos high/critical. npm audit cero y build Docker PASS no reemplazan ese gate.

- [ ] Revisar Spring Web MVC 7.0.8 → 7.0.9 y Tomcat 11.0.22 → 11.0.25 según los hallazgos del job, conservando coherencia con el BOM de Spring Boot.
- [ ] Revisar Jackson databind/core 2.21.5 y 3.1.5 → 2.21.7 y 3.1.7 conforme al scan y los contratos actuales.
- [ ] Actualizar y revisar el digest runtime Chainguard por hallazgos en libcrypto, glibc y zlib.
- [ ] Ejecutar pruebas pertinentes, construir la imagen corregida y repetir Grype con evidencia nueva del SHA/digest; conservar el fallo original.

POM y Dockerfile son idénticos a la base `75f37a023a0e69d991fe7640f6ed24df98d6ca18`; los hallazgos son heredados y siguen sin remediar. Este elemento registra una reparación pendiente; no declara PASS ni bloqueo externo del scanner.

## SCALARIS-AGENDA-001 — pendientes fuera de las primeras 200 tareas

Estado: `KNOWN_LIMITATION_NOT_FIXED`; observación registrada durante la publicación, sin cambio de producto.

La pantalla carga `listTasks()` sin filtro de estado. `GET /api/v1/tasks` aplica por defecto `limit=200`, ordenado por vencimiento, y después `MarketingAgenda` filtra `OPEN`/`IN_PROGRESS` con `pendingTasks()`. Muchas tareas cerradas dentro de esas primeras 200 pueden desplazar seguimientos pendientes del lote cargado y mostrar una agenda incompleta o vacía aunque existan pendientes.

- [ ] Aplicar filtros de pendientes y responsable antes del límite en backend, con paginación o un contrato equivalente completo.
- [ ] Agregar regresión con más de 200 tareas y mayoría cerradas, comprobando que los pendientes sigan visibles.
- [ ] Reconciliar conteos y estados vacíos de la agenda con el resultado completo.

No se corrigió en `331b3e7`; esta observación no autoriza una modificación automática del producto. Referencias: `TimelineController.listTasks`, `TimelineService.listTasks`, `App.refresh`, `MarketingAgenda` y `pendingTasks`.

## Historial publicado — hasta 2026-07-30

Los cierres siguientes conservan su evidencia sobre los commits originales. No cubren los cambios Scalaris locales de octubre; la referencia histórica final es `d6bfe64283cd3bab8dce441e4b01e348f9a55d5e`, doble validación y CI `30530515993`, 22/22.

## SEG-001-GMAIL — OAuth y campañas LIVE controladas

Estado: `COMPLETE_PUBLISHED`

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
- [x] dos validaciones integrales limpias sobre `d6bfe642`;
- [x] secret scan, diff, stage explícito y commits locales revisados;
- [x] gate repetido sobre el commit de CI `d6bfe642`;
- [x] fetch final y push fast-forward `9995b3e..d6bfe64`;
- [x] remoto verificado con `HEAD`, `origin/main` y `ls-remote` coincidentes;
- [x] CI del SHA publicado: run `30530515993`, 22/22 jobs verdes.

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
