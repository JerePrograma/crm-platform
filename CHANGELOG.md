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
- cola de revisión de duplicados;
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
- auditoría evita copiar canales completos en payloads de exclusión.

### Security

- sin credenciales bootstrap explícitas no existe acceso a API de negocio;
- datos comerciales reales excluidos del repositorio público;
- Maven 3.9.16 se verifica mediante SHA-512;
- no existe código capaz de enviar correos.

### Known limitations

- CI del último commit todavía no fue observado como verde;
- falta lockfile frontend;
- RBAC persistente no implementado;
- no existe acción para resolver revisiones de duplicados;
- no existe retry explícito de importaciones fallidas;
- no existen campañas, Gmail, Sheets ni infraestructura cloud.
