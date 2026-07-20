# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama activa: `feat/seg-001-prospect-vertical-slice`;
- comparación más reciente: 163 commits por delante de `main`, 0 por detrás;
- `main` conserva únicamente la inicialización mínima;
- no existe pull request;
- no se realizó merge;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado;
- objetivo vigente: validar y estabilizar `SEG-001` antes de iniciar identidad/RBAC.

## Segmentos

- `SEG-000` — rama real y protocolo de continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

Solo `SEG-001` puede recibir correcciones hasta alcanzar evidencia ejecutada o un bloqueo externo documentado.

## Implementación disponible

### Backend

- Java 21, Spring Boot 4.1 y Maven fijado;
- PostgreSQL, Flyway V1–V5 y `ddl-auto=validate`;
- instituciones, contactos y canales separados;
- prospectos y estados comerciales;
- exclusiones dominantes, retroactivas y equivalentes entre teléfono/WhatsApp;
- normalización y validación central de nombre, correo, teléfono y dominio;
- API paginada, RFC 7807, OpenAPI y auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, métricas y logging estructurado.

### Importación

- CSV con coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- parser por encabezados con rechazo de duplicados normalizados;
- comillas CSV, saltos internos y rechazo de comillas sin cerrar;
- fechas Excel normalizadas en UTC;
- SHA-256, nombre de archivo saneado y límite de 10 MB;
- límite multipart alineado con respuesta HTTP 413;
- `ImportJob`, `ImportRow` y `DuplicateReview` persistentes;
- preview y ejecución confirmada;
- transacción y recuperación por fila;
- idempotencia archivo+modo;
- resultados determinísticos por hoja/fila;
- conteos separados: accepted, excluded, rejected, duplicate y review;
- coincidencias exactas enlazadas al prospecto existente;
- coincidencias ambiguas persistentes también en preview;
- preview que aplica exclusiones sin crear prospectos;
- exclusiones importadas que deshabilitan prospectos existentes y auditan;
- fixture ficticia de 100 prospectos y 16 exclusiones.

### Frontend

- React, TypeScript y Vite;
- login bootstrap sin persistir contraseña;
- codificación Basic UTF-8;
- dashboard;
- listado y ficha de prospectos;
- importaciones y resultados;
- revisiones ambiguas;
- exclusiones y auditoría;
- tipos actualizados con `excludedRows`;
- diseño responsive.

### Tooling

- Dockerfile y Docker Compose PostgreSQL;
- GitHub Actions para backend, frontend, Compose e imagen;
- Maven launcher Linux/macOS y Windows con SHA-512;
- Spotless, Testcontainers y ArchUnit.

## Correcciones realizadas en la revisión estática del 20 de julio

1. exclusiones importadas unificadas con el flujo manual y retroactivo;
2. auditoría generada para exclusiones importadas;
3. evidencia ambigua persistida durante dry-run;
4. filas duplicadas exactas enlazadas al prospecto existente;
5. límite multipart de 10 MB y RFC 7807/413;
6. orden estable por hoja y fila;
7. validación central de correo;
8. recuperación segura de filas malformadas;
9. soporte CSV `;`, comillas y encabezados duplicados;
10. fechas Excel independientes de la zona horaria del host;
11. preview con elegibilidad real;
12. `excludedRows` persistente y expuesto;
13. nombre de archivo reducido a basename seguro;
14. credenciales Basic codificadas en UTF-8;
15. pruebas de regresión agregadas para cada flujo crítico anterior.

## Seguridad

- envío real: inexistente;
- `sending.enabled=false`;
- `sending.dry-run=true`;
- `sending.daily-limit=0`;
- kill switch ambiental y persistente activos;
- sin adaptadores Gmail/SMTP;
- XLSX real y datos operativos fuera de Git;
- secretos fuera del repositorio;
- API cerrada sin credenciales bootstrap explícitas;
- auditoría de exclusión sin copiar el canal completo;
- búsqueda remota sin claves privadas, tokens, correos personales ni el lote XLSX.

## Validación

### Implementada

- pruebas unitarias de normalización, similitud y parser;
- CSV con `;`, comillas inválidas y encabezados duplicados;
- Testcontainers de persistencia, importación, deduplicación y exclusión;
- exclusión importada retroactiva y auditada;
- preview bloqueado sin escrituras de dominio;
- correo malformado rechazado por fila;
- duplicado exacto enlazado al prospecto;
- revisión ambigua persistida en preview;
- prueba de autorización;
- regla ArchUnit;
- jobs CI para backend, frontend, Compose e imagen.

### Ejecutada con evidencia

- lectura remota posterior a escrituras;
- comparación de rama contra `main`;
- revisión estática cruzada de servicios, entidades, migraciones, controladores y DTO frontend;
- comprobación de configuración fail-closed;
- escaneo remoto de secretos y datos reales;
- inspección del entorno local: Java y Node presentes, Maven/Docker/cachés ausentes.

### Pendiente por bloqueo de infraestructura

- `mvn verify` y Spotless;
- Testcontainers/Flyway/Hibernate reales;
- build TypeScript/Vite;
- generación de `package-lock.json`;
- `docker compose config`;
- build de imagen;
- ejecución observable de GitHub Actions.

El conector no dispone de `workflow_dispatch`; sus commits no generaron checks visibles. El contenedor no resuelve GitHub ni repositorios de dependencias y no posee Maven, Docker ni cachés. No se afirma que el proyecto compile hasta registrar evidencia ejecutada.

## Riesgos activos

1. pueden existir fallos de compilación/formato no detectables estáticamente;
2. puede existir divergencia JPA/Flyway no observada en ejecución;
3. HTTP Basic es temporal y no implementa RBAC;
4. exclusiones y auditoría requieren permisos por rol en `SEG-002`;
5. no existe acción auditada para resolver `DuplicateReview`;
6. un trabajo fallido no tiene retry explícito con el mismo SHA/modo;
7. frontend sin lockfile y sin visualización específica de `excludedRows`;
8. la rama acumula muchos commits pequeños por el conector;
9. el lote disponible cubre 100 prospectos, no 298;
10. La Colmena, Collegium, LAEM y Trobada deben excluirse solo con canales verificados;
11. el esquema actual mantiene una relación institución–prospecto uno a uno;
12. la auditoría todavía no registra actor persistente ni política de retención.

## Próxima acción canónica

Leer `docs/next-step.md`: obtener un entorno ejecutable o checks visibles, correr toda la matriz, corregir fallos reales y actualizar `docs/validation/SEG-001.md`. No iniciar `SEG-002` mientras los controles principales permanezcan pendientes.
