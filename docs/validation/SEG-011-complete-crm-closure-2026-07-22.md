# Evidencia final SEG-011 — cierre integral del CRM

Fecha: 2026-07-22

## Identificación

```text
SEGMENT=SEG-011
TESTED_BRANCH=feat/complete-crm-platform
CANONICAL_BRANCH=main
FUNCTIONAL_COMMIT=986523a22a17f4c9159003bce88f4f6903ad7cdb
RESULT=FUNCTIONAL_PASS_TWICE
```

El código funcional fue probado sobre `986523a22a17f4c9159003bce88f4f6903ad7cdb`.
La integración fast-forward posterior en `main` y el cierre documental no
modifican ese código probado.

## Comando ejecutado

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1 `
  -PostgresPort 25432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

## Primera corrida

```text
LOCAL_EVIDENCE_FILE=complete-crm-20260722-173731.json
VERSIONED=false
RESULT=FUNCTIONAL_PASS
DURATION=713,870 s
PHASES=21/21
```

## Segunda corrida

```text
LOCAL_EVIDENCE_FILE=complete-crm-20260722-174938.json
VERSIONED=false
RESULT=FUNCTIONAL_PASS
DURATION=734,162 s
PHASES=21/21
REPRODUCIBILITY=PASS
FLAKINESS=NOT_OBSERVED
```

## Resultados comunes

- backend: 79/79;
- Spotless: 159/159;
- ArchUnit: PASS;
- Vitest: 2/2;
- Playwright: 2/2;
- Docker build sin caché: PASS;
- PostgreSQL, backend y frontend: healthy;
- smoke autenticado host y contenedor: PASS;
- npm audit: PASS;
- Grype High/Critical: PASS;
- migración desde vacío hasta V13: PASS;
- migración V11→V13: PASS;
- Hibernate validate: PASS;
- outbox, workers, retry/dead-letter e inbound fake: PASS;
- webhook HMAC, replay protection y quarantine: PASS;
- cuatro bloqueos de envío efectivos;
- estados `SENT|DELIVERED|READ`: cero;
- backup/restore sintético: PASS;
- perfil productivo validado localmente: PASS;
- repository safety final: PASS;
- cleanup limitado al proyecto Compose y al volumen sintético de cada corrida.

## Frontera externa

```text
GMAIL_REAL=IMPLEMENTED_NOT_CONNECTED
WHATSAPP_REAL=IMPLEMENTED_NOT_CONNECTED
REAL_COMMUNICATIONS=DISABLED_BY_POLICY
REAL_XLSX=BLOCKED_EXTERNAL_FILE
UNIX_INTEGRAL_VALIDATOR=IMPLEMENTED_NOT_RUN
BASH_SYNTAX_AND_WSL_PREFLIGHT=EXECUTED_PASS
REMOTE_CI_FINAL=EXECUTED_PASS
PRODUCTION=NOT_AUTHORIZED
PRODUCTION_DEPLOYMENT=NOT_DEPLOYED
REAL_SENDING=NOT_AUTHORIZED_NOT_TESTED
```

No se realizó despliegue ni se habilitaron comunicaciones reales.

## Advertencia de dependencias

OWASP Dependency-Check quedó
`BLOCKED_EXTERNAL_NVD_RATE_LIMIT` y no se contabiliza como PASS.

Grype fue el gate de imagen ejecutado y terminó PASS sin vulnerabilidades
High/Critical.

La evidencia cruda permanece fuera de Git y este documento no contiene
credenciales, tokens, cookies, firmas, secretos webhook, payloads, datos reales
ni filas del XLSX.

## Validación remota final

```text
PUBLISHED_COMMIT=b904ff37e506f058dab351c2b941e13ee4ed9981
BRANCH=main
WORKFLOW=ci
RUN_ID=29951586239
EVENT=push
STATUS=completed
CONCLUSION=success
JOBS=22/22
FAILED_JOBS=0
CANCELLED_JOBS=0
SKIPPED_JOBS=0
```

El primer run remoto, `29950739875`, falló y no se contabiliza como PASS.
Expuso `mvnw` sin modo ejecutable Unix y referencias de imagen Docker
inconsistentes entre build y consumo dentro de jobs aislados.

El commit `b904ff37e506f058dab351c2b941e13ee4ed9981` aplicó una corrección mínima:

- modo Git de `mvnw`: `100644 → 100755`;
- migraciones vacío y V11 usan explícitamente `crm-platform-backend:ci`;
- dependency scan construye y escanea la misma tag.

El run `29951586239` terminó con los 22 jobs exitosos, incluidos backend,
frontend E2E, migraciones, smoke, perfil productivo, backup/restore, outbox,
inbound, repository safety y Grype.

Las advertencias sobre Node.js 20 en `actions/*@v4` son mantenimiento futuro:
el runner ejecutó esas actions con Node.js 24 y ningún job falló, fue cancelado
u omitido. No se cambió la imagen Chainguard fijada, no se desplegó producción
y no se habilitaron comunicaciones reales.
