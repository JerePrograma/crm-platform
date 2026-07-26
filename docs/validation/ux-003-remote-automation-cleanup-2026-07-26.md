# UX-003 — Limpieza de automatización UX remota obsoleta

Fecha: 2026-07-26

## Base

`	ext
branch: main
parent commit: 4bc128cd4db65199c8b21edb96c342dfb08877f6
historical workflow commit: 9bb8a85dee5cebfc25e3c99c2c157de46060a7c9
historical base guard: a4d975d5a7a041e492894c78583381a466482459
current HEAD grandparent: 5c56cf827d0803e804d9b8b50031ebf355a96d4b
`

## Inventario

`	ext
.github/remote-ux-trigger
.github/workflows/remote-ux-overhaul.yml
scripts/remote-ux-preflight.py
scripts/remote-ux-overhaul.py
scripts/remote-ux-postfix.py
`

## Causa de obsolescencia

- el workflow solo se activaba al modificar .github/remote-ux-trigger;
- el trigger correspondía a una ejecución fechada el 23 de julio de 2026;
- el workflow exigía que HEAD^^ coincidiera con $HistoricalBaseCommit;
- la historia actual ya no cumple esa guarda;
- el propio workflow declaraba la eliminación de los cinco archivos antes de crear su commit funcional;
- los scripts Python eran transformadores de una sola ejecución, no herramientas operativas generales.

## Búsqueda de consumidores

`	ext
consumidores operativos fuera del conjunto: 0
dependencias desde otros workflows: 0
referencias documentales localizadas: 9
`

Referencias documentales observadas antes del cierre:

`	ext
docs/backlog.md:124:- [ ] verificar que `.github/remote-ux-trigger`, el workflow remoto y los scripts `remote-ux-*` siguen presentes;
docs/continuity/continuation-mission.md:55:1. listar archivos `.github/remote-ux-trigger`, workflows y scripts `remote-ux-*`;
docs/estado-integral-y-roadmap.md:372:2. **Eliminar automatización remota obsoleta.** Permanecen `.github/remote-ux-trigger`, `.github/workflows/remote-ux-overhaul.yml`, `scripts/remote-ux-preflight.py`, `scripts/remote-ux-overhaul.py` y `scripts/remote-ux-postfix.py`. La guarda fija de historial los vuelve inertes, pero no deben quedar como deuda permanente.
docs/next-step.md:49:1. inspeccionar `.github/remote-ux-trigger`;
docs/status.md:178:1. verificar si `.github/remote-ux-trigger`, el workflow remoto y los scripts `remote-ux-*` continúan presentes;
`

Las referencias documentales históricas pueden conservarse cuando describen la ejecución pasada. Las fuentes canónicas fueron actualizadas para registrar el cierre.

## Cambios

Se eliminaron exclusivamente los cinco archivos del inventario.

No se modificaron:

- backend;
- frontend;
- modelos o contratos;
- base de datos;
- migraciones;
- dependencias;
- Dockerfiles o Compose;
- políticas de envío;
- demo remota.

## Validaciones

`	ext
inventario exacto: EXECUTED_PASS
trigger histórico: EXECUTED_PASS
workflow histórico: EXECUTED_PASS
guarda obsoleta: EXECUTED_PASS
consumidores operativos externos: 0
otros workflows dependientes: 0
archivos eliminados: 5
repository safety: EXECUTED_PASS
git diff --check: EXECUTED_PASS
full suites repeated: false
`

## Justificación de pruebas

No se ejecutan Maven, npm, builds Docker, migraciones ni Playwright porque UX-003 elimina automatización auxiliar no consumida y no cambia código funcional ni configuración de runtime.

## Límites

`	ext
producción: NOT_AUTHORIZED_NOT_DEPLOYED
envíos reales: DISABLED_BY_POLICY
providers reales: IMPLEMENTED_NOT_CONNECTED
XLSX real: OUTSIDE_GIT_CI_IMAGES
CI del commit final: PENDING_POST_PUSH_VERIFICATION
`

## Próximo paso

UX-004 — métricas tenant-wide del dashboard.
