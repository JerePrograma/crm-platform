# Estado actual

Actualizado: 2026-07-30

## SEG-001 Gmail — cierre previo a publicación

```text
base local/remota sincronizada: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
recuperación Git y fast-forward: EXECUTED_PASS
integridad del working tree preservado: 68/68
backend clean verify: EXECUTED_PASS, 112/112
frontend ci/typecheck/unit/build: EXECUTED_PASS, 13/13
OAuth/Gmail/LIVE E2E con Google falso: EXECUTED_PASS
manual HTML/PDF/32 PNG/JSON/ZIP: EXECUTED_PASS
backup/restore/profile/dependency/secret scans: EXECUTED_PASS
validadores integrales limpios: NOT_RUN
commit/push: NOT_RUN
Google real: IMPLEMENTED_NOT_CONNECTED
producción: NOT_DEPLOYED
```

El estado `EXECUTED_PASS` anterior se limita a pruebas locales sintéticas. No
implica conexión a Google, correo real, despliegue ni publicación Git. El
registro detallado está en
`docs/implementation/SEG-001-gmail-campaign-delivery.md`.

## Repositorio canónico

```text
repositorio: JerePrograma/crm-platform
rama única: main
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
hardening remoto del parser: FUNCTIONAL_PASS
corridas integrales consecutivas: 2/2 FUNCTIONAL_PASS
CI del SHA validado: NO_CHECKS_REPORTED
producción: NOT_AUTHORIZED / NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
proveedores reales: IMPLEMENTED_NOT_CONNECTED
XLSX real: OUTSIDE_GIT_CI_IMAGES
```

`main` es la única fuente de verdad. El cierre funcional se sustenta en salida estructurada local sobre el commit exacto, no únicamente en código versionado.

## Veredicto

El hardening del parser `.Config.Env` quedó funcionalmente cerrado sobre `0448c0e060311c284f4e4be4612982818a8480c4`.

La validación confirmó:

- PowerShell 5.1 y Node 22;
- sintaxis PowerShell y Bash;
- backend Maven Verify con 89/89 pruebas;
- frontend typecheck, 5/5 pruebas unitarias y build;
- Docker, health y smoke;
- migraciones V1–V13 y V11→V13;
- dependency scans;
- Playwright;
- siete guardas exactas de envío;
- cero estados `SENT|DELIVERED|READ`;
- backup/restore;
- perfil productivo local;
- repository safety y árbol final limpio.

El CRM continúa apto para demostración y evaluación interna segura, pero no está autorizado para producción real ni para conectar proveedores externos.

## Candidato post-hardening histórico

```text
tree documentado: 9e058d7044415b80af554ab8ae4fe3170585b1c9
estado remoto: NOT_AVAILABLE_AS_COMMIT_OR_BRANCH
estado de integración: NOT_INTEGRATED
```

No se localizaron objetos remotos verificables que permitan integrar ese candidato. No se reconstruyó por inferencia y no se usaron patches locales inaccesibles.

## Cambios versionados en esta consolidación

- `scripts/container-env-assertions.ps1`;
- `scripts/test-container-env-assertions.ps1`;
- `scripts/assert-container-env.js`;
- `scripts/test-container-env-assertions.js`;
- `scripts/validate-complete-crm.ps1`;
- `scripts/validate-complete-crm.sh`;
- `scripts/verify-production-profile.ps1`;
- `scripts/verify-production-profile.sh`.

Evidencia:

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## Causa raíz corregida

Los validadores canónicos trataban el JSON completo de:

```text
docker inspect <container> --format {{json .Config.Env}}
```

como un string y buscaban valores mediante regex o `grep`. La nueva implementación convierte el JSON en una colección real y exige coincidencia exacta.

Además, los validadores integrales todavía exigían `feat/complete-crm-platform`; ahora exigen `main`, en línea con `AGENTS.md`.

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
- habilitar envíos reales;
- conectar credenciales o proveedores;
- incorporar `.env`, secretos, ZIP de evidencia, logs o datos reales;
- incorporar `gestudio_lote_100_prospectos.xlsx`;
- debilitar RBAC, tenant isolation, CSRF, exclusiones o idempotencia.

## Validación actual

| Gate | Estado |
|---|---|
| inspección y comparación remota | EXECUTED_PASS |
| self-test Node del parser | EXECUTED_PASS |
| self-test PowerShell 5.1 | EXECUTED_PASS |
| sintaxis PowerShell y Bash | EXECUTED_PASS |
| backend Maven Verify | FUNCTIONAL_PASS — 89/89 |
| frontend typecheck/unit/build | FUNCTIONAL_PASS — 5/5 |
| Docker, health y smoke | FUNCTIONAL_PASS |
| migraciones | FUNCTIONAL_PASS |
| dependency scans | FUNCTIONAL_PASS |
| Playwright | FUNCTIONAL_PASS |
| `effectiveSendingBlockade` | FUNCTIONAL_PASS |
| `zeroSent` | FUNCTIONAL_PASS |
| backup/restore | FUNCTIONAL_PASS |
| `productionProfileSmoke` | FUNCTIONAL_PASS |
| repository safety | FUNCTIONAL_PASS |
| `git diff --check` | EXECUTED_PASS |
| `finalTreeClean` | FUNCTIONAL_PASS |
| validador integral corrida 1 | FUNCTIONAL_PASS |
| validador integral corrida 2 | FUNCTIONAL_PASS |
| CI del commit validado | NO_CHECKS_REPORTED |

Evidencia:

```text
complete-crm-20260724-201944.json
SHA-256 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527

complete-crm-20260724-202955.json
SHA-256 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

## VAL-002 — Preflight de `ProductionFrontendPort`

Estado: `FUNCTIONAL_PASS_FOCUSED`

Cambios:

- `scripts/check-host-ports.ps1` acepta un cuarto puerto opcional;
- `scripts/validate-complete-crm.ps1` proporciona `ProductionFrontendPort` durante `tooling`;
- `scripts/check-host-ports.js` implementa el chequeo equivalente para Unix;
- `scripts/validate-complete-crm.sh` acepta `--production-frontend-port`;
- el smoke productivo Unix deja de fijar `18080`;
- se agregaron regresiones PowerShell y Node.

Validación ejecutada:

```text
PowerShell syntax: EXECUTED_PASS
PowerShell free/duplicate/occupied port tests: EXECUTED_PASS
Node free/duplicate/occupied/Docker publication tests: EXECUTED_PASS
Bash syntax: EXECUTED_PASS
demo collision detection: EXECUTED_PASS id=d0420be3a84d
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
full suites repeated: false
```

La prueba con la demo solo consulta su publicación Docker. No ejecuta `stop`, `down`, `rm` ni modifica su configuración.

## UX-003 — Automatización UX remota histórica

Estado: `COMPLETE_WITH_FOCUSED_VALIDATION`

Archivos eliminados:

```text
.github/remote-ux-trigger
.github/workflows/remote-ux-overhaul.yml
scripts/remote-ux-preflight.py
scripts/remote-ux-overhaul.py
scripts/remote-ux-postfix.py
```

Criterios verificados:

- trigger exclusivo;
- guarda histórica obsoleta;
- cero consumidores operativos externos;
- cero dependencias desde otros workflows;
- eliminación limitada a los cinco archivos;
- repository safety aprobado;
- `git diff --check` aprobado;
- suites funcionales no repetidas por no existir cambios funcionales.

Evidencia: `docs/validation/ux-003-remote-automation-cleanup-2026-07-26.md`.

## UX-004 — Métricas tenant-wide del dashboard

Estado: `FUNCTIONAL_PASS`

Implementación:

- `ProspectOperationsService.dashboardMetrics()`;
- `ProspectController.metrics()`;
- `GET /api/v1/prospects/metrics`;
- `getProspectDashboardMetrics()` en frontend;
- consumo desde `App.refresh()` sin usar la página cargada.

Validación:

```text
página sintética: 100 de 105
conteo interesado fuera de página: EXECUTED_PASS
conteo bloqueado fuera de página: EXECUTED_PASS
PROSPECT_READ para VIEWER: EXECUTED_PASS
tenant isolation: EXECUTED_PASS
backend focused tests: EXECUTED_PASS
backend Maven Verify: EXECUTED_PASS
frontend API test: EXECUTED_PASS
frontend typecheck: EXECUTED_PASS
frontend unit: EXECUTED_PASS
frontend build: EXECUTED_PASS
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
demo preserved: EXECUTED_PASS id=d0420be3a84d
```

No se añadieron migraciones ni dependencias.

Evidencia: `docs/validation/ux-004-tenant-wide-dashboard-metrics-2026-07-26.md`.

## Capacidades funcionales publicadas previamente

Permanecen las capacidades SEG-000–SEG-011 y los cierres UX/contactabilidad ya documentados en la historia: identidad, RBAC, tenant isolation, prospectos, contactos, importaciones, duplicados, actividades, tareas, oportunidades, campañas, mensajería simulada, outbox, inbound de prueba, reporting, auditoría, configuración y frontend operativo.

El nuevo candidato local que añadía métricas tenant-wide, paginación adicional, drawer, multibrowser y retorno de foco explícito no se declara publicado mientras no exista en el contenido remoto verificable.

## Próximo paso obligatorio

Cerrar SEG-001 Gmail: validación integral y scans, revisión final, stage
explícito, commit, dos corridas limpias sobre el SHA definitivo, fetch final y
push fast-forward. No conectar Google real ni habilitar flags de envío/red.
