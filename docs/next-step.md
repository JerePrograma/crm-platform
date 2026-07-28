# Continuidad y próximos pasos

Actualizado: 2026-07-24

## Estado remoto consolidado

```text
BRANCH main
VAL_001 FUNCTIONAL_PASS
VAL_002 COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX_003 COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX_004 FUNCTIONAL_PASS
DASHBOARD_PROSPECT_METRICS TENANT_WIDE
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
NEXT UX_006
```

## Qué quedó resuelto

- `VAL-001`, `VAL-002` y `UX-003` permanecen cerrados;
- los conteos “Prospectos con interés” y “Contacto bloqueado” ya no dependen de `prospects`;
- el backend agrega directamente en PostgreSQL con `organization_id`;
- la consulta excluye archivados y no carga páginas;
- la semántica de estados visible se conserva;
- `VIEWER` puede leer el endpoint;
- un segundo tenant recibe sus propios conteos;
- las pruebas con 105 registros demuestran independencia de la primera página;
- backend completo y frontend validaron correctamente.

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

Implementar `UX-006` como cambio independiente:

1. localizar endpoint, servicio y repositorio real de filas de importación;
2. inventariar todos sus consumidores;
3. definir paginación compatible con el frontend;
4. añadir filtros backend por hoja y estado;
5. añadir búsqueda y límites explícitos;
6. preservar tenant isolation e idempotencia;
7. añadir pruebas backend y frontend;
8. actualizar evidencia y fuentes canónicas.

## Después del gate

Después de `UX-006`, continuar en este orden:

1. `OPS-001` — outbox e inbound paginados;
2. `UX-007` — navegadores, foco y móvil;
3. `TECH-001` — modularización gradual;
4. `PERF-001` — escala representativa autorizada.

No reconstruir el candidato histórico `9e058d...` por descripción.

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
