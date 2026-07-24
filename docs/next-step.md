# Continuidad y próximos pasos

Actualizado: 2026-07-24

## Estado remoto consolidado

```text
BRANCH main
REMOTE_BASELINE_BEFORE_CONSOLIDATION f25051884b7aadd5837286dedd9ae0eee899cb5a
FUNCTIONAL_BASELINE 83e181ce614f145bbfe141cc7603c3042569be51
ENV_JSON_PARSER IMPLEMENTED
PRODUCTION_PROFILE_SMOKE IMPLEMENTED_NOT_RUN
FINAL_TREE_CLEAN IMPLEMENTED_NOT_RUN
FULL_VALIDATION_RUN_1 IMPLEMENTED_NOT_RUN
FULL_VALIDATION_RUN_2 IMPLEMENTED_NOT_RUN
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
```

## Qué quedó resuelto

- los validadores Windows y Unix exigen `main`;
- `.Config.Env` se parsea como array JSON real;
- las siete guardas se comparan por membresía exacta;
- JSON vacío, inválido o con raíz incorrecta falla;
- existen self-tests PowerShell y Node;
- el perfil productivo comprueba también `EMAIL_PROVIDER_MODE=NOOP` y `WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY`;
- la evidencia remota quedó registrada en `docs/validation/remote-main-hardening-2026-07-24.md`.

## Qué no quedó resuelto

El tree histórico `9e058d7044415b80af554ab8ae4fe3170585b1c9` no existe en GitHub como commit o rama accesible. Por tanto, no se integraron de manera ficticia:

- métricas tenant-wide del dashboard;
- eliminación de automatización UX remota;
- paginación backend adicional de importaciones;
- paginación de outbox/inbound;
- modularización adicional;
- drawer móvil;
- Playwright multibrowser;
- correcciones históricas de foco no publicadas.

Esas capacidades deben tratarse como backlog o recuperarse desde patches verificados fuera de GitHub.

## Próximo paso obligatorio

No implementar funcionalidad adicional todavía.

Ejecutar en un checkout limpio de `main`, sobre el mismo SHA:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Criterios de cierre:

1. self-test PowerShell PASS en PowerShell 5.1;
2. `productionProfileSmoke=FUNCTIONAL_PASS`;
3. `finalTreeClean=FUNCTIONAL_PASS`;
4. resumen global `FUNCTIONAL_PASS`;
5. dos corridas limpias consecutivas sobre el mismo commit;
6. repository safety PASS;
7. `git diff --check` PASS;
8. CI verde o estado externo documentado sin falsear PASS.

## Después del gate

Solo con el cierre anterior:

1. localizar y verificar los cuatro patches históricos mediante SHA-256;
2. aplicar `git apply --check` sobre un clon temporal del `main` actual;
3. portar únicamente cambios comprobables;
4. volver a ejecutar dos validaciones integrales;
5. actualizar evidencia y documentación.

Sin patches verificables, ejecutar el backlog directamente desde el código remoto actual, en cambios pequeños e independientes.

## Backlog inmediato alternativo

Si el candidato histórico no puede recuperarse, el siguiente orden seguro es:

1. eliminar automatización UX remota obsoleta después de confirmar que no participa en CI;
2. corregir métricas tenant-wide del dashboard;
3. paginar importaciones en backend;
4. paginar outbox e inbound;
5. validar Firefox/WebKit;
6. corregir defectos observados de foco;
7. evaluar drawer móvil y modularización incremental.

Cada punto requiere su propia evidencia y no debe marcarse completo por referencia al candidato local ausente.

## Restricciones

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No desplegar, no enviar, no conectar proveedores, no incorporar datos reales y no usar force push.
