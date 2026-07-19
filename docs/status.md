# Estado actual

Actualizado: 2026-07-19

## Repositorio

- rama activa: `feat/seg-001-prospect-vertical-slice`;
- `main` conserva únicamente la inicialización mínima;
- no existe pull request;
- no se realizó merge;
- no se desplegó ningún ambiente;
- último objetivo: cerrar `SEG-001` con validación ejecutada.

## Segmentos

- `SEG-000` — rama real y protocolo de continuidad: `COMPLETE`;
- `SEG-001` — vertical slice persistente de prospectos: `ACTIVE`;
- `SEG-002` — identidad, usuarios y RBAC: `PLANNED`.

Solo `SEG-001` puede recibir trabajo funcional hasta su cierre o bloqueo documentado.

## Implementación disponible

### Backend

- Java 21 / Spring Boot / Maven verificado;
- PostgreSQL / Flyway / JPA validate;
- instituciones, contactos y canales;
- prospectos y estados completos;
- exclusiones dominantes y retroactivas;
- normalización;
- elegibilidad;
- API paginada;
- RFC 7807;
- auditoría JSONB;
- autenticación bootstrap fail-closed;
- Actuator, métricas y logging estructurado.

### Importación

- CSV y XLSX;
- hojas Prospectos/Exclusiones;
- parser por encabezados;
- SHA-256;
- límite de tamaño;
- ImportJob, ImportRow y DuplicateReview;
- preview y ejecución confirmada;
- transacción por fila;
- idempotencia;
- resultados por fila;
- cola de revisión humana;
- fixture ficticia 100/16.

### Frontend

- React + TypeScript + Vite;
- login bootstrap sin persistir contraseña;
- dashboard;
- listado/ficha de prospectos;
- importaciones y resultados;
- revisiones ambiguas;
- exclusiones;
- auditoría;
- diseño responsive.

### Tooling

- Dockerfile;
- Docker Compose PostgreSQL;
- GitHub Actions backend/frontend/Compose/imagen;
- Maven launcher Linux/macOS y Windows con SHA-512;
- Spotless;
- Testcontainers;
- ArchUnit.

## Seguridad

- envío real: inexistente;
- `sending.enabled=false`;
- `sending.dry-run=true`;
- `sending.daily-limit=0`;
- kill switch ambiental activo;
- kill switch persistente activo;
- sin adaptadores Gmail/SMTP;
- XLSX real y datos de contactos fuera de Git;
- secretos fuera del repositorio;
- API cerrada sin credenciales bootstrap explícitas;
- auditoría de exclusión sin copiar el canal completo.

## Validación

### Implementada

- pruebas unitarias de normalización, similitud y parser;
- pruebas Testcontainers de persistencia, importación, deduplicación y exclusión;
- prueba de autorización;
- regla ArchUnit;
- jobs CI para backend, frontend, Compose e imagen.

### Ejecutada con evidencia

- inspección remota de archivos y hashes Git;
- parseo estructural de configuración realizado durante el desarrollo;
- comparación de rama contra `main`.

### Pendiente

- resultado visible de `mvn verify`;
- resultado visible de Testcontainers/Flyway;
- resultado visible del build frontend;
- `docker compose config`;
- build de imagen;
- `package-lock.json`.

No se afirma que el proyecto compile hasta registrar esas evidencias.

## Riesgos activos

1. pueden existir fallos de compilación o formato no observados;
2. puede existir divergencia JPA/Flyway no observada;
3. HTTP Basic es temporal y no implementa RBAC;
4. la API de exclusiones requiere restricción por rol en SEG-002;
5. las revisiones ambiguas no tienen todavía acción de resolución;
6. trabajos fallidos no tienen retry con el mismo SHA/modo;
7. frontend sin lockfile;
8. rama con muchos commits pequeños por limitaciones del conector;
9. lote disponible: 100 prospectos, no 298;
10. contactos previos deben excluirse solo con canales verificados.

## Próxima acción canónica

Leer `docs/next-step.md`: validar el árbol completo, corregir todos los fallos reales y registrar evidencia en `docs/validation/SEG-001.md`.
