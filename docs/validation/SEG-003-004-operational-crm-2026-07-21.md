# Validación SEG-003 y SEG-004 — CRM operativo

Fecha: 2026-07-21  
Rama: `feat/complete-crm-platform`  
Commit de inicio: `bf64bb0`

## Resultado

```text
FLYWAY_V1_V7_FROM_EMPTY=PASS
FLYWAY_V6_V7_EXISTING_VOLUME=PASS
HIBERNATE_VALIDATE=PASS
PROSPECT_CONTACT_TIMELINE_INTEGRATION=PASS
LIFECYCLE_UNIT=PASS
MAVEN_VERIFY_39_OF_39=PASS
FRONTEND_TYPESCRIPT_BUILD=PASS
DOCKER_BACKEND_FRONTEND_BUILD=PASS
DOCKER_SERVICES_HEALTHY=PASS
HOST_SMOKE_COOKIE_CSRF=PASS
PLAYWRIGHT_OPERATIONAL_FLOW=PASS
REPOSITORY_SAFETY=PASS
REAL_COMMUNICATIONS=NOT_USED
```

## Pruebas ejecutadas

- `mvnw.cmd -B -ntp -f backend/pom.xml verify`: 39 pruebas, cero fallos;
- `npm run build`: TypeScript y Vite PASS;
- `docker compose --profile app build backend frontend`: PASS;
- `docker compose --profile app up -d --force-recreate`: PostgreSQL, backend y
  frontend healthy; el volumen V6 migró a V7 sin destrucción;
- `scripts/smoke-test.ps1`: health, cookie/CSRF, API autenticada y frontend PASS;
- `scripts/check-repository-safety.ps1`: PASS;
- Playwright CLI sobre `http://localhost:5173`: login, alta manual, contacto,
  nota, tarea, transición a `QUALIFYING`, finalización de tarea y timeline PASS.

La primera ejecución focalizada falló porque el fixture usó el consentimiento
inválido `CONSENTED`; el contrato acepta únicamente `UNKNOWN`, `GRANTED` o
`DENIED`. Se corrigió el fixture sin relajar la validación. Una segunda aserción
esperaba tipos específicos donde el timeline expone categoría y título por
separado; se alineó la prueba con el contrato real (`ACTIVITY`/`STATUS` más
`PHONE_CALL`/transición en el título).

## Cobertura relevante

El test integrado demuestra PostgreSQL real, tenant/actor, normalización,
sanitización, contacto create/update/delete, tarea create/complete, próxima
acción, actividad outbound, transición reglada, CSV anti-formula, conflicto
optimista, archivo/restauración y timeline combinado.

## Límites

Este cierre no afirma oportunidades, duplicados, campañas, mensajería, outbox ni
producción. Esas capacidades pertenecen a SEG-005+ y permanecen en la matriz con
su estado real.
