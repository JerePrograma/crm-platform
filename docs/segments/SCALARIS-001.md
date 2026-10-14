# SCALARIS-001 — Marketing Scalaris y operación local Windows

Actualizado: 2026-10-14

Estado: `LOCAL_OPERATIONAL_PASS`; publicación GitHub `AUTHORIZED_IN_PROGRESS`, CI `PENDING_POST_PUSH`, paquete definitivo revisado e instalación de uso preparada.

## Objetivo

Entregar un CRM local que pueda usar una persona de Marketing de Scalaris, con apertura por doble clic, organización del trabajo diario, referencias propias y respaldo/recuperación. Esta misión adapta el CRM histórico de Gestudio y conserva sus contratos, permisos, aislamiento y auditoría.

## Alcance implementado localmente

- Perfil `SCALARIS` con organización propia, catálogo comercial, etiquetas y plantillas iniciales; perfil `LEGACY` conservado.
- Cuenta de Marketing con permisos comerciales, separada de administración y sin permiso de envío real.
- Referencias vigentes: servicio técnico, desarrollo y venta a pedido; WhatsApp 11 7278-9289; Zona Oeste, CABA y Mar del Plata; retiro/entrega a coordinar según el caso.
- Pantalla en español enfocada en personas y negocios, agenda, oportunidades y ayuda de Marketing.
- Importación CSV/XLSX con encabezados comerciales, vista previa, duplicados y exclusiones, conservando los originales.
- Camino Docker independiente mediante `docker-compose.scalaris.yml` y los iniciadores `Abrir/Detener/RespaldarCRMScalaris.cmd`.
- Camino Windows portable mediante `scalaris-portable.ps1`, servidor web local y empaquetador con Java 21, PostgreSQL 18 y Node 24.
- Credenciales generadas por instalación, datos y respaldos privados fuera del proyecto; servicios en loopback y guardas cerradas.
- [Guía operativa para Marketing](../scalaris-marketing-local.md) y manual de la edición.

El bootstrap prepara cuentas, etiquetas y plantillas; no agrega contactos reales ni sustituye los datos de otra organización. Los nombres Java y las migraciones históricas se conservan para compatibilidad.

## Evidencia disponible

| Control | Estado | Límite |
|---|---|---|
| Backend unitario/integración focalizada Java 21 | `EXECUTED_PASS` | 62 unitarias + 6 integración = 68; no equivale a suite integral histórica completa |
| Launcher Docker PowerShell 5.1 | `EXECUTED_PASS` | 55 aserciones; ACL y puertos reales, respaldo con Docker simulado |
| Mensaje ante Docker ausente | `EXECUTED_PASS` | Falla explicativa, sin instalar software ni generar claves |
| Docker/Compose/build/health real | `BLOCKED_EXTERNAL` | Docker no instalado |
| Frontend | `EXECUTED_PASS` | 16 unitarias, typecheck, build, audit sin vulnerabilidades y E2E conjunto 4/4 sin omitidos/inesperados/flaky |
| Paquete Windows real | `EXECUTED_PASS` | 18 comprobaciones PS5.1: apertura, permisos, persistencia, respaldo, recuperación y base anterior conservada |
| Manual visual | `EXECUTED_PASS` | Render y revisión individual a 1440/390/320 px, sin overflow horizontal |
| ZIP definitivo | `EXECUTED_PASS` | Manifiesto/checksum coincidentes; 2.243 entradas y cero archivos privados |
| Instalación de uso | `EXECUTED_PASS` | Loopback 8787, perfil/rol correctos, cero prospectos, 18 etiquetas y 7 plantillas; QA detenido |
| Validador integral repetido | `BLOCKED_EXTERNAL` | Docker no instalado; sin dos corridas limpias de SCALARIS-001 |
| Publicación GitHub | `IN_PROGRESS` | Add/diff/commit/push autorizados; estado `AUTHORIZED_IN_PROGRESS`, sin afirmar éxito |
| CI del SHA publicado | `IN_PROGRESS` | `PENDING_POST_PUSH`; verificar después de publicar |
| Producción/proveedores/comunicaciones reales | `NOT_APPLICABLE` | No desplegados ni habilitados |

La validación específica de esta edición es la autoridad para nuevos resultados. Una lectura de código, un `.dump` legible o la existencia del ZIP no sustituyen su comprobación funcional.

## Criterios de cierre local y entrega

- [x] Compilación y controles backend/frontend ejecutados; E2E conjunto 4/4 aprobado.
- [x] Primera apertura portable, ingreso Marketing y separación de permisos.
- [x] Recorrido de Marketing con datos sintéticos y referencias vigentes.
- [x] Reapertura idempotente, parada de procesos propios y persistencia.
- [x] Respaldo con checksum y recuperación en base nueva, conservando la anterior.
- [x] Revisión visual manual desktop y móvil.
- [x] Consolidación del resultado E2E conjunto, sin omitidos, inesperados ni flaky; fuente local congelada.
- [x] ZIP final y manifiesto/checksum revisados; sin secretos, registros ni contactos operativos.
- [x] Evidencia, limitaciones y guía reconciliadas.
- [x] Instalación de uso preparada sin fixtures de prueba; entornos QA detenidos.

Docker real permanece pendiente mientras no esté disponible. El cierre local portable debe distinguirse del cierre integral del repositorio y de una publicación remota.

## Persistencia y recuperación

Windows conserva los datos en `%LOCALAPPDATA%\ScalarisCRM\portable`; Docker utiliza `%LOCALAPPDATA%\ScalarisCRM` para claves/respaldos y un volumen independiente del proyecto `scalaris-crm-local`. Conservar respaldos y configuración antes de actualizar.

La recuperación portable crea una base nueva y conserva la anterior. Ante pérdida del archivo de claves sobre una base existente, recuperar la configuración; no borrar ni reinicializar la base. Los cambios de versión mayor PostgreSQL requieren una migración revisada.

Las guardas `SENDING_*`, red real y providers permanecen cerradas. No conectar Google, SMTP ni WhatsApp reales, no enviar, no importar contactos operativos y no desplegar producción como efecto de la validación.

## Baseline y autorización

La referencia publicada anterior es `d6bfe64283cd3bab8dce441e4b01e348f9a55d5e`, con doble validación y CI `30530515993`, 22/22, del 2026-07-30. Los cambios de octubre permanecen locales y no heredan ese cierre.

El usuario autorizó explícitamente add, revisión de diff, commit y push a GitHub. La [validación de publicación GitHub](../validation/scalaris-github-publication-2026-10-14.md) es la autoridad para esa fase: `AUTHORIZED_IN_PROGRESS`, con CI `PENDING_POST_PUSH`. No se afirma publicación exitosa hasta verificarla. PR, producción y comunicaciones reales permanecen fuera de alcance.

La operación portable, E2E conjunto y ZIP definitivo están aprobados; la instalación de uso está activa sin prospectos de prueba. El siguiente paso de negocio es uso/piloto local siguiendo la guía, sin cargar contactos reales automáticamente. La validación específica conserva la ruta/checksum del ZIP; evidencias locales adicionales: `validation-output/scalaris-release-latest.json` y `validation-output/scalaris-installed-local.json`.
