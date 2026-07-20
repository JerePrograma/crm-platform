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
5. `docs/validation/SEG-001-complete-validation-automation-2026-07-20.md` para tareas de cierre;
6. ADR y documentación del módulo afectado;
7. `docs/containerized-quickstart.md` o `docs/local-development-and-usage.md` para arranque y operación.

Cuando el usuario indique `continuar`, ejecutar el segmento descrito en `docs/next-step.md` y actualizar todas las fuentes canónicas al finalizar.

## Validación SEG-001

- El comando canónico de cierre local en Windows es `scripts/validate-seg001.ps1`.
- Un build cacheado no es evidencia de compilación limpia.
- Un script implementado o parseado no equivale a un PASS funcional.
- El validador integral debe comenzar desde `main` con archivos rastreados limpios.
- El único cambio esperado después del recorrido es `frontend/package-lock.json`.
- El lockfile debe revisarse, versionarse y volver a validarse desde un árbol limpio.
- `scripts/check-repository-safety.ps1` o `.sh` debe pasar antes del cierre.
- `validation-output/` es evidencia local, no fuente canónica y no debe versionarse.
- Los transcripts deben revisarse antes de compartirse.
- Maven/Testcontainers contenedorizado monta el socket Docker; ejecutarlo únicamente sobre código propio y revisado.
- No marcar SEG-001 `COMPLETE` sin actualizar la matriz con evidencia ejecutada.

## Invariantes

- Los envíos reales permanecen deshabilitados hasta autorización explícita.
- No versionar secretos, tokens, claves privadas ni datos operativos de contactos.
- PostgreSQL es la fuente de verdad.
- No afirmar que una validación pasó sin haberla ejecutado.
- No abrir PR, desplegar producción, reescribir historia ni fusionar ramas sin autorización explícita.
- Una autorización puntual para consolidar no habilita futuras fusiones automáticas.
- Al cerrar un segmento, actualizar `docs/status.md`, `docs/backlog.md`, `docs/next-step.md`, `docs/segments/`, `docs/validation/`, `CHANGELOG.md`, README y documentación operativa afectada.
- Toda migración de base debe ser aditiva o incluir estrategia de compatibilidad y rollback documentada.
- Todo fallo corregido debe incorporar una prueba de regresión cuando sea técnicamente viable.
- No usar `docker compose down -v` salvo intención explícita de destruir la base local.
- No incorporar el XLSX real a Git, CI, imágenes o fixtures.

## Cierre de cada respuesta

- `## Estado actual`
- `## Cambios realizados`
- `## Validaciones ejecutadas`
- `## Riesgos pendientes`
- `## Próximo paso`
- `## Comandos Git`
