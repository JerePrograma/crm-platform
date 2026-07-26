# Continuidad y próximos pasos

Actualizado: 2026-07-24

## Estado remoto consolidado

```text
BRANCH main
VAL_001 FUNCTIONAL_PASS
VAL_002 FUNCTIONAL_PASS_FOCUSED
PRODUCTION_PORT_PREFLIGHT WINDOWS_AND_UNIX
DEMO_18080 PRESERVED
FULL_SUITES_REPEATED false
CI PENDING_POST_PUSH_VERIFICATION
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
```

## Qué quedó resuelto

- `VAL-001` permanece cerrado mediante dos corridas integrales sobre `0448c0e060311c284f4e4be4612982818a8480c4`;
- `VAL-002` comprueba `ProductionFrontendPort` antes de Maven, npm, builds Docker, migraciones y E2E;
- Windows conserva compatibilidad con tres puertos;
- Unix acepta un puerto productivo configurable;
- existen regresiones PowerShell y Node para puerto libre y ocupado;
- la demo autorizada en `127.0.0.1:18080` fue detectada sin detenerla;
- repository safety y `git diff --check` pasaron;
- no se repitieron suites funcionales no afectadas.

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

Implementar `UX-003` como cambio independiente:

1. inspeccionar `.github/remote-ux-trigger`;
2. localizar workflows y scripts `remote-ux-*`;
3. buscar referencias por nombre, ruta, comando y artefacto;
4. demostrar que ningún workflow canónico depende de ellos;
5. eliminar solo archivos obsoletos comprobados;
6. actualizar documentación y evidencia;
7. no mezclar métricas del dashboard ni cambios funcionales de UX.

## Después del gate

Después de `UX-003`, continuar con `UX-004` — métricas tenant-wide del dashboard.

El resto del orden se mantiene:

1. métricas tenant-wide;
2. paginación backend de importaciones;
3. paginación de outbox e inbound;
4. validación multibrowser y accesibilidad manual;
5. modularización incremental.

El candidato histórico `9e058d...` no debe reconstruirse por descripción.

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
