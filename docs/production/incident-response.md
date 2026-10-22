# Operación e incidentes

`deploy/hostinger/healthcheck.sh` consulta readiness por HTTPS, deja un resultado mínimo en `/srv/apps/crm/state/health.json` y falla si el servicio no está UP. Las unidades systemd permiten ejecutarlo cada cinco minutos. `CRM_ALERTING_ENABLED=false` es el valor inicial: no se inventa ni utiliza un destino externo. Configurar un webhook HTTPS y habilitarlo exige un destino aprobado; el payload solo contiene nombre del servicio y estado.

Antes de operación deben acordarse monitoreo desde fuera del VPS, alertas por caída/certificado/disco/memoria/backup y responsables. Un timer alojado en el mismo servidor no detecta por sí solo la pérdida completa del host. Prometheus está disponible para sesión administrativa con `SETTINGS_MANAGE` por acceso privado; Caddy bloquea `/actuator/*` excepto health. No hay plataforma externa de métricas configurada por estos archivos.

Ante un incidente, conservar timestamps UTC y correlation IDs, clasificar alcance y evitar PII/secretos en tickets. Para un problema de mensajería, activar primero el kill switch de entorno y comprobar el persistente; conservar auditoría/outbox/inbound sin ejecutar reenvíos. Contener accesos y rotar claves mediante el gestor externo; invalidar sesiones cuando corresponda.

Recuperar con los runbooks de [respaldo](backup-and-restore.md) y [rollback](rollback.md). Revisar tenant isolation, roles, auditoría, probes, guardas y cero estados prohibidos antes de reabrir. Registrar decisiones, impacto, evidencia sanitizada, responsables y acciones pendientes. Los cambios del CRM no deben detener, reinicializar ni reemplazar otras aplicaciones del VPS.
