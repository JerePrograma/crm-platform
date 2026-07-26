# VAL-002 — Preflight de `ProductionFrontendPort`

Fecha: 2026-07-24

## Contexto

El validador Windows aceptaba `ProductionFrontendPort`, pero durante `tooling` solo comprobaba PostgreSQL, backend y frontend. El validador Unix fijaba `18080` al final del recorrido y tampoco hacía un chequeo temprano.

Dos recorridos históricos llegaron hasta el smoke productivo antes de descubrir que `127.0.0.1:18080` pertenecía a la demo autorizada `gestudio-remote-demo-backend-1`.

## Base

```text
branch: main
parent commit: 5c56cf827d0803e804d9b8b50031ebf355a96d4b
demo state before validation: ACTIVE_AND_PRESERVED id=d0420be3a84d port=18080
```

## Cambios

### `scripts/check-host-ports.ps1`

- conserva los parámetros existentes;
- agrega `ProductionFrontendPort` opcional;
- exige unicidad entre todos los puertos proporcionados;
- comprueba publicación Docker;
- comprueba bind exclusivo en loopback;
- identifica el puerto productivo por nombre en los errores.

### `scripts/validate-complete-crm.ps1`

- transmite `ProductionFrontendPort` al checker durante `tooling`;
- ejecuta las nuevas regresiones dentro de `scriptSyntax`.

### Unix

- agrega `scripts/check-host-ports.js`;
- agrega `--production-frontend-port` a `validate-complete-crm.sh`;
- ejecuta el checker antes de backend/frontend/builds;
- transmite el puerto al smoke productivo;
- agrega regresión Node.

## Validaciones ejecutadas

```text
PowerShell syntax: EXECUTED_PASS
PowerShell three-port compatibility: EXECUTED_PASS
PowerShell four-free-ports: EXECUTED_PASS
PowerShell duplicate port: EXECUTED_PASS
PowerShell occupied loopback port: EXECUTED_PASS
Node parse and uniqueness: EXECUTED_PASS
Node free loopback port: EXECUTED_PASS
Node occupied loopback port: EXECUTED_PASS
Node Docker publication detection: EXECUTED_PASS
Bash syntax: EXECUTED_PASS
demo collision detection: EXECUTED_PASS id=d0420be3a84d
demo preserved: EXECUTED_PASS
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
```

## Pruebas no repetidas

```text
Maven/backend: NOT_REPEATED_UNAFFECTED
npm/frontend: NOT_REPEATED_UNAFFECTED
Docker image builds: NOT_REPEATED_UNAFFECTED
migrations: NOT_REPEATED_UNAFFECTED
Playwright: NOT_REPEATED_UNAFFECTED
dependency scans: NOT_REPEATED_UNAFFECTED
```

VAL-002 cambia únicamente scripts de preflight, validadores y documentación.

## Criterios de aceptación

- un puerto productivo ocupado falla antes de suites costosas;
- cuatro puertos libres permiten continuar;
- tres puertos siguen siendo compatibles en el checker Windows;
- Unix permite configurar el puerto productivo;
- la demo remota no se detiene ni se modifica;
- no se añaden dependencias;
- los bloqueos de envío permanecen intactos.

## Límites y pendientes

```text
CI del commit final: PENDING_POST_PUSH_VERIFICATION
validador integral Unix completo: IMPLEMENTED_NOT_RUN
producción: NOT_AUTHORIZED_NOT_DEPLOYED
envíos reales: DISABLED_BY_POLICY
providers reales: IMPLEMENTED_NOT_CONNECTED
XLSX real: OUTSIDE_GIT_CI_IMAGES
```

## Próximo paso

`UX-003` — inspección y eventual eliminación de automatización UX remota obsoleta.
