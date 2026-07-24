# Progreso de ejecución integral del CRM

Actualizado: 2026-07-24

Este documento registra el estado operativo vigente. Los checkpoints detallados anteriores permanecen en la historia Git y en los documentos de validación de cada segmento.

## Baseline histórico cerrado

```text
SEG-000–SEG-011: COMPLETE histórico
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
cierre integral CI: run 29951586239, 22/22 jobs success
cierre UX: run 30034176306, PASS
contactabilidad/paginación: run 30036648327, PASS
producción real: NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
```

La evidencia histórica no se reutiliza para declarar PASS sobre scripts modificados después.

## Continuidad documental

```text
commit: f25051884b7aadd5837286dedd9ae0eee899cb5a
mensaje: docs: add canonical continuity handoff
contenido: AGENTS.md + docs/continuity/
```

La comparación remota contra `83e181c` confirmó `ahead 1`, `behind 0` y ausencia de cambios funcionales en ese commit.

## Reanudación remota 2026-07-24

### Inspección

```text
REPOSITORY=JerePrograma/crm-platform
BRANCH=main
INITIAL_REMOTE_HEAD=f25051884b7aadd5837286dedd9ae0eee899cb5a
LOCAL_CHECKOUT=NOT_AVAILABLE
DOCKER=NOT_AVAILABLE
POWERSHELL=NOT_AVAILABLE
NODE=22.16.0
```

No se localizaron commits, ramas o PRs accesibles que contuvieran el tree histórico:

```text
9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Decisión: no reconstruir ni declarar integrado un candidato ausente. Corregir únicamente defectos comprobables en el código remoto actual.

## Defectos comprobados

1. `scripts/validate-complete-crm.ps1` exigía `feat/complete-crm-platform`.
2. `scripts/validate-complete-crm.sh` exigía la misma rama histórica.
3. los validadores PowerShell buscaban entradas de `.Config.Env` mediante regex sobre el JSON completo;
4. los scripts Unix usaban `grep` sobre el JSON completo;
5. el smoke productivo no exigía de manera uniforme las siete guardas.

## Implementación remota

### Helpers

- `scripts/container-env-assertions.ps1`;
- `scripts/assert-container-env.js`.

### Regresiones

- `scripts/test-container-env-assertions.ps1`;
- `scripts/test-container-env-assertions.js`.

### Integración

- `scripts/validate-complete-crm.ps1`;
- `scripts/validate-complete-crm.sh`;
- `scripts/verify-production-profile.ps1`;
- `scripts/verify-production-profile.sh`.

### Comportamiento esperado

- JSON vacío → FAIL;
- JSON inválido → FAIL;
- raíz distinta de array → FAIL;
- guardas completas → PASS;
- una guarda ausente → FAIL con lista exacta;
- valor inseguro alternativo → FAIL;
- líneas vacías exteriores → no generan falso negativo.

## Commits funcionales

```text
cb93948d783d4b5de9022cd44ff798cce53993f6
3032b02c073d8643952822d5c8e39f9b0d87abc6
2f3124f97d1545abb5fc07a6e16b4cb9482ecf0a
1760c3756c854b7c1f4597ee130118d7d015003d
df36e0d8c6931dbac18330d99571297f501c61aa
6e106ae8b2a26cd7df990f27a7dd0110cc6e7e40
051ee9424326b013c67cd445aa45254cde6d348b
d5bc869646ebd0300720a3e2d4f6423d3ad6c0ac
5b5fbf63c4f3ef15a50f0407710ae56b26bbe787
```

La serie es fast-forward sobre `main`; no se creó rama ni PR.

## Validaciones

```text
REMOTE_HEAD_RESOLUTION=EXECUTED_PASS
REMOTE_COMPARE=EXECUTED_PASS
NODE_SELF_TEST=EXECUTED_PASS
POWERSHELL_SELF_TEST=IMPLEMENTED_NOT_RUN
PRODUCTION_PROFILE_SMOKE=IMPLEMENTED_NOT_RUN
FINAL_TREE_CLEAN=IMPLEMENTED_NOT_RUN
FULL_VALIDATION_1=IMPLEMENTED_NOT_RUN
FULL_VALIDATION_2=IMPLEMENTED_NOT_RUN
REPOSITORY_SAFETY=IMPLEMENTED_NOT_RUN
GIT_DIFF_CHECK=IMPLEMENTED_NOT_RUN
```

El self-test Node se ejecutó con Node `v22.16.0` y cubrió todos los escenarios requeridos.

## Evidencia canónica

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## Gate pendiente

En Windows, desde un checkout limpio del `HEAD` remoto final:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Las dos salidas JSON deben terminar `FUNCTIONAL_PASS` sobre el mismo commit. `productionProfileSmoke` y `finalTreeClean` deben quedar explícitamente verdes.

## Fronteras conservadas

- no se desplegó producción;
- no se realizaron envíos reales;
- no se conectaron Gmail, SMTP ni WhatsApp Cloud;
- no se inspeccionó ni versionó `.env`;
- no se incorporó el XLSX real;
- no se usó force push;
- no se reescribió historia;
- no se añadieron migraciones ni dependencias.
