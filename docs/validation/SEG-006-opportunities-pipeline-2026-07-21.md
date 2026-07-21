# Validación SEG-006 — Oportunidades y pipeline

Fecha: 2026-07-21  
Plataforma: Windows 11, Java 21.0.7, Docker Desktop 29.3.1, PostgreSQL 17  
Rama: `feat/complete-crm-platform`

## Resultado

```text
OPPORTUNITY_CRUD=EXECUTED_PASS
STAGE_TRANSITIONS=EXECUTED_PASS
OPTIMISTIC_LOCKING=EXECUTED_PASS
WON_PROSPECT_SYNC=EXECUTED_PASS
LOST_REASON_POLICY=EXECUTED_PASS
TENANT_BOUNDARY=EXECUTED_PASS
PIPELINE_METRICS=EXECUTED_PASS
FRONTEND_BUILD=EXECUTED_PASS
PLAYWRIGHT_PIPELINE_FLOW=EXECUTED_PASS
```

## Evidencia ejecutada

- `mvnw.cmd -B -ntp -f backend\pom.xml spotless:apply
  -Dtest=OpportunityIntegrationTest test`: 3/3 PASS, 59.8 s;
- `mvnw.cmd -B -ntp -f backend\pom.xml verify`: 45/45 PASS, Flyway V1–V9,
  Spotless y ArchUnit PASS, 5 min 53 s;
- `npm run build`: TypeScript y Vite PASS;
- `docker compose --profile app build backend frontend`: PASS;
- `docker compose --profile app up -d --no-build`: PostgreSQL, backend y
  frontend healthy; Flyway actualizó el volumen sintético V8→V9;
- `scripts/smoke-test.ps1 -BackendPort 8080 -FrontendPort 5173`: health,
  sesión/API y frontend PASS;
- `scripts/check-repository-safety.ps1`: PASS;
- Playwright CLI sobre `http://localhost:5173`: login, creación por ARS 250.000,
  forecast, movimientos hasta `WON`, probabilidad 100 % y prospecto `CUSTOMER`:
  PASS.

## Cobertura de dominio

Las pruebas integradas verifican etapas permitidas y rechazadas, historia,
cierre ganado, cancelación de tareas, cierre perdido con motivo, conflicto de
versión, oportunidad principal única, métricas exactas y aislamiento entre dos
organizaciones. Cada Testcontainer aplicó V1–V9 desde vacío; el volumen local
sintético migró desde V8.

## Evidencia local fuera de Git

```text
validation-output/seg006-playwright-local-20260721/
```

Los datos son sintéticos. Los artefactos no se versionan.
