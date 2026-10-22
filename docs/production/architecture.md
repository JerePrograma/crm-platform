# Arquitectura de despliegue Scalaris

El CRM conserva el monolito modular Java 21, frontend React/nginx y PostgreSQL 17 como fuente de verdad. Flyway aplica V1–V14; el perfil `SCALARIS` prepara organización, catálogo, administrador y Marketing. Marketing no administra usuarios ni realiza envíos reales.

`deploy/docker-compose.production.yml` define aplicaciones sin root, filesystem de solo lectura salvo tmpfs, límites de CPU/memoria, rotación de logs, readiness y apagado gradual. PostgreSQL tiene volumen propio y ninguna publicación al host. El frontend escucha únicamente en loopback; las guardas de envío son constantes cerradas. Este archivo es una base de despliegue y de ensayo, no evidencia de una instalación productiva.

El destino preparado es Hostinger: `crm.scalaris.com.ar`, proyecto Compose `scalaris-crm`, `/srv/apps/crm` y Caddy compartido existente. Solo frontend se une a la red externa `proxy` mediante el alias exclusivo `scalaris-crm-frontend`; base y backend siguen en la red privada del proyecto. El puerto auxiliar es `127.0.0.1:18095`. El [runbook Hostinger](hostinger.md) describe los archivos revisables y el corte autorizado.

El edge termina TLS. nginx confía en `X-Forwarded-Proto` únicamente cuando la dirección de conexión aparece en un archivo revisado, montado como solo lectura; el archivo por defecto no confía en ningún peer. El edge elimina cabeceras reenviadas alternativas y nginx las vuelve a retirar. La confianza no se extiende a toda la red compartida. Una recreación de Caddy que cambie su IP requiere revisar la allowlist y recargar nginx.

La sesión es local a una instancia backend. Este perfil no habilita múltiples réplicas ni alta disponibilidad; escalar exige decidir persistencia de sesión, estrategia de migración y capacidad de PostgreSQL. Outbox sigue siendo at-least-once, con proveedores y worker reales deshabilitados.
