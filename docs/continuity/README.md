# Continuidad canónica del proyecto

Actualizado: 2026-07-24

Este directorio existe para que una sesión nueva pueda retomar el proyecto sin depender del historial de una conversación, de archivos temporales ni de afirmaciones no verificadas.

## Qué representa este repositorio

`JerePrograma/crm-platform` es el CRM comercial utilizado para prospectar, calificar y acompañar instituciones potencialmente interesadas en Gestudio. No es la aplicación operativa que usan las academias para administrar alumnos, cuotas y asistencia; esa es la solución comercializada.

## Lectura obligatoria

Antes de proponer o ejecutar cambios, leer en este orden:

1. [`product-purpose-architecture.md`](product-purpose-architecture.md)
2. [`configuration-environments-data.md`](configuration-environments-data.md)
3. [`local-operation-and-deployment.md`](local-operation-and-deployment.md)
4. [`validation-release-state-2026-07-24.md`](validation-release-state-2026-07-24.md)
5. [`continuation-mission.md`](continuation-mission.md)
6. `docs/status.md`
7. `docs/next-step.md`
8. `docs/backlog.md`
9. el ADR, módulo y validación específicos del cambio

## Jerarquía de fuentes

Cuando haya contradicciones:

1. código y configuración versionados;
2. salida estructurada de comandos sobre el commit exacto;
3. estos documentos de continuidad;
4. documentos históricos;
5. transcripts o resúmenes conversacionales.

Un transcript no equivale a una validación aprobada. Un resultado solo es `PASS` cuando el JSON de evidencia y los códigos de salida lo demuestran.

## Estado de publicación

El baseline funcional que precede a esta documentación es:

```text
83e181ce614f145bbfe141cc7603c3042569be51
```

Existe un candidato post-hardening reconstruible cuyo tree validado parcialmente es:

```text
9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Ese candidato no estaba publicado en `origin/main` al redactar este documento. La publicación de estos archivos de continuidad puede adelantar `main` respecto del baseline anterior; por eso las sesiones futuras deben resolver siempre el `HEAD` actual y nunca asumir que `83e181c` sigue siendo la punta remota.

## Invariantes

- trabajar directamente sobre `main`, salvo instrucción explícita distinta;
- detenerse ante cambios locales ajenos, conflictos o divergencia;
- no usar force push, reset destructivo ni reescritura de historia;
- no desplegar producción ni habilitar comunicaciones reales;
- no versionar `.env`, secretos, credenciales, datos de clientes ni el XLSX real;
- mantener PostgreSQL como fuente de verdad;
- conservar aislamiento por tenant, CSRF, RBAC, exclusiones, idempotencia y fail-closed;
- no repetir fases aprobadas cuando existe evidencia estructurada íntegra y el código cubierto no cambió;
- volver a ejecutar una fase si cambió cualquiera de sus archivos, dependencias, configuración o entorno relevante.
