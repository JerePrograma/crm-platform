# Consolidación remota de `main` y hardening del parser — 2026-07-24

## Alcance

Esta evidencia documenta una intervención realizada exclusivamente sobre el repositorio remoto `JerePrograma/crm-platform`, sin checkout Windows, sin patches locales y sin acceso a Docker Desktop.

El objetivo ejecutable fue reconciliar el estado real de `main`, corregir el defecto versionado equivalente al falso negativo de `.Config.Env` y alinear los validadores con la rama canónica.

## Estado remoto verificado antes del cambio

```text
rama: main
HEAD: f25051884b7aadd5837286dedd9ae0eee899cb5a
mensaje: docs: add canonical continuity handoff
comparación contra 83e181ce614f145bbfe141cc7603c3042569be51: ahead 1, behind 0
```

El único cambio entre `83e181c` y ese `HEAD` era documental: `AGENTS.md` y `docs/continuity/`.

No se localizaron en GitHub commits, ramas o pull requests accesibles que materializaran el tree histórico `9e058d7044415b80af554ab8ae4fe3170585b1c9`. Ese identificador permanece como referencia de un candidato local no publicado, no como objeto remoto integrable.

## Causa raíz confirmada en código versionado

Los validadores PowerShell capturaban:

```text
docker inspect <container> --format {{json .Config.Env}}
```

pero comprobaban las variables mediante regex sobre el string JSON completo. Además, los validadores integrales seguían exigiendo la rama histórica `feat/complete-crm-platform`, en contradicción con `AGENTS.md`, que establece `main` como única fuente de verdad.

El reanudador externo que produjo el mensaje en español no estaba versionado, pero el mismo patrón ambiguo sí existía en los validadores canónicos.

## Cambios implementados

### Parser y aserciones exactas

- `scripts/container-env-assertions.ps1`
  - rechaza entrada vacía;
  - exige raíz JSON de tipo array;
  - usa `ConvertFrom-Json` con error fail-closed;
  - convierte cada entrada de forma segura a string;
  - usa membresía exacta mediante `-contains`/`-notcontains`;
  - informa solo las entradas requeridas faltantes;
  - no imprime el entorno completo.

- `scripts/assert-container-env.js`
  - ofrece el mismo contrato para Unix mediante `JSON.parse`, `Array.isArray` e `includes` exacto.

### Cobertura de regresión

- `scripts/test-container-env-assertions.ps1`;
- `scripts/test-container-env-assertions.js`.

Casos cubiertos:

1. siete guardas presentes → PASS;
2. falta `SENDING_ENABLED=false` → FAIL;
3. aparece `SENDING_ENABLED=true` sin la guarda segura → FAIL;
4. JSON inválido → FAIL;
5. líneas vacías alrededor del JSON → PASS.

### Validadores alineados

- `scripts/validate-complete-crm.ps1`;
- `scripts/validate-complete-crm.sh`;
- `scripts/verify-production-profile.ps1`;
- `scripts/verify-production-profile.sh`.

Cambios:

- rama requerida: `main`;
- self-tests incorporados al gate de scripts;
- validación exacta de las siete guardas;
- proveedores `NOOP` y `DEEPLINK_ONLY` incluidos también en el smoke productivo;
- no se debilitó ninguna guarda ni se añadió capacidad de envío.

## Commits funcionales remotos

```text
cb93948d783d4b5de9022cd44ff798cce53993f6  test: add exact container environment assertions
3032b02c073d8643952822d5c8e39f9b0d87abc6  test: cover PowerShell environment JSON parsing
2f3124f97d1545abb5fc07a6e16b4cb9482ecf0a  fix: preserve exact environment array membership
1760c3756c854b7c1f4597ee130118d7d015003d  test: add cross-platform environment assertion helper
df36e0d8c6931dbac18330d99571297f501c61aa  test: cover cross-platform environment assertions
6e106ae8b2a26cd7df990f27a7dd0110cc6e7e40  fix: harden complete CRM environment validation on main
051ee9424326b013c67cd445aa45254cde6d348b  fix: parse production container environment exactly
d5bc869646ebd0300720a3e2d4f6423d3ad6c0ac  fix: align Unix validation with main and exact env checks
5b5fbf63c4f3ef15a50f0407710ae56b26bbe787  fix: require exact production environment entries on Unix
```

La granularidad responde a la limitación de la Contents API remota: cada alta o actualización de archivo genera un commit fast-forward independiente.

## Validaciones ejecutadas

| Validación | Estado | Evidencia |
|---|---|---|
| inspección del `HEAD` remoto | EXECUTED_PASS | `main` resolvió a `f250518...` antes del cambio |
| comparación `83e181c...main` | EXECUTED_PASS | ahead 1, behind 0; solo documentación de continuidad |
| búsqueda de candidato remoto | EXECUTED_PASS | no se encontraron commits/ramas recuperables del tree histórico |
| self-test Node del parser | EXECUTED_PASS | ejecutado con Node `v22.16.0`; todos los casos pasaron |
| self-test PowerShell 5.1 | IMPLEMENTED_NOT_RUN | el entorno remoto de esta sesión no ofrece PowerShell |
| sintaxis PowerShell | IMPLEMENTED_NOT_RUN | pendiente de host PowerShell/CI |
| sintaxis Bash | IMPLEMENTED_NOT_RUN | pendiente de corrida sobre los archivos remotos exactos |
| `productionProfileSmoke` | IMPLEMENTED_NOT_RUN | requiere Docker |
| `finalTreeClean` | IMPLEMENTED_NOT_RUN | requiere checkout Git limpio |
| validador integral, corrida 1 | IMPLEMENTED_NOT_RUN | requiere checkout y toolchain completos |
| validador integral, corrida 2 | IMPLEMENTED_NOT_RUN | requiere el mismo commit y toolchain completos |
| `git diff --check` | IMPLEMENTED_NOT_RUN | no existe checkout local en esta intervención |
| repository safety | IMPLEMENTED_NOT_RUN | no existe checkout local en esta intervención |

No se utiliza `FUNCTIONAL_PASS` para fases no ejecutadas.

## Estado de seguridad

Permanecen obligatorias:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

## Límites de esta consolidación

- el candidato funcional local post-hardening no fue reconstruido ni declarado integrado;
- no se incorporaron patches no verificables;
- no se ejecutó Docker;
- no se desplegó producción;
- no se conectaron proveedores;
- no se realizaron envíos reales;
- no se incorporó el XLSX real;
- no se creó rama, PR, fork ni worktree;
- no se usó force push ni se reescribió historia.

## Próximo gate obligatorio

Sobre el `HEAD` remoto final de esta consolidación, ejecutar desde un checkout limpio de `main`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Las dos corridas integrales deben terminar `FUNCTIONAL_PASS` sobre el mismo commit. Después debe comprobarse el estado de GitHub Actions del SHA exacto.
