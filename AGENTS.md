# AGENTS.md

## Rama canónica

- `main` es la única fuente de verdad del repositorio.
- Antes de trabajar, ejecutar `git switch main` y `git pull --ff-only` cuando exista checkout local.
- Las ramas temáticas son transitorias y no deben contradecir la documentación canónica.

## Continuidad obligatoria

Antes de modificar el repositorio, leer en este orden:

1. `docs/status.md`;
2. `docs/next-step.md`;
3. `docs/backlog.md`;
4. `docs/validation/SEG-001.md` mientras SEG-001 esté activo;
5. ADR y documentación del módulo afectado;
6. `docs/local-development-and-usage.md` para tareas de arranque u operación.

Cuando el usuario indique `continuar`, ejecutar el segmento descrito en `docs/next-step.md` y actualizar todas las fuentes canónicas al finalizar.

## Invariantes

- Los envíos reales permanecen deshabilitados hasta autorización explícita.
- No versionar secretos, tokens, claves privadas ni datos operativos de contactos.
- PostgreSQL es la fuente de verdad.
- No afirmar que una validación pasó sin haberla ejecutado.
- No abrir PR, desplegar producción, reescribir historia ni fusionar ramas sin autorización explícita.
- Una autorización puntual para consolidar no habilita futuras fusiones automáticas.
- Al cerrar un segmento, actualizar `docs/status.md`, `docs/backlog.md`, `docs/next-step.md`, `docs/validation/`, `CHANGELOG.md` y documentación operativa afectada.
- Toda migración de base debe ser aditiva o incluir estrategia de compatibilidad y rollback documentada.
- Todo fallo corregido debe incorporar una prueba de regresión cuando sea técnicamente viable.

## Cierre de cada respuesta

- `## Estado actual`
- `## Cambios realizados`
- `## Validaciones ejecutadas`
- `## Riesgos pendientes`
- `## Próximo paso`
- `## Comandos Git`
