# Estado actual

Actualizado: 2026-07-24

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

## Capacidades funcionales publicadas previamente

Permanecen las capacidades SEG-000–SEG-011 y los cierres UX/contactabilidad ya documentados en la historia: identidad, RBAC, tenant isolation, prospectos, contactos, importaciones, duplicados, actividades, tareas, oportunidades, campañas, mensajería simulada, outbox, inbound de prueba, reporting, auditoría, configuración y frontend operativo.

El nuevo candidato local que añadía métricas tenant-wide, paginación adicional, drawer, multibrowser y retorno de foco explícito no se declara publicado mientras no exista en el contenido remoto verificable.

## Próximo paso obligatorio

El siguiente cambio funcional debe ser pequeño e independiente:

1. ampliar el preflight de `scripts/validate-complete-crm.ps1` para comprobar también `ProductionFrontendPort`;
2. localizar y actualizar su equivalente Unix si corresponde;
3. añadir una regresión que falle temprano cuando ese puerto esté ocupado;
4. preservar la demo remota que publica `127.0.0.1:18080`;
5. validar el cambio sobre su nuevo SHA.

No repetir las suites ya cerradas para esta actualización exclusivamente documental.
