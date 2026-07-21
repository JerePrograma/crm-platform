# SEG-001 — Fallo de auto-configuración Flyway en Spring Boot 4

Fecha: 2026-07-21

## Estado

```text
EXECUTED_FAIL — FLYWAY_AUTOCONFIGURATION_MISSING
FIX_IMPLEMENTED_NOT_RUN
```

Esta evidencia registra la sexta ejecución integral real de SEG-001 en Windows sobre:

```text
commit validado: 39f5f9e635722a9a37e0c3abdb3ca452e8cd8bc5
puerto PostgreSQL host: 25432
backend host: 8080
frontend host: 5173
```

## Controles aprobados

La ejecución confirmó:

```text
checkout main: PASS
working tree inicial: PASS
PowerShell syntax: PASS — 11 scripts
preflight container-only: PASS
Docker daemon: PASS
Compose config: PASS
publicaciones Docker libres: PASS
bind loopback Windows: PASS
PostgreSQL publication: PASS
PostgreSQL health: PASS
frontend build --no-cache: PASS
TypeScript strict/Vite production build: PASS
frontend image export: PASS
backend build --no-cache: PASS
Maven package -DskipTests: PASS_PARTIAL
backend image export: PASS
```

El conflicto previo de puertos quedó resuelto. PostgreSQL se publicó en `127.0.0.1:25432` y alcanzó estado `healthy` antes de los builds.

## Primer fallo real

El comando:

```text
docker compose --profile app up -d backend frontend
```

falló porque el backend no pudo inicializar JPA:

```text
Schema validation: missing table [contact]
```

El backend sí:

- resolvió `postgres:5432`;
- abrió una conexión PostgreSQL;
- detectó PostgreSQL 17.10;
- inicializó HikariCP.

No apareció ningún log de Flyway, creación de `flyway_schema_history` ni aplicación de migraciones antes de que Hibernate ejecutara `ddl-auto=validate`.

## Causa raíz

El proyecto usa Spring Boot 4.1.0. La configuración declaraba:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

pero el POM solo incorporaba:

```text
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
```

En Spring Boot 4, la integración debe incorporar el starter de Flyway para incluir su módulo de auto-configuración. Tener únicamente `flyway-core` deja disponibles las clases de Flyway, pero no activa el inicializador de migraciones de Spring Boot.

## Corrección versionada

Se reemplazó la dependencia directa `flyway-core` por:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

Se conserva:

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Además se configuró:

```yaml
spring:
  flyway:
    fail-on-missing-locations: true
```

para fallar explícitamente cuando el directorio de migraciones no esté disponible, en lugar de llegar a un error indirecto de Hibernate.

Commits correctivos:

```text
50a6b1b7c6eedd45da9a8af1462b76e00ff64427 — fix: enable Flyway auto-configuration on Spring Boot 4
a3782c42fdf6c84e83ad8adcebd8770d41438098 — fix: fail fast when Flyway migrations are unavailable
```

## Fases no ejecutadas

Debido al fallo de arranque, permanecen `NOT_RUN`:

```text
backend health
frontend start/health
Flyway V1–V5 confirmed
Hibernate validation PASS
smoke host
smoke container
Maven verify
Spotless
unit tests
ArchUnit
Testcontainers
package-lock generation
npm ci rebuild
final smoke
repository safety final
```

## Estado del repositorio local tras el fallo

La ejecución no generó `frontend/package-lock.json`. Los comandos posteriores de hash, `git add` y commit fallaron o no produjeron cambios. El working tree permaneció limpio.

No se debe crear ni versionar manualmente un lockfile antes de que el validador alcance su fase de generación.

## Próxima ejecución autorizada

Actualizar `main` y volver a ejecutar el validador completo con el mismo puerto 25432. La señal esperada antes de Hibernate es una secuencia Flyway que cree la tabla de historial y aplique las migraciones disponibles.

No iniciar SEG-002 hasta obtener un cierre integral verde y repetir desde árbol limpio con el lockfile versionado.

## Seguridad

La ejecución mantuvo:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No se enviaron comunicaciones, no se importó el lote real y no se desplegó producción.
