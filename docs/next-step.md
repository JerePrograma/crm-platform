# SEG-001 — Vertical slice persistente de prospectos

## Objetivo

Publicar el árbol recuperado del CRM y cerrar un recorrido real desde XLSX hasta PostgreSQL para instituciones, contactos, prospectos y exclusiones, sin ningún mecanismo de envío.

## Primera tarea de continuidad

Publicar por lotes verificables el código preparado fuera de Git. Cada lote debe dejar el repositorio compilable o claramente documentado como incompleto. No incluir `gestudio_lote_100_prospectos.xlsx` ni otros datos operativos.

## Alcance funcional

- Java 21, Spring Boot, Maven y Maven Wrapper.
- Docker Compose con PostgreSQL y migraciones Flyway.
- Instituciones, contactos, canales, prospectos y exclusiones.
- Persistencia de `ImportJob`, `ImportRow` y `DuplicateReview`.
- Preview, dry-run y ejecución confirmada de importaciones CSV/XLSX.
- Deduplicación por correo, teléfono, dominio, nombre y localidad.
- Cola humana para coincidencias ambiguas.
- RFC 7807, OpenAPI y auditoría.
- Listado y ficha inicial de prospectos en React.
- Testcontainers, pruebas de autorización, importación, exclusión e idempotencia.

## Criterios de aceptación

- `./mvnw -B -f backend/pom.xml verify` finaliza correctamente.
- Flyway migra PostgreSQL vacío y JPA valida el esquema.
- Una fixture ficticia produce 100 prospectos y 16 exclusiones.
- Reimportar no crea duplicados.
- Las exclusiones bloquean la elegibilidad.
- Coincidencias ambiguas no se fusionan automáticamente.
- Frontend, Compose e imagen Docker compilan en CI.
- No existe ningún camino de envío real.

## Cierre

Al completar SEG-001, marcarlo `COMPLETE`, mover SEG-002 a `NEXT`, actualizar el changelog y reemplazar este archivo por el alcance de identidad y RBAC.
