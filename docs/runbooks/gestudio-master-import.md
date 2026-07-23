# Importación segura del maestro comercial de Gestudio

## Objetivo

Importar un workbook operativo sin convertir vistas derivadas en fuentes duplicadas y sin volver contactables registros que ya tuvieron conversación o presentan una condición de exclusión.

## Hojas admitidas

El parser acepta únicamente una de estas hojas de prospectos:

1. `Prospectos`, contrato canónico de importación;
2. `Maestro`, compatibilidad explícita con el maestro comercial.

Si ambas existen, se usa `Prospectos`. No se toma la primera hoja del workbook como fallback.

Para exclusiones se admite:

1. `Exclusiones`;
2. `Exclusiones previas`, por compatibilidad con el maestro.

Las demás hojas se ignoran.

## Reglas para `Maestro`

- `ID` se transforma en `maestro:<ID>` para evitar colisiones con identificadores de otros orígenes.
- `Correo principal` se interpreta como correo publicado.
- `Origen` se interpreta como fuente cuando no existe `Fuente`.
- Los campos operativos del maestro se conservan como evidencia, sin crear canales adicionales automáticamente.
- Un registro se incorpora a las exclusiones cuando tiene `Primer contacto` o cuando su estado/entrega indica cierre, respuesta, interés, seguimiento, rebote, invalidez, cliente o no contactar.
- Las exclusiones declaradas y derivadas se deduplican por correo normalizado.

## Flujo obligatorio

1. Mantener el XLSX fuera del repositorio, imágenes, fixtures y CI.
2. Ejecutar primero `Preview`.
3. Revisar aceptadas, excluidas, rechazadas, duplicadas y revisiones ambiguas.
4. No ejecutar la importación confirmada si aparecen filas rechazadas inesperadas o contactos previos aceptados como elegibles.
5. Ejecutar con `X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT` solo después de aprobar el preview.
6. Mantener deshabilitadas las comunicaciones reales y los workers.

## Criterios de aceptación

- Un workbook sin `Prospectos` ni `Maestro` se rechaza.
- `Prospectos` tiene precedencia cuando ambas hojas existen.
- Las vistas derivadas no se importan.
- Los IDs del maestro quedan namespaced.
- Los contactos previos quedan excluidos.
- Reimportar el mismo archivo conserva la idempotencia basada en SHA-256.
- No se persisten estados de envío reales ni se habilita red de mensajería.
