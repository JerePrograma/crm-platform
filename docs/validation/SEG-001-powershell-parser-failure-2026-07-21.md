# Evidencia SEG-001 — fallo de parser PowerShell

## Fecha

2026-07-21

## Entorno aportado

```text
Windows PowerShell
Checkout: C:\laburo\crm-platform
Rama: main
Remoto: origin/main
```

## Actualización del checkout

Comandos ejecutados:

```powershell
git switch main
git fetch origin
git pull --ff-only
```

Resultado:

```text
main actualizado por fast-forward desde dfb8383 a 99c340d
42 archivos modificados
5919 inserciones
1997 eliminaciones
```

Estado: `PASS`.

## Anomalía de mvnw.cmd

Antes y después del pull:

```text
M mvnw.cmd
```

Controles ejecutados:

```powershell
git diff --ignore-space-at-eol -- mvnw.cmd
git restore -- mvnw.cmd
git status --short
```

La comparación ignorando finales de línea no mostró diferencias, pero `git status` siguió mostrando el archivo modificado.

Interpretación: inconsistencia de normalización entre el blob histórico y la regla posterior:

```gitattributes
*.cmd text eol=crlf
```

Corrección aplicada en `main`: `mvnw.cmd` fue reescrito sin cambios funcionales para normalizar el blob versionado.

## Comando de validación intentado

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

El comando se intentó dos veces y falló de la misma forma antes de ejecutar el cuerpo del script.

## Error reproducido

```text
scripts\validate-seg001.ps1:21
La referencia de variable no es válida.
El carácter ':' no va seguido de un carácter de nombre de variable válido.
FullyQualifiedErrorId: InvalidVariableReferenceWithDrive
```

Código causante:

```powershell
throw "Command failed with exit code $LASTEXITCODE: $Command ..."
```

PowerShell interpretó `$LASTEXITCODE:` como una referencia de variable con sintaxis de ámbito o unidad.

Estado: `EXECUTED_FAIL — POWERSHELL_PARSE_ERROR`.

## Alcance real de la ejecución

La falla ocurrió durante el parseo. Por lo tanto:

```text
preflight: NOT_RUN
Compose config: NOT_RUN
frontend build: NOT_RUN
backend build: NOT_RUN
stack: NOT_RUN
health checks: NOT_RUN
smoke: NOT_RUN
Maven verify: NOT_RUN
Testcontainers: NOT_RUN
package-lock: NOT_RUN
npm ci: NOT_RUN
repository safety: NOT_RUN
```

No se generó evidencia JSON o transcript porque PowerShell no llegó a ejecutar `Start-Transcript`.

## Correcciones aplicadas

El mensaje de error se reemplazó por formato explícito:

```powershell
throw ('Command failed with exit code {0}: {1} {2}' -f $LASTEXITCODE, $Command, ($Arguments -join ' '))
```

Archivos corregidos:

```text
scripts/validate-seg001.ps1
scripts/validate-docker-stack.ps1
scripts/verify-backend-container.ps1
```

Los tres contenían el mismo patrón ambiguo.

También se normalizó:

```text
mvnw.cmd
```

## Regresión añadida

CI ahora rechaza explícitamente el patrón:

```text
$LASTEXITCODE:
```

en cualquier `scripts/*.ps1`, además del parser PowerShell ya existente.

## Seguridad

La falla ocurrió antes de Docker y no produjo comunicaciones ni cambios de datos.

Las guardas permanecen:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

## Próxima ejecución

Actualizar `main`, confirmar que `mvnw.cmd` quede limpio y ejecutar nuevamente el validador integral.

El siguiente resultado desconocido comenzará recién después del parser: árbol limpio, `.env`, Docker daemon y preflight.
