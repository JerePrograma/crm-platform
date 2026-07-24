# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad, aislamiento por organización y controles fail-closed.

## Estado actual

`main` es la única fuente canónica.

```text
baseline funcional publicado: 83e181ce614f145bbfe141cc7603c3042569be51
continuidad documental inicial: f25051884b7aadd5837286dedd9ae0eee899cb5a
parser exacto de .Config.Env: IMPLEMENTED_NOT_FULLY_VALIDATED
candidato histórico 9e058d...: NOT_AVAILABLE_REMOTELY / NOT_INTEGRATED
producción: NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
```

El CRM conserva el cierre funcional histórico para demostración, evaluación interna y operación comercial segura sin envíos reales. No está autorizado para producción ni para conectar proveedores externos.

## Consolidación remota del 24 de julio de 2026

Se verificó el estado real de GitHub y se corrigieron defectos objetivos de los validadores canónicos:

- Windows y Unix ahora exigen la rama `main`;
- el JSON de `docker inspect ... {{json .Config.Env}}` se parsea como array real;
- las entradas se comparan por membresía exacta;
- JSON vacío, inválido o con raíz no-array falla;
- se comprueban las siete guardas, incluidos los modos de providers;
- existen self-tests PowerShell y Node;
- el self-test Node fue ejecutado con Node 22 y pasó.

Pendiente antes de declarar cierre:

- PowerShell 5.1;
- Docker `productionProfileSmoke`;
- `finalTreeClean`;
- repository safety;
- `git diff --check`;
- dos validaciones integrales limpias sobre el mismo commit;
- comprobación de CI del SHA exacto.

Evidencia:

```text
docs/validation/remote-main-hardening-2026-07-24.md
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

Los adaptadores Gmail/SMTP/WhatsApp permanecen desconectados. La operación autorizada se limita a simulaciones, borradores y enlaces manuales.

## Arquitectura

### Backend

- Java 21;
- Spring Boot 4.1;
- Spring Security;
- PostgreSQL 17;
- Flyway V1–V13;
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

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-container-env-assertions.ps1
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
node scripts/test-container-env-assertions.js
bash scripts/validate-complete-crm.sh
```

El cierre exige dos corridas limpias consecutivas sobre el mismo commit.

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
