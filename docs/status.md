# Estado actual

Actualizado: 2026-07-22

## Repositorio y consolidación

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
rama de ejecución actual: feat/complete-crm-platform
producción: NO DESPLEGADA
comunicaciones: DESHABILITADAS
lote real: FUERA DE GIT/CI/IMÁGENES
```

`main` continúa siendo la única fuente de verdad. La segunda validación integral
limpia de SEG-001 se ejecutó sobre
`d8a5a449a72c660e2655f4be7144360cd1e719a4` con el lockfile versionado.

## Segmentos

| ID | Estado | Resumen |
|---|---|---|
| SEG-000 | COMPLETE | repositorio, continuidad y documentación canónica |
| SEG-001 | COMPLETE | vertical slice, seguridad, validación local integral y CI verdes |
| SEG-002 | COMPLETE | identidad, organizaciones, usuarios, sesiones y RBAC |
| SEG-003 | COMPLETE | prospectos operativos, contactos y ciclo comercial |
| SEG-004 | COMPLETE | actividades, notas, tareas y timeline |
| SEG-005 | COMPLETE | resolución transaccional de duplicados |
| SEG-006 | COMPLETE | oportunidades y pipeline |
| SEG-007 | COMPLETE | campañas, audiencias, plantillas y simulación |
| SEG-008 | COMPLETE | mensajería segura y adaptadores deshabilitados |
| SEG-009 | COMPLETE | outbox PostgreSQL, workers e inbound fake durable |
| SEG-010 | COMPLETE | reportes, búsqueda, seguridad, observabilidad y producción local |
| SEG-011 | COMPLETE | validación integral Windows limpia y reproducible |

## Estados operativos

```text
IMPLEMENTATION_COMPLETE
HARDENING_COMPLETE
MAIN_CONSOLIDATED
CROSS_PLATFORM_VALIDATION_IMPLEMENTED
POWERSHELL_SYNTAX_PASS
PREFLIGHT_PASS
WINDOWS_AND_DOCKER_PORT_CHECK_PASS
POSTGRES_PUBLICATION_PASS
POSTGRES_HEALTH_PASS
FRONTEND_CLEAN_BUILD_PASS
BACKEND_CLEAN_IMAGE_BUILD_PASS
FLYWAY_V1_V5_PASS
HIBERNATE_VALIDATE_PASS
BACKEND_HEALTH_PASS
FRONTEND_HEALTH_PASS
SMOKE_HOST_PASS
SMOKE_CONTAINER_PASS
MAVEN_VERIFY_PASS
TESTS_29_OF_29_PASS
SPOTLESS_55_OF_55_PASS
ARCHUNIT_PASS
TESTCONTAINERS_PASS
LOCKFILE_VERSIONED
NPM_CI_CLEAN_RUN_PASS
REPOSITORY_SAFETY_PASS
CI_VISIBLE_GREEN
SEG_001_COMPLETE
FLYWAY_V1_V6_PASS
AUTH_COOKIE_CSRF_PASS
RBAC_AND_TENANT_ISOLATION_PASS
SECURITY_TESTS_10_OF_10_PASS
MAVEN_VERIFY_TESTS_36_OF_36_PASS
FRONTEND_SESSION_BUILD_PASS
SMOKE_COOKIE_CSRF_HOST_AND_CONTAINER_PASS
SEG_002_COMPLETE
FLYWAY_V1_V7_PASS
PROSPECT_CONTACT_CRUD_PASS
OPTIMISTIC_LOCKING_AND_CSV_PASS
COMMERCIAL_LIFECYCLE_PASS
ACTIVITY_TASK_TIMELINE_PASS
MAVEN_VERIFY_TESTS_39_OF_39_PASS
FRONTEND_OPERATIONAL_BUILD_PASS
PLAYWRIGHT_OPERATIONAL_FLOW_PASS
SEG_003_COMPLETE
SEG_004_COMPLETE
FLYWAY_V1_V8_PASS
DUPLICATE_RESOLUTION_ACTIONS_PASS
TRANSACTIONAL_IDEMPOTENT_MERGE_PASS
MAVEN_VERIFY_TESTS_42_OF_42_PASS
PLAYWRIGHT_DUPLICATE_REVIEW_PASS
SEG_005_COMPLETE
FLYWAY_V1_V9_PASS
OPPORTUNITY_TRANSITIONS_AND_HISTORY_PASS
WON_LOST_DOMAIN_RULES_PASS
PIPELINE_FORECAST_AND_AGING_PASS
MAVEN_VERIFY_TESTS_45_OF_45_PASS
PLAYWRIGHT_PIPELINE_FLOW_PASS
SEG_006_COMPLETE
FLYWAY_V1_V10_PASS
FROZEN_AUDIENCE_AND_EXCLUSIONS_PASS
SAFE_TEMPLATE_RENDERER_PASS
CAMPAIGN_APPROVAL_RBAC_PASS
FAKE_CAMPAIGN_SIMULATION_PASS
PERSISTENT_SENDING_BLOCKADE_PASS
MAVEN_VERIFY_TESTS_50_OF_50_PASS
PLAYWRIGHT_CAMPAIGN_SIMULATION_PASS
SEG_007_COMPLETE
FLYWAY_V1_V11_PASS
SAFE_MESSAGING_CONTRACTS_PASS
NOOP_AND_FAKE_PROVIDERS_PASS
MESSAGE_POLICY_AND_PERSISTENT_KILL_SWITCH_PASS
REAL_PROVIDER_INITIALIZATION_FAIL_CLOSED_PASS
MESSAGE_SEND_ENDPOINT_ABSENT_PASS
PLAYWRIGHT_FAKE_MESSAGE_SIMULATION_PASS
SEG_008_COMPLETE
FLYWAY_V1_V12_PASS
TRANSACTIONAL_OUTBOX_PASS
SKIP_LOCKED_LEASE_RECOVERY_PASS
RETRY_DEAD_LETTER_REQUEUE_PASS
TENANT_IDEMPOTENCY_PASS
FAKE_INBOUND_HMAC_REPLAY_PASS
QUARANTINE_MANUAL_ASSOCIATION_PASS
INBOUND_ACTIVITY_TASK_TIMELINE_PASS
MAVEN_VERIFY_TESTS_69_OF_69_PASS
PLAYWRIGHT_OUTBOX_INBOUND_PASS
FORBIDDEN_MESSAGE_STATES_ZERO
SEG_009_COMPLETE
FLYWAY_V1_V13_PASS
REPORTING_SEARCH_SETTINGS_TAGS_PASS
CSV_FORMULA_SAFETY_PASS
CORRELATION_ID_AND_METRICS_PASS
MAVEN_VERIFY_TESTS_79_OF_79_PASS
SPOTLESS_159_OF_159_PASS
VITEST_2_OF_2_PASS
PLAYWRIGHT_COMPLETE_FLOW_2_OF_2_PASS
BACKUP_RESTORE_SYNTHETIC_PASS
PRODUCTION_PROFILE_EXECUTED_PASS_LOCALLY
GRYPE_HIGH_CRITICAL_PASS
NPM_AUDIT_HIGH_PASS
REAL_XLSX_PREVIEW_BLOCKED_EXTERNAL_FILE
PRODUCTION_DEPLOYMENT_NOT_AUTHORIZED
COMPLETE_CRM_WINDOWS_RUN_1_PASS
COMPLETE_CRM_WINDOWS_RUN_2_PASS
SEG_010_COMPLETE
SEG_011_COMPLETE
```

## Fallo Jackson corregido

La ejecución sobre `a9e2c44de5d4d9181ef304976597d9d8b1a30014` demostró
Flyway V1–V5 y Hibernate, pero falló al crear `AuditEventWriter`: el código propio
pedía `com.fasterxml.jackson.databind.ObjectMapper` mientras Spring Boot 4.1
administraba `tools.jackson.databind.ObjectMapper` de Jackson 3.1.4.

La corrección migró auditoría/importación a Jackson 3, mantuvo un único mapper
administrado por Spring y agregó una regresión de contexto real y persistencia
JSONB para mapa, UUID, fechas, enum, null y JSON válido. Jackson 2 permanece solo
como dependencia transitiva de springdoc/Swagger.

Evidencia completa:

```text
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
```

## Validación integral final — Windows

```text
commit ejecutado: d8a5a449a72c660e2655f4be7144360cd1e719a4
plataforma: Windows 11 + Docker Desktop 29.3.1
PostgreSQL host port: 25432
Backend host port: 8080
Frontend host port: 5173
inicio UTC: 2026-07-21T16:30:02Z
fin UTC: 2026-07-21T16:39:56Z
resultado: PASS
```

El primer build frontend usó `npm ci`, ambos builds fueron `--no-cache`, Flyway
validó/aplicó V1–V5 antes de Hibernate, los tres servicios quedaron `healthy`,
ambos smoke pasaron, Maven verificó 29 tests, Spotless dejó 55/55 archivos
limpios, ArchUnit y Testcontainers pasaron y el escaneo del repositorio pasó.

Evidencia local, deliberadamente fuera de Git:

```text
validation-output/seg001-complete-20260721-133002.log
validation-output/seg001-complete-20260721-133002.json
validation-output/seg001-docker-20260721-133003.json
```

Lockfile:

```text
ruta: frontend/package-lock.json
commit: d8a5a449a72c660e2655f4be7144360cd1e719a4
SHA-256: 1936217c0598825ef43519069a3ba89a974e2b30e3b9f2619d4e62dd10810c98
node_modules host: AUSENTE
lifecycle scripts durante generación: NO EJECUTADOS
```

## CI

GitHub Actions run `29848718163` para `d8a5a449…` está visible y terminó
`success`. Jobs `backend`, `frontend`, `scripts` y `compose-images-and-smoke`
terminaron verdes.

## Seguridad vigente

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

También permanece prohibido:

- desplegar producción;
- habilitar Gmail/SMTP o cualquier adaptador de envío;
- importar el XLSX real en Git, CI o imágenes;
- versionar `.env` o `validation-output/`;
- usar `docker compose down -v` salvo destrucción intencional;
- usar `docker system prune` como diagnóstico normal;
- alterar los segmentos ya cerrados sin una regresión y nueva evidencia.

## Ejecución integral activa

La misión autorizada el 2026-07-21 activó SEG-002 en la rama
`feat/complete-crm-platform`. El baseline SEG-001 se repitió sobre `7db7e4c` y
pasó integralmente antes de cambios. El plan, progreso y matriz vigentes están
en `docs/execution/` y `docs/validation/COMPLETE-CRM-matrix.md`.

Las comunicaciones reales y producción continúan fuera de autorización.

## Cierre SEG-010 y SEG-011

Flyway V1–V13, 79/79 backend, Spotless 159/159, ArchUnit, frontend
unit/build/E2E, backup/restore, production profile local y el escaneo de imagen
pasaron dentro del validador integral dos veces sobre `986523a`. Las corridas
`complete-crm-20260722-173731.json` y
`complete-crm-20260722-174938.json` finalizaron `FUNCTIONAL_PASS` en 713,870 s
y 734,162 s. Ambas comenzaron y terminaron con árbol limpio y retiraron solo su
proyecto Compose y volumen sintético.

La imagen backend usa JRE mínima no-root fijada por digest y healthcheck Java.
Grype inicialmente detectó vulnerabilidades High/Critical en el runtime Alpine;
se corrigió la causa sin exclusiones y el nuevo scan devolvió cero hallazgos.
Dependency-Check quedó `BLOCKED_EXTERNAL_NVD_RATE_LIMIT` y no se cuenta como
PASS.

El validador Unix integral y el workflow remoto permanecen `IMPLEMENTED_NOT_RUN`;
la sintaxis Bash y el preflight container-only sí se ejecutaron localmente desde
WSL. Gmail/WhatsApp continúan `IMPLEMENTED_NOT_CONNECTED`, el XLSX real
`BLOCKED_EXTERNAL_FILE` y producción `NOT_AUTHORIZED`/`NOT_DEPLOYED`.
