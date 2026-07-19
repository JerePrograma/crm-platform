# Operaciones manuales

## Arranque

1. copiar `.env.example` a `.env`;
2. definir contraseña de PostgreSQL y credenciales bootstrap locales;
3. iniciar PostgreSQL;
4. iniciar backend;
5. iniciar frontend;
6. verificar `/actuator/health`;
7. ingresar desde la UI sin guardar credenciales en el navegador.

## Verificar controles de envío

Antes de cualquier operación:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

En PostgreSQL:

```sql
SELECT setting_key, setting_value
FROM system_setting
WHERE setting_key LIKE 'sending.%'
ORDER BY setting_key;
```

Resultado esperado:

```text
sending.daily-limit  0
sending.dry-run      true
sending.enabled      false
sending.kill-switch  true
```

## Importar prospectos

1. usar primero preview;
2. revisar resumen y filas;
3. corregir archivo si existen rechazadas;
4. ejecutar únicamente con la cabecera de confirmación;
5. revisar duplicados ambiguos;
6. registrar el ID del trabajo en el ticket operativo.

No modificar filas de importación ni conteos directamente en SQL.

## Crear exclusión

Desde UI o API:

1. elegir tipo correcto;
2. ingresar el valor completo;
3. elegir motivo verificable;
4. confirmar que el prospecto relacionado quedó `DO_NOT_CONTACT`;
5. revisar evento `EXCLUSION_CREATED`.

No se implementó eliminación de exclusiones. Una revocación futura deberá exigir rol, motivo y auditoría.

## Revisar auditoría

```text
GET /api/v1/audit?limit=100
```

Buscar:

- `PROSPECT_CREATED`;
- `EXCLUSION_CREATED`;
- `IMPORT_STARTED`;
- `IMPORT_COMPLETED`;
- `IMPORT_FAILED`.

La auditoría no sustituye logs técnicos ni métricas.

## Consultas de diagnóstico

Trabajos recientes:

```sql
SELECT id, file_name, dry_run, status, total_rows,
       accepted_rows, rejected_rows, duplicate_rows, review_rows,
       started_at, completed_at
FROM import_job
ORDER BY created_at DESC
LIMIT 20;
```

Filas problemáticas:

```sql
SELECT source_sheet, row_number, status, error_message
FROM import_row
WHERE import_job_id = :job_id
  AND status IN ('REJECTED', 'DUPLICATE', 'REVIEW_REQUIRED')
ORDER BY source_sheet, row_number;
```

Revisiones pendientes:

```sql
SELECT id, import_row_id, existing_prospect_id, match_type,
       confidence, created_at
FROM duplicate_review
WHERE status = 'PENDING'
ORDER BY created_at;
```

Prospectos inelegibles:

```sql
SELECT p.id, i.name, p.status, p.contact_eligible
FROM prospect p
JOIN institution i ON i.id = p.institution_id
WHERE p.contact_eligible = false;
```

## Fallos

### Backend no inicia

- revisar conexión a PostgreSQL;
- revisar migraciones Flyway;
- no usar `ddl-auto=update` para sortear el error;
- registrar stack trace sin secretos;
- corregir migración o entidad.

### Importación falla completa

- consultar `import_job.error_message`;
- verificar extensión, tamaño y encabezados;
- no editar el trabajo para reintentarlo;
- corregir el archivo y generar un SHA nuevo.

### Filas rechazadas

- consultar resultado por fila;
- corregir solo la fuente;
- no crear prospectos manuales para ocultar el error sin dejar auditoría.

### UI no autentica

- comprobar variables `CRM_BOOTSTRAP_*` del proceso backend;
- reiniciar backend después de cambiarlas;
- no incrustar credenciales en el frontend.

## Apagado

```bash
docker compose down
```

Para conservar datos no usar `-v`. Para eliminar el volumen local de forma consciente:

```bash
docker compose down -v
```

Nunca ejecutar la eliminación de volumen en staging o producción.
