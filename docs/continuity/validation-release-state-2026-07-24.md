# Estado de validación y publicación — 2026-07-24

## Estado ejecutivo

```text
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
hardening parser .Config.Env: FUNCTIONAL_PASS
corridas integrales consecutivas: 2/2 FUNCTIONAL_PASS
CI del SHA validado: NO_CHECKS_REPORTED
candidato post-hardening local: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
producción real: NO DESPLEGADA
envíos reales: DESHABILITADOS
```

## Verificación del remoto

La comparación `83e181c...main` previa a los cambios devolvió:

```text
status: ahead
ahead_by: 1
behind_by: 0
commit adicional: f25051884b7aadd5837286dedd9ae0eee899cb5a
```

Ese commit modificaba únicamente `AGENTS.md` y agregaba `docs/continuity/`.

## Candidato histórico

```text
v6 candidate tree: e3a9728e717b7c8a4d92f9fab31f709bf5d66464
+ locators E2E: 24df4c7f26ffde0f044f681f9130fa254f15debd
+ primera restauración de foco: fa8c15172dfa9a0cfa5cbd00f7aab42733d516ba
+ disparador de foco explícito: 9e058d7044415b80af554ab8ae4fe3170585b1c9
```

No se localizaron commits, ramas o PRs remotos correspondientes. Los patches y su manifiesto no están disponibles en GitHub. Estado correcto:

```text
REMOTE_OBJECT=NOT_FOUND
INTEGRATION=NOT_PERFORMED
```

## Evidencia histórica del fallo

```text
archivo: gestudio-runtime-resume-evidence-9e058d704441-20260724-124206.zip
SHA-256: C70E6105E0D0AFA0A902BBAC2F1F7E1B0DD646F2B9406391FC405249328908ED
status: EXECUTED_FAIL
productionProfileSmoke: EXECUTED_FAIL
finalTreeClean: NOT_RUN
checkoutModified: false
remotePushPerformed: false
```

El entorno mostrado en esa evidencia contenía las guardas seguras, pero el harness externo informó ausente `SENDING_ENABLED=false`.

## Causa raíz encontrada en el repositorio

Aunque el reanudador externo no estaba versionado, el patrón defectuoso sí existía en los validadores canónicos:

- PowerShell aplicaba regex al string JSON completo;
- Unix aplicaba `grep` al string JSON completo;
- ambos validadores integrales exigían una rama histórica en lugar de `main`.

## Corrección publicada en `main`

- `scripts/container-env-assertions.ps1`;
- `scripts/test-container-env-assertions.ps1`;
- `scripts/assert-container-env.js`;
- `scripts/test-container-env-assertions.js`;
- `scripts/validate-complete-crm.ps1`;
- `scripts/validate-complete-crm.sh`;
- `scripts/verify-production-profile.ps1`;
- `scripts/verify-production-profile.sh`.

El parser:

1. rechaza entrada vacía;
2. parsea JSON real;
3. exige raíz array;
4. normaliza las entradas a string;
5. comprueba membresía exacta;
6. informa solo guardas faltantes;
7. falla ante JSON inválido;
8. no imprime secretos ni el entorno completo.

## Guardas obligatorias

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Validación posterior al cambio

| Gate | Estado |
|---|---|
| inspección del remoto | EXECUTED_PASS |
| self-test Node | EXECUTED_PASS |
| self-test PowerShell 5.1 | EXECUTED_PASS |
| sintaxis PowerShell | EXECUTED_PASS |
| sintaxis Bash | EXECUTED_PASS |
| backend Maven Verify | FUNCTIONAL_PASS — 89/89 |
| frontend typecheck/unit/build | FUNCTIONAL_PASS — 5/5 |
| Docker/health/smoke | FUNCTIONAL_PASS |
| migraciones | FUNCTIONAL_PASS |
| dependency scans | FUNCTIONAL_PASS |
| Playwright | FUNCTIONAL_PASS |
| `effectiveSendingBlockade` | FUNCTIONAL_PASS |
| `zeroSent` | FUNCTIONAL_PASS |
| backup/restore | FUNCTIONAL_PASS |
| `productionProfileSmoke` | FUNCTIONAL_PASS |
| cleanup de recursos | EXECUTED_PASS |
| `finalTreeClean` | FUNCTIONAL_PASS |
| repository safety | EXECUTED_PASS |
| `git diff --check` | EXECUTED_PASS |
| corrida integral 1 | FUNCTIONAL_PASS |
| corrida integral 2 | FUNCTIONAL_PASS |
| CI del HEAD validado | NO_CHECKS_REPORTED |

Existe evidencia estructurada suficiente para declarar `FUNCTIONAL_PASS` del commit `0448c0e060311c284f4e4be4612982818a8480c4`.

## Evidencia nueva

```text
docs/validation/remote-main-hardening-2026-07-24.md
docs/validation/main-hardening-functional-closure-2026-07-24.md

complete-crm-20260724-201944.json
SHA-256 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527

complete-crm-20260724-202955.json
SHA-256 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

## Próxima acción

Implementar `VAL-002` en un commit funcional independiente para comprobar `ProductionFrontendPort` durante el preflight.

La demo remota que publica `127.0.0.1:18080` debe permanecer activa. La actualización documental actual no repite suites funcionales ya ejecutadas y no cambia código, configuración, dependencias, contenedores, migraciones ni pruebas.

## Fronteras

- producción continúa `NOT_AUTHORIZED / NOT_DEPLOYED`;
- Gmail, SMTP y WhatsApp Cloud continúan `IMPLEMENTED_NOT_CONNECTED`;
- no se realizaron envíos reales;
- no se incorporó el XLSX real;
- no se creó rama ni PR;
- no se usó force push;
- no se añadieron migraciones ni dependencias.

## VAL-002 — Estado de publicación

```text
estado funcional: FUNCTIONAL_PASS_FOCUSED
preflight Windows: EXECUTED_PASS
preflight Unix/Node: EXECUTED_PASS
puerto libre: EXECUTED_PASS
puerto ocupado: EXECUTED_PASS
publicación Docker ocupada: EXECUTED_PASS
demo 18080: EXECUTED_PASS id=d0420be3a84d
PowerShell syntax: EXECUTED_PASS
Node syntax: EXECUTED_PASS
Bash syntax: EXECUTED_PASS
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
full suites repeated: false
CI: PENDING_POST_PUSH_VERIFICATION
```

No se modificaron backend, frontend, base de datos, dependencias, Dockerfiles, Compose productivo ni políticas de envío.

Evidencia: `docs/validation/production-frontend-port-preflight-2026-07-24.md`.

## UX-003 — Estado de publicación

```text
estado: COMPLETE_WITH_FOCUSED_VALIDATION
archivos eliminados: 5
consumidores operativos externos: 0
dependencias desde otros workflows: 0
guarda histórica obsoleta: true
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
full suites repeated: false
CI: PENDING_POST_PUSH_VERIFICATION
```

No se modificaron backend, frontend, migraciones, dependencias, Dockerfiles, Compose ni políticas de envío.

Evidencia: `docs/validation/ux-003-remote-automation-cleanup-2026-07-26.md`.
