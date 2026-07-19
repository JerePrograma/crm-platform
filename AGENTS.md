# AGENTS.md

## Continuidad obligatoria

Antes de modificar el repositorio, leer en este orden:

1. `docs/status.md`
2. `docs/next-step.md`
3. `docs/backlog.md`
4. ADR y documentación del módulo afectado.

Cuando el usuario indique `continuar`, ejecutar el segmento descrito en `docs/next-step.md`.

## Invariantes

- Los envíos reales permanecen deshabilitados hasta autorización explícita.
- No versionar secretos, tokens, claves privadas ni datos operativos de contactos.
- PostgreSQL es la fuente de verdad.
- No afirmar que una validación pasó sin haberla ejecutado.
- No abrir PR, desplegar producción ni fusionar a `main` sin autorización explícita.
- Al cerrar un segmento, actualizar `docs/status.md`, `docs/backlog.md`, `docs/next-step.md` y `CHANGELOG.md`.

## Cierre de cada respuesta

- `## Estado actual`
- `## Cambios realizados`
- `## Validaciones ejecutadas`
- `## Riesgos pendientes`
- `## Próximo paso`
- `## Comandos Git`
