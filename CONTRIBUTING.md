# Contribuir

## Flujo

1. leer `AGENTS.md`, `docs/status.md`, `docs/next-step.md` y `docs/backlog.md`;
2. trabajar en una rama temática;
3. mantener un único objetivo verificable por segmento;
4. ejecutar backend, frontend, Compose y Docker cuando corresponda;
5. revisar que no existan secretos ni datos operativos;
6. actualizar documentación y changelog;
7. abrir PR solo con autorización explícita.

## Commits

Prefijos:

- `feat:` funcionalidad;
- `fix:` corrección;
- `refactor:` cambio interno;
- `test:` pruebas;
- `docs:` documentación;
- `build:` toolchain;
- `ci:` automatización;
- `security:` hardening;
- `chore:` mantenimiento.

Cada commit debe dejar el repositorio comprensible. Evitar mezclar migraciones, UI y refactors no relacionados en un mismo commit cuando puedan revisarse por separado.

## Calidad

Antes de considerar un cambio listo:

```bash
sh ./mvnw -B -f backend/pom.xml verify
(cd frontend && npm install && npm run build)
docker compose config
docker build -t gestudio-crm:review .
```

No desactivar un test para obtener verde. Corregir la causa o documentar el bloqueo.

## Migraciones

- nunca editar una migración aplicada;
- crear una nueva versión Flyway;
- mantener nombres descriptivos;
- probar base vacía y upgrade con datos representativos;
- no usar `ddl-auto=update`;
- incluir índices, restricciones y rollback operativo documentado.

## Seguridad

Nunca incluir:

- `.env` real;
- tokens;
- contraseñas;
- claves privadas;
- datos de prospectos;
- archivos Gmail/Sheets exportados;
- credenciales Google;
- secretos Terraform.

Cualquier cambio de envío debe mantener fail-closed y requiere autorización comercial explícita.

## API

- rutas versionadas bajo `/api/v1`;
- DTO separados de entidades;
- validación de entrada;
- RFC 7807;
- paginación para colecciones;
- no exponer secretos ni stack traces;
- documentar en OpenAPI.

## Frontend

- TypeScript estricto;
- no guardar contraseñas o tokens en almacenamiento persistente sin una decisión de seguridad;
- accesibilidad básica de formularios y navegación;
- errores visibles y accionables;
- diseño orientado a operación, no decoración.

## Cierre de segmento

Actualizar obligatoriamente:

- `docs/status.md`;
- `docs/backlog.md`;
- `docs/next-step.md`;
- `docs/segments/SEG-XXX.md`;
- `CHANGELOG.md`.
