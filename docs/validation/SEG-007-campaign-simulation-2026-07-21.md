# Validación SEG-007 — Campañas y simulación segura

Fecha: 2026-07-21
Plataforma: Windows 11, Java 21.0.7, Docker Desktop 29.3.1, PostgreSQL 17
Rama: `feat/complete-crm-platform`

## Resultado

```text
CAMPAIGN_LIFECYCLE=EXECUTED_PASS
FROZEN_AUDIENCE=EXECUTED_PASS
EXCLUSION_AND_ELIGIBILITY=EXECUTED_PASS
VERSIONED_TEMPLATES=EXECUTED_PASS
SAFE_RENDERER=EXECUTED_PASS
DECLARATIVE_SEQUENCE=EXECUTED_PASS
APPROVAL_RBAC=EXECUTED_PASS
FAKE_SIMULATION=EXECUTED_PASS
PERSISTENT_KILL_SWITCH=EXECUTED_PASS
REAL_NETWORK_SENDING=NOT_APPLICABLE
```

## Evidencia ejecutada

- focused campaign/renderer: 5/5 PASS; Flyway V1–V10 y PostgreSQL 17;
- campaign + renderer + duplicate merge: 8/8 PASS;
- `mvnw.cmd -B -ntp -f backend\pom.xml verify`: 50/50 PASS, Spotless y
  ArchUnit PASS, 4 min 05 s;
- `SecurityAuthorizationIntegrationTest`: 10/10 PASS; `VIEWER` lee y recibe
  `403` al intentar crear una plantilla;
- `npm run build`: TypeScript estricto y Vite PASS, incluido el editor de
  secuencia declarativa;
- `docker compose --profile app build backend frontend`: PASS;
- stack local: tres servicios healthy y volumen sintético migrado V9→V10;
- smoke host y repository safety: PASS;
- Playwright: alta de prospecto/contacto sintético, plantilla, preview, campaña,
  freeze, audiencia visible, aprobación y simulación de dos borradores: PASS;
- consulta PostgreSQL: campaña `SIMULATED`, `dry_run=true`, límite 0 y dos
  actividades fake;
- inspección de ambiente y settings persistentes: `enabled=false`,
  `dry-run=true`, `daily-limit=0`, `kill-switch=true` en ambas capas.

## Fallos encontrados y corregidos

1. La primera consulta usó un nombre denormalizado inexistente en `prospect`; se
   corrigió para tomar la fuente canónica `institution.name`.
2. Los fixtures de audiencia compartían organización y podían contaminar
   conteos entre métodos; cada caso quedó aislado por provincia sintética.
3. El primer fixture de autorización tenía body inválido y Spring devolvía `400`
   antes del método; el request válido demostró el `403` de RBAC.

## Evidencia local fuera de Git

```text
validation-output/seg007-playwright-local-20260721/
```

No se versionaron datos reales, cuerpos renderizados ni credenciales.
