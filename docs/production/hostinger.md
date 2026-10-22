# Hostinger — crm.scalaris.com.ar

Preparación del 2026-10-22. Estos archivos no fueron subidos ni activados en el VPS como parte de su elaboración. El despliegue real y la recarga del proxy compartido requieren aprobación del resultado concreto y sus gates de validación.

## Destino comprobado por lectura

- Acceso existente: alias SSH `scalaris-vps`, usuario `deploy`; reutilizar la configuración local sin copiar claves al repositorio.
- Caddy compartido: proyecto `scalaris-proxy`, contenedor `scalaris-proxy-caddy-1`, `/srv/infrastructure/caddy/compose.yaml`; el Caddyfile importa `/etc/caddy/sites/*.caddy`.
- Red externa `proxy`; Caddy estaba en `172.18.0.2` al inspeccionar. Volver a consultar antes de preparar la allowlist; no asumir que esa IP será permanente.
- Dominio confirmado: `crm.scalaris.com.ar`, servido desde la raíz del subdominio. No agregar una ruta a las demos existentes ni modificar `apps.caddy`.
- Proyecto CRM: `scalaris-crm`; alias DNS Docker exclusivo `scalaris-crm-frontend`; frontend auxiliar `127.0.0.1:18095`, libre en la inspección. PostgreSQL/backend no publican puertos.
- VPS observado: 2 CPU, unos 8 GiB RAM, 5,5 GiB disponibles y 77 GiB de disco libre. El override limita PostgreSQL/backend/frontend a 512/768/128 MiB y 0,5/0,75/0,25 CPU. Confirmar margen con carga de todas las aplicaciones antes del corte.

## Distribución del release

```text
/srv/apps/crm/releases/<commit>/       fuente y archivos de release revisados
/srv/apps/crm/current                 enlace al release activo
/srv/apps/crm/config/production.env   secretos y referencias de imágenes, 0600
/srv/apps/crm/config/nginx-trusted-edge.conf
/srv/apps/crm/config/operations.conf  operación privada opcional, 0600
/srv/apps/crm/backups/                copias privadas, 0700
/srv/apps/crm/state/                  resultados de salud, 0700
```

El volumen Docker `scalaris-crm_production_postgres` guarda la base. No copiar la instalación Windows ni datos operativos por efecto del despliegue; no montar volúmenes de otras aplicaciones. Mantener la configuración y las copias fuera de cada release.

## Preparar sin publicar

Una vez aprobada la transferencia del release, crear solamente las carpetas CRM y preparar el archivo privado con los nombres del [contrato de entorno](environment-contract.md). No ejecutar `echo` con claves, compartir el archivo ni imprimir Compose renderizado. Definir `PRODUCTION_FRONTEND_PORT=18095`, `SESSION_COOKIE_SECURE=true`, las imágenes exactas y `TRUSTED_EDGE_CONFIG_FILE=/srv/apps/crm/config/nginx-trusted-edge.conf`.

El archivo de confianza contiene únicamente la IP actual de Caddy con máscara `/32`, más comentarios si se desean. Obtenerla con:

```sh
docker inspect scalaris-proxy-caddy-1 --format '{{(index .NetworkSettings.Networks "proxy").IPAddress}}'
```

El formato nginx es `dirección/32 1;`. No permitir `0.0.0.0/0`, `::/0` ni toda `172.18.0.0/16`: otras aplicaciones comparten esa red. El script de preflight exige la dirección exacta y rechaza entradas adicionales.

Desde `/srv/apps/crm/current`, revisar sin iniciar servicios:

```sh
sh deploy/hostinger/preflight.sh
docker compose --env-file /srv/apps/crm/config/production.env --project-name scalaris-crm \
  --file deploy/docker-compose.production.yml --file deploy/hostinger/compose.override.yml config --quiet
```

La selección de imágenes usa registry con digest o archivos transferidos cuyo checksum, tag `release-<commit completo>` e ID se verificaron previamente. Configurar `CRM_RELEASE_COMMIT`, `CRM_BACKEND_IMAGE_ID` y `CRM_FRONTEND_IMAGE_ID` desde el inventario de artefactos aprobado. El preflight exige que ambas imágenes estén presentes localmente, compara sus IDs SHA-256 y verifica cualquier label OCI de revisión. Rechaza referencias `local`/`latest`, etiquetas de otro commit, imágenes ausentes o IDs distintos. PostgreSQL queda fijado en el override a `17.11-alpine@sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb`. La transferencia y carga no sustituyen los scans del artefacto exacto. Mantener la versión anterior disponible.

## Arranque y publicación autorizados

1. Comprobar DNS A/AAAA contra el VPS, certificado y resolución pública; revisar un eventual AAAA obsoleto. Mantener 80/443 administrados exclusivamente por Caddy compartido.
2. En una actualización, generar y probar un respaldo antes de migrar; preservar el release anterior. Iniciar únicamente CRM con el archivo privado y ambos archivos Compose:

```sh
docker compose --env-file /srv/apps/crm/config/production.env --project-name scalaris-crm \
  --file deploy/docker-compose.production.yml --file deploy/hostinger/compose.override.yml \
  up -d --no-build --wait
```

3. Verificar readiness por `http://127.0.0.1:18095/actuator/health/readiness`, perfil Scalaris, roles, V14 y guardas. La cookie productiva es Secure: no desactivarla para ingresar por HTTP auxiliar. Usar el dominio TLS para el recorrido de usuario.
4. Conservar copia del archivo de sitios completo existente y agregar **solo** `deploy/hostinger/crm.caddy` como `/srv/infrastructure/caddy/sites/crm.caddy`. No reemplazar Caddyfile, compose, `apps.caddy` ni otros fragmentos.
5. Validar la configuración completa y, únicamente si pasa, recargar sin detener el proxy:

```sh
docker exec scalaris-proxy-caddy-1 caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
docker exec scalaris-proxy-caddy-1 caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
```

6. Verificar HTTP→HTTPS, certificado público, HSTS, login Marketing/administración, cookie Secure/HttpOnly/SameSite, CSRF, importación sintética >1 MiB, aislamiento, exclusiones y cero estados enviados. Comprobar también las URLs previamente operativas de las otras aplicaciones.
7. Si falla el corte inicial, retirar solo el nuevo `crm.caddy`, volver a validar/recargar y conservar base/imágenes para diagnóstico. No borrar volúmenes ni detener otros proyectos. [Rollback completo](rollback.md).

## Respaldo y monitoreo

Después de verificar la copia/recuperación real, instalar las cuatro unidades `scalaris-crm-{backup,health}.{service,timer}` en `/etc/systemd/system`, conservando propietario root y permisos 0644. Sus scripts son parte del release revisado. Preparar `operations.conf` desde el ejemplo con modo privado; mantener `CRM_ALERTING_ENABLED=false` mientras no haya destino aprobado.

```sh
sudo systemctl daemon-reload
sudo systemctl start scalaris-crm-backup.service
sudo systemctl start scalaris-crm-health.service
sudo systemctl enable --now scalaris-crm-backup.timer scalaris-crm-health.timer
systemctl list-timers scalaris-crm-backup.timer scalaris-crm-health.timer
```

La copia corre aproximadamente a las 03:15 de Argentina, con zona `America/Argentina/Buenos_Aires` explícita; el chequeo, cada cinco minutos. Revisar los resultados de los servicios y `/srv/apps/crm/state/health.json`. Los timers no realizan automáticamente una recuperación ni borran copias. La copia local no es un backup externo: siguen pendientes destino cifrado externo, retención, RPO/RTO y monitoreo independiente del VPS. Ninguna notificación externa se habilita al preparar el release.

El edge público permite health mínimo y bloquea las demás rutas `/actuator/*`. Prometheus requiere sesión administrativa con `SETTINGS_MANAGE` mediante acceso privado; elegir un sistema de métricas no autoriza abrirlo públicamente. Revalidar la IP de confianza después de recrear Caddy y antes de cualquier release del CRM.

## Evidencia y límites

`scripts/test-production-proxy.js` verifica la cadena Caddy→nginx con TLS interno desechable, redirección, cookie de sesión sintética, límites y cabeceras. No solicita un certificado público ni usa usuarios/datos reales. La CI, doble validación integral, backend/E2E y restore reales del release exacto son gates adicionales. Registrar por separado lo preparado, lo probado localmente y lo ejecutado en Hostinger; no convertir este runbook en una afirmación de despliegue.
