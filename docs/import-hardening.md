# Hardening del importador de prospectos

Actualizado: 2026-07-20

## Propósito

Este documento define el comportamiento normativo del importador CSV/XLSX de `SEG-001`. Ante divergencias, el código, las migraciones Flyway y las pruebas deben corregirse para respetar estas reglas.

## Entradas admitidas

- `.xlsx`;
- `.csv` UTF-8;
- tamaño funcional máximo: 10 MiB;
- request multipart máximo: 11 MB para tolerar overhead;
- nombre persistido: basename saneado, máximo 255 caracteres;
- nunca se conserva una ruta enviada por el cliente.

## Estructura XLSX

### Prospectos

La hoja preferida es `Prospectos`. Si no existe, se utiliza la primera hoja solo por compatibilidad inicial. La columna obligatoria es `Institución`.

Aliases relevantes:

- correo: `Correo publicado`, `Correo`, `Email`;
- teléfono: `Teléfono / WhatsApp`, `Teléfono o WhatsApp`, `Teléfono`, `WhatsApp`;
- web: `Sitio web`;
- localidad y provincia;
- ID externo;
- fuente, prioridad y evidencia.

### Exclusiones

La hoja opcional `Exclusiones` requiere `Correo`. Las exclusiones se procesan antes que los prospectos para que una dirección incluida en ambas hojas resulte bloqueada durante la misma ejecución.

## Estructura CSV

- detecta automáticamente `,` o `;` a partir del encabezado;
- respeta campos entre comillas;
- respeta delimitadores y saltos dentro de campos citados;
- interpreta `""` como comilla escapada;
- rechaza comillas sin cerrar;
- rechaza encabezados que colisionan después de normalización;
- requiere `Institución`;
- CSV solo representa prospectos; exclusiones múltiples pertenecen a XLSX o API.

## Normalización

### Encabezados

- minúsculas;
- sin diacríticos;
- puntuación reemplazada por espacios;
- espacios compactados.

Por lo tanto, `Institución` e `INSTITUCION` son la misma clave y no pueden coexistir.

### Correo

- trim;
- lowercase con locale raíz;
- patrón mínimo `usuario@dominio.tld`;
- un formato inválido rechaza la fila, no todo el archivo.

### Teléfono/WhatsApp

- solo dígitos;
- PHONE y WHATSAPP son equivalentes para exclusiones.

### Dominio

- agrega esquema temporal si falta;
- extrae host;
- lowercase;
- elimina prefijo `www.`.

### Fechas

- ISO `yyyy-MM-dd`;
- `d/M/yyyy`;
- `d-M-yyyy`;
- fechas numéricas Excel convertidas explícitamente en UTC.

## Marcadores tratados como ausencia de dato

- `No publicado...`;
- `No relevado...`;
- `Sin dato...`;
- `N/A` después de normalización.

Estos textos nunca se convierten en canales de contacto.

## Ciclo de vida

1. validar basename, extensión, contenido y tamaño;
2. calcular SHA-256;
3. generar clave `prospects:{sha}:dryRun={modo}`;
4. recuperar job existente o crear `ImportJob`;
5. parsear completamente el archivo;
6. procesar exclusiones;
7. procesar prospectos;
8. persistir una transacción independiente por fila;
9. completar contadores y auditoría;
10. ante fallo estructural, marcar job `FAILED` y propagar error.

## Idempotencia

La misma combinación de bytes y modo devuelve el job existente. Preview y ejecución son jobs distintos porque su modo forma parte de la clave.

Limitación: un job `FAILED` no dispone todavía de retry explícito con la misma clave. No modificar silenciosamente esta semántica; el retry debe ser una acción auditable.

## Clasificación de filas

- `ACCEPTED`: operación válida y elegible;
- `EXCLUDED`: prospecto bloqueado por exclusión;
- `REJECTED`: datos inválidos o error acotado a la fila;
- `DUPLICATE`: coincidencia exacta;
- `REVIEW_REQUIRED`: coincidencia ambigua;
- `PENDING`: estado transitorio.

Los contadores del job son independientes:

- `acceptedRows`;
- `excludedRows`;
- `rejectedRows`;
- `duplicateRows`;
- `reviewRows`.

La suma debe corresponder a `totalRows` al completar el job.

## Preview

Preview persiste evidencia operativa, pero no crea:

- instituciones;
- contactos;
- canales;
- prospectos;
- exclusiones nuevas.

Sí persiste:

- ImportJob;
- ImportRow;
- DuplicateReview ambiguo;
- auditoría del ciclo del job.

Preview aplica las exclusiones ya existentes y marca la fila `EXCLUDED`. De este modo, el resultado anticipa la ejecución real sin mutar el dominio.

## Ejecución

Requiere:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

Una exclusión importada debe utilizar `ExclusionApplicationService` para:

- normalizar;
- detectar duplicado;
- persistir;
- deshabilitar prospecto existente;
- aplicar `DO_NOT_CONTACT`;
- registrar auditoría con fingerprint del canal.

No guardar exclusiones directamente mediante repositorio desde importadores.

## Deduplicación

Orden de evaluación:

1. ID externo;
2. email;
3. teléfono/WhatsApp;
4. dominio;
5. nombre+localidad;
6. similitud nominal en la misma localidad.

Coincidencia exacta:

- fila `DUPLICATE`;
- referencia a prospecto existente persistida.

Coincidencia ambigua:

- fila `REVIEW_REQUIRED`;
- DuplicateReview `PENDING`;
- no se crea ni fusiona prospecto;
- se conserva también durante preview.

## Recuperación por fila

Cada fila usa `REQUIRES_NEW`. Si una fila falla:

1. la transacción de procesamiento se revierte;
2. se crea una nueva transacción de evidencia;
3. normalización tolerante evita repetir el error;
4. se persiste `REJECTED` y mensaje truncado a 1000 caracteres;
5. el job continúa.

Un error de parser o infraestructura fuera de una fila marca el job completo `FAILED`.

## Auditoría y privacidad

- fileName saneado;
- SHA-256 registrado;
- contadores completos;
- rawData solo en PostgreSQL, no en logs;
- exclusiones auditadas mediante fingerprint;
- datos comerciales reales nunca se versionan;
- fixtures usan `.test`.

## Pruebas normativas

- workbook 100/16;
- reimportación idempotente;
- dry-run sin escrituras de dominio;
- preview con exclusión;
- exclusión importada retroactiva;
- auditoría de exclusión;
- correo inválido aislado por fila;
- CSV `;` y delimitador citado;
- comillas sin cerrar;
- encabezados duplicados;
- ambiguo persistido en preview;
- duplicado exacto enlazado.

## Pendientes

- ejecución real de toda la suite;
- retry auditable de jobs fallidos;
- resolución auditada de DuplicateReview;
- UI específica de `excludedRows`;
- límites de filas/tiempo además de bytes;
- política de retención y borrado de rawData;
- RBAC para datos operativos.
