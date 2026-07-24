# Continuidad y próximos pasos

Actualizado: 2026-07-23

Fuente detallada:

```text
docs/estado-integral-y-roadmap.md
```

## Estado

```text
SEG-000 COMPLETE
SEG-001 COMPLETE
SEG-002 COMPLETE
SEG-003 COMPLETE
SEG-004 COMPLETE
SEG-005 COMPLETE
SEG-006 COMPLETE
SEG-007 COMPLETE
SEG-008 COMPLETE
SEG-009 COMPLETE
SEG-010 COMPLETE
SEG-011 COMPLETE
UX_OPERATOR_OVERHAUL COMPLETE
CONTACTABILITY_SYNC COMPLETE
PROSPECT_PAGINATION COMPLETE
BRANCH main
FUNCTIONAL_HEAD 19732dec9638cd47fee4b39ac41c5968693b5b7a
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
```

No existe un segmento funcional activo. El CRM está cerrado para demostración y operación segura simulada, pero tiene deuda y validaciones pendientes antes de una fase productiva.

## Próximo cambio recomendado

El siguiente cambio debe ser pequeño y autónomo:

### 1. Limpieza de automatización remota obsoleta

Eliminar únicamente:

```text
.github/remote-ux-trigger
.github/workflows/remote-ux-overhaul.yml
scripts/remote-ux-preflight.py
scripts/remote-ux-overhaul.py
scripts/remote-ux-postfix.py
```

Motivo:

- fueron utilizados para aplicar la primera etapa UX de forma remota;
- la guarda del workflow depende de una historia fija que ya no coincide con `main`;
- el workflow está inerte, pero conservar código operativo obsoleto confunde el mantenimiento;
- la eliminación no debe tocar la implementación funcional ni la CI canónica.

Validaciones mínimas:

```text
git diff --check
bash scripts/check-repository-safety.sh
revisión de .github/workflows y scripts
```

### 2. Corregir métricas parciales del dashboard

Problema:

- `frontend/src/App.tsx` calcula “prospectos con interés” y “contacto bloqueado” sobre `prospects`, que contiene solo la página actual;
- después de introducir paginación real, esas métricas pueden ser menores que el total real.

Resultado esperado:

- usar un endpoint agregado tenant-scoped existente o ampliarlo de forma compatible;
- no cargar todas las páginas para calcular métricas;
- mantener etiquetas y seguridad existentes;
- añadir prueba backend y frontend que demuestre que la métrica no depende de la página seleccionada.

## Roadmap prioritario

### Prioridad alta

1. eliminar la automatización remota obsoleta;
2. corregir métricas parciales del dashboard;
3. ejecutar auditoría manual WCAG 2.1 AA;
4. probar escala con datos sintéticos equivalentes a producción;
5. validar Firefox y WebKit.

### Prioridad media

1. llevar búsqueda, filtros y paginación de filas de importación al backend para lotes grandes;
2. dividir `frontend/src/App.tsx` por módulos sin cambiar contratos;
3. evaluar drawer móvil frente a la navegación horizontal actual;
4. ampliar skeletons y estados de carga;
5. revisar contraste y estados disabled manualmente;
6. normalizar términos técnicos residuales en OpenAPI y paneles avanzados.

### Prioridad baja

1. internacionalización formal;
2. preferencias de densidad;
3. atajos de teclado;
4. personalización visual avanzada;
5. telemetría UX anonimizada con política aprobada.

## Validaciones vigentes

### Plataforma completa

```text
run: 29951586239
commit: b904ff37e506f058dab351c2b941e13ee4ed9981
resultado: 22/22 jobs success
```

### Mejora UX

```text
run: 30034176306
commit funcional: 8d12f8ff772d3445440e4419b22d5c81b102cb15
resultado: PASS
```

### Contactabilidad y paginación

```text
run: 30036648327
commit funcional: 19732dec9638cd47fee4b39ac41c5968693b5b7a
resultado: PASS
```

## Pendientes externos

- validador integral en un host Unix real;
- evaluación del XLSX real autorizado fuera de Git y CI;
- infraestructura productiva;
- gestión de secretos;
- conexión de proveedores;
- pruebas con red real;
- autorización de despliegue y comunicaciones.

Ninguno de estos puntos debe resolverse dentro de un cambio UX o de mantenimiento general.

## Condiciones para una fase productiva

Antes de producción deben existir:

- plan de despliegue y rollback;
- backup/restore probado en el entorno objetivo;
- secretos fuera de Git;
- revisión de privacidad y retención;
- auditoría manual de accesibilidad;
- pruebas de carga;
- observabilidad y alertas;
- proveedores reales revisados en una fase separada;
- límites, kill switch e idempotencia verificados;
- CI verde sobre el commit exacto;
- autorización explícita.

## Contrato de seguridad

Debe permanecer:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No realizar:

- despliegue productivo;
- envío real;
- conexión de credenciales;
- importación de datos reales en Git, CI o imágenes;
- migraciones destructivas;
- reescritura del frontend;
- debilitamiento de permisos, exclusiones, CSRF, idempotencia o tenant isolation.

## Flujo Git para el próximo cambio

```text
1. git status --short
2. git branch --show-current
3. git remote -v
4. git fetch origin
5. git switch main
6. git pull --ff-only origin main
7. detenerse ante cambios locales, conflicto o divergencia
8. modificar solo archivos del alcance
9. ejecutar pruebas específicas y generales
10. ejecutar git diff --check
11. revisar status y diff completo
12. git add únicamente archivos relacionados
13. commit lógico
14. git push origin main solo con validaciones verdes
```

## Evidencia viva

```text
docs/status.md
docs/estado-integral-y-roadmap.md
docs/backlog.md
docs/ux-operador.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
```
