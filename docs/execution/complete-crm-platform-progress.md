# Progreso de ejecución integral del CRM

Actualizado: 2026-07-21

## BASELINE_VALIDATION

```text
CHECKPOINT_ID=CHECKPOINT_0
PHASE=baseline_and_gap_analysis
BASELINE_COMMIT=7db7e4c6688db2b35318383aeed4796b80a339b1
BASELINE_BRANCH=main
BASELINE_RESULT=EXECUTED_PASS
BASELINE_FAILURES=none
START_COMMIT=7db7e4c6688db2b35318383aeed4796b80a339b1
END_COMMIT=7db7e4c6688db2b35318383aeed4796b80a339b1
FILES_CHANGED=none
MIGRATIONS=V1-V5
TEST_COMMANDS=powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173
TEST_RESULT=PASS
KNOWN_WARNINGS=deprecated API notices and Spring Data PageImpl serialization warning
RESIDUAL_RISKS=all SEG-002+ capabilities remain unimplemented
```

Inicio UTC: `2026-07-21T17:22:06.8478073Z`

Fin UTC: `2026-07-21T17:28:51.5220026Z`

Resultado ejecutado:

- working tree inicial limpio;
- preflight y guardas de envío: PASS;
- PostgreSQL/frontend/backend clean build: PASS;
- Flyway V1-V5 antes de Hibernate: PASS;
- PostgreSQL/backend/frontend healthy;
- smoke host y contenedor: PASS;
- Maven verify: PASS, 29/29 tests;
- Spotless 55/55, ArchUnit y Testcontainers: PASS;
- lockfile sin cambios, `npm ci`: PASS;
- repository safety: PASS;
- stack retirado sin borrar volumen.

Evidencia local fuera de Git:

```text
validation-output/seg001-complete-20260721-142206.log
validation-output/seg001-complete-20260721-142206.json
validation-output/seg001-docker-20260721-142207.json
```

## CHECKPOINT_0_GAP_ANALYSIS

```text
PHASE=inventory_and_gap_analysis
COMMIT_OR_WORKING_TREE=feat/complete-crm-platform at 7db7e4c
CHANGES=documentation living plan and initial matrix
MIGRATIONS=none
TESTS_EXECUTED=baseline validator only
RESULT=IN_PROGRESS
FAILURES_FOUND=no frontend tests; Basic stateless; no tenant/RBAC; read-only duplicate review; no operational CRM modules
ROOT_CAUSE=SEG-001 intentionally delivered a narrow vertical slice
CORRECTION_APPLIED=segments SEG-002 through SEG-011 defined; implementation pending
REMAINING_RISKS=all functional checkpoints after baseline
```

## Registro de checkpoints

## CHECKPOINT_1_IDENTITY_RBAC

```text
CHECKPOINT_ID=CHECKPOINT_1
PHASE=identity_users_sessions_rbac
START_COMMIT=3fbfb33ff31aa6f901fff559d80d10a8624db833
END_COMMIT=0546e6ed8818b627d25982d7758b43181e5f4ce5
FILES_CHANGED=45
MIGRATIONS=V6__organizations_users_roles_and_tenant_scope.sql
TEST_COMMANDS=mvn verify; npm ci; npm run build; docker compose build/up; smoke host/container; repository safety; script syntax
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=deprecated API, Mockito dynamic agent, PageImpl serialization; transient Docker builder snapshot retry succeeded
RESIDUAL_RISKS=production cookie Secure depends on production profile; external identity provider not selected or required
```

Cambios ejecutados:

- organización bootstrap, backfill e índices/FK `organization_id`;
- usuarios persistentes, hash delegado, roles, permisos y membresías;
- login/logout/cambio de contraseña con cookie HttpOnly, CSRF y rotación;
- bloqueo tras cinco fallos, expiración e invalidación por contraseña/inactividad;
- autorización por acción y aislamiento de tenant en servicios/repositorios existentes;
- UI de login, usuarios, activación y cambio de contraseña;
- auditoría de identidad con actor, resultado, origen y organización;
- smoke Windows y contenedor migrados de Basic a cookie/CSRF.

Fallos encontrados y corregidos:

1. API anónima devolvía `403` sin entry point explícito: se configuró `401` y se
   mantuvo `403` para permisos insuficientes.
2. ArchUnit interpretó `SecurityContextRepository` como acceso directo desde un
   controller: la persistencia de sesión se movió a `AuthSessionService`.
3. El smoke contenedor usaba un header CSRF fijo: ahora consume `headerName` del
   contrato del backend.
4. Docker Desktop perdió un snapshot de build cache al exportar la imagen: el
   reintento acotado del backend completó sin limpiar caches ni volúmenes.

Evidencia ejecutada:

- `SecurityAuthorizationIntegrationTest`: 10/10;
- focused Architecture + Security: 11/11;
- Maven verify integral: 36/36, Spotless y ArchUnit PASS;
- Flyway V1–V6 desde vacío + Hibernate validate: PASS en Testcontainers;
- upgrade V1–V5→V6 en el volumen sintético local: PASS;
- frontend `npm ci` y `npm run build`: PASS;
- health backend/frontend/PostgreSQL, smoke host y contenedor: PASS;
- repository safety, 11 scripts PowerShell, Bash smoke syntax y Compose config: PASS.

Los checkpoints posteriores se agregan aquí al ejecutarse. Ninguna fila se anticipa como PASS.

## CHECKPOINT_2_PROSPECTS_CONTACTS

```text
CHECKPOINT_ID=CHECKPOINT_2
PHASE=operational_prospects_and_contacts
START_COMMIT=bf64bb0
END_COMMIT=58c303d
FILES_CHANGED=backend prospect/contact/common, frontend App/api/types/styles, V7, tests and canonical docs
MIGRATIONS=V7__operational_prospects_contacts_and_timeline.sql
TEST_COMMANDS=focused integration; mvn verify; npm run build; Docker build/up; smoke; repository safety; Playwright
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=deprecated API and Mockito dynamic-agent notices; anonymous auth/me logs expected 401 in browser console
RESIDUAL_RISKS=bulk actions and configurable columns remain part of later frontend hardening
```

## CHECKPOINT_3_COMMERCIAL_LIFECYCLE

```text
CHECKPOINT_ID=CHECKPOINT_3
PHASE=explicit_commercial_lifecycle
START_COMMIT=bf64bb0
END_COMMIT=58c303d
FILES_CHANGED=ProspectLifecycle, ProspectOperationsService, V7 status history, tests and UI transitions
MIGRATIONS=V7 status history and lifecycle columns
TEST_COMMANDS=ProspectLifecycleTest; OperationalProspectIntegrationTest; Maven verify; Playwright transition
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=opportunity-aware PROPOSAL uses a documented exception until SEG-006 provides opportunities
RESIDUAL_RISKS=opportunity linkage intentionally belongs to SEG-006
```

## CHECKPOINT_4_ACTIVITIES_TASKS_TIMELINE

```text
CHECKPOINT_ID=CHECKPOINT_4
PHASE=notes_activities_tasks_timeline
START_COMMIT=bf64bb0
END_COMMIT=58c303d
FILES_CHANGED=activity module, TimelineController/Service, V7, integration test and operational frontend
MIGRATIONS=V7 prospect_note activity crm_task
TEST_COMMANDS=OperationalProspectIntegrationTest; Maven verify 39/39; frontend build; Docker smoke; Playwright flow
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=timeline uses generic event categories plus typed titles by contract
RESIDUAL_RISKS=automated frontend regression suite remains for SEG-011; current flow was executed by Playwright CLI
```
