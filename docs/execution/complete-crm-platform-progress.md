# Progreso de ejecución integral del CRM

Actualizado: 2026-07-22

## Reanudación 2026-07-22

```text
RESUME_TIMESTAMP=2026-07-22T09:50:08.6378184-03:00
RESUME_BRANCH=feat/complete-crm-platform
RESUME_HEAD=1a6a98bc5bd2f9cf97a7c2978cd633ba77f74cb3
LAST_KNOWN_GOOD_COMMIT_PRESENT=true (HEAD exacto y ancestor-check-exit=0)
WORKTREE_STATE=CLEAN
UNCOMMITTED_FILES=none
LOCAL_COMMITS_AFTER_1A6A98B=none
REMOTE_DIVERGENCE=origin/main...HEAD: 0 behind, 13 ahead; origin/main...main: 0/0 after git fetch origin
SENSITIVE_FILE_CHECK=PASS; no tracked validation-output, .playwright-cli, XLSX, .env, logs or tsbuildinfo
RESUME_DECISION=continue from the exact validated SEG-008 checkpoint; no partial SEG-009 work exists
```

La inspección de configuraciones peligrosas encontró solamente el endpoint del
adaptador Gmail deliberadamente no conectado, la regresión que exige `404` para
`/api/v1/messages/send` y referencias documentales. No encontró una ruta pública
de envío ni valores permisivos versionados.

## RESUME_BASELINE_VALIDATION

```text
RESUME_BASELINE_COMMIT=1a6a98bc5bd2f9cf97a7c2978cd633ba77f74cb3
RESUME_BASELINE_COMMANDS=scripts/verify-backend-container.ps1; npm ci; npm run typecheck; npm run build; scripts/check-powershell-syntax.ps1; scripts/check-repository-safety.ps1; bash -n on 8 tracked scripts; scripts/validate-docker-stack.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173 -KeepRunning; authenticated send-path and PostgreSQL guards
RESUME_BASELINE_TEST_COUNT=57/57 backend; frontend had no versioned automated test suite at resume
RESUME_BASELINE_RESULT=EXECUTED_PASS
RESUME_BASELINE_FAILURES=none
RESUME_BASELINE_ROOT_CAUSE=not applicable
RESUME_BASELINE_FIXES=none
```

Evidencia ejecutada desde el checkpoint sin cambios:

- backend Java 21 contenedorizado recompiló 100 fuentes, aplicó Flyway V1–V11
  desde vacío, validó Hibernate, ejecutó 57/57 pruebas, ArchUnit y Spotless
  119/119 en 03:09 min;
- frontend Node 22 ejecutó `npm ci`, TypeScript estricto y Vite build (17
  módulos, 686 ms);
- stack Docker sin caché quedó healthy en PostgreSQL `25432`, backend `8080`
  y frontend `5173`; smoke autenticado host/contenedor PASS en 07:07 min;
- repository safety, sintaxis de 11 scripts PowerShell y 8 scripts Bash: PASS;
- ambiente y PostgreSQL conservaron los cuatro bloqueos, providers `NOOP` /
  `DEEPLINK_ONLY`, red real deshabilitada, endpoint público de envío `404` y
  conteo PostgreSQL de mensajes `SENT=0`;
- evidencia local ignorada: `validation-output/seg001-docker-20260722-094016.*`.

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

## CHECKPOINT_5_DUPLICATE_RESOLUTION

```text
CHECKPOINT_ID=CHECKPOINT_5
PHASE=duplicate_review_and_transactional_merge
START_COMMIT=c57652c
END_COMMIT=157f3a5
FILES_CHANGED=deduplication module, DuplicateReview status, V8, integration test, frontend and canonical docs
MIGRATIONS=V8__duplicate_resolution_and_merge_trace.sql
TEST_COMMANDS=focused Testcontainers test; Maven verify; npm build; Docker build/up; smoke; repository safety; Playwright
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=deprecated API and Mockito dynamic-agent notices; expected anonymous auth/me 401 in browser console
RESIDUAL_RISKS=opportunity and campaign references will be added to merge propagation when their tables are introduced
```

Fallos encontrados y corregidos:

1. los conteos iniciales del fixture de merge eran globales y dependían del
   orden de tests: quedaron acotados por `duplicate_review_id`;
2. el enum JPA heredado no reconocía estados V8: se agregaron `DEFERRED` y
   `RESOLVED` sin eliminar valores históricos;
3. un usuario `VIEWER` hacía consultas globales a auditoría y duplicados que el
   backend rechazaba correctamente: la UI ahora consulta y navega según permisos.

El recorrido Playwright ejecutó un preview sintético que produjo una revisión
ambigua, verificó comparación lado a lado y resolvió por vínculo al prospecto
existente. El merge completo y su rollback se probaron en PostgreSQL real.

## CHECKPOINT_6_OPPORTUNITIES_PIPELINE

```text
CHECKPOINT_ID=CHECKPOINT_6
PHASE=opportunities_and_sales_pipeline
START_COMMIT=1e32a90a34c3fb457284071dc1b73ade2a9f4c0a
END_COMMIT=6f7b7a727684e672269433a70f881e15dedc1388
FILES_CHANGED=sales module, duplicate merge propagation, V9, integration test and frontend pipeline
MIGRATIONS=V9__opportunities_and_sales_pipeline.sql
TEST_COMMANDS=focused Testcontainers test; Maven verify; npm build; Docker build/up; smoke; repository safety; Playwright
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=Hikari emitted closed Testcontainer connection warnings during long suite; no test failure or data impact
RESIDUAL_RISKS=custom pipelines and cross-currency aggregation intentionally belong to organization settings; V9 enforces one tenant currency
```

El recorrido Playwright creó una oportunidad ARS 250.000, verificó forecast
ponderado, avanzó por etapas permitidas hasta `WON`, comprobó probabilidad
100 %, cierre de la oportunidad y transición del prospecto a `CUSTOMER`.

## CHECKPOINT_7_CAMPAIGNS_TEMPLATES_SIMULATION

```text
CHECKPOINT_ID=CHECKPOINT_7
PHASE=campaigns_audiences_templates_sequences
START_COMMIT=739c6861775678ca1d65e86c12fb9b92a8d02877
END_COMMIT=47806257d9836a0753a3b0be617d968ef67a7533
FILES_CHANGED=campaign module, duplicate propagation, V10, security/integration tests and frontend campaigns
MIGRATIONS=V10__campaigns_audiences_templates_and_simulation.sql
TEST_COMMANDS=focused Testcontainers; Maven verify; security focused; npm build; Docker build/up; smoke; repository safety; Playwright; PostgreSQL policy query
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=Mockito dynamic-agent notice and transient Hikari warnings after Testcontainers close
RESIDUAL_RISKS=real providers intentionally absent until SEG-008; no real network sending exists
```

Playwright creó una plantilla, verificó preview sintético, congeló dos
destinatarios, aprobó y simuló. PostgreSQL registró dos actividades
`EMAIL_DRAFTED`, cero envíos y los cuatro bloqueos activos.

## CHECKPOINT_8_SAFE_MESSAGING

```text
CHECKPOINT_ID=CHECKPOINT_8
PHASE=safe_messaging_and_disabled_adapters
START_COMMIT=cf3e8ed4a5c78ceecffdb91cf9487ae0537d5b06
END_COMMIT=working_tree_validated_before_local_commit
FILES_CHANGED=messaging module, duplicate merge propagation, V11, frontend messages/integrations, tests and docs
MIGRATIONS=V11__safe_messaging_and_integrations.sql
TEST_COMMANDS=focused 20 tests; mvn verify; npm build; Docker no-cache build/up; smoke; repository safety; Playwright; PostgreSQL policy query
TEST_RESULT=EXECUTED_PASS
KNOWN_WARNINGS=Mockito dynamic-agent and closed Testcontainer Hikari notices; Meta docs access/rate limit
RESIDUAL_RISKS=Gmail OAuth and WhatsApp Cloud require external accounts, credentials, domain and provider verification
```

Fallos encontrados y corregidos:

1. el runtime Playwright no incluía su Chromium descargado: se reutilizó Chrome
   del sistema sin instalar dependencias;
2. la primera captura se hizo antes de que terminara la carga asíncrona de
   prospectos: el recorrido esperó condiciones de opciones reales;
3. el merge de contactos podía dejar referencias de mensajes en canales
   absorbidos: ahora remapea mensajes antes de consolidar canales y tiene
   regresión PostgreSQL;
4. el primer comando Maven focalizado fue interpretado por PowerShell debido a
   la coma: el selector `-Dtest` quedó entre comillas y ejecutó 20/20.

Evidencia ejecutada:

- Flyway V1–V11 desde vacío y V10→V11 sobre volumen local: PASS;
- focused messaging/provider/merge/security: 20/20;
- Maven verify integral: 57/57 en 03:04 min, Spotless 119/119 y ArchUnit PASS;
- frontend TypeScript/Vite: PASS;
- Docker PostgreSQL/backend/frontend healthy y smoke autenticado: PASS;
- Playwright fake simulation con `BLOCKED_BY_KILL_SWITCH`: PASS;
- configuración ambiente/DB bloqueada y filas `SENT`: cero;
- repository safety y `git diff --check`: PASS.

## CHECKPOINT_9_OUTBOX_WORKERS_INBOUND

```text
CHECKPOINT_ID=CHECKPOINT_9
PHASE=transactional_outbox_workers_idempotency_inbound
START_COMMIT=1a6a98bc5bd2f9cf97a7c2978cd633ba77f74cb3
END_COMMIT=fe29f66f556019980dc414132968cfb22ad3c44c
WORKTREE_BEFORE=CLEAN
WORKTREE_AFTER=CLEAN_AFTER_FE29F66
FILES_CHANGED=V12, outbox/inbound/common backend modules, messaging/campaign integration, security, frontend operations, tests, ADR and canonical docs
MIGRATIONS=V12__transactional_outbox_and_durable_inbound.sql
TEST_COMMANDS=verify-backend-container.ps1; npm run typecheck; npm run build; validate-docker-stack.ps1 --no-cache; authenticated Playwright; signed webhook HTTP cases; PostgreSQL evidence; repository safety
TEST_COUNT=69/69 backend; 6/6 OutboxInboundIntegrationTest within full suite
TEST_RESULT=EXECUTED_PASS
DURATION=backend 02:28; Docker no-cache approximately 13:50; frontend build 270 ms
FAILURES=local Windows Ryuk callback blocked before tests; stale prospect 409; manual association SQL type inference
ROOT_CAUSES=named-pipe callback topology; selected DTO not refreshed after background mutation; untyped nullable PostgreSQL placeholder
FIXES=canonical container validator; typed ApiError plus refresh; static SQL branches plus PostgreSQL regression
KNOWN_WARNINGS=Mockito self-attach, Spring Data PageImpl warning, expected anonymous auth 401 console events
RESIDUAL_RISKS=real inbound/providers remain not connected; scheduler disabled in local evidence; production not deployed
```

Evidencia ejecutada:

- Flyway V1–V12 desde vacío y upgrade V11→V12, Hibernate validate: PASS;
- backend Java 21: 69/69, Spotless 148/148 y ArchUnit PASS;
- frontend TypeScript strict y Vite: PASS;
- Docker no-cache y tres servicios healthy en 25432/8080/5173: PASS;
- Playwright: campaña bloqueada, outbox, worker, webhook HMAC/replay,
  quarantine/asociación manual, actividad, tarea, timeline y `REPLIED`: PASS;
- firma/timestamp/media type/payload limit: 401/401/415/413 esperados;
- PostgreSQL: `SENT|DELIVERED|READ=0`, outbox `BLOCKED=1/SUCCEEDED=4`;
- endpoint `/api/v1/messages/send=404`, providers reales no inicializados;
- secreto fake solo en entorno de prueba y endpoint nuevamente deshabilitado.

## CHECKPOINT_10_OPERATIONS_PRODUCTION_CANDIDATE

```text
CHECKPOINT_ID=CHECKPOINT_10
PHASE=reporting_search_settings_tags_security_observability_production
START_COMMIT=645fa6c0a4594b23b7ab90ff62225208df243c73
END_COMMIT=7b490d85801c9c454a99e81549cebeea3d6ca162
WORKTREE_BEFORE=CLEAN
WORKTREE_AFTER=CLEAN_AFTER_7B490D8
FILES_CHANGED=V13, reporting/settings/tags/search/import/common/security backend, frontend, tests, backup/restore, production profile, CI, validators and docs
MIGRATIONS=V13__reporting_search_settings_and_tags.sql
TEST_COMMANDS=verify-backend-container.ps1; npm ci/typecheck/test:unit/build; npm test:e2e; verify-migrations.ps1; verify-backup-restore.ps1; verify-production-profile.ps1; Grype; npm audit; repository safety; script syntax
TEST_COUNT=79/79 backend; 2/2 Vitest; 2/2 Playwright
TEST_RESULT=EXECUTED_PASS_FOCUSED; INTEGRAL_VALIDATOR_NOT_RUN
DURATION=backend 02:27; Playwright 13.3s; parser 100/1k/10k 29/168/1571ms
FAILURES=metrics denied to authenticated admin; desktop logout outside viewport; Alpine runtime and JDBC High CVEs; stale compose image ID; Dependency-Check NVD rate limit
ROOT_CAUSES=actuator matcher too restrictive; sidebar without scroll; package-heavy JRE plus outdated patch versions; compose image query referred to replaced ID; NVD update lacks API key
FIXES=authenticated metrics rule/test; scrollable sidebar/mobile override; pinned minimal non-root JRE plus Java probe and JDBC/Jackson patches; derive image tag from live container; canonical npm audit plus pinned Grype
KNOWN_WARNINGS=Mockito self-attach and PageImpl serialization; Dependency-Check BLOCKED_EXTERNAL_NVD_RATE_LIMIT
RESIDUAL_RISKS=integral validator and clean repeat pending; Unix functional/remote CI not run; real providers/XLSX/deployment external
```

Evidencia focalizada:

- backend Java 21/PostgreSQL 17: 79/79, Spotless 159/159, ArchUnit, V1–V13 y
  Hibernate validate PASS;
- frontend: npm ci, strict typecheck, Vitest 2/2, Vite y Playwright integral 2/2
  PASS;
- reporting/search/settings/tags, CSV, permisos y tenant isolation integrados;
- backup/restore aislado y perfil productivo local endurecido PASS;
- imagen backend UID 65532, healthcheck Java y Grype sin hallazgos;
- XLSX real ausente: `BLOCKED_EXTERNAL_FILE`, sin abrir/copiar/importar;
- producción, push, PR y comunicaciones reales permanecen no autorizados.

## SEG011_VALIDATION_ATTEMPT_1_SCRIPT_SYNTAX

```text
COMMIT=1f9c3d8636a08253c6ee23180193737bb32ba625
COMMAND=powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173
RESULT=EXECUTED_FAIL
FIRST_FAILURE=scriptSyntax: WSL bash could not resolve C:\laburo\... absolute paths
ROOT_CAUSE=PowerShell loop passed FileInfo.FullName to bash; the already-passing relative syntax check did not exercise that path form
FIX=pass repository-relative scripts/<name> paths for every Bash syntax check
FUNCTIONAL_PHASES_NOT_RUN=backend, frontend, images, migrations, E2E, scans, backup/restore, production profile
DATA_IMPACT=none; failure occurred before stack creation
```

## SEG011_VALIDATION_ATTEMPT_2_E2E_BOOTSTRAP

```text
COMMIT=4c212a7bc011b10312eb1060298405e2d49cef2f
COMMAND=powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173
RESULT=EXECUTED_FAIL
DURATION=692.018s
PASSED_PHASES=repository safety; PowerShell/Bash syntax; secret scan; backend 79/79; Spotless 159/159; ArchUnit; frontend install/typecheck/unit/build; Docker no-cache health/smoke; npm audit; Grype no vulnerabilities; empty-to-V13 and V11-to-V13 migrations with Hibernate validate; outbox/worker/inbound PostgreSQL suite
FIRST_FAILURE=frontendE2E: both scenarios remained on the login screen
ROOT_CAUSE=preflight.ps1 loaded every .env value into the process and overwrote the validator's explicit deterministic bootstrap credentials and fake-inbound configuration before Docker Compose created the isolated stack
FIX=preserve explicit non-empty process environment values while using .env only as fallback; assert the effective bootstrap/fake-inbound/worker environment immediately after stack creation
FOCUSED_RETRY_FAILURE=the first isolated retry then failed in smoke-test.ps1 with HTTP 401 because that host smoke independently reloaded .env with overwrite semantics
FOCUSED_RETRY_FIX=apply the same explicit-environment precedence to Windows and Unix smoke/preflight loaders before repeating the isolated stack
FOCUSED_REGRESSION_COMMAND=isolated cached Docker stack on 25432/8080/5173; host/container smoke; npm run test:e2e; PostgreSQL forbidden-state query
FOCUSED_REGRESSION_RESULT=EXECUTED_PASS; host/container smoke PASS; Playwright 2/2 in 9.9s; SENT|DELIVERED|READ=0; isolated volume removed
NOT_RUN_AFTER_FAILURE=effective sending blockade; zero SENT query; backup/restore; production profile; final tree safety
DATA_IMPACT=none; the isolated Compose project and its volume were removed by the validator finally block
```

## SEG011_VALIDATION_ATTEMPT_3_E2E_SESSION_TRANSITION

```text
COMMIT=dbf56555810a37cb16189f8967b69a6e08c07af3
COMMAND=powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173
RESULT=EXECUTED_FAIL
DURATION=863.999s
PASSED_PHASES=repository/tooling/syntax/secret checks; backend 79/79 and Spotless 159/159; frontend unit/typecheck/build; Docker no-cache health and host/container smoke; npm audit; Grype no vulnerabilities; empty-to-V13 and V11-to-V13 plus Hibernate; outbox/worker/inbound suite
FIRST_FAILURE=frontendE2E: main scenario timed out at the second login after completing the ADMIN business journey; responsive scenario passed
EVIDENCE=Playwright failure snapshot still showed the authenticated ADMIN Dashboard while the test searched for the login username field
ROOT_CAUSE=the test clicked asynchronous logout and immediately navigated for VIEWER login without waiting for the observable unauthenticated state
FIX=wait for the Ingresar heading after logout before initiating the VIEWER login; no sleep or timeout increase
FOCUSED_REGRESSION_COMMAND=npm run typecheck; isolated cached stack and authenticated smokes; npm run test:e2e; PostgreSQL forbidden-state query
FOCUSED_REGRESSION_RESULT=EXECUTED_PASS; typecheck PASS; Playwright 2/2 in 11.3s including ADMIN-to-VIEWER transition; SENT|DELIVERED|READ=0; isolated volume removed
NOT_RUN_AFTER_FAILURE=effective sending blockade; zero SENT query; backup/restore; production profile; final tree safety
DATA_IMPACT=none; isolated Compose project and volume removed by validator cleanup
```

## SEG011_VALIDATION_RUN_1

```text
COMMIT=986523a22a17f4c9159003bce88f4f6903ad7cdb
RESULT=FUNCTIONAL_PASS
DURATION=713.870s
PHASES=21/21
BACKEND=79/79
SPOTLESS=159/159
ARCHUNIT=PASS
FRONTEND_UNIT=2/2
FRONTEND_E2E=2/2
MIGRATIONS=EMPTY_TO_V13_PASS; V11_TO_V13_PASS
HIBERNATE_VALIDATE=PASS
SECURITY=NPM_AUDIT_PASS; GRYPE_HIGH_CRITICAL_PASS
OUTBOX_WORKERS_INBOUND=PASS
MESSAGING_BLOCKS=PASS
FORBIDDEN_STATES=0
BACKUP_RESTORE=PASS
PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY
CLEANUP=ISOLATED_COMPOSE_AND_SYNTHETIC_VOLUME_ONLY
WORKTREE_AFTER=CLEAN
```

## SEG011_VALIDATION_RUN_2

```text
COMMIT=986523a22a17f4c9159003bce88f4f6903ad7cdb
RESULT=FUNCTIONAL_PASS
DURATION=734.162s
PHASES=21/21
BACKEND=79/79
SPOTLESS=159/159
ARCHUNIT=PASS
FRONTEND_UNIT=2/2
FRONTEND_E2E=2/2
MIGRATIONS=EMPTY_TO_V13_PASS; V11_TO_V13_PASS
HIBERNATE_VALIDATE=PASS
SECURITY=NPM_AUDIT_PASS; GRYPE_HIGH_CRITICAL_PASS
OUTBOX_WORKERS_INBOUND=PASS
MESSAGING_BLOCKS=PASS
FORBIDDEN_STATES=0
BACKUP_RESTORE=PASS
PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY
CLEANUP=ISOLATED_COMPOSE_AND_SYNTHETIC_VOLUME_ONLY
WORKTREE_AFTER=CLEAN
REPRODUCIBILITY=PASS
FLAKINESS=NOT_OBSERVED
```

## CHECKPOINT_11_COMPLETE_CRM_CLOSURE

```text
START_COMMIT=986523a22a17f4c9159003bce88f4f6903ad7cdb
PREVIOUS_DOCUMENTATION_COMMIT=3c42d8548dcb77976e36c03df4772f9bf1c6e803
FINAL_BRANCH=main
SEG_010=COMPLETE
SEG_011=COMPLETE
WINDOWS_VALIDATION=EXECUTED_PASS_TWICE
UNIX_VALIDATION=IMPLEMENTED_NOT_RUN
CI_REMOTE_BEFORE_PUBLICATION=IMPLEMENTED_NOT_RUN
REAL_PROVIDERS=IMPLEMENTED_NOT_CONNECTED
REAL_XLSX=BLOCKED_EXTERNAL_FILE
PRODUCTION=NOT_AUTHORIZED_NOT_DEPLOYED
REAL_SENDING=DISABLED_BY_POLICY
```
