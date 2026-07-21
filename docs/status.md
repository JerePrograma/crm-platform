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

`main` continúa siendo la única fuente de verdad. La segunda validación integral
limpia de SEG-001 se ejecutó sobre
`d8a5a449a72c660e2655f4be7144360cd1e719a4` con el lockfile versionado.

## Segmentos

| ID | Estado | Resumen |
|---|---|---|
| SEG-000 | COMPLETE | repositorio, continuidad y documentación canónica |
| SEG-001 | COMPLETE | vertical slice, seguridad, validación local integral y CI verdes |
| SEG-002 | PLANNED | siguiente segmento; todavía no implementado ni activado |

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
- iniciar SEG-002 antes del cierre de SEG-001.

## Próxima acción canónica

SEG-001 está cerrado. No modificarlo salvo una regresión demostrada. SEG-002
permanece `PLANNED`: revisar su alcance y solicitar autorización explícita antes
de activarlo o implementar campañas, integraciones o envíos.

Comandos de continuidad: `docs/next-step.md`.
