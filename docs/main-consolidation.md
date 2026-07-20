# Consolidación en `main`

## Fecha

2026-07-20

## Decisión

Todo el trabajo de `feat/seg-001-prospect-vertical-slice` se consolidó en `main` mediante un avance fast-forward. No se utilizó force push, squash ni rebase.

## Estado antes de consolidar

```text
main: 7fca32b9b777f8b8c5c305c0699d14877babebc6
feature: 1c089cd19ce953e93531c1ef3c54f79d28575444
ahead: 171
behind: 0
```

La comparación confirmó que la rama temática descendía linealmente de `main`.

## Operación realizada

La referencia `refs/heads/main` avanzó a:

```text
1c089cd19ce953e93531c1ef3c54f79d28575444
```

La actualización se ejecutó con `force=false`.

Después del avance, la comparación entre `main` y `feat/seg-001-prospect-vertical-slice` resultó:

```text
status: identical
ahead: 0
behind: 0
```

## Cambios posteriores en `main`

Después de consolidar se realizaron directamente en `main`:

- alineación de `.env.example` y Docker Compose;
- documentación completa de arranque y uso;
- actualización de `README.md` para convertirlo en punto de entrada canónico;
- actualización de estado, backlog, validación y continuidad;
- documentación de esta operación.

## Fuente canónica

Desde esta consolidación:

- `main` contiene todo el código vigente;
- nuevas sesiones deben partir de `main`;
- `docs/status.md` define el estado real;
- `docs/next-step.md` define el trabajo siguiente;
- ninguna rama temática anterior debe considerarse fuente de verdad.

## Lo que la consolidación no implica

Consolidar no equivale a declarar `SEG-001` completo.

Continúan pendientes:

- ejecución verde de Maven y Spotless;
- Testcontainers, Flyway y Hibernate reales;
- instalación y build frontend;
- generación de `package-lock.json`;
- validación de Compose e imagen;
- evidencia de GitHub Actions o validación local equivalente.

## Seguridad

La consolidación no modificó las guardas de envío:

```text
sending.enabled=false
sending.dry-run=true
sending.daily-limit=0
sending.environment-kill-switch=true
```

El kill switch persistente también continúa activo. No existe adaptador Gmail, SMTP ni otro mecanismo de envío.

## Estrategia futura de ramas

Hasta cerrar `SEG-001`, las correcciones pequeñas de estabilización pueden aplicarse directamente en `main` por autorización expresa del propietario.

Para segmentos funcionales posteriores se recomienda:

1. crear una rama temática desde `main` verde;
2. mantener commits agrupados por unidad lógica;
3. exigir CI verde;
4. consolidar mediante pull request o fast-forward explícitamente autorizado;
5. actualizar estado, backlog, validación y changelog en la misma entrega.
