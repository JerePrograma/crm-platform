# SCALARIS-DEPLOY-001 — preparación del despliegue

Fecha: 2026-10-22

## Objetivo y alcance

Cerrar los bloqueos de código, configuración y validación identificados en la auditoría de `2a22e4bf3227a87d120c22bdfc8aa05615a40a6d`. El usuario autorizó continuar y terminar todo lo posible y eligió el VPS Hostinger con `crm.scalaris.com.ar`. La preparación funcional local está validada; publicación/CI, secretos y puesta en servicio real siguen pendientes de autorización final. El segmento no está `COMPLETE`.

## Trabajo de la entrega

- Remediar dependencias Java mediante un BOM de Spring Boot coherente y renovar el runtime manteniendo Java 21.
- Corregir agenda y ficha de prospecto con filtros y paginación antes del límite, conservando el endpoint histórico.
- Preparar el perfil productivo Scalaris con cuentas separadas y guardas de comunicación cerradas.
- Alinear el límite de importación del proxy y preservar HTTPS solo desde un edge explícitamente confiable.
- Hacer explícito el proyecto Compose de backup/restore y comprobar integridad antes de restaurar.
- Incorporar Marketing Scalaris y sus importaciones al gate E2E, además del recorrido histórico y Google falso.
- Ejecutar seguridad, migraciones, recuperación y dos validaciones integrales sobre el mismo commit.

## Evidencia

El contrato nuevo `GET /api/v1/tasks/page` aplica `statuses` repetidos, `ownerUserId`, `prospectId`, `dueFrom` inclusivo y `dueUntil` exclusivo antes de contar/paginar. Devuelve contenido, totales, número/tamaño y primera/última página; limita el tamaño a 100. La agenda y las fichas usan páginas propias; `GET /tasks` conserva su respuesta de lista para compatibilidad. Las fechas comerciales se interpretan en Buenos Aires.

La autoridad de esta misión es [la validación de preparación productiva](../validation/scalaris-production-readiness-2026-10-22.md). Los resultados de octubre 14 y julio se conservan como historia y no cubren esta entrega.

La fuente funcional `4b556a5c6e91e42325ed12c393c8003d7914d48a` aprobó dos integrales limpias consecutivas, 23/23 fases cada una. Los IDs productivos elegidos, sus scans controlados sin high/critical ni exclusiones y el export están vinculados por hashes y cadena OCI. Este cierre documental se registra como descendiente separado; no reemplaza el SHA funcional de los artefactos.

No se actualiza la instalación operativa ni el ZIP portable como efecto de construir imágenes. PostgreSQL sigue siendo la fuente de verdad; las pruebas usan proyectos, bases y volúmenes sintéticos aislados.

## Límite operativo

La entrega conserva email `NOOP`, WhatsApp `DEEPLINK_ONLY`, red y workers reales bloqueados, límite diario cero y kill switch activo. La infraestructura y los secretos se configuran fuera de Git. Un gate local aprobado no equivale a producción desplegada ni a CI remota verde.

La copia externa elegida es cifrada en esta PC. El helper está instalado y probado con datos sintéticos; la tarea diaria 04:15 BA está preparada, sin registrar. Primera copia real, recuperación operativa, timers del VPS, DNS/TLS y credenciales siguen pendientes de aprobación final y ejecución.
