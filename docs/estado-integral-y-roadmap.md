# Estado integral y roadmap de Gestudio CRM

Actualizado: 2026-07-23

Este documento es la fuente canónica para comprender qué está implementado, qué fue validado, qué permanece deliberadamente bloqueado y qué trabajo falta. Los documentos de `docs/validation/` conservan la evidencia detallada de cada cierre y no deben sustituirse por afirmaciones sin salida de comandos.

## 1. Veredicto ejecutivo

Gestudio CRM se encuentra **funcionalmente completo para demostración, evaluación interna y operación comercial segura sin envíos reales**.

La plataforma implementa el ciclo completo de CRM definido en los segmentos SEG-000 a SEG-011, y recibió una mejora integral de experiencia de operador el 23 de julio de 2026. La rama canónica es `main` y el último commit funcional de esta etapa es:

```text
19732dec9638cd47fee4b39ac41c5968693b5b7a
fix: synchronize contactability and paginate prospects
```

El producto **no debe describirse como listo para producción real** porque:

- no existe autorización de despliegue productivo;
- Gmail, SMTP, WhatsApp Cloud y cualquier red real permanecen desconectados;
- la API no ofrece un endpoint de envío real;
- el XLSX real no fue incorporado a Git, CI, imágenes ni evidencia;
- faltan validaciones manuales de accesibilidad y pruebas de escala con datos reales autorizados;
- persiste deuda técnica que no bloquea la demo, pero debe resolverse antes de una fase productiva.

## 2. Estado del repositorio

```text
repositorio: JerePrograma/crm-platform
rama canónica: main
implementación UX principal: 8d12f8ff772d3445440e4419b22d5c81b102cb15
seguimiento de contactabilidad y paginación: 19732dec9638cd47fee4b39ac41c5968693b5b7a
producción: NO DESPLEGADA
comunicaciones reales: DESHABILITADAS
proveedores externos: IMPLEMENTADOS COMO CONTRATOS, NO CONECTADOS
lote real: FUERA DE GIT, CI E IMÁGENES
```

No se creó una rama funcional paralela ni un pull request para esta etapa. Los cambios se publicaron directamente sobre `main`, con validaciones remotas fail-closed antes de cada commit funcional.

## 3. Arquitectura vigente

### Backend

- Java 21.
- Spring Boot 4.1.
- Spring Security con sesión cookie HttpOnly same-origin y CSRF.
- PostgreSQL 17 como fuente de verdad.
- Flyway V1–V13.
- Hibernate con validación del esquema.
- API REST con OpenAPI y errores RFC 7807.
- Auditoría JSONB.
- Testcontainers, ArchUnit y Maven Verify.

### Frontend

- React.
- TypeScript en modo estricto.
- Vite.
- Vitest.
- Playwright con Chromium.
- CSS propio con variables, componentes y breakpoints existentes; no se cambió de framework ni se añadió una librería de UI.

### Operación e infraestructura

- Dockerfiles multi-stage.
- Docker Compose para PostgreSQL, backend, frontend y smoke.
- Health, readiness, liveness y métricas.
- Scripts PowerShell y Bash.
- GitHub Actions con pruebas backend, frontend, migraciones, smoke, seguridad y E2E.
- Perfil productivo local endurecido, no desplegado.

## 4. Alcance funcional implementado

### 4.1 Identidad, seguridad y aislamiento

- organizaciones y membresías;
- usuarios persistentes;
- roles y permisos;
- login, logout, expiración e invalidación de sesión;
- bloqueo temporal y usuario inactivo;
- aislamiento por organización;
- auditoría de autenticación y acciones sensibles;
- control optimista en operaciones relevantes.

### 4.2 Prospectos y contactos

- alta, edición, archivo y restauración de prospectos;
- institución, razón social, localidad, provincia, país, sitio, fuente, prioridad, puntuación y responsable;
- múltiples contactos por institución;
- correo, teléfono, WhatsApp y otros canales normalizados;
- contacto principal, consentimiento, verificación y canal preferido;
- búsqueda por institución, razón social, localidad, provincia, sitio, contacto, canal normalizado, etiquetas y resumen de notas;
- filtros, orden y paginación real de la API;
- conservación de búsqueda, estado y página en la URL;
- exportación CSV segura contra fórmulas.

### 4.3 Contactabilidad

La contactabilidad se calcula a partir de canales utilizables y exclusiones vigentes.

Comportamiento actual:

- sin correo, teléfono ni WhatsApp utilizable, el prospecto queda en `NEEDS_ENRICHMENT` y no aparece como plenamente contactable;
- un sitio web puede aportar evidencia y participar en exclusiones, pero no convierte por sí solo al prospecto en contactable;
- crear o eliminar contactos recalcula la contactabilidad;
- agregar, modificar o eliminar canales recalcula la contactabilidad;
- una exclusión activa domina y mantiene `DO_NOT_CONTACT`/`EXCLUDED`;
- la vista previa y la ejecución de importaciones aplican el mismo criterio.

### 4.4 Actividades, tareas y timeline

- notas sanitizadas;
- actividades tipadas;
- llamadas, reuniones, demostraciones, correo y WhatsApp manuales;
- tareas con responsable, prioridad, vencimiento, recordatorio y estado;
- próxima acción derivada;
- timeline paginado y aislado por organización;
- historial de transiciones comerciales.

### 4.5 Duplicados

- detección exacta y ambigua;
- cola de revisión humana;
- comparación del registro importado con el candidato existente;
- vinculación con existente;
- confirmación de no duplicado;
- creación independiente;
- fusión transaccional e idempotente;
- descarte y postergación;
- trazabilidad de superviviente y absorbido;
- auditoría y preservación de referencias.

`CREATE_SEPARATE` y `MARK_NOT_DUPLICATE` conservan de forma controlada:

- institución;
- localidad y provincia;
- categoría;
- sitio web;
- fuente y evidencia;
- prioridad;
- fecha de verificación;
- correo;
- teléfono o WhatsApp;
- identificador externo cuando es seguro.

La extracción se limita a campos conocidos mediante `SanitizedDuplicateImportData`; no se persisten propiedades arbitrarias del JSON crudo.

### 4.6 Importaciones

- CSV y XLSX;
- vista previa obligatoria;
- ejecución confirmada;
- idempotencia por archivo y modo;
- evidencia persistida por fila;
- hojas de prospectos y exclusiones;
- resultados aceptados, bloqueados, rechazados, duplicados y a revisión;
- búsqueda, filtros y paginación visual de resultados;
- prevención de doble ejecución desde la interfaz;
- exclusiones dominantes;
- ausencia de envíos reales.

### 4.7 Oportunidades y pipeline

- oportunidades por prospecto;
- etapas, probabilidad, valor, moneda, responsable y fecha estimada;
- transiciones controladas;
- ganada y perdida con motivo;
- forecast y valor ponderado;
- aging y detección de estancamiento;
- vista kanban y listado.

### 4.8 Campañas, plantillas y mensajes

- campañas tenant-scoped;
- plantillas versionadas;
- renderer limitado y seguro;
- audiencias congeladas;
- exclusiones aplicadas;
- secuencias declarativas;
- aprobación antes de simulación;
- simulación sin envío;
- borradores y enlaces manuales;
- contratos Gmail y WhatsApp aislados y desconectados.

### 4.9 Outbox e inbound

- transactional outbox PostgreSQL;
- worker con lock, lease, retry y dead-letter;
- idempotencia asíncrona;
- reencolado autorizado;
- inbound de prueba firmado con HMAC;
- protección contra replay;
- cuarentena y asociación manual;
- descarte lógico;
- creación de actividad, tarea y transición desde inbound.

### 4.10 Exclusiones

- exclusión por canal normalizado;
- equivalencia segura entre teléfono y WhatsApp;
- motivos tipados;
- impacto sobre prospectos existentes;
- auditoría con huella del canal, no valor completo;
- prioridad sobre campañas y contacto comercial.

### 4.11 Auditoría, reportes y configuración

- auditoría legible con resumen principal;
- JSON técnico bajo revelado progresivo;
- ocultamiento de claves sensibles;
- reportes por estado, fuente, etapa, tareas, outbox y cuarentena;
- monedas separadas;
- exportación CSV segura;
- settings con invariantes de envío;
- etiquetas y asignación a prospectos;
- métricas, correlation ID y logs sanitizados.

## 5. Enfoque de experiencia de operador

La mejora UX no fue una traducción superficial. El enfoque aplicado fue:

1. conservar contratos internos, enums, rutas, tablas y claves JSON;
2. centralizar la representación humana en español;
3. explicar contexto, consecuencias, bloqueos y siguiente paso;
4. reducir densidad mediante secciones, filtros, paginación y revelado progresivo;
5. reemplazar diálogos nativos por decisiones accesibles;
6. mantener la seguridad fail-closed visible y comprensible;
7. mejorar móvil y teclado sin cambiar de framework;
8. evitar dependencias nuevas cuando el repositorio podía resolverlo con componentes propios.

### Implementaciones principales

- `frontend/src/uiLabels.ts`: diccionario centralizado de estados, canales, roles, etapas, acciones, motivos y eventos.
- `frontend/src/decisionDialog.ts`: diálogo con `role=dialog`, foco inicial, focus trap, Escape y retorno de foco.
- `frontend/src/styles.css`: variables, foco visible, controles táctiles, responsive, diálogos, tarjetas, comparaciones y revelado progresivo.
- `frontend/src/App.tsx`: navegación y módulos humanizados, formularios orientativos, estados vacíos, errores, tablas y flujos guiados.
- `frontend/src/api.ts`: paginación real de prospectos y parámetros explícitos.
- `frontend/src/api.test.ts` y `frontend/src/uiLabels.test.ts`: regresiones de presentación y API.
- `frontend/tests/complete-crm.spec.ts`: recorrido integral, responsive y teclado.

## 6. Accesibilidad y responsive

Validado automáticamente:

- foco visible;
- navegación por teclado en flujos principales;
- filas accionables con Enter y Espacio;
- labels y nombres accesibles;
- mensajes de error con `role=alert`;
- mensajes de éxito con estado anunciable;
- diálogos con focus trap, Escape y retorno de foco;
- controles de al menos 44 px;
- reducción de movimiento;
- tablas con scroll controlado;
- formularios y comparaciones adaptadas a móvil;
- recorrido Playwright a 390 × 844.

No existe todavía una auditoría manual completa WCAG 2.1 AA con lector de pantalla, contraste medido en todas las combinaciones y prueba en múltiples navegadores.

## 7. Seguridad innegociable

Debe permanecer:

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

- no existe endpoint de envío real;
- no se deben cargar secretos en Git;
- no se debe versionar `.env`;
- no se deben usar datos reales de clientes en pruebas o documentación;
- no se debe desplegar producción sin autorización;
- no se debe habilitar una integración externa desde la UI;
- las exclusiones y el aislamiento por organización no pueden relajarse para mejorar UX.

## 8. Evidencia de validación

### Cierre integral previo

- GitHub Actions run `29951586239` sobre `b904ff37e506f058dab351c2b941e13ee4ed9981`.
- Resultado: 22/22 jobs exitosos.
- Flyway V1–V13.
- Maven Verify y pruebas integrales.
- frontend typecheck, unit, build y E2E.
- migraciones desde vacío y V11.
- compose smoke, perfil productivo, backup/restore y scans.

### Mejora de experiencia de operador

- commit final: `8d12f8ff772d3445440e4419b22d5c81b102cb15`;
- workflow funcional sobre `9b1d6483864291a35cce3e3ad9a2932268353fdd`;
- run `30034176306`;
- frontend: instalación limpia, typecheck, unit y build;
- backend: `./mvnw -B -f backend/pom.xml verify`;
- Docker Compose fail-closed;
- Playwright Chromium: recorrido integral y responsive;
- repository safety y `git diff --check`;
- ausencia de `window.prompt` y `window.confirm` en `frontend/src`.

### Contactabilidad y paginación

- commit final: `19732dec9638cd47fee4b39ac41c5968693b5b7a`;
- workflow funcional sobre `42d1574e7558f544e5894aef1a69a72a7e08da6f`;
- run `30036648327`;
- Spotless aplicado y verificado;
- pruebas específicas de prospectos, importaciones, reportes, campañas y oportunidades;
- Maven Verify completo;
- frontend typecheck, unit y build;
- Docker Compose fail-closed;
- Playwright Chromium;
- repository safety y `git diff --check`.

Evidencia:

```text
docs/validation/UX-operator-overhaul-2026-07-23.md
docs/validation/UX-contactability-pagination-2026-07-23.md
docs/validation/COMPLETE-CRM-matrix.md
```

## 9. Estado por nivel

### Completo y validado

- segmentos SEG-000 a SEG-011;
- arquitectura backend y frontend;
- identidad, RBAC y tenant isolation;
- CRM operativo;
- importaciones y duplicados;
- pipeline y campañas simuladas;
- mensajería segura sin red real;
- outbox e inbound de prueba;
- reportes, settings y etiquetas;
- experiencia de operador en español;
- flujos críticos sin diálogos nativos;
- conservación segura de datos importados;
- contactabilidad coherente;
- paginación real de prospectos;
- recorrido E2E principal y responsive.

### Implementado, no conectado

- adaptador Gmail;
- adaptador WhatsApp Cloud;
- proveedores externos;
- configuración de infraestructura para despliegue futuro.

### No autorizado o fuera de alcance actual

- producción real;
- envío de correo o WhatsApp real;
- importación del lote real en CI o imágenes;
- credenciales de proveedores;
- migraciones destructivas;
- eliminación de controles fail-closed.

## 10. Pendientes reales

### Prioridad alta antes de una fase productiva

1. **Corregir métricas parciales del dashboard.** Las métricas “prospectos con interés” y “contacto bloqueado” se calculan actualmente sobre la página de prospectos cargada, no sobre el total. Deben provenir de un endpoint agregado o del reporte tenant-scoped.
2. **Eliminar automatización remota obsoleta.** Permanecen `.github/remote-ux-trigger`, `.github/workflows/remote-ux-overhaul.yml`, `scripts/remote-ux-preflight.py`, `scripts/remote-ux-overhaul.py` y `scripts/remote-ux-postfix.py`. La guarda fija de historial los vuelve inertes, pero no deben quedar como deuda permanente.
3. **Auditoría manual WCAG 2.1 AA.** Probar con teclado completo, lector de pantalla, zoom 200 %, contraste y mensajes dinámicos.
4. **Validación de escala autorizada.** Ejecutar búsquedas, importaciones, duplicados, auditoría y reportes con volúmenes representativos y datos sintéticos equivalentes a producción.

### Prioridad media

1. mover filtros y paginación de filas de importación al backend si los lotes esperados superan varios miles de filas;
2. validar Firefox y WebKit además de Chromium;
3. convertir la navegación móvil horizontal en drawer o menú colapsable si las pruebas con operadores muestran saturación;
4. dividir `frontend/src/App.tsx` por módulos sin cambiar contratos ni comportamiento;
5. ampliar skeletons y estados de carga en operaciones lentas;
6. realizar revisión visual manual de contraste en temas, badges y estados disabled;
7. revisar nomenclatura residual en paneles técnicos y documentación OpenAPI.

### Prioridad baja

1. internacionalización formal si se incorpora otro idioma;
2. preferencias de densidad de tablas;
3. atajos de teclado documentados;
4. personalización visual avanzada por organización;
5. telemetría UX anonimizada, solo si existe una política aprobada.

## 11. Próximo orden recomendado

1. eliminar scripts y workflow remotos obsoletos;
2. corregir las métricas parciales del dashboard con datos agregados reales;
3. ejecutar auditoría manual de accesibilidad;
4. ejecutar pruebas de escala con datos sintéticos autorizados;
5. validar Firefox y WebKit;
6. preparar un plan separado de despliegue, secretos y proveedores;
7. habilitar producción o comunicaciones solo mediante autorización explícita y un nuevo cierre de seguridad.

## 12. Criterios para declarar producción lista

No declarar producción lista hasta demostrar:

- infraestructura y secretos administrados fuera de Git;
- backup, restore y rollback en el entorno objetivo;
- migraciones verificadas sobre una copia segura;
- observabilidad y alertas operativas;
- auditoría manual de accesibilidad;
- pruebas de carga y rendimiento;
- revisión de privacidad y retención;
- proveedores reales conectados mediante una revisión separada;
- límites, kill switch e idempotencia verificados con el proveedor;
- CI verde sobre el commit exacto a desplegar;
- autorización formal de despliegue y comunicaciones.

## 13. Fuentes documentales

```text
README.md
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

## 14. Regla de continuidad

Todo cambio posterior debe:

1. partir de `main` actualizada por fast-forward;
2. detenerse ante cambios locales no relacionados;
3. preservar contratos, seguridad, aislamiento y bloqueos de envío;
4. añadir pruebas específicas;
5. ejecutar validaciones generales razonables;
6. ejecutar `git diff --check` y repository safety;
7. revisar el diff completo;
8. publicar en `main` solo con evidencia verde;
9. actualizar este documento cuando cambie el estado real.

## Actualización 2026-07-24 — cierre del hardening de validación

El commit `0448c0e060311c284f4e4be4612982818a8480c4` fue validado mediante dos corridas integrales consecutivas en Windows PowerShell 5.1 y Docker Desktop.

```text
run 1: complete-crm-20260724-201944.json
SHA-256: 4D87175F8985B406DB1EB29E2B6F60EDB26F1C1A1FE9F927FB76FEB9A4DB4527

run 2: complete-crm-20260724-202955.json
SHA-256: 2A538402F41AD30FF14F683DA2709B3F0369C6F9946026A1E3FBDE8522602774
```

Resultado: `2/2 FUNCTIONAL_PASS`, incluido backend, frontend, Docker, migraciones, E2E, dependency scans, backup/restore, perfil productivo, bloqueo efectivo de envíos, cero estados enviados, repository safety y árbol final limpio.

La demo remota continúa activa en `127.0.0.1:18080`. El siguiente gap verificable es anticipar la comprobación de `ProductionFrontendPort` durante el preflight (`VAL-002`).

Producción permanece no autorizada y no desplegada. Gmail, SMTP y WhatsApp reales continúan desconectados. No se realizaron envíos reales ni se incorporó el XLSX externo.

## Actualización 2026-07-24 — VAL-002

El puerto productivo sintético se valida ahora durante el preflight:

- Windows: cuarto parámetro opcional en `check-host-ports.ps1`;
- Unix: checker Node y argumento `--production-frontend-port`;
- puertos duplicados fallan antes de consultar servicios;
- listeners ocupados y publicaciones Docker fallan antes de suites costosas;
- la demo autorizada en `127.0.0.1:18080` fue preservada.

Validación focalizada: PowerShell, Node, Bash, repository safety y `git diff --check`.

No se repitieron backend, frontend, builds, migraciones, E2E ni dependency scans.

Siguiente iniciativa: `UX-003`.

## Actualización 2026-07-26 — UX-003

Se eliminó la automatización remota histórica `remote-ux-overhaul`:

- trigger;
- workflow con permisos de escritura;
- preflight Python;
- transformación Python;
- postfix Python.

La eliminación se realizó después de comprobar cero consumidores operativos externos y cero dependencias desde otros workflows.

No se modificó funcionalidad del CRM. No se repitieron backend, frontend, Docker, migraciones ni E2E.

Siguiente iniciativa: `UX-004` — métricas tenant-wide del dashboard.
