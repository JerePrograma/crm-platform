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

Después del avance, la comparación entre `main` y la rama temática resultó:

```text
status: identical
ahead: 0
behind: 0
```

## Evolución posterior

Después de consolidar se realizaron directamente en `main`:

- alineación de `.env.example`, Compose y backend;
- documentación completa de arranque y uso;
- perfil Compose con PostgreSQL, backend y frontend;
- imágenes backend y frontend;
- health checks y proxy Nginx;
- preflight y smoke tests Unix/PowerShell;
- Makefile;
- CI ampliado;
- `.dockerignore` y `.gitattributes`;
- evidencia estática ejecutada;
- actualización de README, estado, backlog, segmento, validación y changelog.

## Comparación más reciente

La comparación más reciente entre la rama histórica y `main` arrojó:

```text
base: feat/seg-001-prospect-vertical-slice
head: main
status: ahead
main ahead: 44
main behind: 0
```

Interpretación:

- la rama histórica no contiene cambios exclusivos;
- todo su contenido está en `main`;
- `main` contiene además 44 commits posteriores;
- no debe realizarse trabajo nuevo en la rama histórica.

## Fuente canónica

- `main` contiene todo el código vigente;
- nuevas sesiones deben partir de `main`;
- `docs/status.md` define el estado real;
- `docs/next-step.md` define el trabajo siguiente;
- `docs/validation/SEG-001.md` define la evidencia;
- ninguna rama temática anterior es fuente de verdad.

## Lo que la consolidación no implica

Consolidar no equivale a declarar `SEG-001` completo.

Continúan pendientes:

- ejecución verde de Maven y Spotless;
- Testcontainers, Flyway y Hibernate reales;
- instalación frontend y lockfile;
- typecheck/build;
- validación semántica de Compose;
- builds de ambas imágenes;
- stack y smoke test;
- evidencia de GitHub Actions o validación local equivalente.

## Seguridad

La consolidación no modificó las guardas:

```text
sending.enabled=false
sending.dry-run=true
sending.daily-limit=0
sending.environment-kill-switch=true
```

El kill switch persistente continúa activo. No existe adaptador Gmail, SMTP ni otro mecanismo de envío.

## Estrategia futura de ramas

Hasta cerrar `SEG-001`, las correcciones pequeñas de estabilización pueden aplicarse directamente en `main` por autorización expresa del propietario.

Para segmentos funcionales posteriores:

1. partir de `main` verde;
2. crear una rama temática;
3. agrupar commits por unidad lógica;
4. exigir CI verde;
5. consolidar mediante PR o fast-forward autorizado;
6. actualizar estado, backlog, validación y changelog.
