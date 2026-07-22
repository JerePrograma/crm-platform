# Evidencia de performance y accesibilidad SEG-010

Fecha: 2026-07-22

## Entorno

Windows 11, Docker Desktop 29.3.1, 7.752 MiB asignados, Java 21.0.11,
PostgreSQL 17.10 y Node 22. El objetivo fue detectar comportamiento obviamente
no escalable; no se define un SLA universal.

## Import preview sintético

Medido por `ProspectImportFileParserTest` dentro del validador backend:

| Filas | Bytes | Duración observada |
|---:|---:|---:|
| 100 | 11.422 | 29 ms |
| 1.000 | 72.646 | 168 ms |
| 10.000 | 677.456 | 1.571 ms |

La entrada está limitada a 10 MiB, 10.000 filas, 100 columnas y 10.000
caracteres por celda. El resultado no conserva el workbook real ni filas en la
evidencia.

## Superficies operativas

El E2E completo ejecutó búsqueda paginada, dashboard/reporting, timeline,
audiencia congelada, pipeline, batch worker y exportación CSV en 13.3 s sobre un
dataset sintético funcional. Las pruebas PostgreSQL verifican agregación exacta,
monedas separadas, tenant isolation, worker claim doble y exportación formula
safe. La carga masiva de 10.000 se usa en el parser; no se afirma una medición
aislada de 10.000 entidades persistidas para cada pantalla.

La implementación evita cargar tablas completas: usa `LIMIT/OFFSET`, límites
máximos, agregaciones SQL, batch/`SKIP LOCKED`, índices tenant-first y CSV
acotado. V13 agrega GIN trigram para campos buscables e índices de tareas,
actividades, oportunidades y tags.

`EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` sobre la búsqueda tenant-scoped del
stack sintético (41 prospectos/instituciones) devolvió 13 filas, `LIMIT 20`,
quicksort en memoria (25 KiB), cero lecturas/temporales y 2,661 ms de ejecución;
el planner eligió sequential scans por el tamaño pequeño. Esto es esperable y no
se presenta como prueba del plan de 10.000 filas. Antes de fijar SLO productivo
debe repetirse el plan con una distribución representativa y estadísticas
actualizadas.

## Accesibilidad y responsive

- navegación semántica y links/botones operables por teclado;
- labels asociados y mensajes de loading/empty/error;
- focus visible, estados con texto además de color y colores de tags validados;
- confirmaciones para cancel/requeue y manejo de 409;
- viewport E2E 390x844 además de desktop;
- sidebar desktop scrollable, corrigiendo logout fuera del viewport;
- tablas/paneles con overflow acotado y layouts responsivos.

El E2E automatiza teclado y viewport, pero no sustituye una auditoría humana con
lector de pantalla ni una certificación WCAG. Esa revisión es externa al perfil
local ejecutado y no se declara como PASS total de conformidad.
