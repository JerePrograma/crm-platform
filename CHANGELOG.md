# Changelog

Todos los cambios relevantes se documentan aquí. El proyecto todavía no tiene una versión publicada.

## Unreleased

### Added

- rama real y protocolo de continuidad por segmentos;
- Java 21, Spring Boot, Maven verificado, PostgreSQL y Flyway;
- configuración de envío fail-closed y kill switch persistente;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización de nombre, correo, teléfono y dominio;
- deduplicación exacta y revisión nominal ambigua;
- importaciones CSV/XLSX persistentes con SHA-256 e idempotencia;
- preview, ejecución confirmada y resultado por fila;
- métrica persistente `excludedRows` separada de filas aceptadas;
- cola de revisión de duplicados, incluso durante preview;
- auditoría JSONB y API de consulta;
- exclusiones retroactivas con equivalencia teléfono/WhatsApp;
- API REST versionada y RFC 7807;
- autenticación bootstrap fail-closed;
- React, TypeScript y Vite con dashboard, prospectos, importaciones, exclusiones y auditoría;
- Docker, Docker Compose y GitHub Actions;
- fixtures anónimas y pruebas con Testcontainers;
- documentación de arquitectura, dominio, datos, seguridad, pruebas, importación y operación.

### Changed

- institución utiliza localidad y dominio normalizados para deduplicación;
- CI utiliza el lanzador Maven fijado por el repositorio;
- API stateless ignora CSRF únicamente bajo `/api/**`;
- auditoría evita copiar canales completos en payloads de exclusión;
- resultados de importación se ordenan de forma determinística por hoja y fila;
- credenciales Basic del frontend se codifican desde bytes UTF-8;
- nombres de archivos importados se reducen a un basename seguro antes de persistir o auditar;
- CSV admite delimitadores coma y punto y coma;
- fechas numéricas Excel se interpretan en UTC.

### Fixed — hardening 2026-07-20

- exclusiones cargadas desde XLSX atraviesan el mismo caso de uso que las exclusiones manuales, deshabilitan prospectos existentes y generan auditoría;
- coincidencias ambiguas del preview conservan `DuplicateReview` para inspección humana;
- filas duplicadas exactas conservan referencia al prospecto existente;
- el preview aplica exclusiones de email, teléfono, WhatsApp y dominio sin escribir datos de dominio;
- filas `EXCLUDED` ya no se mezclan con `acceptedRows`;
- correos malformados se rechazan antes de persistir canales;
- el registro de una fila rechazada no vuelve a lanzar el mismo error de normalización;
- límite multipart de Spring alineado con el límite funcional de 10 MB;
- archivos que exceden el límite devuelven RFC 7807 con HTTP 413;
- CSV con comillas sin cerrar o encabezados normalizados duplicados se rechaza explícitamente;
- evidencia de filas de hojas distintas deja de intercalarse ambiguamente.

### Security

- sin credenciales bootstrap explícitas no existe acceso a API de negocio;
- datos comerciales reales excluidos del repositorio público;
- Maven 3.9.16 se verifica mediante SHA-512;
- no existe código capaz de enviar correos;
- búsqueda remota no encontró claves privadas, tokens, correos personales ni el XLSX real;
- rutas y caracteres de control del nombre de archivo no llegan a auditoría.

### Known limitations

- CI del último commit todavía no fue observado como verde;
- falta lockfile frontend;
- RBAC persistente no implementado;
- no existe acción para resolver revisiones de duplicados;
- no existe retry explícito de importaciones fallidas;
- el frontend todavía no muestra `excludedRows` como tarjeta independiente;
- no existen campañas, Gmail, Sheets ni infraestructura cloud.
