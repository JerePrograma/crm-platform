# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad, aislamiento por organización y controles fail-closed.

## Estado actual

`main` es la única fuente canónica.

```text
base sincronizada para SEG-001 Gmail: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
OAuth/Gmail/campaña LIVE contra proveedor falso: EXECUTED_PASS
backend clean verify: EXECUTED_PASS, 112/112
frontend install/typecheck/unit/build: EXECUTED_PASS, 13/13
validación integral local: 2/2 FUNCTIONAL_PASS, commit d724b80
publicación SEG-001 Gmail: VALIDATED_LOCALLY_PENDING_PUSH
producción: NOT_DEPLOYED
Google real: IMPLEMENTED_NOT_CONNECTED
comunicaciones reales por defecto: DISABLED_BY_POLICY
```

La extensión SEG-001 incorpora cuentas Gmail OAuth, ejecución LIVE controlada
por campaña/outbox, baja one-click y observabilidad individual. Se validó
exclusivamente contra Google falso y datos sintéticos. No está desplegada ni
conectada a Google real; todos los defaults continúan fail-closed.

## Consolidación remota del 24 de julio de 2026

Se corrigieron y validaron los defectos objetivos de los validadores canónicos:

- Windows y Unix exigen la rama `main`;
- `.Config.Env` se parsea como un array JSON real;
- las siete guardas se comparan por membresía exacta;
- JSON vacío, inválido o con raíz no-array falla;
- existen self-tests PowerShell 5.1 y Node 22;
- el perfil productivo exige también `EMAIL_PROVIDER_MODE=NOOP` y `WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY`;
- no se imprimen secretos ni el entorno completo.

El cierre local se realizó sobre `0448c0e060311c284f4e4be4612982818a8480c4` mediante dos corridas integrales consecutivas:

```text
complete-crm-20260724-201944.json
SHA-256 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527

complete-crm-20260724-202955.json
SHA-256 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

Ambas terminaron `FUNCTIONAL_PASS`, incluido `productionProfileSmoke`, bloqueo efectivo de envíos, cero estados enviados, repository safety y `finalTreeClean`.

Evidencia canónica:

```text
docs/validation/main-hardening-functional-closure-2026-07-24.md
```

## Candidato post-hardening histórico

La documentación registra el tree:

```text
9e058d7044415b80af554ab8ae4fe3170585b1c9
```

No está disponible en GitHub como commit, rama o PR accesible. No se reconstruyó por inferencia y no se declara integrado.

Las capacidades descritas solo en ese candidato permanecen en backlog hasta que existan patches verificados o se implementen de nuevo desde el código remoto actual.

## Alcance funcional publicado

- organizaciones, usuarios, roles, permisos y sesiones seguras;
- tenant isolation, CSRF, cookies HttpOnly y auditoría;
- instituciones, prospectos, contactos y canales;
- normalización, exclusiones y contactabilidad;
- búsqueda, filtros, orden, paginación y CSV seguro;
- notas, actividades, tareas y timeline;
- importaciones CSV/XLSX sintéticas con vista previa e idempotencia;
- duplicados exactos/ambiguos y resolución transaccional;
- oportunidades, pipeline, forecast y aging;
- campañas, plantillas, audiencias congeladas y simulación;
- cuentas remitentes Gmail tenant-scoped con OAuth offline y refresh token
  AES-256-GCM;
- campañas `SIMULATION|LIVE`, aprobación, confirmación reforzada, programación,
  pausa/reanudación/cancelación y resultados por destinatario;
- Gmail `users.messages.send` exclusivamente por outbox, MIME
  `multipart/alternative` y baja visible/one-click;
- borradores y enlaces manuales;
- outbox PostgreSQL, retry, dead-letter y requeue;
- inbound fake firmado, replay protection y cuarentena;
- reportes, configuración, etiquetas, cuenta y auditoría;
- frontend React/TypeScript/Vite;
- backend Java 21/Spring Boot/PostgreSQL;
- Docker Compose, Testcontainers, ArchUnit, Vitest y Playwright.

## Seguridad de envío

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

El adaptador Gmail de campañas está implementado pero no conectado a Google
real. No existe un endpoint de envío directo: LIVE requiere campaña aprobada,
audiencia congelada, cuenta conectada, `MESSAGE_SEND`, confirmación
`SEND_LIVE_CAMPAIGN`, límites positivos y todas las guardas. Los valores
versionados bloquean esa combinación.

## Arquitectura

### Backend

- Java 21;
- Spring Boot 4.1;
- Spring Security;
- PostgreSQL 17;
- Flyway V1–V14;
- Hibernate validate;
- OpenAPI y Problem Details;
- Maven Wrapper;
- Testcontainers y ArchUnit.

### Frontend

- React;
- TypeScript strict;
- Vite;
- Vitest;
- Playwright;
- CSS y componentes propios.

### Infraestructura

- Dockerfiles multi-stage;
- Docker Compose;
- runtimes no-root;
- PostgreSQL privado en el perfil productivo local;
- scripts PowerShell/Bash;
- GitHub Actions.

## Obtener o actualizar `main`

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
```

Checkout existente:

```bash
git status --short
git branch --show-current
git remote -v
git fetch origin
git switch main
git pull --ff-only origin main
git rev-parse HEAD
```

Detenerse ante cambios locales no relacionados, conflicto o divergencia.

## Validación integral

El cierre funcional del hardening ya fue ejecutado dos veces sobre el mismo commit. Esta actualización es exclusivamente documental y no modifica código, configuración, dependencias, contenedores, migraciones ni pruebas; por ello no repite Maven, npm, Docker, Playwright ni los análisis ya cerrados.

Para futuros cambios funcionales continúan disponibles:

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
bash scripts/validate-complete-crm.sh
```

Todo nuevo cambio funcional debe volver a generar evidencia sobre su SHA exacto.

## Documentación canónica

```text
docs/continuity/README.md
docs/status.md
docs/estado-integral-y-roadmap.md
docs/next-step.md
docs/backlog.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/remote-main-hardening-2026-07-24.md
```

## Fuera de autorización actual

- producción real;
- Gmail, SMTP o WhatsApp real;
- secretos o credenciales;
- datos reales en Git, CI o imágenes;
- el XLSX real;
- migraciones destructivas;
- debilitamiento de RBAC, tenant isolation, CSRF, exclusiones o idempotencia.

## VAL-002 — Preflight de `ProductionFrontendPort`

El validador integral comprueba ahora los cuatro puertos antes de iniciar suites costosas:

```text
PostgreSQL
Backend
Frontend
Production frontend
```

Windows conserva compatibilidad con los consumidores de tres puertos y acepta `ProductionFrontendPort` como parámetro opcional en `scripts/check-host-ports.ps1`. `scripts/validate-complete-crm.ps1` lo proporciona siempre.

Unix acepta `--production-frontend-port`, ejecuta el checker Node fail-closed durante el preflight y transmite el valor al smoke productivo.

Cobertura focalizada:

- tres puertos para compatibilidad;
- cuatro puertos libres;
- puertos duplicados;
- listener de loopback ocupado;
- publicación Docker ocupada;
- detección de la demo autorizada en `127.0.0.1:18080` sin detenerla;
- sintaxis PowerShell, Node y Bash;
- repository safety y `git diff --check`.

No se repitieron backend, frontend, builds Docker, migraciones, Playwright ni dependency scans porque VAL-002 modifica exclusivamente el preflight y sus regresiones.

Evidencia:

```text
docs/validation/production-frontend-port-preflight-2026-07-24.md
```

## UX-003 — Limpieza de automatización UX remota

Se eliminó la automatización histórica de una sola ejecución:

```text
.github/remote-ux-trigger
.github/workflows/remote-ux-overhaul.yml
scripts/remote-ux-preflight.py
scripts/remote-ux-overhaul.py
scripts/remote-ux-postfix.py
```

La inspección confirmó:

- el workflow solo se activaba mediante `.github/remote-ux-trigger`;
- exigía una relación histórica fija con `a4d975d5a7a041e492894c78583381a466482459`;
- la guarda ya no coincide con la historia actual de `main`;
- los tres scripts Python solo eran consumidos por ese workflow;
- ningún otro workflow, script, configuración o ruta funcional los consumía;
- el propio workflow intentaba eliminar los cinco archivos después de publicar su resultado.

No se modificaron backend, frontend, base de datos, dependencias, Docker, migraciones ni políticas de envío.

Evidencia:

```text
docs/validation/ux-003-remote-automation-cleanup-2026-07-26.md
```

## UX-004 — Métricas tenant-wide del dashboard

El resumen principal obtiene ahora los conteos globales desde:

```text
GET /api/v1/prospects/metrics
```

La consulta:

- exige `PROSPECT_READ`;
- filtra por la organización autenticada;
- excluye prospectos archivados;
- cuenta como interés `INTERESTED`, `QUALIFIED`, `TRIAL_ACTIVE`, `QUOTED` y `NEGOTIATION`;
- cuenta como bloqueo `NOT contact_eligible`;
- no recorre ni concatena páginas de prospectos.

La regresión PostgreSQL crea 105 prospectos con prefijo único, comprueba que la primera página contiene 100 y confirma que la métrica agrega los 105 registros. También se valida acceso de `VIEWER`, aislamiento entre organizaciones y el contrato frontend.

Evidencia:

```text
docs/validation/ux-004-tenant-wide-dashboard-metrics-2026-07-26.md
```
