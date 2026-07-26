# Continuidad y próximos pasos

Actualizado: 2026-07-24

## Estado remoto consolidado

```text
BRANCH main
VAL_001 FUNCTIONAL_PASS
VAL_002 COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX_003 COMPLETE_WITH_FOCUSED_VALIDATION
REMOTE_UX_AUTOMATION REMOVED
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
NEXT UX_004
```

## Qué quedó resuelto

- `VAL-001` continúa cerrado;
- `VAL-002` continúa cerrado con verificación focalizada y `NO_CHECKS_REPORTED`;
- se localizaron los cinco archivos exactos de la automatización remota histórica;
- el trigger era el único evento del workflow;
- la guarda de ancestry fija ya no coincide con `main`;
- los scripts Python no tenían consumidores fuera del workflow;
- ningún otro workflow dependía del conjunto;
- los cinco archivos fueron eliminados sin modificar funcionalidad;
- repository safety y `git diff --check` pasaron.

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

Implementar `UX-004` como cambio independiente:

1. inspeccionar dashboard, API, reportes y tipos reales;
2. identificar los conteos que dependen de la página cargada;
3. localizar una agregación tenant-scoped existente o crear el mínimo contrato compatible;
4. no cargar todas las páginas para calcular métricas;
5. añadir pruebas con más de una página;
6. validar permisos y aislamiento entre organizaciones;
7. actualizar documentación y evidencia.

## Después del gate

Después de `UX-004`, continuar en este orden:

1. `UX-006` — importaciones de gran volumen;
2. `OPS-001` — outbox e inbound paginados;
3. `UX-007` — navegadores, foco y móvil;
4. `TECH-001` — modularización gradual;
5. `PERF-001` — escala representativa autorizada.

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
