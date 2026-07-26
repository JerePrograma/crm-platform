# Misión de continuidad para la próxima sesión

Actualizado: 2026-07-24

## Objetivo inmediato

Ejecutar `UX-003`: demostrar si la automatización UX remota histórica está obsoleta y eliminar únicamente los archivos sin consumidores.

## Estado de entrada

```text
rama: main
VAL-001: FUNCTIONAL_PASS
VAL-002: FUNCTIONAL_PASS_FOCUSED
ProductionFrontendPort preflight: WINDOWS_AND_UNIX
demo remota 18080: PRESERVED
producción: NOT_DEPLOYED
envíos reales: DISABLED
siguiente gate: UX-003
```

## Lectura obligatoria

- `AGENTS.md`;
- `docs/continuity/README.md`;
- `docs/status.md`;
- `docs/next-step.md`;
- `docs/backlog.md`;
- `docs/estado-integral-y-roadmap.md`;
- `docs/validation/COMPLETE-CRM-matrix.md`;
- `docs/validation/remote-main-hardening-2026-07-24.md`;
- los cuatro scripts de aserción/test;
- los cuatro validadores modificados;
- Compose y Dockerfiles afectados.

## Inicio seguro

```powershell
git status --short
git branch --show-current
git remote -v
git fetch origin
git switch main
git pull --ff-only origin main
git rev-parse HEAD
git rev-parse origin/main
```

Detenerse ante cambios locales no relacionados, divergencia, remoto inesperado o falta de fast-forward.

## Validación focalizada

Para `UX-003`:

1. listar archivos `.github/remote-ux-trigger`, workflows y scripts `remote-ux-*`;
2. buscar consumidores por ruta, nombre, comando, variable y artefacto;
3. revisar triggers y dependencias de workflows;
4. detenerse si existe un consumidor canónico;
5. eliminar solo automatización demostrablemente obsoleta;
6. ejecutar repository safety, sintaxis afectada y `git diff --check`.

## Validación integral

No ejecutar suites backend/frontend/Docker salvo que la inspección revele que los archivos remotos participan en una ruta funcional.

No mezclar `UX-003` con:

- métricas tenant-wide;
- paginación;
- drawer móvil;
- foco;
- multibrowser;
- modularización.

## Después de validar

1. ejecutar `git diff --check`;
2. ejecutar repository safety;
3. revisar el diff completo;
4. confirmar que solo existen cambios de `VAL-002`;
5. validar el nuevo SHA;
6. commit y push fast-forward a `main`;
7. actualizar evidencia con resultados realmente ejecutados.

La documentación actual ya registra el cierre de `VAL-001`.

## Candidato histórico

No intentar integrar `9e058d7044415b80af554ab8ae4fe3170585b1c9` desde la documentación.

Solo puede retomarse si aparecen:

- los cuatro patches;
- un manifiesto de hashes verificable;
- o commits/ramas remotos que materialicen el contenido.

En ese caso:

1. verificar SHA-256;
2. usar clon temporal del `main` actual;
3. ejecutar `git apply --check` en orden;
4. revisar cada diff;
5. preservar continuidad;
6. validar nuevamente dos veces.

## Condiciones de parada

Detenerse sin nuevos commits ni push cuando:

- falle el self-test PowerShell;
- falle `productionProfileSmoke`;
- falle `finalTreeClean`;
- una corrida integral no termine `FUNCTIONAL_PASS`;
- el segundo recorrido use otro commit;
- aparezcan secretos o datos reales;
- se requiera force push;
- el remoto avance y no pueda reconciliarse por fast-forward;
- CI falle y el fallo no pueda resolverse dentro del alcance.

## Seguridad

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

Producción continúa no desplegada y no se autorizan envíos reales.
