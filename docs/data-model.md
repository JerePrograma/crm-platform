# Modelo de datos

## Convenciones

- claves primarias UUID;
- timestamps `TIMESTAMPTZ` en UTC;
- optimistic locking mediante `version`;
- Flyway es la única autoridad para cambios de esquema;
- Hibernate usa `ddl-auto=validate`;
- las zonas horarias se aplican en presentación y programación, no en almacenamiento;
- valores de búsqueda se normalizan antes de persistir.

## Relaciones actuales

```text
Institution 1 --- * Contact 1 --- * ContactChannel
     |
     +--- 1 Prospect

Exclusion (canal normalizado independiente)

ImportJob 1 --- * ImportRow 0..1 --- 1 Prospect
                       |
                       +--- 0..1 DuplicateReview --- 0..1 Prospect existente

AuditEvent (referencia lógica por entity_type/entity_id)
SystemSetting
```

## Tablas

### `institution`

Campos relevantes:

- `name`;
- `normalized_name`;
- `category`;
- `locality`;
- `normalized_locality`;
- `province`;
- `country`;
- `website`;
- `website_domain`.

Restricciones:

- único `(normalized_name, normalized_locality)`;
- índice por `website_domain`.

La unicidad con localidad nula sigue las reglas de PostgreSQL: varias instituciones sin localidad pueden compartir nombre. La deduplicación por dominio y revisión nominal compensa esa situación; una política más estricta queda pendiente.

### `contact`

- pertenece a una institución;
- nombre opcional;
- rol opcional.

Un contacto puede representar una persona, una secretaría, una administración o un buzón genérico.

### `contact_channel`

- pertenece a un contacto;
- tipo: email, teléfono, WhatsApp, sitio o red social;
- valor de presentación;
- valor normalizado;
- indicador de canal principal.

Restricción única `(type, normalized_value)`.

### `prospect`

- relación uno a uno lógica con institución;
- ID externo opcional y único;
- estado comercial;
- prioridad y puntuación;
- alumnos estimados;
- herramientas actuales;
- dolor administrativo;
- fuente y evidencia;
- propietario;
- fechas de verificación, contacto y próxima acción;
- `contact_eligible`.

La migración V3 aplica unicidad sobre `institution_id` para evitar múltiples prospectos activos sobre la misma institución durante este segmento. En el futuro puede evolucionar a leads por unidad comercial si aparece un caso real.

### `exclusion`

- tipo de canal;
- valor normalizado;
- motivo.

Restricción única `(channel_type, normalized_value)`. La capa de aplicación considera teléfono y WhatsApp equivalentes para evitar supresiones parciales.

### `import_job`

- archivo y SHA-256;
- clave idempotente única;
- tipo CSV/XLSX;
- dry-run;
- estado;
- conteos por resultado;
- error y timestamps de ejecución.

La clave actual es:

```text
prospects:{sha256}:dryRun={true|false}
```

Un preview y una ejecución real son trabajos distintos. Repetir exactamente el mismo archivo y modo devuelve el trabajo existente.

### `import_row`

- trabajo;
- hoja de origen;
- número de fila;
- JSON crudo serializado como texto;
- email/teléfono normalizados;
- estado;
- error;
- prospecto resultante opcional.

Restricción única `(import_job_id, source_sheet, row_number)`.

### `duplicate_review`

- fila de importación única;
- prospecto existente opcional;
- tipo de coincidencia;
- confianza entre 0 y 1;
- estado humano;
- notas.

Las coincidencias nominales solo crean esta entidad. No existe fusión automática.

### `audit_event`

- acción;
- tipo e ID lógico de entidad;
- payload JSONB;
- timestamp.

Los payloads no deben contener contraseñas, tokens ni canales completos. Para exclusiones se almacena una huella SHA-256.

### `system_setting`

Inicializa:

- `sending.kill-switch=true`;
- `sending.enabled=false`;
- `sending.dry-run=true`;
- `sending.daily-limit=0`.

## Migraciones

- `V1`: esquema comercial inicial y configuración de envío cerrada;
- `V2`: claves normalizadas de institución;
- `V3`: importaciones persistentes y revisión de duplicados;
- `V4`: hoja de origen en filas importadas.

## Pendiente

- usuarios, roles y organizaciones;
- soft delete selectivo;
- etiquetas y segmentos;
- campañas, destinatarios, plantillas y adjuntos;
- comunicaciones, reservas e intentos;
- Gmail IDs e hilos;
- oportunidades, tareas, pruebas y cotizaciones;
- sincronización Sheets;
- índices de reporting y búsqueda;
- estrategia de archivado y retención.
