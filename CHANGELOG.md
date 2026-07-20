# Changelog

Todos los cambios relevantes se documentan aquí. El proyecto todavía no tiene una versión publicada.

## Unreleased

### Added

- Java 21, Spring Boot, Maven Wrapper verificado, PostgreSQL y Flyway;
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
- API REST versionada, OpenAPI y RFC 7807;
- autenticación bootstrap fail-closed;
- React, TypeScript y Vite con dashboard, prospectos, importaciones, exclusiones y auditoría;
- Docker, Docker Compose y GitHub Actions;
- fixtures anónimas y pruebas con Testcontainers;
- documentación de arquitectura, dominio, datos, seguridad, pruebas, importación y operación;
- guía completa `docs/local-development-and-usage.md`;
- evidencia de consolidación `docs/main-consolidation.md`.

### Changed

- `main` es la única rama canónica;
- todo SEG-001 se consolidó en `main` mediante fast-forward sin force push;
- README es el punto de entrada para instalación y uso;
- `AGENTS.md` exige partir de `main` y leer validación/guía operativa;
- institución utiliza localidad y dominio normalizados para deduplicación;
- CI utiliza el lanzador Maven fijado por el repositorio;
- API stateless ignora CSRF únicamente bajo `/api/**`;
- auditoría evita copiar canales completos en payloads de exclusión;
- resultados de importación se ordenan de forma determinística por hoja y fila;
- credenciales Basic del frontend se codifican desde bytes UTF-8;
- nombres de archivos importados se reducen a un basename seguro;
- CSV admite delimitadores coma y punto y coma;
- fechas numéricas Excel se interpretan en UTC;
- Docker Compose consume base, usuario y contraseña desde `.env`;
- `.env.example` y PostgreSQL local utilizan valores coherentes.

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
- evidencia de filas de hojas distintas deja de intercalarse ambiguamente;
- la configuración inicial ya no entrega una contraseña backend distinta a la del contenedor PostgreSQL.

### Documentation

- estado, backlog, puntero y validación reorientados a `main`;
- instrucciones Linux/macOS y Windows;
- health, Swagger y ejemplos de autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- formatos CSV/XLSX y semántica de estados por fila;
- detención, reinicio, eliminación de volumen y troubleshooting;
- riesgos, tareas finalizadas y tareas pendientes unificados.

### Security

- sin credenciales bootstrap explícitas no existe acceso a API de negocio;
- datos comerciales reales excluidos del repositorio público;
- Maven 3.9.16 se verifica mediante SHA-512;
- no existe código capaz de enviar correos;
- búsquedas remotas no encontraron claves privadas, tokens, correos personales ni el XLSX real;
- rutas y caracteres de control del nombre de archivo no llegan a auditoría;
- PostgreSQL local permanece enlazado a `127.0.0.1`;
- las cuatro variables `SENDING_*` continúan cerradas.

### Known limitations

- CI del último commit todavía no fue observado como verde;
- falta `package-lock.json`;
- RBAC persistente no implementado;
- no existe acción para resolver revisiones de duplicados;
- no existe retry explícito de importaciones fallidas;
- el frontend todavía no muestra `excludedRows` como control independiente;
- no existen campañas, Gmail, Sheets ni infraestructura cloud.
