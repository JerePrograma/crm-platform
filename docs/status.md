# Estado actual

Actualizado: 2026-10-22

## SCALARIS-DEPLOY-001 — preparación productiva actual

El usuario autorizó cerrar los pendientes de preparación y eligió VPS Hostinger con `crm.scalaris.com.ar`. La fuente de entrada fue `2a22e4b`, sincronizada y limpia. Docker Desktop/motor Linux 29.8.2 ya están disponibles; «Docker no instalado» es un bloqueo histórico resuelto.

La entrega corrige dependencias/runtime Java 21, tareas paginadas por estado/responsable/prospecto, perfil productivo Scalaris/Marketing, proxy HTTPS/importaciones y selección/integridad de respaldos. Se amplían los gates de Marketing y escaneo backend/frontend. La evidencia y el estado de ejecución de cada control son [los de esta misión](validation/scalaris-production-readiness-2026-10-22.md); no se atribuye CI verde al código aún no publicado.

La instalación portable anterior, los contactos operativos y proveedores reales quedan fuera de las pruebas sintéticas. El historial siguiente mantiene su alcance original y no describe la entrega actual.

## Historial SCALARIS-001 — operación local Windows comprobada

La misión actual adapta el CRM al trabajo de Marketing de Scalaris: servicio técnico, desarrollo y venta a pedido. La referencia vigente es WhatsApp **11 7278-9289**, con cobertura en **Zona Oeste, CABA y Mar del Plata** y retiro/entrega a coordinar según el caso.

Se implementaron localmente el perfil y la organización Scalaris, cuenta de Marketing separada de administración, etiquetas/plantillas, agenda y referencias comerciales en español, importación con encabezados comerciales y dos caminos de apertura por doble clic: Docker y paquete Windows portable. La [guía de uso](scalaris-marketing-local.md) y [SCALARIS-001](segments/SCALARIS-001.md) describen la entrega.

```text
SCALARIS_STATE=LOCAL_OPERATIONAL_PASS
SOURCE_STATE=PUBLISHED_FAST_FORWARD
IMPLEMENTATION_COMMIT=331b3e7be5ad52b93b285a0ff9654abedce79b7d
BACKEND_UNIT=EXECUTED_PASS_62
BACKEND_FOCUSED_INTEGRATION=EXECUTED_PASS_6
BACKEND_TOTAL=EXECUTED_PASS_68
DOCKER_LAUNCHER_PS51=EXECUTED_PASS_55_ASSERTIONS
DOCKER_REAL=BLOCKED_EXTERNAL_NOT_INSTALLED
PORTABLE_WINDOWS=EXECUTED_PASS_18_ASSERTIONS_PS51
PORTABLE_RUNTIMES=JAVA_21_POSTGRESQL_18_NODE_24
FRONTEND_UNIT_TYPECHECK_BUILD=EXECUTED_PASS_16_UNIT
FRONTEND_AUDIT=EXECUTED_PASS_ZERO_VULNERABILITIES
FRONTEND_E2E=EXECUTED_PASS_4_OF_4_ZERO_SKIPPED_UNEXPECTED_FLAKY
SOURCE_FOR_DELIVERY=PUBLISHED_IMPLEMENTATION_DISTINCT_FROM_PRIOR_ZIP
FINAL_ZIP=EXECUTED_PASS_MANIFEST_CHECKSUM_PRIVATE_FILES_ZERO
USE_INSTALLATION=EXECUTED_PASS_LOOPBACK_8787_PROSPECTS_ZERO
USE_CATALOG=TAGS_18_TEMPLATES_7
QA_PROCESSES=STOPPED
MANUAL_VISUAL_QA=EXECUTED_PASS_1440_390_320_PX
CURRENT_COMPLETE_VALIDATOR=BLOCKED_EXTERNAL_DOCKER_NOT_INSTALLED
CURRENT_CI=FAILED_DEPENDENCY_SCAN
CURRENT_CI_RUN=38600419532
CURRENT_CI_FINAL=21_SUCCESS_1_FAILURE_FINAL
CURRENT_PUBLICATION=PUBLISHED_FAST_FORWARD
CURRENT_REMOTE_VERIFICATION=HEAD_ORIGIN_MAIN_LS_REMOTE_EQUAL_DIVERGENCE_0_0
DOCUMENTATION=POST_PUSH_RESULTS_RECORDED
REAL_CONTACTS=NOT_IMPORTED
REAL_COMMUNICATIONS=DISABLED_BY_POLICY
PRODUCTION=NOT_DEPLOYED
```

La validación local del backend incluye 62 pruebas unitarias y 6 de integración focalizada. Frontend aprobó 16 unitarias, typecheck, build y audit con cero vulnerabilidades. La corrida E2E conjunta final del 2026-10-14, 13:03:29 Argentina, aprobó 4/4 casos (flujo integral, responsive, Gmail desconectado y Marketing Scalaris), sin omitidos, inesperados ni flaky, en aproximadamente 24 segundos. La fuente local quedó congelada para la entrega. Estos controles no sustituyen el validador integral repetido ni la suite histórica completa Testcontainers.

El paquete Windows real aprobó 18 comprobaciones en PowerShell 5.1: primera apertura, claves privadas/ACL, Marketing, loopback, 18 etiquetas y 7 plantillas, persistencia de un prospecto ficticio tras reapertura, respaldo/checksum, recuperación en base nueva con ID/nombre conservados, existencia de la base anterior, cero estados enviados y parada conservando datos. Evidencia: `validation-output/scalaris-portable-latest.json`, ignorada por Git.

Los 55 controles del launcher Docker incluyen ACL y puertos reales, con Docker simulado para el respaldo. Este host no tiene Docker: Compose, imágenes, health y recuperación Docker reales siguen bloqueados. El manual se revisó visualmente a 1440/390/320 px; se corrigió un desborde de nombres de archivos en 320 px y se volvió a renderizar sin overflow.

El ZIP definitivo y su manifiesto/checksum están revisados, con 2.243 entradas y cero archivos privados. La instalación de uso está activa en `http://127.0.0.1:8787`, con perfil Scalaris y rol Marketing correctos, cero prospectos, 18 etiquetas y 7 plantillas, todos los servicios en loopback. Los entornos QA quedaron detenidos. La ruta y el checksum concretos se registran en la validación específica; evidencia local: `validation-output/scalaris-release-latest.json` y `validation-output/scalaris-installed-local.json`.

Los cambios de octubre no heredan la doble validación ni la CI de julio. La implementación [331b3e7](https://github.com/JerePrograma/crm-platform/commit/331b3e7be5ad52b93b285a0ff9654abedce79b7d) quedó `PUBLISHED_FAST_FORWARD`: 64 archivos, +4.126/-252; HEAD, `origin/main` y `ls-remote` coincidieron, divergencia `0 0`, y el working tree estaba limpio después del push antes de este cierre documental. La [CI 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532) avanzó de queued a un fallo real `FAILED_DEPENDENCY_SCAN`: [dependency-scan](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532/job/115977926787) terminó con Grype exit 2 por vulnerabilidades high/critical en librerías Java e imagen runtime. npm audit dio cero vulnerabilidades y Docker build pasó; no sustituyen el gate Grype. Resultado final: 21/22 jobs success; solo dependency-scan failure; frontend-e2e y Gmail fake Google SUCCESS. No se afirma PASS integral. Este cierre documental registra el resultado; autoridad: [publicación GitHub Scalaris](validation/scalaris-github-publication-2026-10-14.md).

La fuente publicada incorpora las correcciones finales de rol predeterminado y proxy, verificadas con 5/5 recorridos de navegador y 2/2 pruebas Node. El ZIP y la instalación anteriores mantienen su frontend/servidor, checksum y evidencia originales; no se atribuyen automáticamente al commit publicado. Agenda mantiene una limitación conocida: se cargan hasta 200 tareas antes de filtrar las pendientes, lo que puede ocultar seguimientos cuando hay muchas tareas cerradas; permanece sin corrección en el backlog.

## Historial: SEG-001 Gmail — validado y publicado el 2026-07-30

```text
base de publicación: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
recuperación Git y fast-forward: EXECUTED_PASS
integridad del working tree preservado: 68/68
backend clean verify: EXECUTED_PASS, 112/112
frontend ci/typecheck/unit/build: EXECUTED_PASS, 13/13
OAuth/Gmail/LIVE E2E con Google falso: EXECUTED_PASS
manual HTML/PDF/32 PNG/JSON/ZIP: EXECUTED_PASS
backup/restore/profile/dependency/secret scans: EXECUTED_PASS
validadores integrales limpios: 2/2 FUNCTIONAL_PASS
commit implementación: df2c79a77e27203f7eb63325f3e526cf38d1820a
commit integración del validador: d724b80a2d1eecbe2f4994366571ecd009342343
commit funcional validado y publicado: d6bfe64283cd3bab8dce441e4b01e348f9a55d5e
push: EXECUTED_PASS, fast-forward 9995b3e..d6bfe64
verificación remota: HEAD=origin/main=ls-remote, conteo 0 0
CI del SHA publicado: EXECUTED_PASS, run 30530515993, 22/22
Google real: IMPLEMENTED_NOT_CONNECTED
producción: NOT_DEPLOYED
```

El estado `EXECUTED_PASS` anterior se limita a pruebas locales sintéticas. No
implica conexión a Google, correo real ni despliegue; la publicación Git sí fue
verificada. El registro detallado está en
`docs/implementation/SEG-001-gmail-campaign-delivery.md`.

## Historial: repositorio canónico y hardening de julio

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

Este cierre documental registra la publicación y CI; continuar la remediación según `docs/next-step.md`. El gate real de dependencias falló; reparar dependencias Java e imagen en una misión posterior y repetir el scan, conservando este fallo como evidencia. No se cambiaron POM/runtime durante esta publicación. El uso/piloto local no autoriza importar contactos, comunicaciones reales ni producción. Docker local y el validador integral histórico permanecen bloqueados.
