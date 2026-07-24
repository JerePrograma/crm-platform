# Misión de continuidad para la próxima sesión

Actualizado: 2026-07-24

## Objetivo inmediato

Validar funcionalmente el hardening remoto del parser `.Config.Env` ya publicado en `main`. No comenzar otra mejora funcional antes de cerrar este gate.

## Estado de entrada

```text
rama: main
parser PowerShell: IMPLEMENTED_NOT_RUN
parser Node: EXECUTED_PASS en self-test aislado
productionProfileSmoke: IMPLEMENTED_NOT_RUN
finalTreeClean: IMPLEMENTED_NOT_RUN
validación integral 1: IMPLEMENTED_NOT_RUN
validación integral 2: IMPLEMENTED_NOT_RUN
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

Ejecutar primero:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
node scripts/test-container-env-assertions.js
```

Confirmar en PowerShell 5.1:

- guardas completas → PASS;
- falta `SENDING_ENABLED=false` → FAIL;
- `SENDING_ENABLED=true` → FAIL;
- JSON inválido → FAIL;
- líneas vacías → PASS;
- el resultado de `ConvertFrom-Json` se maneja como colección real;
- no se imprime el entorno completo.

## Validación integral

Ejecutar dos veces, sin modificar el commit entre corridas:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Exigir en ambos JSON:

```text
status=FUNCTIONAL_PASS
productionProfileSmoke=FUNCTIONAL_PASS
finalTreeClean=FUNCTIONAL_PASS
```

Revisar además:

- parser y self-tests;
- backend format/unit/integration/architecture/security;
- frontend install/typecheck/unit/build;
- Compose y health;
- dependency scans;
- migraciones;
- outbox/inbound;
- Playwright;
- siete guardas exactas;
- cero estados enviados;
- backup/restore;
- repository safety;
- tree limpio.

No convertir bloqueos externos de red en PASS.

## Después de validar

1. ejecutar `git diff --check`;
2. ejecutar repository safety;
3. revisar `git status --short`;
4. comprobar que no hay `.env`, XLSX, ZIP, logs, `validation-output/` ni secretos versionados;
5. comprobar CI del SHA exacto;
6. actualizar evidencia y documentación solo con resultados ejecutados.

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
