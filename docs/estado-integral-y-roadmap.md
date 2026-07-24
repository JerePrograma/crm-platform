# Estado integral y roadmap de Gestudio CRM

Actualizado: 2026-07-24

Este documento resume únicamente el contenido publicado y la evidencia verificable. Los trees locales, patches no disponibles y transcripts sin salida estructurada no se tratan como implementación remota.

## 1. Veredicto ejecutivo

Gestudio CRM mantiene el baseline funcional publicado para demostración, evaluación interna y operación comercial segura sin envíos reales.

```text
repositorio: JerePrograma/crm-platform
rama canónica: main
baseline funcional: 83e181ce614f145bbfe141cc7603c3042569be51
continuidad documental inicial: f25051884b7aadd5837286dedd9ae0eee899cb5a
producción: NOT_AUTHORIZED / NOT_DEPLOYED
comunicaciones reales: DISABLED_BY_POLICY
```

La consolidación remota del 24 de julio añadió hardening de validación sobre `main`: parseo exacto de `.Config.Env`, self-tests y alineación Windows/Unix. Esa corrección está implementada, pero no puede declararse funcionalmente cerrada hasta ejecutar PowerShell, Docker y dos validaciones integrales limpias sobre el mismo commit.

## 2. Distinción entre producto y repositorio

Gestudio es el producto para academias, escuelas y estudios con alumnos, clases, cuotas, pagos, asistencia y operación administrativa.

Este repositorio contiene el CRM comercial utilizado para prospectar, calificar y acompañar instituciones potencialmente interesadas en Gestudio. Administra prospectos, contactos, actividades, importaciones, oportunidades, campañas simuladas, outbox, inbound de prueba, reportes, auditoría, usuarios y configuración.

## 3. Arquitectura publicada

### Backend

- Java 21;
- Spring Boot 4.1;
- PostgreSQL 17;
- Flyway V1–V13;
- Spring Security, sesión HttpOnly same-origin y CSRF;
- RBAC y tenant isolation;
- OpenAPI, Problem Details, auditoría JSONB y Micrometer;
- Maven Verify, Testcontainers y ArchUnit.

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
- runtimes no-root y perfil productivo local endurecido;
- PostgreSQL privado en el perfil productivo;
- validadores PowerShell y Bash;
- GitHub Actions para gates versionados.

## 4. Capacidades funcionales publicadas

El baseline funcional conserva:

- organizaciones, usuarios, roles, permisos y sesiones;
- aislamiento por organización y auditoría;
- instituciones, prospectos, contactos y canales;
- exclusiones dominantes y contactabilidad;
- búsqueda, filtros, orden, paginación y CSV seguro;
- notas, actividades, tareas y timeline;
- importaciones CSV/XLSX sintéticas con preview e idempotencia;
- deduplicación y resolución transaccional;
- oportunidades, pipeline, forecast y aging;
- campañas, plantillas, audiencias congeladas y simulación;
- borradores y enlaces manuales;
- outbox, retry, dead-letter y requeue;
- inbound fake firmado, replay protection y cuarentena;
- reporting, settings, etiquetas, cuenta y auditoría;
- frontend administrativo en español y responsive.

## 5. Seguridad innegociable

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

Además:

- no existe autorización de despliegue;
- no se conectan Gmail, SMTP ni WhatsApp Cloud;
- no se versionan secretos ni `.env`;
- no se incorporan datos reales, dumps ni el XLSX real;
- las exclusiones dominan cualquier intención de contacto;
- RBAC, tenant isolation, CSRF e idempotencia no se relajan.

## 6. Hardening remoto del validador

### Problemas encontrados

1. `scripts/validate-complete-crm.ps1` y `.sh` exigían `feat/complete-crm-platform`, aunque `AGENTS.md` define `main` como rama única.
2. Los scripts comprobaban `.Config.Env` mediante regex o `grep` sobre el string JSON completo.
3. El smoke productivo no exigía de forma exacta los dos modos de provider en ambos sistemas operativos.

### Solución implementada

- parser PowerShell con `ConvertFrom-Json` y raíz array obligatoria;
- parser Node para Unix con `JSON.parse` y `Array.isArray`;
- membresía exacta de las siete guardas;
- mensajes de faltantes sin imprimir el entorno completo;
- regresiones para presencia, ausencia, valor inseguro, JSON inválido y líneas vacías;
- validadores integrales alineados con `main`;
- self-tests incorporados al gate de scripts.

Evidencia:

```text
docs/validation/remote-main-hardening-2026-07-24.md
```

## 7. Estado de validación

| Área | Estado |
|---|---|
| baseline histórico SEG-000–SEG-011 | EXECUTED_PASS histórico |
| UX/contactabilidad publicados | EXECUTED_PASS histórico |
| inspección del estado remoto | EXECUTED_PASS |
| comparación `83e181c...f250518` | EXECUTED_PASS |
| self-test Node del parser | EXECUTED_PASS |
| self-test PowerShell 5.1 | IMPLEMENTED_NOT_RUN |
| `productionProfileSmoke` tras el fix | IMPLEMENTED_NOT_RUN |
| `finalTreeClean` tras el fix | IMPLEMENTED_NOT_RUN |
| dos validaciones integrales del nuevo HEAD | IMPLEMENTED_NOT_RUN |
| CI del nuevo HEAD | PENDING_VERIFICATION |

No se reutiliza la evidencia histórica para declarar PASS sobre archivos de validación modificados.

## 8. Candidato post-hardening local

La documentación previa registra:

```text
v6: e3a9728e717b7c8a4d92f9fab31f709bf5d66464
locators: 24df4c7f26ffde0f044f681f9130fa254f15debd
foco inicial: fa8c15172dfa9a0cfa5cbd00f7aab42733d516ba
foco explícito: 9e058d7044415b80af554ab8ae4fe3170585b1c9
```

Esos valores son trees históricos, no commits accesibles en GitHub. No se localizaron ramas o PRs que permitan integrarlos. Por tanto:

```text
REMOTE_AVAILABILITY=NOT_FOUND
INTEGRATION_STATUS=NOT_INTEGRATED
```

No se afirma que estén publicados:

- métricas tenant-wide del dashboard;
- eliminación de automatización remota obsoleta;
- paginación backend adicional de importaciones;
- paginación de outbox/inbound;
- drawer móvil;
- modularización adicional;
- Playwright Firefox/WebKit;
- corrección histórica de retorno de foco explícito.

## 9. Roadmap verificable

### Gate inmediato

1. ejecutar self-test PowerShell 5.1;
2. ejecutar `scripts/validate-complete-crm.ps1` dos veces sobre el mismo commit;
3. exigir `productionProfileSmoke=FUNCTIONAL_PASS`;
4. exigir `finalTreeClean=FUNCTIONAL_PASS`;
5. ejecutar repository safety y `git diff --check`;
6. comprobar CI del SHA exacto.

### Recuperación opcional del candidato

1. localizar los cuatro patches y su manifiesto;
2. verificar SHA-256;
3. ejecutar `git apply --check` sobre un clon temporal del `main` actual;
4. portar solo cambios compatibles y revisados;
5. validar nuevamente dos veces.

### Desarrollo incremental si no hay patches

1. eliminar automatización UX remota obsoleta;
2. corregir métricas tenant-wide;
3. paginar importaciones en backend;
4. paginar outbox/inbound;
5. ejecutar multibrowser y corregir foco reproducible;
6. evaluar drawer móvil;
7. modularizar gradualmente `App.tsx`;
8. auditar WCAG 2.1 AA y escala.

## 10. Fronteras externas

| Capacidad | Estado |
|---|---|
| Gmail/SMTP real | IMPLEMENTED_NOT_CONNECTED |
| WhatsApp Cloud real | IMPLEMENTED_NOT_CONNECTED |
| XLSX real | BLOCKED_EXTERNAL_FILE |
| host Unix integral | IMPLEMENTED_NOT_RUN |
| infraestructura productiva | NOT_PROVISIONED |
| despliegue | NOT_AUTHORIZED |
| envíos reales | DISABLED_BY_POLICY |

## 11. Fuentes canónicas

```text
docs/status.md
docs/next-step.md
docs/backlog.md
docs/continuity/README.md
docs/continuity/validation-release-state-2026-07-24.md
docs/validation/COMPLETE-CRM-matrix.md
docs/validation/remote-main-hardening-2026-07-24.md
```
