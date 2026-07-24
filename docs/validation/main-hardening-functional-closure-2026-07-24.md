# Cierre funcional local del hardening de `main` — 2026-07-24

## Objetivo

Cerrar con evidencia ejecutada el hardening del parser de `.Config.Env` y de las guardas fail-closed publicado en `main`.

## Commit y rama

```text
repositorio: JerePrograma/crm-platform
rama: main
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
```

El árbol estaba limpio y coincidía con `origin/main` antes de validar.

## Validaciones focalizadas

```text
PowerShell syntax: EXECUTED_PASS — 21 scripts
PowerShell parser self-test: EXECUTED_PASS
Node parser self-test: EXECUTED_PASS
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
focused production profile smoke: FUNCTIONAL_PASS
focused production port: 18081
```

## Corridas integrales consecutivas

### Corrida 1

```text
archivo: complete-crm-20260724-201944.json
commit: 0448c0e060311c284f4e4be4612982818a8480c4
branch: main
status: FUNCTIONAL_PASS
productionFrontendPort: 48080
durationSeconds: 609.812
SHA-256: 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527
```

### Corrida 2

```text
archivo: complete-crm-20260724-202955.json
commit: 0448c0e060311c284f4e4be4612982818a8480c4
branch: main
status: FUNCTIONAL_PASS
productionFrontendPort: 48080
durationSeconds: 623.648
SHA-256: 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

## Gates confirmados en ambas corridas

- árbol inicial limpio;
- tooling;
- repository safety;
- sintaxis PowerShell y Bash;
- secret scan;
- backend Maven Verify con 89/89 pruebas;
- Spotless;
- frontend install, typecheck, 5/5 pruebas unitarias y build;
- Docker sin caché, health y smoke;
- dependency scans;
- migraciones desde vacío y desde V11;
- outbox, workers, inbound y webhook sintéticos;
- Playwright;
- siete guardas exactas de envío;
- bloqueo persistente de envío;
- cero estados `SENT|DELIVERED|READ`;
- backup/restore;
- perfil productivo local;
- cleanup;
- árbol final limpio.

## Guardas verificadas

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Incidencia de puerto

Dos intentos anteriores alcanzaron el final de la batería y fallaron al iniciar el perfil productivo porque `127.0.0.1:18080` ya estaba ocupado.

El propietario era:

```text
contenedor: gestudio-remote-demo-backend-1
publicación: 127.0.0.1:18080->8080/tcp
```

La demo autorizada no fue detenida ni modificada. Las dos corridas válidas utilizaron `48080`.

## Gap pendiente: VAL-002

El validador recibe `ProductionFrontendPort`, pero el preflight comprueba únicamente PostgreSQL, backend y frontend. La colisión del cuarto puerto se detecta demasiado tarde.

El próximo cambio funcional debe:

1. comprobar `ProductionFrontendPort` durante tooling/preflight;
2. fallar antes de Maven, npm, Docker build, migraciones y E2E cuando esté ocupado;
3. preservar compatibilidad y puertos configurables;
4. cubrir puerto libre y ocupado;
5. mantener intacta la demo remota.

## Estado externo y límites

```text
GitHub checks del SHA validado: NO_CHECKS_REPORTED
producción: NOT_AUTHORIZED / NOT_DEPLOYED
proveedores reales: IMPLEMENTED_NOT_CONNECTED
envíos reales realizados: false
XLSX real: OUTSIDE_GIT_CI_IMAGES
candidato histórico 9e058d...: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
validador Unix en host real: pendiente externo
```

`NO_CHECKS_REPORTED` no se presenta como CI verde.

## Decisión sobre esta actualización documental

Esta actualización modifica exclusivamente archivos Markdown. No cambia código, configuración, dependencias, lockfiles, Dockerfiles, Compose, migraciones ni pruebas.

Por instrucción explícita se reutiliza la evidencia funcional ya cerrada y no se repiten Maven, npm, Docker, migraciones, Playwright ni dependency scans. Antes del commit y push se ejecutan únicamente:

- verificación de los dos JSON y sus SHA-256;
- `git diff --check`;
- repository safety;
- control de alcance del diff;
- fast-forward seguro a `main`.

No se versionan los JSON, logs ni `validation-output/`.
