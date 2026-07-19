# Gestudio CRM Platform

Monolito modular para prospección, campañas, conversaciones y operación comercial de Gestudio.

## Estado de seguridad

La configuración inicial es deliberadamente no operativa:

- `sending.enabled=false`;
- `sending.dry-run=true`;
- `sending.daily-limit=0`;
- kill switch ambiental activo;
- kill switch persistente inicializado en PostgreSQL.

El repositorio no contiene un adaptador capaz de enviar correos reales.

## Stack inicial

- Java 21 y Spring Boot 4.1;
- Maven;
- PostgreSQL y Flyway;
- Spring Web, Security, Data JPA, Validation y Actuator;
- Testcontainers, JUnit 5 y ArchUnit;
- Docker y Docker Compose.

Spring Boot 4.1.0 es la línea estable seleccionada para el proyecto y Java 21 es la versión de ejecución obligatoria.

## Inicio local

```bash
docker compose up -d postgres
mvn -f backend/pom.xml spring-boot:run
```

PowerShell:

```powershell
docker compose up -d postgres
mvn -f backend/pom.xml spring-boot:run
```

La primera ejecución aplica las migraciones Flyway y valida el esquema mediante Hibernate.

## Continuidad

Antes de modificar el proyecto, leer:

1. `AGENTS.md`;
2. `docs/status.md`;
3. `docs/next-step.md`;
4. `docs/backlog.md`.

La instrucción `continuar` ejecuta el lote definido en `docs/next-step.md`.
