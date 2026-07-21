# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

Rama canónica: `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | fuente canónica, reglas y documentación |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | importación, exclusiones, UI y stack con matriz verde |
| SEG-002 | Prospección operativa segura | PLANNED | SEG-001 COMPLETE | flujo comercial sin envío automático |

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

## SEG-001 — Próxima tarea obligatoria

- [ ] actualizar checkout al último `main`;
- [ ] retirar stack parcial sin `-v`;
- [ ] reejecutar validador completo con PostgreSQL host `25432`;
- [ ] confirmar que Flyway crea/valida `flyway_schema_history`;
- [ ] confirmar aplicación de migraciones V1–V5;
- [ ] confirmar Hibernate validate PASS;
- [ ] confirmar backend/frontend healthy;
- [ ] ejecutar smoke host y contenedor;
- [ ] ejecutar Maven verify;
- [ ] ejecutar Spotless;
- [ ] ejecutar unit tests;
- [ ] ejecutar ArchUnit;
- [ ] ejecutar Testcontainers;
- [ ] generar `frontend/package-lock.json` mediante el validador;
- [ ] revisar SHA-256 y diff;
- [ ] versionar únicamente el lockfile;
- [ ] repetir desde árbol limpio para demostrar `npm ci`;
- [ ] ejecutar seguridad final;
- [ ] conservar JSON/transcript local fuera de Git;
- [ ] observar CI verde o documentar explícitamente su ausencia;
- [ ] sincronizar estado final;
- [ ] marcar SEG-001 COMPLETE;
- [ ] activar SEG-002.

## Bloqueo actual

```text
LATEST_REAL_RUN=FAIL_FLYWAY_AUTOCONFIGURATION_MISSING
FIX_IMPLEMENTED_NOT_RUN
```

La ejecución sobre `39f5f9e` aprobó puertos, PostgreSQL y builds limpios. El backend conectó a PostgreSQL, pero Hibernate validó un esquema vacío porque Flyway no había sido auto-configurado.

Evidencia:

```text
docs/validation/SEG-001-flyway-autoconfiguration-failure-2026-07-21.md
```

## SEG-002 — Bloqueado

No implementar todavía:

- campañas;
- Gmail/SMTP;
- Sheets/Drive;
- workers de envío;
- despliegues cloud;
- automatizaciones comerciales reales;
- importación del lote operativo real.

## Restricciones permanentes durante SEG-001

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No versionar `.env`, `validation-output/`, datos reales, claves o credenciales. No usar `docker compose down -v` salvo destrucción intencional ni `docker system prune` como diagnóstico normal.
