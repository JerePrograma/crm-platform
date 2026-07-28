# UX-004 — Métricas tenant-wide del dashboard

Fecha: 2026-07-26

## Base

`	ext
branch: main
parent commit: 9da507f1287ad5bbf7773f4d08a5463fb0000ab8
demo state before validation: ACTIVE_AND_PRESERVED id=d0420be3a84d port=18080
`

## Defecto reproducido

App.tsx conservaba una página de prospectos con tamaño 100 y calculaba:

`	ext
interested = prospects.filter(...).length
blocked = prospects.filter(!contactEligible).length
`

Por tanto, ambos valores podían representar solo la página visible.

## Implementación

### Backend

`	ext
service: ProspectOperationsService.dashboardMetrics()
endpoint: GET /api/v1/prospects/metrics
permission: PROSPECT_READ
scope: currentActor.organizationId()
archived_at: IS NULL
`

Interés:

`	ext
INTERESTED
QUALIFIED
TRIAL_ACTIVE
QUOTED
NEGOTIATION
`

Bloqueo:

`	ext
NOT contact_eligible
`

### Frontend

`	ext
type: ProspectDashboardMetrics
API: getProspectDashboardMetrics()
consumer: App.refresh()
page-array calculation: removed
`

## Regresiones

La prueba PostgreSQL:

1. captura métricas iniciales;
2. crea 105 prospectos sintéticos con prefijo único;
3. marca cinco estados de interés;
4. habilita contacto en siete registros;
5. consulta una página de tamaño 100;
6. confirma 	otalElements=105 y content.size=100;
7. confirma deltas interested=5 y locked=98.

Seguridad:

`	ext
VIEWER access: EXECUTED_PASS
tenant B metrics: interested=0 blocked=0
tenant A prospect leakage: none
`

Frontend:

`	ext
aggregate endpoint contract: EXECUTED_PASS
credentials same-origin: EXECUTED_PASS
`

## Validaciones ejecutadas

`	ext
focused backend tests: EXECUTED_PASS
backend Maven Verify: EXECUTED_PASS
frontend npm ci: EXECUTED_PASS
frontend focused API test: EXECUTED_PASS
frontend typecheck: EXECUTED_PASS
frontend unit tests: EXECUTED_PASS
frontend build: EXECUTED_PASS
PowerShell syntax: EXECUTED_PASS
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
demo preserved: EXECUTED_PASS id=d0420be3a84d
`

## Alcance no modificado

- migraciones;
- esquema de base;
- dependencias;
- endpoints de listado;
- filtros y paginación de prospectos;
- reporte general;
- Docker y Compose;
- políticas de envío;
- producción;
- datos reales.

## Seguridad

`	ext
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
`

## Estado

`	ext
UX-004: FUNCTIONAL_PASS
CI del commit final: PENDING_POST_PUSH_VERIFICATION
producción: NOT_AUTHORIZED_NOT_DEPLOYED
envíos reales: DISABLED_BY_POLICY
`

## Próximo paso

UX-006 — importaciones de gran volumen.
