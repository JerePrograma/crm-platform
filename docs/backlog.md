# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

La rama canónica es `main`.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Repositorio y continuidad | COMPLETE | — | `main`, AGENTS, estado, backlog y puntero canónicos |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | Importación, deduplicación, exclusiones y UI con matriz verde |
| SEG-002 | Identidad, usuarios y RBAC | PLANNED | SEG-001 | Usuarios persistentes y matriz de permisos probada |
| SEG-003 | Ficha integral, contactos, búsqueda y tags | PLANNED | SEG-002 | CRUD completo, filtros combinables y trazabilidad |
| SEG-004 | Campañas, plantillas y adjuntos | PLANNED | SEG-003 | Borrador aprobado, preview y hashes, sin envío |
| SEG-005 | Safety gate y kill switch operativo | PLANNED | SEG-004 | Guardas acumulativas probadas y panel de control |
| SEG-006 | OAuth Google de desarrollo | PLANNED | SEG-002 | Cuenta personal conectada de forma segura |
| SEG-007 | MIME y Gmail fake | PLANNED | SEG-004, SEG-006 | MIME multipart probado contra adaptador fake |
| SEG-008 | Google Sheets bidireccional | PLANNED | SEG-003, SEG-006 | Preview, conflictos, auditoría y DB dominante |
| SEG-009 | Reservas e idempotencia de comunicación | PLANNED | SEG-005, SEG-007 | Reserva única, intentos y reconciliación |
| SEG-010 | Despacho local simulado | PLANNED | SEG-009 | Tareas locales, rate limit y backoff sin Gmail real |
| SEG-011 | Gmail lectura, threads y reconciliación | PLANNED | SEG-006, SEG-009 | IDs Gmail/RFC y estados conciliados |
| SEG-012 | Respuestas, rebotes y bajas | PLANNED | SEG-011 | Clasificación y acciones determinísticas |
| SEG-013 | Seguimientos programados | PLANNED | SEG-012 | Días hábiles, cancelación y `NO_RESPONSE` |
| SEG-014 | Oportunidades, ventas y tareas | PLANNED | SEG-003 | Pipeline, actividades, responsables y vencimientos |
| SEG-015 | Pruebas, pilotos y cotizaciones | PLANNED | SEG-014 | Versionado, estados y conversión |
| SEG-016 | Reporting y dashboard completo | PLANNED | SEG-013, SEG-015 | Métricas comerciales y desempeño temporal |
| SEG-017 | Observabilidad y hardening | PLANNED | SEG-010 | Logs, métricas, trazas, rate limiting y alertas |
| SEG-018 | Terraform base de staging | PLANNED | SEG-017 | APIs, IAM, Registry, Run, SQL, Secrets, red y monitoring |
| SEG-019 | Cloud Tasks, Pub/Sub y Scheduler | PLANNED | SEG-018 | Workers idempotentes y DLQ documentada |
| SEG-020 | Google Workspace y delegación | PLANNED | SEG-018 | Cuenta de servicio y delegación de dominio |
| SEG-021 | Staging E2E | PLANNED | SEG-019, SEG-020 | Flujos completos con límites cerrados |
| SEG-022 | Piloto controlado | PLANNED | SEG-021 | Aprobación humana y límite extremadamente bajo |
| SEG-023 | Producción | PLANNED | SEG-022 | DR, SLO, runbook y despliegue manual aprobado |

## SEG-001 — tareas activas

### Implementación finalizada

- [x] stack backend Java 21/Spring Boot;
- [x] PostgreSQL y Flyway V1–V5;
- [x] institución, contacto, canal y prospecto;
- [x] estados comerciales;
- [x] exclusiones dominantes y retroactivas;
- [x] normalización y elegibilidad;
- [x] importaciones CSV/XLSX persistentes;
- [x] preview y ejecución confirmada;
- [x] transacción y evidencia por fila;
- [x] idempotencia;
- [x] deduplicación exacta y ambigua;
- [x] cola de revisión humana;
- [x] auditoría;
- [x] seguridad bootstrap fail-closed;
- [x] API REST y OpenAPI;
- [x] RFC 7807;
- [x] frontend operativo;
- [x] Dockerfile y Compose;
- [x] CI versionado;
- [x] pruebas unitarias e integración implementadas;
- [x] documentación técnica y operativa.

### Hardening finalizado

- [x] exclusiones importadas retroactivas y auditadas;
- [x] preview con paridad de elegibilidad;
- [x] métricas separadas para filas bloqueadas;
- [x] revisión ambigua persistida en preview;
- [x] duplicado exacto enlazado al existente;
- [x] validación de correo;
- [x] recuperación segura de errores por fila;
- [x] CSV con coma/punto y coma y comillas;
- [x] rechazo de encabezados duplicados;
- [x] fechas Excel UTC;
- [x] límite multipart y HTTP 413;
- [x] saneamiento de nombres de archivo;
- [x] Basic Auth UTF-8.

### Consolidación y operación finalizadas

- [x] consolidar toda la rama temática en `main` por fast-forward;
- [x] confirmar igualdad de ramas al momento de consolidar;
- [x] convertir `main` en fuente canónica;
- [x] alinear `.env.example` y Docker Compose;
- [x] documentar instalación Linux/macOS;
- [x] documentar instalación Windows;
- [x] documentar arranque, health, Swagger y autenticación;
- [x] documentar flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- [x] documentar detención, reinicio y borrado de volumen;
- [x] documentar troubleshooting;
- [x] documentar consolidación en `docs/main-consolidation.md`.

### Bloqueantes para cierre

- [ ] obtener una ejecución CI visible o checkout local con red;
- [ ] ejecutar `mvn verify`;
- [ ] corregir compilación backend si falla;
- [ ] corregir Spotless si falla;
- [ ] validar Testcontainers/Flyway/Hibernate;
- [ ] ejecutar `npm install`;
- [ ] generar y versionar `package-lock.json`;
- [ ] ejecutar typecheck y build frontend;
- [ ] validar `docker compose config`;
- [ ] construir imagen backend;
- [ ] ejecutar smoke test manual;
- [ ] ejecutar escaneo local de secretos/datos;
- [ ] registrar evidencia completa en `docs/validation/SEG-001.md`;
- [ ] marcar SEG-001 `COMPLETE`;
- [ ] activar SEG-002.

### Mejoras no bloqueantes candidatas

- [ ] mostrar `excludedRows` en la UI;
- [ ] resolución auditada de `DuplicateReview`;
- [ ] retry explícito de `ImportJob`;
- [ ] filtros combinables adicionales;
- [ ] exportación de resultados de importación;
- [ ] contenedor o distribución estática del frontend;
- [ ] pruebas HTTP adicionales;
- [ ] prueba de concurrencia de idempotencia;
- [ ] accesibilidad básica.

## SEG-002 — alcance preparado, no iniciado

Cuando SEG-001 cierre:

- organización/tenant;
- usuario persistente;
- roles `OWNER`, `ADMIN`, `SALES`, `REVIEWER`, `READ_ONLY`;
- password hashing y lifecycle de credenciales;
- actor de auditoría;
- autorización por endpoint y método;
- restricciones específicas sobre exclusiones, auditoría e importación;
- migración segura desde bootstrap;
- pruebas de matriz de permisos;
- documentación de administración de usuarios.

## Reglas de priorización

1. fallos de seguridad;
2. pérdida o corrupción de datos;
3. idempotencia y exclusiones;
4. compilación y migraciones;
5. pruebas;
6. operación comercial;
7. UX;
8. optimización.

No comenzar SEG-002 mientras SEG-001 tenga un fallo bloqueante sin resolución o sin una decisión explícita de aplazamiento documentada.
