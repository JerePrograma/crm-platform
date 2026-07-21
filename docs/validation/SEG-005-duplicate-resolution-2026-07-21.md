# Validación SEG-005 — Resolución de duplicados

Fecha: 2026-07-21  
Plataforma: Windows 11, Java 21.0.7, Docker Desktop 29.3.1, PostgreSQL 17  
Rama: `feat/complete-crm-platform`

## Resultado

```text
DUPLICATE_QUEUE=EXECUTED_PASS
ALL_RESOLUTION_ACTIONS=EXECUTED_PASS
TRANSACTIONAL_MERGE=EXECUTED_PASS
IDEMPOTENT_RETRY=EXECUTED_PASS
REFERENCE_PRESERVATION=EXECUTED_PASS
TENANT_BOUNDARY=EXECUTED_PASS
AUDIT_TRACE=EXECUTED_PASS
FRONTEND_BUILD=EXECUTED_PASS
PLAYWRIGHT_REVIEW_FLOW=EXECUTED_PASS
```

## Evidencia ejecutada

- `mvnw.cmd -B -ntp -f backend\pom.xml spotless:apply
  -Dtest=DuplicateResolutionIntegrationTest test`: 3/3 PASS, 53.9 s;
- `mvnw.cmd -B -ntp -f backend\pom.xml verify`: 42/42 PASS,
  Spotless y ArchUnit PASS, 3 min 14 s;
- `npm run build`: TypeScript y Vite PASS;
- `docker compose --profile app build backend frontend`: PASS;
- `docker compose --profile app up -d --no-build`: PostgreSQL, backend y
  frontend healthy; Flyway actualizó el volumen sintético V7→V8;
- `scripts/smoke-test.ps1`: health, sesión/API y frontend PASS;
- `scripts/check-repository-safety.ps1`: PASS;
- Playwright CLI sobre `http://localhost:5173`: login, alta sintética, preview
  CSV, `REVIEW_REQUIRED=1`, comparación nominal 89 %, seis acciones visibles,
  resolución `LINK_TO_EXISTING` y bandeja vacía: PASS.

## Cobertura del merge

La prueba integrada verifica combinación de contactos y canales, traslado de
notas, actividades, tareas e import evidence, mapa de IDs, archivo/redirección
del absorbido, reintento con la misma clave y rollback ante un destino inválido.
La migración V8 fue aplicada desde esquema vacío por cada Testcontainer y sobre
el volumen local V7 sin editar V1–V7.

## Evidencia local fuera de Git

```text
validation-output/seg005-ambiguous.csv
validation-output/seg005-playwright-local-20260721/
```

Los datos son sintéticos (`example.test`). Los artefactos no se versionan.
