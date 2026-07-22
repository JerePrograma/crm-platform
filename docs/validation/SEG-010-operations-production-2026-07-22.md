# Validación SEG-010 — operación, seguridad y producción local

Fecha: 2026-07-22

## Resultado del checkpoint

```text
IMPLEMENTATION=EXECUTED_PASS_FOCUSED
INTEGRAL_VALIDATOR=NOT_RUN_AT_THIS_CHECKPOINT
FINAL_SEGMENT_STATE=IMPLEMENTED_NOT_RUN
PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY
PRODUCTION_DEPLOYMENT=NOT_AUTHORIZED
REAL_XLSX_PREVIEW=BLOCKED_EXTERNAL_FILE
REAL_SENDING=DISABLED_BY_POLICY
```

## Modelo y UI

V13 crea settings operativos, `crm_tag`, `prospect_tag` e índices GIN/B-tree.
Los servicios agregan búsqueda, reporting por tenant/fecha/timezone, valores por
currency, CSV seguro, settings invariantes y tags auditados. React incorpora
reportes, búsqueda, configuración y etiquetas sin URLs manuales; reutiliza el
sistema visual y conserva permisos backend.

## Pruebas ejecutadas

```text
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
RESULT=PASS
TESTS=79/79
SPOTLESS=159/159
ARCHUNIT=PASS
DATABASE=PostgreSQL 17.10 via Testcontainers
MIGRATIONS=V1-V13
HIBERNATE_VALIDATE=PASS
DURATION=02:27
```

```text
npm ci --no-audit --no-fund
npm run typecheck
npm run test:unit
npm run build
RESULT=PASS
UNIT=2/2
```

```text
npm run test:e2e
RESULT=PASS
TESTS=2/2
DURATION=13.3s
STACK=PostgreSQL/backend/frontend real
```

El E2E crea usuario viewer, prospectos/contacto/nota/tarea/actividad,
importación sintética, exclusión, duplicado, oportunidad/pipeline,
plantilla/campaña/audiencia/simulación, outbox/worker, webhook HMAC/replay,
inbound/actividad/tarea/timeline, reportes/settings/integraciones, health,
métricas, auditoría, RBAC read-only y logout. Verifica viewport móvil y teclado.

## Seguridad de dependencias e imagen

El primer Grype sobre `eclipse-temurin:21-jre-alpine` falló por High/Critical en
paquetes del runtime y JDBC 42.7.11. No se ignoró. Se retiró `curl`, se cambió a
JRE mínima Chainguard fijada por digest y healthcheck Java, se elevó PostgreSQL
JDBC a 42.7.12 y Jackson 3/2 a 3.1.5/2.21.5. La suite completa pasó después.

```text
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  -v crm_grype_cache:/root/.cache/grype \
  anchore/grype@sha256:fd4ab4d1042b522c896e73bdf09ab8bf384fa417df99d6dd0d6e1008c7e7c821 \
  crm-platform-backend:latest --fail-on high
RESULT=PASS
OUTPUT=No vulnerabilities found
```

OWASP Dependency-Check intentó poblar 369.464 registros NVD sin API key y quedó
`BLOCKED_EXTERNAL_NVD_RATE_LIMIT`; se detuvo de forma acotada y no cuenta como
PASS. Grype y npm audit son los gates ejecutables canónicos.

## Fallos reales corregidos

1. `/actuator/metrics` quedaba denegado también al administrador: se agregó la
   regla autenticada y regresión anónimo 401/admin 200.
2. logout del sidebar quedaba fuera del viewport desktop: sidebar ahora scrolla
   y el E2E lo prueba.
3. la primera imagen runtime contenía CVE High/Critical: se reemplazó la capa,
   las dependencias y el healthcheck, luego se repitieron build/suite/scan.
4. `docker compose images -q` podía devolver un image ID reemplazado: el
   validador deriva la tag desde el container efectivo.

No se relajaron permisos, CSRF, tenant scope, tests ni bloqueos de envío.

## Fronteras

- cero llamadas a Google/Meta;
- Gmail/WhatsApp implementados pero no conectados;
- producción solo perfil local;
- XLSX real ausente y no inspeccionado;
- evidencia cruda fuera de Git.
