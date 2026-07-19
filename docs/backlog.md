# Backlog ejecutable

Solo un segmento puede estar `ACTIVE`. `COMPLETE` exige evidencia ejecutada, no solo código versionado.

## Resumen

| ID | Segmento | Estado | Dependencia | Resultado verificable |
|---|---|---|---|---|
| SEG-000 | Rama real y continuidad | COMPLETE | — | Rama, AGENTS, estado y puntero reales |
| SEG-001 | Vertical slice persistente de prospectos | ACTIVE | SEG-000 | Importación/deduplicación/exclusiones/UI con CI verde |
| SEG-002 | Identidad, usuarios y RBAC | PLANNED | SEG-001 | Usuarios persistentes y matriz de permisos probada |
| SEG-003 | Ficha integral, contactos, búsqueda y tags | PLANNED | SEG-002 | CRUD completo, filtros combinables y trazabilidad |
| SEG-004 | Campañas, plantillas y adjuntos | PLANNED | SEG-003 | Borrador aprobado, preview y hashes, sin envío |
| SEG-005 | Safety gate y kill switch operativo | PLANNED | SEG-004 | Guardas acumulativas probadas y panel de control |
| SEG-006 | OAuth Google de desarrollo | PLANNED | SEG-002 | Cuenta personal conectada mediante Secret Manager local/seguro |
| SEG-007 | MIME y Gmail fake | PLANNED | SEG-004, SEG-006 | MIME multipart probado contra adaptador fake |
| SEG-008 | Google Sheets bidireccional | PLANNED | SEG-003, SEG-006 | Preview, conflictos, auditoría y DB dominante |
| SEG-009 | Reservas e idempotencia de comunicación | PLANNED | SEG-005, SEG-007 | Reserva única, intentos y reconciliación |
| SEG-010 | Despacho local simulado | PLANNED | SEG-009 | Tareas locales, rate limit y backoff sin Gmail real |
| SEG-011 | Gmail lectura, threads y reconciliación | PLANNED | SEG-006, SEG-009 | IDs Gmail/RFC y estados conciliados |
| SEG-012 | Respuestas, rebotes y bajas | PLANNED | SEG-011 | Clasificación y acciones determinísticas |
| SEG-013 | Seguimientos programados | PLANNED | SEG-012 | Días hábiles 5/12, cancelación y NO_RESPONSE |
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

### Implementadas

- [x] stack backend;
- [x] esquema y migraciones;
- [x] institución/contacto/canal/prospecto;
- [x] exclusiones y elegibilidad;
- [x] importaciones persistentes;
- [x] deduplicación exacta y ambigua;
- [x] auditoría;
- [x] seguridad bootstrap;
- [x] API;
- [x] frontend;
- [x] Docker/Compose;
- [x] CI;
- [x] pruebas implementadas;
- [x] documentación principal.

### Bloqueantes para cierre

- [ ] observar CI del último commit;
- [ ] corregir compilación backend;
- [ ] corregir Spotless;
- [ ] corregir Flyway/JPA/Testcontainers;
- [ ] generar `package-lock.json`;
- [ ] corregir build frontend;
- [ ] validar Compose;
- [ ] validar imagen;
- [ ] ejecutar escaneo de secretos/datos;
- [ ] registrar evidencia en `docs/validation/SEG-001.md`.

### Mejoras no bloqueantes candidatas

- [ ] resolución auditada de DuplicateReview;
- [ ] retry explícito de ImportJob;
- [ ] filtros combinables adicionales;
- [ ] exportación de resultados de importación;
- [ ] contenedor frontend;
- [ ] pruebas API adicionales;
- [ ] prueba de concurrencia de idempotencia.

## Reglas de priorización

1. fallos de seguridad;
2. fallos de datos/idempotencia;
3. compilación y migraciones;
4. pruebas;
5. operación comercial;
6. UX;
7. optimización.

No comenzar SEG-002 mientras SEG-001 tenga un fallo bloqueante sin resolución o sin una decisión explícita de aplazamiento documentada.
