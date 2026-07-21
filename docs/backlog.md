# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

Rama canónica: `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | fuente canónica, reglas y documentación |
| SEG-001 | Vertical slice persistente de prospectos | COMPLETE | SEG-000 | importación, exclusiones, UI y stack con matriz verde |
| SEG-002 | Identidad, usuarios y RBAC | ACTIVE | SEG-001 COMPLETE | sesión segura, permisos y aislamiento por organización |
| SEG-003 | Prospectos operativos y contactos | PLANNED | SEG-002 | CRUD, ciclo comercial y búsqueda |
| SEG-004 | Actividades, tareas y timeline | PLANNED | SEG-003 | seguimiento comercial trazable |
| SEG-005 | Resolución de duplicados | PLANNED | SEG-004 | bandeja y merge transaccional |
| SEG-006 | Oportunidades y pipeline | PLANNED | SEG-003 | ciclo de venta y forecast |
| SEG-007 | Campañas y plantillas | PLANNED | SEG-003, SEG-006 | audiencia congelada y simulación |
| SEG-008 | Mensajería segura y adaptadores | PLANNED | SEG-007 | no-op, fake, manual y red deshabilitada |
| SEG-009 | Outbox, workers e inbound | PLANNED | SEG-008 | async idempotente y respuesta fake |
| SEG-010 | Reportes, seguridad y producción | PLANNED | SEG-002–009 | operación local endurecida |
| SEG-011 | Validación integral y cierre | PLANNED | SEG-010 | recorrido reproducible y evidencia |

## SEG-001 — Completado

- [x] modelo institucional, contactos, canales, prospectos y exclusiones;
- [x] normalización y deduplicación exacta/ambigua;
- [x] importación CSV/XLSX con preview y ejecución confirmada;
- [x] métricas `ACCEPTED`, `EXCLUDED`, `REJECTED`, `DUPLICATE`, `REVIEW_REQUIRED`;
- [x] persistencia, auditoría y API;
- [x] autenticación bootstrap fail-closed;
- [x] frontend React/TypeScript/Vite;
- [x] Dockerfiles multi-stage;
- [x] perfiles Compose `app` y `smoke`;
- [x] puertos host configurables y loopback-only;
- [x] preflight Windows/Unix;
- [x] checker PowerShell de sintaxis;
- [x] checker de puertos Windows y propiedad Docker;
- [x] validadores integrales PowerShell/Bash;
- [x] Maven verify/Testcontainers contenedorizado;
- [x] generación segura de lockfile;
- [x] escaneo de seguridad del repositorio;
- [x] CI estructural y smoke E2E;
- [x] consolidación completa en `main`;
- [x] PowerShell syntax ejecutado;
- [x] preflight ejecutado;
- [x] frontend clean build ejecutado;
- [x] backend clean image build ejecutado;
- [x] comprobación Windows/Docker de puertos ejecutada;
- [x] publicación PostgreSQL ejecutada;
- [x] health PostgreSQL ejecutado;
- [x] diagnóstico de auto-configuración Flyway;
- [x] `spring-boot-starter-flyway` versionado;
- [x] fail-fast por ubicación de migraciones versionado.
- [x] Flyway V1–V5 ejecutado antes de Hibernate;
- [x] Jackson propio alineado con el mapper administrado por Spring Boot 4;
- [x] regresión de contexto y persistencia JSONB de auditoría;
- [x] PostgreSQL/backend/frontend healthy;
- [x] smoke host y contenedor;
- [x] Maven verify, 29/29 tests, Spotless, ArchUnit y Testcontainers;
- [x] lockfile generado, revisado y versionado;
- [x] segunda ejecución limpia mediante `npm ci`;
- [x] repository safety;
- [x] GitHub Actions visible y verde;
- [x] documentación y evidencia final sincronizadas.

## SEG-001 — Cierre ejecutado

- [x] actualizar checkout al último `main`;
- [x] retirar stack parcial sin `-v`;
- [x] reejecutar validador completo con PostgreSQL host `25432`;
- [x] confirmar que Flyway crea/valida `flyway_schema_history`;
- [x] confirmar aplicación de migraciones V1–V5;
- [x] confirmar Hibernate validate PASS;
- [x] confirmar backend/frontend healthy;
- [x] ejecutar smoke host y contenedor;
- [x] ejecutar Maven verify;
- [x] ejecutar Spotless;
- [x] ejecutar unit tests;
- [x] ejecutar ArchUnit;
- [x] ejecutar Testcontainers;
- [x] generar `frontend/package-lock.json` mediante el validador;
- [x] revisar SHA-256 y diff;
- [x] versionar únicamente el lockfile;
- [x] repetir desde árbol limpio para demostrar `npm ci`;
- [x] ejecutar seguridad final;
- [x] conservar JSON/transcript local fuera de Git;
- [x] observar CI verde;
- [x] sincronizar estado final;
- [x] marcar SEG-001 COMPLETE;
- [ ] activar SEG-002.

## Evidencia de cierre

```text
LOCAL_COMPLETE_VALIDATION=PASS
LOCKFILE_VERSIONED
SECOND_CLEAN_NPM_CI_RUN=PASS
CI_VISIBLE_GREEN
SEG_001_COMPLETE
```

La segunda ejecución limpia sobre `d8a5a449…` cerró toda la matriz. GitHub
Actions run `29848718163` terminó `success`.

Evidencia:

```text
docs/validation/SEG-001-jackson-objectmapper-failure-2026-07-21.md
```

## SEG-002 — Activo

- [ ] organización bootstrap y backfill seguro;
- [ ] usuarios persistentes con contraseña hasheada;
- [ ] roles, permisos y membresías;
- [ ] sesión cookie same-origin con CSRF;
- [ ] login, logout, expiración e invalidación;
- [ ] bloqueo temporal y usuario inactivo;
- [ ] tenant isolation;
- [ ] auditoría de autenticación;
- [ ] UI de login y usuarios;
- [ ] migración desde vacío y V1-V5;
- [ ] pruebas y checkpoint verde.

Contrato completo y checkpoints: `docs/execution/complete-crm-platform-plan.md`.

## Restricciones permanentes durante SEG-001

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No versionar `.env`, `validation-output/`, datos reales, claves o credenciales. No usar `docker compose down -v` salvo destrucción intencional ni `docker system prune` como diagnóstico normal.
