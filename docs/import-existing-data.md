# Importación de datos existentes

## Principio

PostgreSQL es la fuente de verdad. Los archivos son entradas controladas, no bases permanentes.

El lote real no se versiona porque el repositorio es público. Debe permanecer en almacenamiento operativo autorizado y enviarse a la API desde una estación confiable.

## Formatos

- `.xlsx`;
- `.csv` para prospectos solamente.

Tamaño máximo actual: 10 MB.

## Hojas XLSX

### `Prospectos`

La columna obligatoria es `Institución`. El parser reconoce por encabezado normalizado, no por posición.

Encabezados conocidos:

```text
ID
Institución
Localidad
Provincia
Categoría
Sitio web
Redes sociales
Correo publicado
Teléfono / WhatsApp
Fuente
Fecha de verificación
Motivo de encaje
Prioridad
Estado comercial
Fecha último contacto
Validación Gmail
Validación publicada
Asunto sugerido
Apertura personalizada
Observaciones
Auditoría operativa
Cruce Gmail exacto
Prueba técnica adjuntos
Resultado envío
Fecha auditoría
Observación de control
```

Solo se utilizan actualmente los campos necesarios para institución, canales, fuente, evidencia, prioridad y verificación. Las columnas restantes se conservan en `import_row.raw_data` para trazabilidad.

### `Exclusiones`

Encabezados conocidos:

```text
Institución
Correo
Motivo de exclusión
Fecha de verificación
Resultado
```

La exclusión se procesa antes de los prospectos del mismo archivo.

## Marcadores ignorados

Valores que no se interpretan como canales:

- `No publicado`;
- `No relevado`;
- `Sin dato`;
- `N/A`.

## Preview

```bash
curl --user "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  --form "file=@/ruta/prospectos.xlsx" \
  http://localhost:8080/api/v1/imports/prospects/preview
```

PowerShell:

```powershell
$pair = "$env:CRM_BOOTSTRAP_USERNAME`:$env:CRM_BOOTSTRAP_PASSWORD"
$token = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/imports/prospects/preview `
  -Method Post `
  -Headers @{ Authorization = "Basic $token" } `
  -Form @{ file = Get-Item 'C:\ruta\prospectos.xlsx' }
```

El preview:

- calcula SHA-256;
- crea trabajo y filas de evidencia;
- valida estructura;
- ejecuta deduplicación;
- no crea instituciones, contactos, prospectos, exclusiones ni revisiones persistentes.

## Ejecución

Debe revisarse primero el preview. Luego:

```bash
curl --user "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  --header "X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT" \
  --form "file=@/ruta/prospectos.xlsx" \
  http://localhost:8080/api/v1/imports/prospects/execute
```

PowerShell:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/imports/prospects/execute `
  -Method Post `
  -Headers @{
    Authorization = "Basic $token"
    'X-Import-Confirmation' = 'EXECUTE_PROSPECT_IMPORT'
  } `
  -Form @{ file = Get-Item 'C:\ruta\prospectos.xlsx' }
```

## Revisión de resultados

```text
GET /api/v1/imports/prospects/{jobId}
GET /api/v1/imports/prospects/{jobId}/rows
GET /api/v1/imports/prospects/duplicate-reviews/pending
```

Resultados de fila:

- `ACCEPTED`;
- `EXCLUDED`;
- `REJECTED`;
- `DUPLICATE`;
- `REVIEW_REQUIRED`.

## Idempotencia

La misma combinación archivo+modo devuelve el trabajo existente. Para volver a ejecutar un archivo fallido no debe alterarse manualmente la base: se implementará una acción de retry explícita. Mientras tanto, corregir el archivo genera un SHA distinto y un nuevo trabajo.

## Deduplicación

Exacta:

- ID externo;
- correo;
- teléfono/WhatsApp;
- dominio;
- nombre+localidad.

Ambigua:

- similitud nominal en misma localidad.

No fusionar manualmente en SQL. La cola de revisión tendrá acciones auditadas en una iteración posterior.

## Comprobaciones posteriores

1. verificar conteos del trabajo;
2. revisar todas las filas rechazadas;
3. revisar duplicados ambiguos;
4. comprobar exclusiones;
5. muestrear instituciones y canales normalizados;
6. confirmar que `contact_eligible=false` para excluidos;
7. exportar un informe del trabajo antes de eliminar el archivo local.

## Datos faltantes

El alcance comercial menciona 298 prospectos, pero el lote disponible contiene 100. No deben inventarse los 198 restantes. Se importarán cuando exista una fuente real y autorizada.

Los cuatro contactos previos —La Colmena, Collegium, LAEM y Trobada— deben agregarse a exclusiones solo cuando se disponga de sus canales exactos y verificados. Excluir por nombre sin canal produciría una supresión ambigua.
