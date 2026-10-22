# ADR-0011 — Java 21 y actualizaciones de seguridad coherentes

- Estado: Accepted
- Fecha: 2026-10-22

## Contexto

La CI de `2a22e4b` falló por vulnerabilidades altas/críticas en bibliotecas Java y en el runtime Chainguard fijado. El tag público actual de Chainguard JRE ejecuta Java 27.0.1 y los tags Java 21 probados no son accesibles; el proyecto exige Java 21 y no necesita migrar de versión mayor.

## Decisión

- Actualizar el parent a Spring Boot 4.1.2 y usar su BOM para Spring, Tomcat, Jackson 2/3 y PostgreSQL JDBC. Retirar los overrides antiguos que impedían recibir esas correcciones.
- Mantener Testcontainers 1.21.3 de forma coherente en test; una migración a Testcontainers 2 no forma parte de esta reparación.
- Usar Eclipse Temurin Java 21 JRE Alpine oficial, fijada por digest multiarquitectura. Registrar versión/digest y Grype por cada renovación.
- Conservar UID/GID 65532, red privada, filesystem de aplicación de solo lectura y límites de recursos.
- Escanear backend y frontend con el umbral high/critical existente, sin exclusiones para silenciar hallazgos.

El primer scan ejecutado de la base Temurin terminó con código cero, sin high/critical y con hallazgos low/medium. La autoridad de la aplicación completa es [la validación de esta misión](../validation/scalaris-production-readiness-2026-10-22.md); una base aprobada no prueba las bibliotecas del JAR.

## Compatibilidad y rollback

Las migraciones V1–V14 se conservan sin modificación. La actualización se valida contra bases nuevas y upgrades V11/V13→V14. Un rollback usa imágenes validadas y conserva la base; no restaura el runtime vulnerable como solución permanente ni modifica Flyway history. El proveedor de imagen podrá revisarse cuando haya un runtime Java 21 público, verificable y aprobado por los gates.
