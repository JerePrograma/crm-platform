# Rollback del release

Registrar antes del despliegue: commit e IDs/digests de ambas imágenes, release anterior, versión PostgreSQL, migraciones aplicadas, configuración privada y copia verificable. El esquema actual llega a V14; V13 y V14 son aditivas, pero eso no prueba automáticamente que cualquier binario anterior pueda usar la base actual. Ensayar la imagen elegida contra una copia migrada, incluyendo perfil Scalaris, cuentas y guardas.

Rollback de aplicación: detener escrituras durante el cambio, mantener PostgreSQL y su volumen, seleccionar imágenes anteriores verificadas y recrear solamente backend/frontend del proyecto `scalaris-crm`. Exigir readiness y repetir login/tenant/exclusiones/cero envíos. Cambiar el enlace `current` por sí solo no cambia contenedores ni imágenes. No borrar ni editar `flyway_schema_history` y no bajar PostgreSQL de versión mayor.

Rollback de datos: preservar la base fallida, detener escritores, crear y comprobar una copia fresca, restaurar el backup elegido en una base aislada y revisar integridad/Flyway/roles. El cambio de datasource necesita una decisión expresa porque pierde cambios posteriores a la copia. Conservar la base previa y la auditoría del incidente.

Si solo falla la publicación inicial, retirar exclusivamente `crm.caddy` del directorio de sitios, validar Caddy completo y recargarlo. Preservar `apps.caddy`, fragmentos de otras aplicaciones y volúmenes del CRM. No ejecutar `docker compose down -v` en un proyecto operativo.
