# Operación local, validación y despliegue

Actualizado: 2026-07-24

## Requisitos

- Git;
- Docker Desktop o Docker Engine con Compose;
- Java 21 para ejecución separada;
- Node.js 22 y npm;
- PowerShell 5.1+ en Windows o Bash en Unix.

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

El validador integral exige ahora la rama `main`. Detenerse ante cambios no relacionados, conflicto, divergencia o remoto inesperado.

## Self-test del parser de contenedores

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-powershell-syntax.ps1
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
```

Unix:

```bash
node scripts/test-container-env-assertions.js
```

Los self-tests verifican:

- las siete guardas presentes;
- una guarda ausente;
- un valor inseguro alternativo;
- JSON inválido;
- líneas vacías alrededor del JSON.

## Validadores canónicos

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
bash scripts/validate-complete-crm.sh
```

O:

```bash
make validate-complete-crm
```

El cierre exige dos corridas limpias consecutivas sobre el mismo commit.

## Parseo de `.Config.Env`

Los validadores capturan:

```text
docker inspect <container> --format {{json .Config.Env}}
```

Contrato:

1. entrada no vacía;
2. JSON válido;
3. raíz de tipo array;
4. entradas convertidas a string sin alterar nombre ni valor;
5. membresía exacta;
6. fallo si falta una guarda;
7. no imprimir el entorno completo.

No usar regex, `-match` o `grep` sobre el transcript completo para decidir el gate de seguridad.

## Perfil productivo local

El perfil productivo local es un smoke técnico, no un despliegue. Debe comprobar:

- PostgreSQL, backend y frontend saludables;
- filesystem y usuarios endurecidos;
- PostgreSQL sin puerto público;
- siete variables fail-closed exactas;
- cero estados `SENT`, `DELIVERED` o `READ`;
- cleanup exclusivo de sus recursos sintéticos.

```text
PRODUCTION_PROFILE=EXECUTED_PASS_LOCALLY
```

no significa producción desplegada.

## Guardas

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Evidencia válida

Debe incluir:

- commit exacto;
- rama `main`;
- códigos de salida;
- JSON de resumen;
- fases ejecutadas y no ejecutadas;
- `productionProfileSmoke`;
- `finalTreeClean`;
- puertos e IDs de imágenes cuando corresponda;
- cleanup;
- estado de envíos;
- repository safety;
- `git diff --check`;
- estado de CI.

`validation-output/` y los ZIP son evidencia local y no deben versionarse.

## Reutilización

Solo omitir una fase cuando:

1. existe evidencia estructurada íntegra;
2. el commit/tree cubierto es identificable;
3. no cambiaron archivos ni dependencias de la fase;
4. el entorno es equivalente;
5. la omisión queda registrada.

Los scripts de validación cambiaron en la consolidación del 24 de julio; por eso `productionProfileSmoke`, `finalTreeClean` y el cierre integral deben volver a ejecutarse.

## Despliegue

Producción continúa:

```text
NOT_AUTHORIZED / NOT_DEPLOYED
```

Antes de cualquier despliegue se requieren infraestructura, TLS, secretos administrados, backup/restore, rollback, privacidad, observabilidad, carga, accesibilidad, providers revisados, CI verde y autorización explícita.

No desplegar como consecuencia automática de una validación local.
