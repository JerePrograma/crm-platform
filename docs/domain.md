# Dominio comercial

## Producto comercializado

Gestudio es una plataforma web para academias, estudios, escuelas e instituciones con alumnos y clases recurrentes. Centraliza alumnos, inscripciones, actividades, profesores, grupos, horarios, asistencias, mensualidades, pagos, deuda, caja, recibos y reportes desde PC o celular.

Planes vigentes usados como contexto comercial:

| Alumnos activos | Precio mensual |
|---:|---:|
| 1–50 | ARS 39.900 |
| 51–150 | ARS 59.900 |
| 151–300 | ARS 89.900 |
| Más de 300 | Desde ARS 119.900, sujeto a cotización |

Todos los módulos están incluidos y no se cobra por usuarios administrativos adicionales.

## Lenguaje ubicuo

### Institución

Organización potencialmente cliente. Conserva identidad, categoría, ubicación y presencia web.

### Contacto

Persona o función dentro de una institución: dirección, administración, secretaría, sistemas, propietario, docente, recepción o contacto genérico.

### Canal

Dato concreto para relacionarse con un contacto: correo, teléfono, WhatsApp, sitio web o red social. Un canal siempre tiene una forma normalizada para búsqueda y deduplicación.

### Prospecto

Representación comercial de una institución dentro del CRM. Conserva fuente, evidencia, prioridad, puntuación, estimación de alumnos, propietario y estado.

### Elegibilidad

Decisión determinística sobre si un prospecto puede considerarse para contacto. No equivale a autorización de envío: una campaña futura deberá superar guardas adicionales.

### Exclusión

Supresión dominante sobre un canal. Prevalece sobre campaña, segmento, prioridad y automatización.

### Importación

Trabajo persistente que analiza un archivo, registra cada fila, calcula resultados y permite auditoría. Puede ser preview o ejecución.

### Duplicado exacto

Coincidencia determinística por ID externo, correo, teléfono/WhatsApp, dominio o nombre+localidad.

### Revisión de duplicado

Coincidencia ambigua, principalmente nominal. Requiere una persona; nunca se fusiona automáticamente.

### Auditoría

Registro inmutable de una acción comercial relevante, su entidad y un payload mínimo sin secretos.

## Invariantes implementados

1. Una institución tiene como máximo un prospecto en este segmento.
2. Un ID externo no puede identificar dos prospectos.
3. Un canal normalizado por tipo no puede repetirse.
4. Una exclusión exacta vuelve inelegible el prospecto.
5. Teléfono y WhatsApp se consideran equivalentes para supresión.
6. Una exclusión creada después del prospecto lo cambia a `DO_NOT_CONTACT`.
7. Una coincidencia ambigua no crea ni fusiona prospectos.
8. Repetir un archivo con el mismo SHA-256 y modo no repite la importación.
9. Un error de fila no revierte las filas procesadas correctamente.
10. El preview no crea instituciones, contactos, prospectos, exclusiones ni revisiones permanentes; sí conserva evidencia del trabajo y sus filas.
11. La ejecución exige una confirmación HTTP explícita.
12. No existe ningún caso de uso de envío real.

## Estados de prospecto

```text
NEW
NEEDS_ENRICHMENT
READY_FOR_REVIEW
APPROVED
QUEUED
CONTACTED
REPLIED
INTERESTED
QUALIFIED
TRIAL_PROPOSED
TRIAL_ACTIVE
QUOTED
NEGOTIATION
WON
LOST
NO_RESPONSE
BOUNCED
UNSUBSCRIBED
DO_NOT_CONTACT
INVALID
DUPLICATE
ARCHIVED
```

En este segmento solo se automatiza la transición inicial:

- elegible → `NEW`;
- excluido → `DO_NOT_CONTACT`.

El resto se implementará con una máquina de estados y reglas de autorización en segmentos posteriores.

## Motivos de exclusión

- solicitud de baja;
- respuesta negativa;
- rebote permanente;
- contacto inválido;
- relación cerrada;
- cliente existente;
- conversación existente;
- institución no pertinente;
- exclusión manual.

## Deduplicación

Orden de evaluación:

1. ID externo;
2. correo normalizado;
3. teléfono/WhatsApp normalizado;
4. dominio;
5. nombre normalizado + localidad normalizada;
6. similitud nominal dentro de la misma localidad.

La similitud nominal combina distancia de edición y superposición de tokens. Las diferencias entre sufijos numéricos reciben un límite de confianza para evitar confundir sedes o fixtures numeradas.

## Fuera de alcance de SEG-001

- campañas y destinatarios;
- plantillas y adjuntos comerciales;
- conversaciones y respuestas;
- Gmail y Google Sheets;
- oportunidades, tareas, pruebas y cotizaciones;
- reservas de envío;
- entregabilidad;
- workers cloud;
- IA de clasificación.
