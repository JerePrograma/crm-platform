# Validación de experiencia del operador — 2026-07-23

## Alcance validado

- etiquetas visibles centralizadas en español;
- navegación, formularios, tablas, estados vacíos, avisos y datos técnicos humanizados;
- diálogos accesibles en reemplazo de `window.prompt` y `window.confirm`;
- importaciones con vista previa, filtros, paginación y resolución guiada de duplicados;
- creación independiente desde duplicados con conservación controlada de canales importados;
- experiencia responsive y foco visible;
- bloqueos de envío, simulación y ausencia de red real preservados.

## Validaciones ejecutadas

- `npm ci --no-audit --no-fund`;
- `npm run typecheck`;
- `npm run test:unit`;
- `npm run build`;
- `./mvnw -B -f backend/pom.xml verify`;
- Docker Compose con perfil `app` y configuración fail-closed;
- `npm run test:e2e` con Playwright Chromium;
- `bash scripts/check-repository-safety.sh`;
- `git diff --check`;
- comprobación de ausencia de diálogos nativos en `frontend/src`.

## Seguridad

Durante la validación se mantuvieron `SENDING_ENABLED=false`, `SENDING_DRY_RUN=true`, límite diario cero, protección de emergencia activa y red real deshabilitada.

Commit funcional base: `96eaf921bbc5e3ef70854a3299c9b03f97a4b70e`.
Ejecución final: `30034176306` sobre `9b1d6483864291a35cce3e3ad9a2932268353fdd`.
