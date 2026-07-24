# Misión de continuidad para la próxima sesión

Actualizado: 2026-07-24

## Objetivo inmediato

Publicar el cierre documental del hardening ya validado y continuar con `VAL-002`, que debe detectar tempranamente colisiones de `ProductionFrontendPort`.

## Estado de entrada

```text
rama: main
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
parser PowerShell: EXECUTED_PASS
parser Node: EXECUTED_PASS
productionProfileSmoke: FUNCTIONAL_PASS
effectiveSendingBlockade: FUNCTIONAL_PASS
zeroSent: FUNCTIONAL_PASS
finalTreeClean: FUNCTIONAL_PASS
validación integral 1: FUNCTIONAL_PASS
validación integral 2: FUNCTIONAL_PASS
CI: NO_CHECKS_REPORTED
demo remota: ACTIVE_ON_127.0.0.1_18080
candidato histórico 9e058d...: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
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

No repetir las validaciones ya cerradas para una actualización exclusivamente documental.

Evidencia vigente:

```text
complete-crm-20260724-201944.json
SHA-256 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527

complete-crm-20260724-202955.json
SHA-256 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

Ambas corridas corresponden a `main` y al commit exacto `0448c0e060311c284f4e4be4612982818a8480c4`.

## Validación integral

Para `VAL-002`:

1. inspeccionar rutas, firmas y consumidores reales;
2. agregar la comprobación de `ProductionFrontendPort` al preflight;
3. añadir pruebas focalizadas;
4. ejecutar primero esas pruebas;
5. ejecutar validaciones generales solo si el cambio funcional lo requiere;
6. detenerse ante cambios locales, conflictos, secretos, fallos o necesidad de force push.

No detener ni modificar la demo remota que ocupa `127.0.0.1:18080`.

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
