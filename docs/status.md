# Estado actual

Actualizado: 2026-07-21

## Repositorio y consolidación

```text
repositorio: JerePrograma/crm-platform
rama predeterminada: main
rama canónica: main
producción: NO DESPLEGADA
comunicaciones: DESHABILITADAS
lote real: FUERA DE GIT/CI/IMÁGENES
```

`main` continúa siendo la única fuente de verdad. La ejecución real más reciente se realizó sobre `39f5f9e635722a9a37e0c3abdb3ca452e8cd8bc5`; las correcciones posteriores ya están publicadas en `main` y requieren reejecución local.

## Segmentos

| ID | Estado | Resumen |
|---|---|---|
| SEG-000 | COMPLETE | repositorio, continuidad y documentación canónica |
| SEG-001 | ACTIVE | implementación y hardening completos; validación integral pendiente |
| SEG-002 | PLANNED | bloqueado hasta SEG-001 verde |

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
LATEST_REAL_RUN=FAIL_FLYWAY_AUTOCONFIGURATION_MISSING
FLYWAY_STARTER_FIX_IMPLEMENTED_NOT_RUN
FUNCTIONAL_VALIDATION_PENDING
LOCKFILE_PENDING
CI_NOT_VISIBLE
```

## Sexta ejecución real — Windows

Fecha: 2026-07-21.

Configuración:

```text
commit ejecutado: 39f5f9e635722a9a37e0c3abdb3ca452e8cd8bc5
PostgreSQL host port: 25432
Backend host port: 8080
Frontend host port: 5173
Docker: 29.3.1
```

### Aprobado

```text
checkout main y working tree limpio
PowerShell syntax — 11 scripts
preflight container-only
Docker daemon y Compose config
guardas fail-closed
publicaciones Docker libres
bind exclusivo Windows
PostgreSQL publication
PostgreSQL health
frontend clean build --no-cache
TypeScript strict
Vite production build
frontend image export
backend clean build --no-cache
Maven package -DskipTests
backend image export
```

El conflicto anterior de puertos quedó cerrado: PostgreSQL se publicó en `127.0.0.1:25432` y alcanzó `healthy` antes de los builds.

### Primer fallo real

El arranque de backend/frontend terminó con:

```text
Schema validation: missing table [contact]
```

El backend resolvió PostgreSQL, abrió HikariCP y detectó PostgreSQL 17.10. No apareció ningún log de Flyway, creación de `flyway_schema_history` o aplicación de migraciones antes de `ddl-auto=validate`.

Diagnóstico:

```text
Flyway libraries present
Spring Boot 4 Flyway auto-configuration starter absent
migrations NOT_RUN
Hibernate validation FAIL because schema remained empty
```

Evidencia:

```text
docs/validation/SEG-001-flyway-autoconfiguration-failure-2026-07-21.md
```

## Correcciones publicadas después de la ejecución

```text
50a6b1b7c6eedd45da9a8af1462b76e00ff64427
fix: enable Flyway auto-configuration on Spring Boot 4

a3782c42fdf6c84e83ad8adcebd8770d41438098
fix: fail fast when Flyway migrations are unavailable
```

Cambios:

- `org.flywaydb:flyway-core` directo fue reemplazado por `org.springframework.boot:spring-boot-starter-flyway`;
- se conserva `org.flywaydb:flyway-database-postgresql`;
- `spring.flyway.fail-on-missing-locations=true` evita fallos indirectos de Hibernate cuando no existen recursos de migración.

Estado de esas correcciones:

```text
IMPLEMENTED
VERSIONED_IN_MAIN
STATICALLY_REVIEWED
FUNCTIONALLY_NOT_RUN
```

## Fases pendientes

```text
Flyway auto-configuration real
creación de flyway_schema_history
aplicación V1–V5
Hibernate validate PASS
backend health
frontend start/health
smoke host
smoke container
Maven verify
Spotless
unit tests
ArchUnit
Testcontainers
package-lock generation
SHA-256 del lockfile
frontend rebuild mediante npm ci
smoke final
repository safety final
segunda ejecución desde árbol limpio con lockfile versionado
CI verde visible o excepción documentada
```

## Lockfile

La sexta ejecución falló antes de la fase de generación. Por lo tanto:

```text
frontend/package-lock.json: NO GENERADO
hash: NO DISPONIBLE
git add/commit posterior: SIN CAMBIOS
working tree: LIMPIO
```

No crear ni versionar manualmente el lockfile antes de que el validador llegue a esa fase.

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
- iniciar SEG-002 antes del cierre de SEG-001.

## Próxima acción canónica

1. actualizar el checkout a `main`;
2. detener el stack parcial sin borrar volúmenes;
3. reejecutar `scripts/validate-seg001.ps1` con PostgreSQL host `25432` y sin caché;
4. confirmar logs Flyway antes de Hibernate;
5. continuar hasta el primer PASS integral;
6. revisar y versionar únicamente `frontend/package-lock.json`;
7. repetir desde árbol limpio para demostrar `npm ci` desde el primer build.

Comandos exactos: `docs/next-step.md`.
