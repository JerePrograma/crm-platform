# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

`main` es la fuente canónica.

```text
segmentos SEG-000–SEG-011: COMPLETE
experiencia integral de operador: COMPLETE
sincronización de contactabilidad: COMPLETE
paginación real de prospectos: COMPLETE
último commit funcional: 19732dec9638cd47fee4b39ac41c5968693b5b7a
producción: NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
```

El CRM está validado para demostración, evaluación interna y operación comercial segura sin envíos reales. No está autorizado para producción ni para conectar proveedores externos.

Documentación canónica:

```text
docs/estado-integral-y-roadmap.md
docs/status.md
docs/next-step.md
docs/backlog.md
docs/ux-operador.md
```

## Alcance

- organizaciones, usuarios, roles, permisos y sesiones seguras;
- tenant isolation, CSRF, cookies HttpOnly y auditoría;
- instituciones, prospectos, contactos y canales;
- normalización, exclusiones y contactabilidad;
- búsqueda por institución, ubicación, sitio, contacto, correo, teléfono y etiquetas;
- filtros, orden, paginación y CSV seguro;
- notas, actividades, tareas y timeline;
- importaciones CSV/XLSX con vista previa, confirmación e idempotencia;
- duplicados exactos y ambiguos;
- vinculación, creación independiente, merge, descarte y postergación;
- oportunidades, pipeline, forecast y aging;
- campañas, plantillas, audiencias congeladas y simulación;
- borradores y enlaces manuales;
- outbox PostgreSQL, workers, retry, dead-letter y requeue;
- inbound de prueba firmado, replay protection, cuarentena y asociación;
- reportes, configuración, etiquetas, usuarios y cuenta;
- correlation ID, métricas, health, readiness y liveness;
- backup/restore sintético y perfil productivo local endurecido;
- frontend React/TypeScript/Vite;
- backend Java 21/Spring Boot/PostgreSQL;
- Docker Compose, Testcontainers, ArchUnit, Vitest y Playwright.

## Experiencia de operador

La interfaz principal fue endurecida para uso administrativo y comercial:

- textos visibles en español claro;
- estados técnicos representados mediante etiquetas centralizadas;
- navegación, títulos y acciones humanizados;
- formularios con labels, ayudas y validación;
- errores y mensajes de éxito orientativos;
- estados vacíos y datos faltantes explícitos;
- tablas con filtros, conteos, paginación y scroll controlado;
- ficha de prospecto organizada por revelado progresivo;
- auditoría con resumen principal y datos técnicos expandibles;
- importaciones con resumen, búsqueda, filtros y paginación visual;
- duplicados con comparación y consecuencias de cada acción;
- diálogos accesibles sin `window.prompt` ni `window.confirm`;
- foco visible, teclado, Escape, focus trap y retorno de foco;
- responsive probado en móvil.

Detalles:

```text
docs/ux-operador.md
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
```

## Contactabilidad

- sin correo, teléfono ni WhatsApp utilizable, el prospecto queda pendiente de completar datos;
- un sitio web no convierte por sí solo al prospecto en contactable;
- las exclusiones siguen siendo dominantes;
- crear o eliminar contactos recalcula la condición;
- agregar, modificar o eliminar canales recalcula la condición;
- vista previa y ejecución de importación aplican la misma regla.

## Duplicados

`CREATE_SEPARATE` y `MARK_NOT_DUPLICATE` conservan únicamente campos importados conocidos y validados: institución, ubicación, categoría, sitio, fuente, evidencia, prioridad, fecha, correo y teléfono/WhatsApp.

La extracción se implementa en:

```text
backend/src/main/java/com/gestudio/crm/deduplication/SanitizedDuplicateImportData.java
```

No se persisten propiedades arbitrarias desde el JSON crudo.

## Seguridad de envío

Los adaptadores Gmail y WhatsApp existen como contratos aislados para pruebas y conexión futura, pero no están conectados. La API no ofrece endpoint de envío real.

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

PostgreSQL contiene además un kill switch persistente. La operación disponible solo crea simulaciones, borradores o enlaces manuales.

## Arquitectura

### Backend

- Java 21;
- Spring Boot 4.1;
- Spring Security;
- PostgreSQL 17;
- Flyway V1–V13;
- Hibernate validate;
- OpenAPI y RFC 7807;
- Maven Wrapper;
- Testcontainers y ArchUnit.

### Frontend

- React;
- TypeScript strict;
- Vite;
- CSS propio con variables y componentes reutilizables;
- Vitest;
- Playwright Chromium.

### Infraestructura

- Dockerfiles multi-stage;
- Docker Compose perfiles `app` y `smoke`;
- Nginx y proxy same-origin;
- health checks encadenados;
- scripts PowerShell y Bash;
- GitHub Actions.

## Evidencia ejecutada

### Cierre integral de plataforma

```text
run: 29951586239
commit: b904ff37e506f058dab351c2b941e13ee4ed9981
resultado: success, 22/22 jobs
```

### Cierre UX

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

Los cierres UX ejecutaron frontend limpio, TypeScript, Vitest, build, Maven Verify, Docker Compose fail-closed, Playwright, repository safety y `git diff --check`.

## Requisitos recomendados

Para levantar y validar todo:

- Git;
- Docker Desktop o Docker Engine;
- Docker Compose v2;
- PowerShell en Windows o Bash en Linux/macOS.

Docker Desktop debe usar contenedores Linux.

## Obtener o actualizar `main`

Checkout nuevo:

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

## Crear `.env` local

Crear solo si no existe.

PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Bash:

```bash
[ -f .env ] || cp .env.example .env
```

Valores mínimos orientativos para desarrollo local:

```dotenv
POSTGRES_DB=gestudio_crm
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No versionar `.env` ni copiar `.env.example` sobre credenciales locales existentes.

## Validación principal

Frontend:

```bash
cd frontend
npm ci --no-audit --no-fund
npm run typecheck
npm run test:unit
npm run build
cd ..
```

Backend:

```bash
./mvnw -B -f backend/pom.xml verify
```

Seguridad del repositorio:

```bash
bash scripts/check-repository-safety.sh
git diff --check
```

Stack y E2E:

```bash
docker compose --profile app up -d --build --wait
cd frontend
npx playwright install chromium
npm run test:e2e
cd ..
docker compose --profile app down --remove-orphans
```

No usar datos reales ni habilitar red real durante estas validaciones.

## Validadores integrales

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1
```

Unix:

```bash
bash scripts/validate-complete-crm.sh
```

La ejecución integral en host Unix real continúa pendiente; la sintaxis Bash, los jobs Unix de CI y las validaciones contenedorizadas sí pasaron.

## Pendientes principales

1. corregir métricas del dashboard calculadas sobre la página actual;
2. eliminar workflow y scripts remotos UX obsoletos;
3. auditoría manual WCAG 2.1 AA;
4. pruebas de escala con datos sintéticos representativos;
5. Firefox y WebKit;
6. paginación backend de filas de importación para lotes grandes;
7. modularización gradual de `frontend/src/App.tsx`.

Roadmap completo:

```text
docs/next-step.md
docs/backlog.md
```

## Fuera de autorización actual

- producción real;
- Gmail, SMTP o WhatsApp real;
- credenciales de proveedores;
- datos reales en Git, CI o imágenes;
- migraciones destructivas;
- debilitamiento de permisos, exclusiones, idempotencia o tenant isolation.

## Documentación

```text
docs/estado-integral-y-roadmap.md
docs/status.md
docs/next-step.md
docs/backlog.md
docs/ux-operador.md
docs/execution/complete-crm-platform-plan.md
docs/execution/complete-crm-platform-progress.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
```
