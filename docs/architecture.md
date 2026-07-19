# Arquitectura

## Objetivo

Construir un CRM comercial mantenible, seguro y extensible sin introducir microservicios prematuros. La unidad inicial de despliegue es un monolito modular; los límites internos deben permitir separar workers o integraciones cuando exista una razón operativa medible.

## Vista de contenedores

```text
Navegador
  |
  | HTTPS / JSON / multipart
  v
React + TypeScript + Vite
  |
  | /api/v1
  v
Spring Boot modular monolith
  |-- prospect / institution / contact
  |-- exclusion / eligibility
  |-- imports / duplicate review
  |-- audit / security / settings
  |-- future campaign / mail / google adapters
  |
  +--> PostgreSQL (fuente de verdad)
  +--> future Google APIs
  +--> future Cloud Tasks / Pub/Sub
```

## Reglas de dependencia

1. El dominio no depende de controladores ni proveedores externos.
2. Los controladores dependen de casos de uso, no de repositorios directamente.
3. La normalización es central y determinística.
4. Las exclusiones se consultan antes de declarar elegible un canal.
5. Las importaciones procesan cada fila en una transacción independiente.
6. Las coincidencias ambiguas generan revisión; nunca una fusión automática.
7. Los eventos de auditoría no almacenan contraseñas, tokens ni valores completos de canales en sus payloads.
8. PostgreSQL domina estados, idempotencia, reservas futuras, auditoría y resultados de integración.

## Módulos implementados

### `common`

- entidad base UUID/version/timestamps;
- normalización;
- excepciones de dominio;
- respuestas RFC 7807.

### `institution`

- identidad institucional;
- nombre y localidad normalizados;
- dominio web normalizado;
- consultas de deduplicación.

### `contact`

- persona o buzón funcional;
- canales separados y normalizados;
- unicidad por tipo y valor normalizado.

### `prospect`

- estado comercial;
- elegibilidad de contacto;
- metadatos de fuente, prioridad, puntuación y tamaño estimado;
- API paginada y ficha.

### `exclusion`

- supresión dominante;
- equivalencia teléfono/WhatsApp;
- aplicación retroactiva a prospectos existentes;
- auditoría con huella SHA-256 del canal.

### `imports`

- parser CSV/XLSX por encabezados;
- trabajo y filas persistentes;
- SHA-256 e idempotencia;
- preview y ejecución confirmada;
- deduplicación exacta y revisión ambigua;
- consultas operativas de filas y revisiones.

### `audit`

- escritura JSONB dentro de la transacción de negocio;
- consulta limitada de actividad reciente.

### `security`

- acceso bootstrap solo cuando existen credenciales explícitas;
- API stateless con HTTP Basic temporal;
- salud pública y resto denegado por defecto;
- CSRF ignorado exclusivamente en `/api/**`, donde la autorización se envía explícitamente.

## Separación futura

Los primeros candidatos para procesos separados son:

1. despachador de comunicaciones;
2. consumidor de Gmail/Pub/Sub;
3. sincronización de Google Sheets;
4. generación de reportes pesados;
5. clasificación asistida por IA.

La separación solo debe ocurrir cuando concurrencia, aislamiento, escalado o permisos distintos lo justifiquen.

## Decisiones no tomadas todavía

- proveedor de identidad productivo;
- mecanismo final de sesiones del frontend;
- estrategia exacta de Cloud Tasks local;
- motor de búsqueda avanzada;
- almacenamiento productivo de adjuntos;
- estrategia de lectura analítica;
- clasificación IA y proveedor.
