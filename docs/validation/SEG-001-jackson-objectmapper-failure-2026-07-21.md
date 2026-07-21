# SEG-001 — Jackson ObjectMapper y cierre integral — 2026-07-21

## Alcance

Esta evidencia registra el primer fallo posterior a Flyway/Hibernate observado en
Windows, su causa raíz, las correcciones derivadas y las dos ejecuciones integrales
que cerraron la validación local de SEG-001.

```text
repositorio: JerePrograma/crm-platform
rama: main
plataforma: Windows 11 + Docker Desktop 29.3.1
PostgreSQL host port: 25432
backend host port: 8080
frontend host port: 5173
comunicaciones reales: DESHABILITADAS
```

## Ejecución de origen

La validación adjunta se ejecutó sobre:

```text
commit: a9e2c44de5d4d9181ef304976597d9d8b1a30014
comando: powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173 -KeepRunning
resultado: FAIL
```

### Fases aprobadas antes del fallo

| Control | Estado | Evidencia |
|---|---|---|
| rama `main` y árbol limpio | PASS | precondición del validador |
| PowerShell syntax | PASS | 11 scripts |
| preflight/Compose/guardas | PASS | modo container-only y envío fail-closed |
| puertos Windows/Docker | PASS | 25432, 8080 y 5173 |
| PostgreSQL publication/health | PASS | PostgreSQL 17.10 |
| frontend clean build | PASS | TypeScript strict y Vite |
| backend clean image build | PASS | Maven package con tests omitidos |
| Maven package del Dockerfile | PASS_PARTIAL | `-DskipTests package`; no reemplaza `verify` |
| Flyway | PASS | cinco migraciones validadas y aplicadas, esquema v5 |
| Hibernate | PASS | `EntityManagerFactory` inicializado después de Flyway |

### Primer fallo real

Spring canceló el contexto al construir `AuditEventWriter`:

```text
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available
```

Por ese fallo:

| Control | Estado |
|---|---|
| `AuditEventWriter` | FAIL |
| backend health | FAIL |
| frontend health | NOT_RUN |
| smoke host/contenedor | NOT_RUN |
| Maven verify/Spotless/tests/ArchUnit/Testcontainers | NOT_RUN |
| package-lock | NOT_RUN |
| npm ci | NOT_RUN |

## Causa raíz confirmada

El árbol efectivo de dependencias de Spring Boot 4.1.0 contiene:

```text
spring-boot-starter-jackson / spring-boot-jackson 4.1.0
tools.jackson:jackson-databind 3.1.4
tools.jackson:jackson-core 3.1.4
```

Spring Boot administra e inyecta `tools.jackson.databind.ObjectMapper`. El código
propio todavía solicitaba Jackson 2 en:

```text
backend/src/main/java/com/gestudio/crm/audit/AuditEventWriter.java
backend/src/main/java/com/gestudio/crm/imports/ProspectImportRowProcessor.java
```

`springdoc`/Swagger mantiene artefactos Jackson 2 `com.fasterxml.jackson` 2.21.4
como compatibilidad transitiva de terceros. No existe una razón para que el código
propio dependa de ese mapper ni para registrar un segundo bean con configuración
divergente.

## Correcciones

### Código y regresión

Commit `f39e881 fix: align audit JSON serialization with Spring Boot 4`:

- migró los usos propios de `ObjectMapper`, `JsonNode` y excepciones a Jackson 3;
- mantuvo el mapper administrado por Spring Boot como única fuente de configuración;
- conservó `CAST(? AS jsonb)` para que PostgreSQL valide y persista JSON real;
- convirtió `Instant` a `java.sql.Timestamp` en parámetros JDBC después de que la
  regresión expusiera que el driver no podía inferir el tipo SQL del `Instant`;
- integró la regresión en `ExclusionIntegrationTest` con contexto Spring real,
  Testcontainers, Flyway y Hibernate;
- cubrió mapa, UUID, `Instant`, `OffsetDateTime`, enum, null, JSON válido y escritura
  efectiva de `audit_event`;
- retiró el `@Transactional` artificial del test de deduplicación para respetar el
  límite real `REQUIRES_NEW` del procesador de filas.

No se añadió un mapper Jackson 2 paralelo. Los payloads nulos siguen serializándose
como `{}` y un fallo de serialización sigue produciendo una excepción explícita.
Los fixtures usan únicamente datos ficticios y no contienen secretos.

### Validación y portabilidad

- `6f0bafb style: apply backend formatter`: dejó Spotless 55/55 limpio;
- `41a1548 fix: support current Docker API in backend verification`: corrigió
  `--environment` a `--env` y negocia la API del daemon para Testcontainers;
- `951d19b fix: use IPv4 frontend health check`: usa `127.0.0.1` porque BusyBox
  resolvía `localhost` a IPv6 mientras Nginx escuchaba IPv4;
- `d8a5a44 build: lock frontend dependencies`: versionó exclusivamente el lockfile
  generado con scripts deshabilitados.

## Validaciones focalizadas

| Comando/control | Resultado |
|---|---|
| `mvn -B -ntp -f backend/pom.xml -DskipTests compile` | PASS |
| `mvn ... -Dtest=ExclusionIntegrationTest test` | PASS, 2/2 después del fix JDBC |
| `mvn ... -Dtest=ProspectDeduplicationIntegrationTest test` | PASS, 3/3 |
| `mvn ... spotless:check` | PASS, 55/55 después de aplicar formato |
| backend image build `--no-cache` | PASS |
| stack focalizado PostgreSQL/backend | PASS, ambos healthy |
| smoke host | PASS |
| smoke contenedor con perfiles app/smoke | PASS |
| `scripts/verify-backend-container.ps1` | PASS después de corregir flag/API |

Los intentos fallidos previos se trataron como diagnósticos, no como evidencia
verde: `Instant` JDBC, flag Docker inválido, API Docker 1.32 rechazada y fixture
transaccional de deduplicación.

## Primera validación integral completa

```text
commit: 951d19b369e9178e94176374bbafb88d92ba4251
inicio UTC: 2026-07-21T16:16:42Z
resultado: PASS
lockfile SHA-256: 1936217c0598825ef43519069a3ba89a974e2b30e3b9f2619d4e62dd10810c98
```

Evidencia local:

```text
validation-output/seg001-complete-20260721-131642.log
validation-output/seg001-complete-20260721-131642.json
validation-output/seg001-docker-20260721-131643.json
```

Esta ejecución generó el lockfile con `--package-lock-only --ignore-scripts`, no
creó `frontend/node_modules`, reconstruyó mediante `npm ci`, repitió smoke y cerró
con `Repository safety scan passed` y `Complete SEG-001 validation passed`.

## Segunda validación limpia con lockfile versionado

```text
commit: d8a5a449a72c660e2655f4be7144360cd1e719a4
inicio UTC: 2026-07-21T16:30:02Z
fin UTC: 2026-07-21T16:39:56Z
resultado: PASS
```

Evidencia local:

```text
validation-output/seg001-complete-20260721-133002.log
validation-output/seg001-complete-20260721-133002.json
validation-output/seg001-docker-20260721-133003.json
```

Matriz final ejecutada:

| Control | Estado |
|---|---|
| working tree limpio | PASS |
| PowerShell syntax | PASS, 11 scripts |
| preflight/guardas/Compose/puertos | PASS |
| builds frontend/backend sin caché | PASS |
| primer build frontend mediante `npm ci` | PASS |
| Flyway V1–V5 antes de Hibernate | PASS |
| PostgreSQL/backend/frontend healthy | PASS |
| smoke host/contenedor inicial | PASS |
| Maven verify | PASS |
| unit/integration tests | PASS, 29/29 |
| Spotless | PASS, 55/55 |
| ArchUnit | PASS |
| Testcontainers | PASS, Docker API 1.54 |
| lockfile sin cambio y sin lifecycle scripts | PASS |
| segundo rebuild frontend mediante `npm ci` | PASS |
| smoke host/contenedor final | PASS |
| repository safety | PASS |
| JSON/transcript | PASS, fuera de Git |

## CI

GitHub Actions run `29848718163` para `d8a5a449a72c660e2655f4be7144360cd1e719a4`
terminó `success` el 2026-07-21. Jobs visibles:

```text
backend: success
frontend: success
scripts: success
compose-images-and-smoke: success
```

Run: `https://github.com/JerePrograma/crm-platform/actions/runs/29848718163`.

## Seguridad y riesgos

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No se desplegó producción, no se habilitaron adaptadores de envío, no se importó
el XLSX real y no se versionaron `.env`, credenciales o `validation-output/`.

Riesgos no bloqueantes observados:

- Jackson 2 permanece transitivo por springdoc/Swagger, aislado de código propio;
- los tests emiten advertencias Hikari al cerrar PostgreSQL efímeros de contextos
  anteriores, sin fallos ni recursos persistentes;
- HTTP Basic continúa siendo temporal;
- revisiones ambiguas aún no tienen resolución UI y los jobs no tienen retry.

## Cierre

SEG-001 cumple la matriz local, el lockfile está versionado, la segunda corrida
limpia usa `npm ci` desde el primer build y CI es visible y verde. SEG-001 queda
marcado `COMPLETE`. SEG-002 permanece sin implementación hasta una instrucción
explícita posterior.
