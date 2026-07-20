# Estado actual

Actualizado: 2026-07-20

## Repositorio

- rama canónica y activa: `main`;
- `main` recibió por fast-forward los 171 commits que componían `feat/seg-001-prospect-vertical-slice`;
- el avance se realizó con `force=false`;
- inmediatamente después de consolidar, ambas ramas resultaron idénticas;
- las correcciones y documentos posteriores se realizaron directamente en `main`;
- no existe pull request abierto para esta consolidación;
- no se desplegó ningún ambiente;
- ningún envío fue habilitado;
- documento de evidencia: `docs/main-consolidation.md`.

Toda sesión nueva debe partir de `main`. Ninguna rama temática anterior constituye fuente de verdad.

## Segmentos

- `SEG-000` — repositorio y protocolo de continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

`SEG-001` está implementado y endurecido, pero no puede marcarse `COMPLETE` hasta registrar validación ejecutada.

## Implementación disponible

### Backend

- Java 21 y Spring Boot 4.1;
- Maven Wrapper fijado a Maven 3.9.16 con verificación SHA-512;
- PostgreSQL 17;
- Flyway V1–V5;
- Hibernate con `ddl-auto=validate`;
- instituciones, contactos y canales separados;
- prospectos y estados comerciales;
- exclusiones dominantes y retroactivas;
- equivalencia teléfono/WhatsApp;
- normalización y validación central de nombre, correo, teléfono y dominio;
- API paginada;
- OpenAPI;
- RFC 7807;
- auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, Prometheus y logging estructurado.

### Importación

- CSV UTF-8 con coma o punto y coma;
- XLSX con hojas `Prospectos` y `Exclusiones`;
- parser por encabezados normalizados;
- rechazo de encabezados duplicados normalizados;
- soporte de comillas, delimitadores y saltos internos en CSV;
- rechazo de comillas sin cerrar;
- fechas Excel normalizadas en UTC;
- SHA-256;
- nombre de archivo saneado;
- límite funcional de 10 MB;
- límite multipart alineado y respuesta HTTP 413;
- `ImportJob`, `ImportRow` y `DuplicateReview` persistentes;
- preview y ejecución confirmada;
- transacción y recuperación por fila;
- idempotencia por contenido y modo;
- orden determinístico por hoja y fila;
- métricas separadas `acceptedRows`, `excludedRows`, `rejectedRows`, `duplicateRows` y `reviewRows`;
- coincidencias exactas enlazadas al prospecto existente;
- coincidencias ambiguas persistidas durante preview;
- preview que aplica exclusiones sin crear datos de dominio;
- exclusiones importadas que deshabilitan prospectos existentes y generan auditoría;
- fixture ficticia de 100 prospectos y 16 exclusiones.

### Frontend

- React, TypeScript y Vite;
- credenciales bootstrap conservadas solo en memoria;
- codificación Basic desde bytes UTF-8;
- Dashboard;
- listado y ficha de prospectos;
- importaciones preview/execute;
- resultado por fila;
- revisiones ambiguas pendientes;
- exclusiones;
- auditoría;
- tipos compatibles con `excludedRows`;
- diseño responsive.

Pendiente no bloqueante: mostrar `excludedRows` como control separado en la vista resumen.

### Tooling

- Dockerfile backend;
- Docker Compose PostgreSQL;
- configuración local unificada mediante `.env`;
- GitHub Actions para backend, frontend, Compose e imagen;
- Spotless;
- Testcontainers;
- ArchUnit;
- documentación operativa completa en `docs/local-development-and-usage.md`.

## Consolidación y operación local realizadas

1. `main` avanzó al árbol completo de SEG-001 sin reescritura de historia;
2. se confirmó igualdad entre `main` y la rama temática al momento de consolidar;
3. `.env.example` incorporó `POSTGRES_DB` y credenciales bootstrap locales explícitas;
4. Docker Compose consume `POSTGRES_DB`, `DATABASE_USER` y `DATABASE_PASSWORD` desde `.env`;
5. se eliminó la contradicción de contraseñas entre backend y PostgreSQL local;
6. `README.md` se convirtió en punto de entrada canónico de `main`;
7. se añadió una guía completa de instalación, arranque, uso, detención y troubleshooting;
8. se añadió evidencia formal de consolidación en `docs/main-consolidation.md`.

## Correcciones de hardening ya finalizadas

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
15. pruebas de regresión para los flujos anteriores.

## Seguridad

- envío real: inexistente;
- `sending.enabled=false`;
- `sending.dry-run=true`;
- `sending.daily-limit=0`;
- kill switch ambiental activo;
- kill switch persistente activo;
- sin adaptadores Gmail o SMTP;
- XLSX real y datos operativos fuera de Git;
- secretos fuera del repositorio;
- API cerrada sin ambas credenciales bootstrap;
- auditoría de exclusión sin copiar el canal completo;
- PostgreSQL local expuesto únicamente en `127.0.0.1`;
- búsquedas remotas sin claves privadas, tokens, correos personales ni el lote XLSX.

## Validación

### Implementada

- pruebas unitarias de normalización, similitud y parser;
- pruebas CSV con `;`, comillas inválidas y encabezados duplicados;
- Testcontainers de persistencia, importación, deduplicación y exclusión;
- exclusión importada retroactiva y auditada;
- preview bloqueado sin escrituras de dominio;
- correo malformado rechazado por fila;
- duplicado exacto enlazado al prospecto;
- revisión ambigua persistida en preview;
- autorización fail-closed;
- regla ArchUnit;
- workflow CI para backend, frontend, Compose e imagen.

### Ejecutada con evidencia

- avance fast-forward de `main`;
- comparación posterior idéntica entre ramas;
- lectura remota posterior a escrituras;
- revisión estática cruzada de servicios, entidades, migraciones, controladores y tipos frontend;
- comprobación de configuración fail-closed;
- escaneo remoto de secretos y datos reales;
- comprobación de que el entorno local disponible carece de Maven, Docker y cachés.

### Pendiente por infraestructura

- `mvn verify` y Spotless;
- Testcontainers, Flyway y Hibernate reales;
- `npm install`, typecheck y build;
- generación de `package-lock.json`;
- `docker compose config`;
- build de imagen;
- ejecución observable de GitHub Actions.

Los commits realizados mediante el conector no muestran checks en `get_commit_combined_status`. No se afirma que el proyecto compile hasta registrar evidencia ejecutada en `docs/validation/SEG-001.md`.

## Tareas finalizadas

- [x] consolidar código y documentación en `main`;
- [x] conservar historia mediante fast-forward;
- [x] convertir `main` en fuente canónica;
- [x] corregir configuración local PostgreSQL;
- [x] documentar arranque Linux/macOS;
- [x] documentar arranque Windows;
- [x] documentar flujo funcional de la UI;
- [x] documentar API, comprobaciones y troubleshooting;
- [x] mantener envío completamente bloqueado;
- [x] actualizar README y documentación de continuidad.

## Tareas pendientes

### Bloqueantes de SEG-001

- [ ] obtener checkout con red o ejecución CI visible;
- [ ] ejecutar Maven, Spotless y todas las pruebas;
- [ ] validar Flyway V1–V5 y Hibernate contra PostgreSQL real;
- [ ] instalar frontend y generar `package-lock.json`;
- [ ] ejecutar typecheck y build;
- [ ] validar Compose;
- [ ] construir imagen backend;
- [ ] corregir todo fallo observado;
- [ ] actualizar la matriz de validación con salida real;
- [ ] cerrar SEG-001 y activar SEG-002.

### No bloqueantes

- [ ] visualizar `excludedRows` en la UI;
- [ ] resolver `DuplicateReview` mediante acción auditada;
- [ ] retry explícito de `ImportJob` fallido;
- [ ] filtros combinables adicionales;
- [ ] exportar resultados de importación;
- [ ] accesibilidad básica;
- [ ] estrategia de distribución del frontend.

## Riesgos activos

1. pueden existir fallos de compilación o formato no detectables estáticamente;
2. puede existir divergencia JPA/Flyway no observada en ejecución;
3. HTTP Basic es temporal y no implementa RBAC;
4. exclusiones y auditoría requieren permisos por rol en SEG-002;
5. no existe acción auditada para resolver `DuplicateReview`;
6. un trabajo fallido no tiene retry explícito con el mismo SHA y modo;
7. frontend sin lockfile;
8. el historial contiene muchos commits pequeños procedentes del conector;
9. el lote disponible cubre 100 prospectos, no 298;
10. La Colmena, Collegium, LAEM y Trobada deben excluirse solo con canales exactos verificados;
11. el esquema conserva una relación institución–prospecto uno a uno;
12. la auditoría todavía no registra actor persistente ni política de retención.

## Próxima acción canónica

Leer `docs/next-step.md`: ejecutar la matriz completa desde `main`, corregir fallos reales y documentar evidencia. No iniciar SEG-002 mientras los controles principales permanezcan pendientes.
