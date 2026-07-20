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
- React, TypeScript y Vite;
- Docker, Docker Compose y GitHub Actions;
- perfil Compose `app` con PostgreSQL, backend y frontend;
- imagen backend multi-stage con health check;
- imagen frontend multi-stage con Nginx y proxy API;
- preflight Unix y PowerShell con modo local/container-only;
- smoke tests Unix y PowerShell;
- Makefile para DB, stack, logs, verificación y smoke;
- `.dockerignore` raíz y frontend;
- `.gitattributes` multiplataforma;
- fixtures anónimas y pruebas con Testcontainers;
- documentación de arquitectura, dominio, datos, seguridad, pruebas e importación;
- guía completa `docs/local-development-and-usage.md`;
- inicio rápido `docs/containerized-quickstart.md`;
- documentación de scripts en `scripts/README.md`;
- evidencia de consolidación `docs/main-consolidation.md`.

### Changed

- `main` es la única rama canónica;
- todo SEG-001 se consolidó en `main` mediante fast-forward sin force push;
- README presenta stack contenedorizado y procesos separados;
- `AGENTS.md` exige partir de `main`;
- institución utiliza localidad y dominio normalizados para deduplicación;
- API stateless ignora CSRF únicamente bajo `/api/**`;
- auditoría evita copiar canales completos en exclusiones;
- resultados de importación se ordenan por hoja y fila;
- credenciales Basic del frontend se codifican desde bytes UTF-8;
- nombres de archivos importados se reducen a basename seguro;
- CSV admite coma y punto y coma;
- fechas numéricas Excel se interpretan en UTC;
- Compose consume base, usuario y contraseña desde `.env`;
- puertos locales se publican solo en loopback;
- CI valida scripts Unix/PowerShell, preflight, typecheck, Compose y ambas imágenes;
- caché npm permanece desactivada hasta disponer de lockfile;
- contextos Docker excluyen secretos, datos, cachés y artefactos.

### Fixed — hardening 2026-07-20

- exclusiones XLSX usan el mismo caso de uso que exclusiones manuales;
- exclusiones importadas deshabilitan prospectos existentes y auditan;
- coincidencias ambiguas del preview conservan `DuplicateReview`;
- duplicados exactos conservan referencia al prospecto existente;
- preview aplica exclusiones sin escribir dominio;
- filas `EXCLUDED` dejan de mezclarse con `acceptedRows`;
- correos malformados se rechazan antes de persistir;
- registro de rechazo no vuelve a lanzar el mismo error;
- multipart y límite funcional comparten 10 MB;
- exceso de tamaño devuelve RFC 7807 con HTTP 413;
- CSV inválido y encabezados duplicados se rechazan;
- evidencia de distintas hojas se ordena de forma estable;
- backend y PostgreSQL local comparten credenciales coherentes;
- sintaxis PowerShell del smoke test fue corregida;
- CI evita caché npm incompatible con ausencia de lockfile.

### Documentation

- estado, backlog, puntero, segmento y validación reorientados a `main`;
- instrucciones Linux/macOS y Windows;
- modalidad solo Docker sin Java/Node en host;
- modalidad de desarrollo por procesos;
- health, Swagger y autenticación;
- flujo Dashboard → Exclusiones → Preview → Ejecución → Prospectos → Auditoría;
- formatos CSV/XLSX y estados por fila;
- detención, reinicio, eliminación de volumen y troubleshooting;
- comandos Make, preflight y smoke tests;
- riesgos, tareas finalizadas y pendientes unificados.

### Security

- sin credenciales bootstrap no existe acceso a API de negocio;
- datos comerciales reales fuera del repositorio público;
- Maven 3.9.16 se verifica mediante SHA-512;
- no existe código capaz de enviar correos;
- búsquedas remotas sin claves privadas, tokens, correos personales o XLSX real;
- rutas y caracteres de control no llegan a auditoría;
- PostgreSQL, backend y frontend quedan ligados a `127.0.0.1`;
- las cuatro variables `SENDING_*` continúan cerradas;
- preflight falla si alguna guarda de envío cambia;
- archivos `.env`, planillas y claves quedan fuera de imágenes.

### Known limitations

- CI todavía no fue observado como verde;
- falta `package-lock.json`;
- la imagen frontend usa `npm install` hasta disponer del lockfile;
- RBAC persistente no implementado;
- no existe acción para resolver revisiones de duplicados;
- no existe retry explícito de importaciones fallidas;
- el frontend todavía no muestra `excludedRows` como control independiente;
- no existen campañas, Gmail, Sheets o infraestructura cloud;
- el stack Compose es exclusivamente local y no productivo.
