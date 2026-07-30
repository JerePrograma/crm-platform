# Validación SEG-001 Gmail y campañas LIVE — 2026-07-30

Estado: `PRE_PUBLICATION_VALIDATION`

## Recuperación Git

```text
HEAD inicial: 12421c53375deabebe8f48f17af3ae95af95893b
origin/main observado: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
merge-base: 12421c53375deabebe8f48f17af3ae95af95893b
local ancestor of remote: exit 0
fast-forward final: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
working tree preservado: 23 tracked + 45 untracked
integridad posterior: 68/68 SHA-256 idénticos
```

Se creó un snapshot externo verificable con archivos, patches binarios,
manifiesto y hashes. Se retiró exclusivamente
`refs/codex/turn-diffs/captures/1785355393050/0a385f69-64b6-4589-ad54-1e6abdddb027/base`.
`git fsck --full --no-reflogs` devolvió exit 0; solo informó un commit remoto
dangling no alcanzable.

## Implementación validada

- V14 aditiva desde V1, V11 y V13;
- cuentas Gmail tenant-scoped y una default por tenant;
- OAuth Authorization Code offline, state single-use y replay protection;
- AES-256-GCM, nonce único, AAD y key version;
- refresh/reconnect/revoke e `invalid_grant → REAUTH_REQUIRED`;
- Gmail `users.messages.send`, MIME UTF-8 y one-click unsubscribe;
- campaña LIVE aprobada/confirmada, audiencia congelada y revalidación pre-send;
- un destinatario por mensaje/outbox, lease, retry, dead-letter y ambiguous;
- límites, ventana, días, intervalo, concurrencia 1 y ledger local;
- exclusión inmediata y cancelación de mensajes/seguimientos pendientes;
- RBAC, tenant isolation, CSRF, redacción y Problem Details;
- UI conjunta de cuentas, campaña, progreso, resultados y errores;
- NOOP y simulación conservados; red/envíos reales deshabilitados por defecto.

## Comandos ejecutados

| Comando | Resultado |
|---|---|
| `mvn -f backend/pom.xml clean verify` con Java 21 | `EXECUTED_PASS`, 112/112, Spotless/ArchUnit/V1→V14 |
| `npm ci --no-audit --no-fund` | `EXECUTED_PASS` |
| `npm run typecheck` | `EXECUTED_PASS` |
| `npm run test:unit` | `EXECUTED_PASS`, 13/13 |
| `npm run build` | `EXECUTED_PASS` |
| `node --test scripts/fake-google-server.test.mjs` | `EXECUTED_PASS`, 2/2 |
| `scripts/validate-gmail-live-fake.ps1` | `EXECUTED_PASS`, 10 fases |
| Playwright `gmail-no-oauth.spec.ts` | `EXECUTED_PASS` |
| Playwright `gmail-live-campaign.spec.ts` | `EXECUTED_PASS`, 29.2 s |
| `scripts/verify-migrations.ps1` | `EXECUTED_PASS`, vacío/V11/V13→V14 |
| `scripts/verify-backup-restore.ps1` | `EXECUTED_PASS`, V14 y probe sintético |
| `scripts/verify-production-profile.ps1 -FrontendPort 48080` | `EXECUTED_PASS`, non-root/read-only/bloqueos/zero SENT |
| `npm audit --audit-level=high` | `EXECUTED_PASS`, 0 vulnerabilidades |
| Grype por digest sobre imagen backend | `EXECUTED_PASS`, sin vulnerabilidades |
| `scripts/check-repository-safety.ps1` | `EXECUTED_PASS` |
| sintaxis PowerShell/Bash | `EXECUTED_PASS` |
| `git diff --check` | `EXECUTED_PASS`; solo avisos EOL informativos |

El primer intento del build final usó un `JAVA_HOME` inexistente y falló antes
de Maven. Se corrigió al JDK instalado `C:\Program Files\Java\corretto-21.0.7`;
el comando soportado posterior terminó `BUILD SUCCESS`.

## Evidencia fake

`validation-output/gmail-live-fake-20260730-070501.json`:

- 10/10 fases `FUNCTIONAL_PASS`;
- NOOP arrancó sin secretos Google;
- OAuth/token/refresh/userinfo/revoke y Gmail send se resolvieron solo contra el
  servidor fake loopback;
- provider assertions y seguridad PostgreSQL pasaron;
- el resumen afirma `realGoogleContacted=false`;
- el proyecto Compose aislado fue eliminado con su volumen propio.

## Manual y capturas

| Artefacto | Bytes | SHA-256 |
|---|---:|---|
| HTML | 21025 | `6b66f50e8109642901b0b02771495d5892e44ec5a9b080ff62c6d4521724daeb` |
| PDF | 3820732 | `25f51cac0c4d55f86cff354dd4be23d0c117731e64b12eaece71a40c439a7aee` |
| index.json | 11646 | `c8911249ad12b0ff4e20cfd5b3addf549ab2e516c8ef777a106f93b5fe76bda8` |
| ZIP | 8921266 | `6e2d50aa78a791fe1ec413e5c3e15d5e51f799ec030c85b6a5b0d150fec713ec` |

Se generaron 32 PNG sintéticos. El PDF tiene 34 páginas; todas fueron
renderizadas y se inspeccionaron el contacto completo y páginas
representativas sin overflow ni recorte. `validation-output/` no se versiona.

## Pendiente para publicación

- dos corridas integrales limpias sobre el mismo SHA definitivo;
- revisión final del diff y stage explícito;
- commit, fetch final, ancestría y push fast-forward.

Google real, credenciales reales, destinatarios reales y despliegue productivo
permanecen fuera de alcance.
