# SCALARIS-DEPLOY-001 — preparación del despliegue

Fecha: 2026-10-22

## Objetivo y alcance

Cerrar los bloqueos de código, configuración y validación identificados en la auditoría de `2a22e4bf3227a87d120c22bdfc8aa05615a40a6d`. El usuario autorizó continuar y terminar todo lo posible. Se prepara una entrega verificable con datos sintéticos; el alojamiento, dominio, secretos operativos y puesta en servicio real necesitan un destino concreto.

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

No se actualiza la instalación operativa ni el ZIP portable como efecto de construir imágenes. PostgreSQL sigue siendo la fuente de verdad; las pruebas usan proyectos, bases y volúmenes sintéticos aislados.

## Límite operativo

La entrega conserva email `NOOP`, WhatsApp `DEEPLINK_ONLY`, red y workers reales bloqueados, límite diario cero y kill switch activo. La infraestructura y los secretos se configuran fuera de Git. Un gate local aprobado no equivale a producción desplegada ni a CI remota verde.
