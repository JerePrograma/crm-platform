# Matriz de validación integral del CRM

Actualizado: 2026-10-14

Estados permitidos: `NOT_STARTED`, `IN_PROGRESS`, `IMPLEMENTED_NOT_RUN`, `EXECUTED_PASS`,
`EXECUTED_FAIL`, `BLOCKED_EXTERNAL`, `NOT_APPLICABLE`.

## SCALARIS-001 — evidencia de la edición local de octubre

La implementación quedó `PUBLISHED_FAST_FORWARD` en `331b3e7be5ad52b93b285a0ff9654abedce79b7d`, con HEAD/origin/main/ls-remote coincidentes y divergencia `0 0`. CI `FAILED_DEPENDENCY_SCAN`, Grype high/critical real; [run 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532), resultado final 21/22 jobs success; solo dependency-scan failure. Este cierre registra el resultado en [publicación GitHub Scalaris](scalaris-github-publication-2026-10-14.md). ZIP/instalación previos conservan su frontend/servidor y evidencia; la fuente publicada agrega ajustes rol/proxy comprobados con navegador 5/5 y Node 2/2. Los PASS de julio no cubren estos cambios.

| Capacidad | Estado | Evidencia actual y límites |
|---|---|---|
| Perfil/organización Scalaris y cuenta Marketing | EXECUTED_PASS | Bootstrap y rol comprobados en portable real e integración focalizada; sin administración/envío real |
| Referencias, etiquetas y plantillas Scalaris | EXECUTED_PASS | Runtime real: 18 etiquetas y 7 plantillas; sin prospectos reales |
| Backend unitario Java 21 | EXECUTED_PASS | 62 pruebas; no sustituye integración PostgreSQL |
| Backend integración focalizada | EXECUTED_PASS | 6 pruebas de integración; total backend 68, sin sustituir suite histórica completa |
| Frontend typecheck/unit/build/audit | EXECUTED_PASS | 16 unitarias, compilación aprobada, audit sin vulnerabilidades |
| Pantalla/agenda/ayuda Marketing y E2E | EXECUTED_PASS | Corrida conjunta final 4/4: integral, responsive, Gmail desconectado y Marketing Scalaris; cero omitidos/inesperados/flaky, 2026-10-14 13:03:29 Argentina |
| Launcher Docker PowerShell 5.1 | EXECUTED_PASS | 55 aserciones: ACL, claves persistentes, carpetas seguras, puertos y respaldo simulado |
| Mensaje Docker no instalado | EXECUTED_PASS | Falla explicativa sin instalación automática ni credenciales nuevas |
| Compose/build/health Docker Scalaris | BLOCKED_EXTERNAL | Docker no instalado en el host actual |
| Respaldo/restauración Docker Scalaris | BLOCKED_EXTERNAL | Orquestación probada con mock; no hay PostgreSQL Docker real |
| Paquete Windows Java 21/PostgreSQL 18/Node 24 | EXECUTED_PASS | 18 comprobaciones reales PowerShell 5.1; `validation-output/scalaris-portable-latest.json` |
| Persistencia y parada portable | EXECUTED_PASS | Prospecto ficticio conservado al reabrir; parada conserva datos y apunta a procesos propios |
| Respaldo/recuperación portable | EXECUTED_PASS | Checksum, recuperación en base nueva, ID/nombre preservados y base anterior existente |
| Cero estados enviados portable | EXECUTED_PASS | `SENT|DELIVERED|READ=0` en la base restaurada |
| Manual visual desktop/móvil | EXECUTED_PASS | Render e inspección individual a 1440/390/320 px; sin overflow tras corrección focal CSS |
| ZIP final, manifiesto y checksum | EXECUTED_PASS | Manifiesto/checksum coincidentes, 2.243 entradas y cero archivos privados; evidencia `scalaris-release-latest.json` |
| Instalación preparada para uso | EXECUTED_PASS | Loopback 8787, perfil/rol correctos, 0 prospectos, 18 etiquetas y 7 plantillas; evidencia `scalaris-installed-local.json`; QA detenido |
| Validador integral repetido de esta edición | BLOCKED_EXTERNAL | Docker no instalado; sin dos corridas limpias sobre el contenido final Scalaris |
| Publicación GitHub de esta edición | EXECUTED_PASS | `PUBLISHED_FAST_FORWARD`, SHA `331b3e7`, 64 archivos +4.126/-252, HEAD/origin/main/ls-remote iguales y divergencia `0 0` |
| CI de esta edición | EXECUTED_FAIL | `FAILED_DEPENDENCY_SCAN`; Grype exit 2 high/critical Java/runtime, npm audit cero y build Docker PASS; resultado final 21/22 jobs success; solo dependency-scan failure |
| Remediación de dependencias/imagen | NOT_STARTED | Spring Web MVC, Tomcat, Jackson y runtime Chainguard pendientes en backlog; POM/runtime sin cambios |
| Agenda con muchas tareas cerradas | NOT_STARTED | Límite 200 aplicado antes del filtro de pendientes; `SCALARIS-AGENDA-001`, sin corrección |
| Contactos reales y comunicaciones reales | NOT_APPLICABLE | No importados ni enviados en esta misión; guardas cerradas |
| Producción de esta edición | NOT_APPLICABLE | No desplegada |

Consultas canónicas: `docs/segments/SCALARIS-001.md`, `docs/status.md` y `docs/scalaris-marketing-local.md`. Los nuevos resultados se incorporan con evidencia ejecutada; implementado no equivale a PASS funcional.

## Historial validado — baseline publicado hasta 2026-07-30

El commit `d6bfe64283cd3bab8dce441e4b01e348f9a55d5e` tuvo dos corridas integrales limpias y CI `30530515993`, 22/22. La tabla siguiente conserva esos resultados y sus antecedentes sobre sus commits originales.

| Capacidad | Estado | Evidencia actual |
|---|---|---|
| baseline SEG-001 Windows | EXECUTED_PASS | validador 2026-07-21, commit `7db7e4c`, 29/29 |
| inventario/gap analysis | EXECUTED_PASS | plan integral y análisis de código completo |
| documentación viva | EXECUTED_PASS | fuentes canónicas reconciliadas con dos corridas y frontera externa |
| organización/tenant | EXECUTED_PASS | V6 desde vacío y V1-V5; FK/índices/backfill |
| usuarios persistentes | EXECUTED_PASS | hash, alta, activación y cambio de contraseña |
| roles y permisos | EXECUTED_PASS | ADMIN/MANAGER/SALES/VIEWER y 18 permisos |
| sesión cookie/CSRF | EXECUTED_PASS | tests MockMvc y smoke host/contenedor |
| login/logout/bloqueo | EXECUTED_PASS | 10/10 security integration tests |
| aislamiento entre tenants | EXECUTED_PASS | segundo tenant persistente devuelve 404 |
| bootstrap admin seguro | EXECUTED_PASS | solo sin admin y con variables completas |
| auditoría de identidad | EXECUTED_PASS | login/fallo/logout/usuario/contraseña |
| prospect CRUD | EXECUTED_PASS | integración PostgreSQL y UI real |
| contactos CRUD | EXECUTED_PASS | create/update/delete y normalización integrados |
| archivo/restauración | EXECUTED_PASS | estado previo y elegibilidad preservados |
| búsqueda/filtros/paginación | EXECUTED_PASS | JDBC paginado, allow-list y prueba integrada |
| exportación CSV segura | EXECUTED_PASS | prueba de neutralización de fórmula |
| optimistic locking 409 | EXECUTED_PASS | conflicto integrado y Problem Details |
| ciclo comercial | EXECUTED_PASS | máquina explícita y flujo integrado |
| historial de estados | EXECUTED_PASS | persistencia y timeline verificados |
| notas y sanitización | EXECUTED_PASS | script escapado en integración |
| actividades | EXECUTED_PASS | actividad outbound y metadata JSONB |
| tareas/seguimientos | EXECUTED_PASS | create/complete/próxima acción y UI |
| timeline | EXECUTED_PASS | paginado combinado y recorrido Playwright |
| importación sintética | EXECUTED_PASS | SEG-001 100 prospectos/16 exclusiones |
| preview/exclusiones | EXECUTED_PASS | SEG-001 integración |
| resolución de duplicados | EXECUTED_PASS | V8, seis acciones, API/UI y Playwright |
| merge transaccional | EXECUTED_PASS | 3/3 Testcontainers: referencias, idempotencia y rollback |
| oportunidades | EXECUTED_PASS | V9, 3/3 integración, API/UI y Playwright |
| pipeline/forecast | EXECUTED_PASS | kanban, métricas exactas, aging y cierre WON |
| campañas | EXECUTED_PASS | V10, estados, API/UI y Playwright |
| audiencias congeladas | EXECUTED_PASS | filtros SQL, decisiones persistidas y E2E |
| plantillas versionadas | EXECUTED_PASS | versión inmutable, API/UI y preview |
| renderer seguro | EXECUTED_PASS | allow-list, escaping e inyección rechazada |
| secuencias declarativas | EXECUTED_PASS | tipos/condiciones limitados, API/test y UI |
| aprobación/simulación | EXECUTED_PASS | RBAC, idempotencia, fake activity y Playwright |
| email no-op | EXECUTED_PASS | provider seleccionado, integración y UI |
| email fake | EXECUTED_PASS | simulación persistida, actividad y recorrido visual |
| Gmail OAuth/cuentas remitentes | EXECUTED_PASS | Authorization Code offline, AES-GCM, tenant/RBAC y ciclo reconnect/revoke contra fake |
| Gmail campaña LIVE adapter | EXECUTED_PASS | `users.messages.send`, MIME, errores y aceptación individual contra Google falso |
| Google real conectado | BLOCKED_EXTERNAL | implementación disponible; sin credenciales, tráfico ni destinatarios reales |
| unsubscribe one-click | EXECUTED_PASS | token hash, GET/POST, rate limit, exclusión y cancelación de pendientes/seguimientos |
| campaña LIVE/outbox | EXECUTED_PASS | aprobación, confirmación, límites, lease, retry/dead/ambiguous y Playwright fake |
| WhatsApp no-op | EXECUTED_PASS | NOOP seleccionado por DEEPLINK_ONLY |
| WhatsApp fake | EXECUTED_PASS | contract/integration fake sin red |
| WhatsApp real adapter | BLOCKED_EXTERNAL | contrato implementado; Meta/credenciales no conectados |
| borradores y enlaces manuales | EXECUTED_PASS | API/UI mailto y wa.me sin apertura automática |
| message policy tenant/exclusión | EXECUTED_PASS | PostgreSQL, consentimiento y bloqueo antes de persistir cuerpo |
| inexistencia de endpoint send | EXECUTED_PASS | MockMvc administrador autenticado obtiene 404 |
| policy/kill switch persistente | EXECUTED_PASS | ambiente + DB verificados antes de simular |
| outbox | EXECUTED_PASS | V12, publicación transaccional, UI/API y PostgreSQL real |
| workers/retry/dead-letter | EXECUTED_PASS | SKIP LOCKED, lease, retry/dead/requeue y doble worker |
| idempotencia asíncrona | EXECUTED_PASS | scope tenant, request hash, repetición y colisión 409 |
| inbound/webhook fake | EXECUTED_PASS | HMAC/replay/quarantine/asociación/dominio y E2E |
| frontend integral | EXECUTED_PASS | navegación completa, permisos, desktop/mobile y 2/2 Playwright |
| frontend E2E | EXECUTED_PASS | suite CRM 2/2 histórica y suite Gmail LIVE sintética 1/1 |
| dashboard/reportes | EXECUTED_PASS | agregaciones PostgreSQL tenant/fecha/timezone y monedas separadas |
| métricas tenant-wide del resumen | EXECUTED_PASS | endpoint prospect metrics, 105 registros con página de 100, VIEWER y aislamiento tenant |
| búsqueda PostgreSQL | EXECUTED_PASS | institución/contacto/canal/localidad/website/notas/tags, allow-list y tenant test |
| configuración/etiquetas | EXECUTED_PASS | settings hard-blocked, tags CRUD/bulk/audit/tenant |
| threat model | EXECUTED_PASS | `docs/security.md`, fronteras/amenazas/mitigaciones/riesgos |
| controles de seguridad | EXECUTED_PASS | security focal 12/12, CSRF/headers/RBAC/tenant/webhook/send absent |
| observabilidad integral | EXECUTED_PASS | correlation/request ID, actor/tenant MDC, Micrometer/probes protegidos |
| preview XLSX real | BLOCKED_EXTERNAL | ubicaciones conocidas ausentes; archivo no abierto ni copiado |
| performance sintética | EXECUTED_PASS | parser 100/1k/10k y evidencia operativa/query plan acotada |
| accesibilidad responsive | EXECUTED_PASS | teclado/mobile E2E y corrección sidebar; no certificación WCAG |
| backup/restore | EXECUTED_PASS | pg_dump/checksum/metadata y restore drill aislado sintético |
| perfil productivo validado localmente | EXECUTED_PASS | non-root/read-only/private network/health, sending bloqueado |
| preflight `ProductionFrontendPort` | EXECUTED_PASS | Windows y Unix fallan temprano ante puerto ocupado; regresiones libres/ocupadas; demo 18080 preservada |
| automatización UX remota histórica | EXECUTED_PASS | trigger, workflow y tres scripts eliminados tras comprobar cero consumidores operativos |
| dependency scan | EXECUTED_PASS | npm audit High y Grype High/Critical; imagen final sin hallazgos |
| CI integral | EXECUTED_PASS | run `29951586239`, commit `b904ff3`, push a `main`, 22/22 jobs success |
| validador Gmail fake Windows | EXECUTED_PASS | integrado al gate: `gmail-live-fake-20260730-090951.json` y `...-092044.json`, 10 fases verdes cada uno, sin Google real |
| validador completo Windows | EXECUTED_PASS | cierre histórico `986523a` y doble corrida final sobre `d6bfe642` |
| validación integral SEG-001 Gmail Windows | EXECUTED_PASS | `complete-crm-20260730-090155.json` 620,955 s y `...-091224.json` 640,545 s; 22 fases verdes |
| CI SEG-001 Gmail | EXECUTED_PASS | run `30530515993`, SHA `d6bfe642`, 22/22 jobs verdes |
| validador completo Unix | IMPLEMENTED_NOT_RUN | sintaxis y preflight WSL PASS; recorrido integral no ejecutado |
| repetición limpia | EXECUTED_PASS | mismo commit, 21 fases verdes, 734,162 s, sin flakiness |
| despliegue producción | NOT_APPLICABLE | no autorizado |
| envío real | NOT_APPLICABLE | deshabilitado por política |
