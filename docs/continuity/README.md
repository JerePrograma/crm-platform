# Continuidad canónica del proyecto

Actualizado: 2026-07-24

Este directorio permite retomar `JerePrograma/crm-platform` sin depender de chats, archivos temporales ni afirmaciones no verificadas.

## Qué representa el repositorio

Este repositorio implementa el CRM comercial utilizado para vender Gestudio. No es la aplicación operativa de academias que administra alumnos, cuotas y asistencia.

## Lectura obligatoria

1. [`product-purpose-architecture.md`](product-purpose-architecture.md)
2. [`configuration-environments-data.md`](configuration-environments-data.md)
3. [`local-operation-and-deployment.md`](local-operation-and-deployment.md)
4. [`validation-release-state-2026-07-24.md`](validation-release-state-2026-07-24.md)
5. [`continuation-mission.md`](continuation-mission.md)
6. `docs/status.md`
7. `docs/next-step.md`
8. `docs/backlog.md`
9. `docs/validation/remote-main-hardening-2026-07-24.md`
10. el ADR, módulo y validación específicos del cambio

## Jerarquía de fuentes

1. código y configuración versionados en `main`;
2. salida estructurada de comandos sobre el commit exacto;
3. documentos de validación;
4. documentos de continuidad;
5. documentos históricos;
6. transcripts o resúmenes conversacionales.

Un script implementado no equivale a un script ejecutado. `IMPLEMENTED_NOT_RUN`, `BLOCKED_EXTERNAL` y `EXECUTED_PASS` no son intercambiables.

## Estado remoto consolidado

```text
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
parser exacto de .Config.Env: FUNCTIONAL_PASS
corridas integrales consecutivas: 2/2 FUNCTIONAL_PASS
CI del SHA validado: NO_CHECKS_REPORTED
candidato histórico 9e058d...: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
producción: NOT_DEPLOYED
envíos reales: DISABLED
```

## Hardening incorporado

Los validadores canónicos:

- exigen `main`;
- parsean `.Config.Env` como array JSON;
- comprueban siete guardas mediante coincidencia exacta;
- fallan ante JSON vacío o inválido;
- incluyen self-tests PowerShell y Node;
- no imprimen el entorno completo;
- mantienen providers reales desconectados.

PowerShell 5.1, Node 22, sintaxis Bash, Docker, backend, frontend, migraciones, E2E, dependency scans, backup/restore, `productionProfileSmoke`, bloqueo efectivo de envíos, cero estados enviados, repository safety y `finalTreeClean` pasaron.

La evidencia principal está en `docs/validation/main-hardening-functional-closure-2026-07-24.md`.

## Candidato local histórico

```text
v6: e3a9728e717b7c8a4d92f9fab31f709bf5d66464
locators: 24df4c7f26ffde0f044f681f9130fa254f15debd
foco inicial: fa8c15172dfa9a0cfa5cbd00f7aab42733d516ba
foco explícito: 9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Son referencias de trees locales. No se encontraron commits, ramas o PRs remotos que los materialicen. No deben describirse como integrados.

## Invariantes

- trabajar directamente sobre `main`;
- no crear ramas o PRs salvo instrucción explícita distinta;
- no usar force push, reset destructivo ni reescritura de historia;
- no desplegar producción ni habilitar comunicaciones reales;
- no versionar `.env`, secretos, credenciales, ZIP, logs, datos reales ni el XLSX real;
- mantener PostgreSQL como fuente de verdad;
- conservar RBAC, tenant isolation, CSRF, exclusiones, idempotencia y fail-closed;
- volver a ejecutar cualquier fase cuyos archivos o dependencias hayan cambiado.

## VAL-002 — Puerto productivo en preflight

El puerto del perfil productivo sintético forma parte del preflight en Windows y Unix.

```text
Windows checker: scripts/check-host-ports.ps1
Unix checker: scripts/check-host-ports.js
PowerShell regression: scripts/test-check-host-ports.ps1
Node regression: scripts/test-check-host-ports.js
estado: FUNCTIONAL_PASS_FOCUSED
full suites repeated: false
```

La demo autorizada que publica `127.0.0.1:18080` permanece fuera del proyecto Compose sintético y no fue modificada.

Siguiente gate: `UX-003`.
