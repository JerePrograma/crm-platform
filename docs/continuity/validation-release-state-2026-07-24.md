# Estado de validación y publicación — 2026-07-24

## Estado ejecutivo

```text
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
HEAD remoto antes de la consolidación: f25051884b7aadd5837286dedd9ae0eee899cb5a
hardening parser .Config.Env: IMPLEMENTED_NOT_FULLY_VALIDATED
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
| self-test PowerShell 5.1 | IMPLEMENTED_NOT_RUN |
| sintaxis PowerShell | IMPLEMENTED_NOT_RUN |
| sintaxis Bash | IMPLEMENTED_NOT_RUN |
| `productionProfileSmoke` | IMPLEMENTED_NOT_RUN |
| cleanup de recursos | IMPLEMENTED_NOT_RUN |
| `finalTreeClean` | IMPLEMENTED_NOT_RUN |
| repository safety | IMPLEMENTED_NOT_RUN |
| `git diff --check` | IMPLEMENTED_NOT_RUN |
| corrida integral 1 | IMPLEMENTED_NOT_RUN |
| corrida integral 2 | IMPLEMENTED_NOT_RUN |
| CI del HEAD final | PENDING_VERIFICATION |

No existe evidencia suficiente para declarar `FUNCTIONAL_PASS` del nuevo HEAD.

## Evidencia nueva

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## Próxima acción

Desde un checkout limpio de `main` con PowerShell 5.1 y Docker:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Las dos corridas deben terminar `FUNCTIONAL_PASS` sobre el mismo commit. Hasta entonces, el hardening está implementado pero no cerrado.

## Fronteras

- producción continúa `NOT_AUTHORIZED / NOT_DEPLOYED`;
- Gmail, SMTP y WhatsApp Cloud continúan `IMPLEMENTED_NOT_CONNECTED`;
- no se realizaron envíos reales;
- no se incorporó el XLSX real;
- no se creó rama ni PR;
- no se usó force push;
- no se añadieron migraciones ni dependencias.
