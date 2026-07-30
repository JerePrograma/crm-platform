# Validación SEG-001 Gmail y campañas LIVE — 2026-07-30

Estado: `FUNCTIONAL_PASS_PUBLISHED`

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
| `scripts/validate-complete-crm.ps1` corrida 1 | `FUNCTIONAL_PASS`, 22 fases, 620,955 s, `d6bfe642` |
| `scripts/validate-complete-crm.ps1` corrida 2 | `FUNCTIONAL_PASS`, 22 fases, 640,545 s, `d6bfe642` |

El primer intento del build final usó un `JAVA_HOME` inexistente y falló antes
de Maven. Se corrigió al JDK instalado `C:\Program Files\Java\corretto-21.0.7`;
el comando soportado posterior terminó `BUILD SUCCESS`.

## Evidencia fake

`validation-output/gmail-live-fake-20260730-090951.json` y
`validation-output/gmail-live-fake-20260730-092044.json`:

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
| HTML | 21025 | `ad78cc4f6964db629b4038fa527e842b6ecb168a13b75fab06714bea801ca472` |
| PDF | 3865910 | `646c346b939afae0c6a4a0361ce0a38d2073b60a2eabbda7b8e7b01b4b3cf1dc` |
| index.json | 11647 | `bac71398ecd40598fa8eaafe7f537b47b2e59b40ea0757bf482c30920c7a483e` |
| ZIP | 9038006 | `c50e78481edfa46c9f7860a8fc62eec1d4bcca822f55807a2b4553e1df313289` |

Se generaron 32 PNG sintéticos. El PDF tiene 34 páginas; todas fueron
renderizadas y se inspeccionaron el contacto completo y páginas
representativas sin overflow ni recorte. `validation-output/` no se versiona.

## Doble validación integral

Ambas corridas usaron `d6bfe64283cd3bab8dce441e4b01e348f9a55d5e`,
árbol limpio y proyectos Compose/puertos independientes:

| Evidencia | Estado | Duración | SHA-256 |
|---|---|---:|---|
| `complete-crm-20260730-090155.json` | `FUNCTIONAL_PASS` | 620,955 s | `5a8c1e6e28bcfdc2c5721fab9a87b47b001e44ec2d78435a323cb91d646e52c7` |
| `complete-crm-20260730-091224.json` | `FUNCTIONAL_PASS` | 640,545 s | `0f789a354f64d95dfc75c85b79d1b1184d8bd2528f563effb3cf7ac092cbba16` |

El primer intento integral, sobre `df2c79a`, falló en `frontendE2E`: el runner
general descubría el spec Gmail LIVE sin el identificador de su stack falso.
Se corrigió el contrato en `d724b80`: E2E general/NOOP ejecuta sus tres
escenarios y Gmail LIVE se valida en una fase separada con
`validate-gmail-live-fake.ps1`. No se omitió ningún escenario; la repetición
quedó `3/3` general y `1/1` Gmail LIVE en cada corrida.

## Corrección de CI remota

El run `30528538137` sobre `61885e2` terminó 21/22. El único fallo fue
`frontend-e2e`: el workflow aún usaba `npm run test:e2e`, por lo que descubría
Gmail LIVE dentro del stack general y la guarda
`CRM_E2E_COMPOSE_PROJECT` abortaba correctamente. Se cambió el job para:

- ejecutar explícitamente los tres escenarios general/NOOP;
- desmontar su stack aislado;
- ejecutar `validate-gmail-live-fake.ps1` en un stack `crm-gmail-fake-*`;
- admitir HEAD detached solo dentro de GitHub Actions y únicamente cuando
  `GITHUB_SHA` coincide exactamente con `git rev-parse HEAD`.

La validación focal local posterior volvió a pasar 10/10 fases, incluida
NOOP 1/1 y Gmail LIVE 1/1 contra Google falso.

El run definitivo `30530515993` sobre `d6bfe642` terminó 22/22 verde. El job
`frontend-e2e` pasó los tres escenarios general/NOOP y después el escenario
Gmail LIVE en el stack fake aislado.

## Publicación

- fetch final: `origin/main=9995b3e`, sin commits remotos exclusivos;
- push: `EXECUTED_PASS`, fast-forward `9995b3e..d6bfe64`;
- verificación posterior: `HEAD=origin/main=ls-remote=d6bfe642`, conteo `0 0`;
- árbol final limpio y `git diff --check` sin hallazgos.

Google real, credenciales reales, destinatarios reales y despliegue productivo
permanecen fuera de alcance.
