# Misión de continuidad para la próxima sesión

Actualizado: 2026-07-24

## Objetivo inmediato

Ejecutar `UX-004`: corregir las métricas tenant-wide del dashboard sin depender de la página de prospectos cargada.

## Estado de entrada

```text
rama: main
VAL-001: FUNCTIONAL_PASS
VAL-002: COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX-003: COMPLETE_WITH_FOCUSED_VALIDATION
remote UX automation: REMOVED
producción: NOT_DEPLOYED
envíos reales: DISABLED
siguiente gate: UX-004
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

Para `UX-004`:

1. localizar el componente real del dashboard;
2. identificar APIs, reportes y selectores usados por sus métricas;
3. reproducir el problema con más de una página;
4. localizar todos los consumidores del contrato que cambie;
5. preservar RBAC y tenant isolation;
6. añadir pruebas backend y frontend;
7. evitar cargar todas las páginas.

## Validación integral

Ejecutar primero pruebas focalizadas del endpoint/reporte y del consumidor frontend.

Después ejecutar las validaciones generales reales del repositorio que correspondan al alcance:

- formato y tipos;
- pruebas backend afectadas;
- pruebas frontend afectadas;
- build;
- repository safety;
- `git diff --check`.

No mezclar importaciones, outbox/inbound, multibrowser ni modularización.

## Después de validar

1. revisar el diff completo;
2. confirmar aislamiento tenant y permisos;
3. comprobar que las métricas no dependen de la página visible;
4. actualizar status, backlog, next-step, continuidad, matriz y evidencia;
5. commit lógico único;
6. push fast-forward a `main`;
7. verificar CI del SHA exacto.

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
