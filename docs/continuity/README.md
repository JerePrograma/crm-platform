# Continuidad canónica del proyecto

Actualizado: 2026-10-22

## Misión actual: SCALARIS-DEPLOY-001

El usuario autorizó completar los pendientes de preparación productiva y eligió el VPS Hostinger con `crm.scalaris.com.ar`. Leer primero [el segmento actual](../segments/SCALARIS-DEPLOY-001.md), [su validación](../validation/scalaris-production-readiness-2026-10-22.md), status, next-step y backlog. La misión reemplaza las restricciones de alcance del cierre documental anterior para las correcciones identificadas; mantiene contactos y comunicaciones reales fuera de las pruebas.

La fuente funcional `4b556a5c6e91e42325ed12c393c8003d7914d48a` aprobó dos integrales limpias consecutivas, 23/23 fases cada una. Scans controlados sin high/critical ni exclusiones y export verifican los IDs productivos exactos. Este cierre documental se registra como descendiente separado. Publicación/push, CI actual de 23 jobs y producción siguen pendientes de autorización final; no se activaron DNS, credenciales ni timers. Esta PC fue elegida para la copia cifrada: helper instalado, tarea 04:15 BA preparada sin registrar y primera copia real pendiente. La validación actual es la autoridad de resultados y límites; el historial siguiente conserva su alcance original.

## Historial de misión: SCALARIS-001

La edición actual prepara el CRM para Marketing de Scalaris: servicio técnico, desarrollo y venta a pedido. Incluye perfil/organización propios, cuenta Marketing, agenda, referencias comerciales, etiquetas/plantillas y apertura local Windows mediante paquete portable o Docker. Los datos y claves de la instalación quedan fuera de Git.

La operación de octubre está aprobada localmente y la implementación quedó `PUBLISHED_FAST_FORWARD` en `331b3e7be5ad52b93b285a0ff9654abedce79b7d`, con HEAD/origin/main/ls-remote coincidentes y divergencia `0 0`. CI `FAILED_DEPENDENCY_SCAN`: fallo real Grype high/critical, [run 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532), resultado final 21/22 jobs success; solo dependency-scan failure. La autoridad es [publicación GitHub Scalaris](../validation/scalaris-github-publication-2026-10-14.md). La operación portable aprobó 18 comprobaciones PowerShell 5.1 con Java 21/PostgreSQL 18/Node 24. Backend: 62 unitarias y 6 integración; frontend de la entrega anterior: 16 unitarias/typecheck/build/audit y E2E 4/4. La fuente publicada agregó los ajustes rol/proxy, comprobados con navegador 5/5 y Node 2/2; el ZIP/instalación anteriores no se actualizaron automáticamente. Este cierre documental registra la publicación y sus límites.

El launcher Docker PowerShell 5.1 aprobó 55 aserciones, con respaldo simulado. Docker real y la suite integral histórica están bloqueados por ausencia de Docker. El manual se revisó visualmente a 1440/390/320 px y quedó sin desborde horizontal. No declarar cierre integral, CI ni publicación de esta edición por los resultados de julio.

La instalación de uso está activa en `http://127.0.0.1:8787`, con cero prospectos, 18 etiquetas y 7 plantillas; perfil/rol correctos y todos los servicios en loopback. QA quedó detenido. Leer primero `docs/segments/SCALARIS-001.md`, `docs/scalaris-marketing-local.md`, `docs/status.md` y `docs/next-step.md`, junto con la lectura obligatoria siguiente. El backlog conserva remediación Java/imagen y limitación agenda 200 pendientes; no cargar contactos reales, conectar proveedores ni desplegar producción automáticamente.

## Extensión SEG-001 Gmail — 2026-07-30

La misión histórica del 2026-07-30 recuperó de forma verificable 68 archivos parciales, retiró
solo la referencia interna rota informada y avanzó `main` por fast-forward de
`12421c5` a `9995b3e`. La implementación OAuth/Gmail/campaña LIVE pasa backend,
frontend y E2E contra Google falso. El commit `d6bfe642` pasó dos validaciones
integrales limpias consecutivas, fue publicado por fast-forward
`9995b3e..d6bfe64` y obtuvo 22/22 jobs verdes en la CI `30530515993`;
`HEAD`, `origin/main` y `ls-remote` quedaron coincidentes.

Fuente primaria:
`docs/implementation/SEG-001-gmail-campaign-delivery.md`.

Google real continúa `IMPLEMENTED_NOT_CONNECTED`; producción `NOT_DEPLOYED` y
los defaults de red/envío permanecen bloqueados.

El índice original de continuidad fue actualizado el 2026-07-24; sus evidencias de julio se conservan como historial.

Este directorio permite retomar `JerePrograma/crm-platform` sin depender de chats, archivos temporales ni afirmaciones no verificadas.

## Qué representa el repositorio

Este repositorio implementa el CRM comercial de Scalaris para Marketing. Conserva el perfil `LEGACY` y los contratos históricos del CRM que se utilizó para vender Gestudio. No es la aplicación operativa de academias que administra alumnos, cuotas y asistencia.

## Lectura obligatoria

1. [`product-purpose-architecture.md`](product-purpose-architecture.md)
2. [`configuration-environments-data.md`](configuration-environments-data.md)
3. [`local-operation-and-deployment.md`](local-operation-and-deployment.md)
4. [`validation-release-state-2026-07-24.md`](validation-release-state-2026-07-24.md)
5. [`continuation-mission.md`](continuation-mission.md)
6. `docs/status.md`
7. `docs/next-step.md`
8. `docs/backlog.md`
9. `docs/validation/remote-main-hardening-2026-07-24.md`
10. el ADR, módulo y validación específicos del cambio

## Jerarquía de fuentes

1. código y configuración versionados en `main`;
2. salida estructurada de comandos sobre el commit exacto;
3. documentos de validación;
4. documentos de continuidad;
5. documentos históricos;
6. transcripts o resúmenes conversacionales.

Un script implementado no equivale a un script ejecutado. `IMPLEMENTED_NOT_RUN`, `BLOCKED_EXTERNAL` y `EXECUTED_PASS` no son intercambiables.

## Historial de estado remoto consolidado — julio de 2026

```text
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
commit funcional validado: 0448c0e060311c284f4e4be4612982818a8480c4
parser exacto de .Config.Env: FUNCTIONAL_PASS
corridas integrales consecutivas: 2/2 FUNCTIONAL_PASS
CI del SHA validado: NO_CHECKS_REPORTED
candidato histórico 9e058d...: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
producción: NOT_DEPLOYED
envíos reales: DISABLED
```

## Hardening incorporado

Los validadores canónicos:

- exigen `main`;
- parsean `.Config.Env` como array JSON;
- comprueban siete guardas mediante coincidencia exacta;
- fallan ante JSON vacío o inválido;
- incluyen self-tests PowerShell y Node;
- no imprimen el entorno completo;
- mantienen providers reales desconectados.

PowerShell 5.1, Node 22, sintaxis Bash, Docker, backend, frontend, migraciones, E2E, dependency scans, backup/restore, `productionProfileSmoke`, bloqueo efectivo de envíos, cero estados enviados, repository safety y `finalTreeClean` pasaron.

La evidencia principal está en `docs/validation/main-hardening-functional-closure-2026-07-24.md`.

## Candidato local histórico

```text
v6: e3a9728e717b7c8a4d92f9fab31f709bf5d66464
locators: 24df4c7f26ffde0f044f681f9130fa254f15debd
foco inicial: fa8c15172dfa9a0cfa5cbd00f7aab42733d516ba
foco explícito: 9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Son referencias de trees locales. No se encontraron commits, ramas o PRs remotos que los materialicen. No deben describirse como integrados.

## Invariantes

- trabajar directamente sobre `main`;
- no crear ramas o PRs salvo instrucción explícita distinta;
- no usar force push, reset destructivo ni reescritura de historia;
- no desplegar producción ni habilitar comunicaciones reales;
- no versionar `.env`, secretos, credenciales, ZIP, logs, datos reales ni el XLSX real;
- mantener PostgreSQL como fuente de verdad;
- conservar RBAC, tenant isolation, CSRF, exclusiones, idempotencia y fail-closed;
- volver a ejecutar cualquier fase cuyos archivos o dependencias hayan cambiado.

## VAL-002 — Puerto productivo en preflight

El puerto del perfil productivo sintético forma parte del preflight en Windows y Unix.

```text
Windows checker: scripts/check-host-ports.ps1
Unix checker: scripts/check-host-ports.js
PowerShell regression: scripts/test-check-host-ports.ps1
Node regression: scripts/test-check-host-ports.js
estado: FUNCTIONAL_PASS_FOCUSED
full suites repeated: false
```

La demo autorizada que publica `127.0.0.1:18080` permanece fuera del proyecto Compose sintético y no fue modificada.

Siguiente gate: `UX-003`.

## UX-003 — Automatización remota eliminada

La automatización `remote-ux-overhaul` fue eliminada después de comprobar que:

- su único trigger era un archivo histórico;
- su guarda fija ya no podía cumplirse sobre `main`;
- los tres scripts Python solo eran consumidos por ese workflow;
- ningún otro workflow o script dependía del conjunto;
- la funcionalidad generada por aquella ejecución ya está integrada en el código publicado.

Estado: `COMPLETE_WITH_FOCUSED_VALIDATION`.

Siguiente gate: `UX-004`.

## UX-004 — Métricas globales del resumen

El dashboard usa `GET /api/v1/prospects/metrics` para interés y bloqueo.

```text
scope: organization_id del actor
archived: excluded
interested statuses: INTERESTED, QUALIFIED, TRIAL_ACTIVE, QUOTED, NEGOTIATION
blocked: NOT contact_eligible
pagination dependency: none
status: FUNCTIONAL_PASS
```

La regresión usa 105 prospectos y una página de 100.

`UX-006` fue publicado en `12421c53375deabebe8f48f17af3ae95af95893b`.
La implementación de SCALARIS-001 está publicada en `331b3e7`; CI falló realmente en dependency-scan y la remediación está en backlog. La publicación fast-forward de SEG-001 Gmail corresponde al historial del 2026-07-30 y no prueba PASS de la CI actual.
