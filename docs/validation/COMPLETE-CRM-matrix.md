# Matriz de validación integral del CRM

Actualizado: 2026-07-24

Estados permitidos: `NOT_STARTED`, `IN_PROGRESS`, `IMPLEMENTED_NOT_RUN`, `EXECUTED_PASS`, `EXECUTED_FAIL`, `BLOCKED_EXTERNAL`, `NOT_APPLICABLE`.

Esta matriz separa la evidencia histórica del baseline funcional y la validación pendiente de los scripts modificados durante la consolidación remota.

## Baseline funcional publicado

| Capacidad | Estado | Evidencia actual |
|---|---|---|
| SEG-000–SEG-011 | EXECUTED_PASS | cierres históricos publicados antes de `83e181c` |
| organizaciones, usuarios, RBAC y tenant isolation | EXECUTED_PASS | integración PostgreSQL, seguridad y arquitectura históricas |
| prospectos, contactos y ciclo comercial | EXECUTED_PASS | backend/frontend/E2E históricos |
| importaciones y duplicados | EXECUTED_PASS | CSV/XLSX sintético, preview, merge e idempotencia |
| actividades, tareas y timeline | EXECUTED_PASS | integración y E2E históricos |
| oportunidades, pipeline y forecast | EXECUTED_PASS | integración PostgreSQL y frontend históricos |
| campañas, plantillas y simulación | EXECUTED_PASS | RBAC, audiencia congelada y E2E históricos |
| outbox, workers e inbound fake | EXECUTED_PASS | idempotencia, retry, replay y cuarentena históricos |
| reportes, auditoría y configuración | EXECUTED_PASS | agregaciones tenant-scoped y CSV seguro históricos |
| UX de operador y contactabilidad | EXECUTED_PASS | runs históricos documentados |
| producción real | NOT_APPLICABLE | no autorizada |
| envíos reales | NOT_APPLICABLE | deshabilitados por política |
| Gmail/SMTP real | BLOCKED_EXTERNAL | contrato no conectado |
| WhatsApp Cloud real | BLOCKED_EXTERNAL | contrato no conectado |
| XLSX real | BLOCKED_EXTERNAL | fuera de Git, CI e imágenes |

## Estado remoto y parser `.Config.Env`

| Capacidad | Estado | Evidencia actual |
|---|---|---|
| resolución de `main` antes del cambio | EXECUTED_PASS | `f25051884b7aadd5837286dedd9ae0eee899cb5a` |
| comparación contra baseline | EXECUTED_PASS | `83e181c...f250518`: ahead 1, behind 0; solo continuidad documental |
| localización del candidato `9e058d...` | BLOCKED_EXTERNAL | no existe como commit/rama/PR accesible |
| parser PowerShell de JSON array | IMPLEMENTED_NOT_RUN | `scripts/container-env-assertions.ps1` |
| parser Unix/Node de JSON array | EXECUTED_PASS | self-test ejecutado con Node 22 |
| guardas completas → PASS | EXECUTED_PASS | self-test Node |
| falta `SENDING_ENABLED=false` → FAIL | EXECUTED_PASS | self-test Node |
| `SENDING_ENABLED=true` → FAIL | EXECUTED_PASS | self-test Node |
| JSON inválido → FAIL | EXECUTED_PASS | self-test Node |
| línea vacía adicional → PASS | EXECUTED_PASS | self-test Node |
| self-test PowerShell 5.1 | IMPLEMENTED_NOT_RUN | requiere host Windows/PowerShell 5.1 |
| rama canónica en validador Windows | IMPLEMENTED_NOT_RUN | código exige `main`; pendiente ejecución |
| rama canónica en validador Unix | IMPLEMENTED_NOT_RUN | código exige `main`; pendiente ejecución |
| guardas exactas en stack app Windows | IMPLEMENTED_NOT_RUN | parser conectado a `validate-complete-crm.ps1` |
| guardas exactas en stack app Unix | IMPLEMENTED_NOT_RUN | parser conectado a `validate-complete-crm.sh` |
| guardas exactas en perfil productivo Windows | IMPLEMENTED_NOT_RUN | siete entradas requeridas |
| guardas exactas en perfil productivo Unix | IMPLEMENTED_NOT_RUN | siete entradas requeridas |
| `productionProfileSmoke` post-fix | IMPLEMENTED_NOT_RUN | Docker no disponible en esta intervención |
| cleanup productivo | IMPLEMENTED_NOT_RUN | pendiente de smoke ejecutado |
| `finalTreeClean` post-fix | IMPLEMENTED_NOT_RUN | requiere checkout limpio |
| repository safety post-fix | IMPLEMENTED_NOT_RUN | requiere checkout |
| `git diff --check` post-fix | IMPLEMENTED_NOT_RUN | requiere checkout |
| validador integral corrida 1 | IMPLEMENTED_NOT_RUN | pendiente |
| validador integral corrida 2 | IMPLEMENTED_NOT_RUN | pendiente sobre el mismo commit |
| CI del HEAD final | IN_PROGRESS | comprobar después de completar la serie remota |

## Seguridad que debe observar el gate

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Evidencia

```text
docs/validation/remote-main-hardening-2026-07-24.md
docs/continuity/validation-release-state-2026-07-24.md
```

No convertir `IMPLEMENTED_NOT_RUN` o `BLOCKED_EXTERNAL` en `EXECUTED_PASS` sin salida de comandos sobre el SHA exacto.
