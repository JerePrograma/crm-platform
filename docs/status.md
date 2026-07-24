# Estado actual

Actualizado: 2026-07-24

## Repositorio canónico

```text
repositorio: JerePrograma/crm-platform
rama única: main
HEAD verificado antes de la consolidación: f25051884b7aadd5837286dedd9ae0eee899cb5a
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
hardening remoto del parser: IMPLEMENTED_NOT_FULLY_VALIDATED
producción: NOT_AUTHORIZED / NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
proveedores reales: IMPLEMENTED_NOT_CONNECTED
XLSX real: OUTSIDE_GIT_CI_IMAGES
```

`main` es la única fuente de verdad. La comparación remota confirmó que `f250518...` estaba exactamente un commit por delante de `83e181c`, únicamente por `AGENTS.md` y `docs/continuity/`.

## Veredicto

El CRM publicado conserva el cierre funcional previo para demostración, evaluación interna y operación comercial segura sin envíos reales.

La consolidación remota del 24 de julio corrigió los validadores versionados para:

- trabajar sobre `main`;
- parsear `.Config.Env` como arreglo JSON real;
- comprobar membresía exacta de las siete guardas;
- fallar ante JSON vacío, inválido o con raíz no-array;
- ejecutar regresiones PowerShell y Node;
- no imprimir el entorno completo ni debilitar el fail-closed.

Ese hardening está implementado en `main`, pero no puede declararse cerrado: faltan PowerShell 5.1, Docker, `productionProfileSmoke`, `finalTreeClean` y las dos corridas integrales limpias sobre el mismo commit.

## Candidato post-hardening histórico

```text
tree documentado: 9e058d7044415b80af554ab8ae4fe3170585b1c9
estado remoto: NOT_AVAILABLE_AS_COMMIT_OR_BRANCH
estado de integración: NOT_INTEGRATED
```

No se localizaron objetos remotos verificables que permitan integrar ese candidato. No se reconstruyó por inferencia y no se usaron patches locales inaccesibles.

## Cambios versionados en esta consolidación

- `scripts/container-env-assertions.ps1`;
- `scripts/test-container-env-assertions.ps1`;
- `scripts/assert-container-env.js`;
- `scripts/test-container-env-assertions.js`;
- `scripts/validate-complete-crm.ps1`;
- `scripts/validate-complete-crm.sh`;
- `scripts/verify-production-profile.ps1`;
- `scripts/verify-production-profile.sh`.

Evidencia:

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## Causa raíz corregida

Los validadores canónicos trataban el JSON completo de:

```text
docker inspect <container> --format {{json .Config.Env}}
```

como un string y buscaban valores mediante regex o `grep`. La nueva implementación convierte el JSON en una colección real y exige coincidencia exacta.

Además, los validadores integrales todavía exigían `feat/complete-crm-platform`; ahora exigen `main`, en línea con `AGENTS.md`.

## Seguridad vigente

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

Permanece prohibido:

- desplegar producción sin autorización;
- habilitar envíos reales;
- conectar credenciales o proveedores;
- incorporar `.env`, secretos, ZIP de evidencia, logs o datos reales;
- incorporar `gestudio_lote_100_prospectos.xlsx`;
- debilitar RBAC, tenant isolation, CSRF, exclusiones o idempotencia.

## Validación actual

| Gate | Estado |
|---|---|
| inspección y comparación remota | EXECUTED_PASS |
| self-test Node del parser | EXECUTED_PASS |
| self-test PowerShell | IMPLEMENTED_NOT_RUN |
| parser conectado al validador Windows | IMPLEMENTED_NOT_RUN |
| parser conectado al validador Unix | IMPLEMENTED_NOT_RUN |
| `productionProfileSmoke` | IMPLEMENTED_NOT_RUN |
| `finalTreeClean` | IMPLEMENTED_NOT_RUN |
| validador integral corrida 1 | IMPLEMENTED_NOT_RUN |
| validador integral corrida 2 | IMPLEMENTED_NOT_RUN |
| CI del commit final | PENDING_VERIFICATION |

No debe interpretarse código versionado como `FUNCTIONAL_PASS`.

## Capacidades funcionales publicadas previamente

Permanecen las capacidades SEG-000–SEG-011 y los cierres UX/contactabilidad ya documentados en la historia: identidad, RBAC, tenant isolation, prospectos, contactos, importaciones, duplicados, actividades, tareas, oportunidades, campañas, mensajería simulada, outbox, inbound de prueba, reporting, auditoría, configuración y frontend operativo.

El nuevo candidato local que añadía métricas tenant-wide, paginación adicional, drawer, multibrowser y retorno de foco explícito no se declara publicado mientras no exista en el contenido remoto verificable.

## Próximo paso obligatorio

Desde un checkout limpio de `main` en Windows con Docker operativo:

1. ejecutar `scripts/test-container-env-assertions.ps1`;
2. ejecutar dos veces `scripts/validate-complete-crm.ps1` sobre el mismo commit;
3. exigir `FUNCTIONAL_PASS` en ambas salidas JSON;
4. comprobar `productionProfileSmoke` y `finalTreeClean`;
5. verificar `git diff --check` y repository safety;
6. comprobar GitHub Actions del SHA exacto.
