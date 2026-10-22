# Scalaris CRM

CRM local para el trabajo de Marketing de **Scalaris**: servicio técnico, desarrollo y venta a pedido. Centraliza personas y negocios, contactos, consultas, seguimientos, oportunidades, etiquetas, importaciones y campañas preparadas o simuladas. PostgreSQL conserva los datos y el historial comercial.

La edición Scalaris utiliza una organización propia y una cuenta de Marketing con permisos operativos separados de administración. La referencia vigente es **11 7278-9289**, con cobertura en **Zona Oeste, CABA y Mar del Plata**. El retiro y la entrega se coordinan según servicio, localidad y caso.

## Abrir el CRM en Windows

La [guía de instalación y uso para Marketing](docs/scalaris-marketing-local.md) explica el primer ingreso, el trabajo diario, los respaldos, la recuperación y las actualizaciones.

| Modalidad | Abrir | Detener | Respaldar | Requisito |
|---|---|---|---|---|
| Paquete Windows | Extraer el ZIP completo y abrir `AbrirCRMScalarisPortable.cmd` | `DetenerCRMScalarisPortable.cmd` | `RespaldarCRMScalarisPortable.cmd` | Windows de 64 bits; Java, Node y PostgreSQL incluidos |
| Repositorio con Docker | `AbrirCRMScalaris.cmd` | `DetenerCRMScalaris.cmd` | `RespaldarCRMScalaris.cmd` | Docker Desktop con contenedores Linux |

Los archivos se abren con doble clic. La primera apertura prepara la configuración y las cuentas locales, espera que el CRM esté disponible y abre el navegador. Las claves iniciales se guardan en un archivo privado fuera del proyecto. Cerrar el navegador conserva los datos; los archivos de detener cierran los servicios sin borrar la base.

El paquete Windows se genera desde los binarios compilados mediante `scripts/package-scalaris-portable.ps1`; los wrappers con sufijo `Portable` están dentro de esa entrega. Su operación se implementa en `scripts/scalaris-portable.ps1` y `scripts/scalaris-web-server.mjs`. El camino Docker utiliza `scripts/scalaris-local.ps1` y `docker-compose.scalaris.yml`.

## Preparación productiva — 2026-10-22

La misión [SCALARIS-DEPLOY-001](docs/segments/SCALARIS-DEPLOY-001.md) corrige seguridad Java 21, agenda/fichas paginadas, Scalaris/Marketing productivo, proxy/importaciones y respaldos. La fuente funcional `4b556a5` aprobó dos integrales limpias consecutivas, 23/23 fases cada una; los scans y el export verifican las imágenes productivas exactas. Los resultados y hashes están en [la validación de esta entrega](docs/validation/scalaris-production-readiness-2026-10-22.md); este cierre documental se registra como descendiente separado.

El destino elegido es el VPS Hostinger con `crm.scalaris.com.ar` y la copia externa será cifrada en esta PC. El helper está instalado y la tarea 04:15 BA preparada sin registrar; aún no hay copia real. Publicación, CI actual de 23 jobs y puesta en servicio siguen pendientes de autorización final; [la guía Hostinger](docs/production/hostinger.md) describe el procedimiento. La instalación portable anterior conserva su versión y las comunicaciones reales siguen bloqueadas.

## Historial de la edición Scalaris — 2026-10-14

`main` sigue siendo la rama canónica. **SCALARIS-001 tiene validación funcional local aprobada** y su implementación se publicó por fast-forward en [331b3e7](https://github.com/JerePrograma/crm-platform/commit/331b3e7be5ad52b93b285a0ff9654abedce79b7d). HEAD, `origin/main` y `ls-remote` coincidieron, con divergencia `0 0`. La [CI 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532) tiene un fallo real `FAILED_DEPENDENCY_SCAN`: Grype detectó vulnerabilidades high/critical en dependencias Java y la imagen runtime. Resultado final: 21/22 jobs success y solo dependency-scan failure; no hay PASS integral. El registro es [publicación GitHub Scalaris](docs/validation/scalaris-github-publication-2026-10-14.md); este cierre documental registra los resultados post-push.

| Control de la edición Scalaris | Estado |
|---|---|
| Backend Java 21: unitarias e integración focalizada | `EXECUTED_PASS`, 62 unitarias y 6 de integración; 68 en total |
| Launcher Docker: regresión PowerShell 5.1 | `EXECUTED_PASS`, 55 aserciones; Docker simulado para respaldo |
| Docker real, Compose, imágenes y servicios | `BLOCKED_EXTERNAL`: Docker no está instalado en este equipo |
| Paquete Windows: Java 21, PostgreSQL 18 y Node 24 | `EXECUTED_PASS`, 18 comprobaciones reales en PowerShell 5.1, incluida recuperación en base nueva |
| Frontend | `EXECUTED_PASS`: 16 unitarias, typecheck, build y audit sin vulnerabilidades; E2E conjunto 4/4, sin omitidos ni flaky |
| Manual de Marketing | Revisado visualmente en desktop 1440 px y móviles 390/320 px; sin desborde horizontal |
| ZIP de entrega definitivo | `EXECUTED_PASS`: manifiesto/checksum revisados, 2.243 entradas y ningún archivo privado |
| Instalación de uso | Activa en `http://127.0.0.1:8787`, perfil/rol correctos, 0 prospectos, 18 etiquetas y 7 plantillas |
| Dos validaciones integrales limpias de esta edición | `BLOCKED_EXTERNAL`: requieren Docker; el resultado histórico no cubre estos cambios |
| Publicación GitHub de esta edición | `PUBLISHED_FAST_FORWARD`, implementación `331b3e7`; 64 archivos, +4.126/-252 |
| CI de esta edición | `FAILED_DEPENDENCY_SCAN`; Grype exit 2, snapshot 21/22 jobs success; solo dependency-scan failure |
| Producción de esta edición | No desplegada; permanece fuera de autorización |
| Comunicaciones reales y datos de contactos reales | Deshabilitadas; no cargados en esta misión |

El detalle se mantiene en [SCALARIS-001](docs/segments/SCALARIS-001.md), [estado actual](docs/status.md) y [matriz de validación](docs/validation/COMPLETE-CRM-matrix.md). Un script implementado, un respaldo legible o una prueba unitaria aprobada no equivalen por sí solos a un recorrido operativo completo.

## Trabajo diario de Marketing

- Organizar la jornada desde el resumen y la agenda de seguimientos.
- Registrar particulares, comercios, empresas y profesionales, conservando el origen de cada consulta.
- Clasificar intereses por servicio, localidad y tipo de cliente mediante etiquetas.
- Registrar notas, actividades y próximas acciones; actualizar oportunidades y presupuestos.
- Importar CSV/XLSX después de revisar la vista previa, los excluidos y los duplicados.
- Preparar plantillas y audiencias con las referencias de Scalaris; comprobarlas mediante simulación.
- Respetar bajas y exclusiones desde **Exclusiones**.

La cuenta de Marketing opera el CRM sin administrar usuarios ni habilitar envíos reales. La cuenta de administración queda para el responsable. El bootstrap prepara cuentas, etiquetas y plantillas propias del negocio; la entrega no agrega prospectos reales.

## Datos y respaldos locales

| Modalidad | Carpeta privada | Persistencia |
|---|---|---|
| Paquete Windows | `%LOCALAPPDATA%\ScalarisCRM\portable` | PostgreSQL local y configuración propia del usuario de Windows |
| Docker | `%LOCALAPPDATA%\ScalarisCRM` | Proyecto `scalaris-crm-local` y volumen PostgreSQL independiente |

El puerto habitual de la pantalla es `8787`, publicado únicamente en `127.0.0.1`; la primera configuración puede elegir otro puerto libre. Consultar la dirección efectiva en `Acceso-inicial.txt`.

Los respaldos y sus checksums contienen información privada y quedan fuera de Git. Conservar también la configuración de la instalación y la versión anterior del paquete hasta comprobar una actualización. La [guía operativa](docs/scalaris-marketing-local.md) distingue la recuperación Windows de la modalidad Docker.

El recorrido portable real comprobó primera apertura, claves/permisos privados, rol Marketing, servicios en loopback, 18 etiquetas y 7 plantillas, un prospecto ficticio que persistió al reabrir, checksum, recuperación en una base nueva y conservación de la anterior. El conteo de mensajes `SENT`, `DELIVERED` o `READ` permaneció en cero. La evidencia local se guarda fuera de Git en `validation-output/scalaris-portable-latest.json`.

El ZIP definitivo quedó revisado y la instalación de uso inicia sin prospectos de prueba. Los entornos QA están detenidos. La validación específica conserva la ruta/checksum del artefacto; la evidencia técnica local está en `validation-output/scalaris-release-latest.json` y `validation-output/scalaris-installed-local.json`. El ZIP y la instalación corresponden a la validación local anterior: la fuente publicada incorpora después la corrección del rol predeterminado y del proxy ante respuestas interrumpidas, comprobadas con 5/5 recorridos de navegador y 2/2 pruebas Node. Esas copias no se actualizaron automáticamente ni heredan el nuevo SHA. El siguiente paso de negocio es piloto local, sin importar contactos reales automáticamente.

## Configuración y continuidad

El perfil `CRM_BUSINESS_PROFILE=SCALARIS` selecciona el negocio local. El comportamiento anterior se conserva como perfil `LEGACY` para compatibilidad. Backend Java 21/Spring Boot 4.1 y Flyway V1–V14 permanecen; Docker utiliza PostgreSQL 17 y el paquete Windows se prepara con PostgreSQL 18 y Node 24.

Leer [AGENTS.md](AGENTS.md), el [índice de continuidad](docs/continuity/README.md), [estado](docs/status.md), [próximo paso](docs/next-step.md) y [backlog](docs/backlog.md) antes de modificar o actualizar el proyecto. La implementación está publicada; este cierre registra la publicación y CI; resta reparar el gate de dependencias en una misión posterior. Producción, PR y comunicaciones reales permanecen fuera de alcance. La agenda con muchas tareas cerradas y las actualizaciones Java/imagen pendientes se conservan en el backlog; no se modificaron POM/runtime para ocultar el fallo.

Las guardas de envío real permanecen cerradas. Los caminos locales fijan `SENDING_ENABLED=false`, `SENDING_DRY_RUN=true`, límite cero, kill switch activo, red real bloqueada, email `NOOP`, WhatsApp `DEEPLINK_ONLY` y workers deshabilitados. Preparar o simular un mensaje no lo envía. No se conectaron proveedores reales ni se cargaron contactos operativos en esta misión.

## Historial de la plataforma Gestudio — hasta 2026-07-30

El registro siguiente conserva los resultados históricos de la plataforma. El commit **`d6bfe64283cd3bab8dce441e4b01e348f9a55d5e`** se publicó por fast-forward `9995b3e..d6bfe64`, pasó dos validaciones integrales limpias y la CI **`30530515993`**, con **22/22** jobs verdes. Estos resultados corresponden al baseline anterior y a Google falso; no cubren los cambios Scalaris de octubre. Las instrucciones históricas siguientes se consultan junto con el próximo paso actual.

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad, aislamiento por organización y controles fail-closed.

## Estado actual

`main` es la única fuente canónica.

```text
base sincronizada para SEG-001 Gmail: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
OAuth/Gmail/campaña LIVE contra proveedor falso: EXECUTED_PASS
backend clean verify: EXECUTED_PASS, 112/112
frontend install/typecheck/unit/build: EXECUTED_PASS, 13/13
validación integral local: 2/2 FUNCTIONAL_PASS, commit d6bfe642
publicación SEG-001 Gmail: PUBLISHED_FAST_FORWARD, origin/main d6bfe642
CI del commit publicado: EXECUTED_PASS, run 30530515993, 22/22
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
