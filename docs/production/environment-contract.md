# Contrato del entorno productivo

Secretos fuera de Git, imágenes, registros y tickets: `DATABASE_PASSWORD`, `CRM_BOOTSTRAP_USERNAME`, `CRM_BOOTSTRAP_PASSWORD`, `CRM_MARKETING_USERNAME` y `CRM_MARKETING_PASSWORD`. Las cuentas deben diferir y sus claves deben cumplir la política mínima de 12 caracteres. Inyectarlos con el gestor de secretos del entorno o un archivo privado 0600/0400; nunca imprimir `docker compose config` sin `--quiet` en producción. Rotar las contraseñas iniciales desde la aplicación; editar las variables no cambia usuarios ya persistidos.

El perfil fija `SPRING_PROFILES_ACTIVE=scalaris`, `CRM_BUSINESS_PROFILE=SCALARIS` y guardas cerradas:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
OUTBOX_WORKER_ENABLED=false
FAKE_INBOUND_ENABLED=false
```

No incluir credenciales de Gmail/WhatsApp en este despliegue. Mantener la política persistente de envío cerrada. Habilitar proveedores requiere una misión y autorización independientes.

| Variable | Valor/contrato Hostinger |
|---|---|
| `PRODUCTION_FRONTEND_PORT` | `18095`, publicado solo en `127.0.0.1` |
| `SESSION_COOKIE_SECURE` | `true` con TLS; `false` solo en ensayo HTTP aislado |
| `TRUSTED_EDGE_CONFIG_FILE` | `/srv/apps/crm/config/nginx-trusted-edge.conf`, peer Caddy exacto `/32` |
| `CRM_BACKEND_IMAGE`, `CRM_FRONTEND_IMAGE` | Requeridas en Hostinger: digest de registry o tag `release-<commit completo>`; se rechazan `local`/`latest` |
| `CRM_RELEASE_COMMIT` | SHA completo de 40 caracteres del release revisado, común a ambas imágenes |
| `CRM_BACKEND_IMAGE_ID`, `CRM_FRONTEND_IMAGE_ID` | IDs locales `sha256:...` registrados al validar cada artefacto; preflight los compara con `docker image inspect` |
| `CRM_MARKETING_DISPLAY_NAME` | `Marketing Scalaris` por defecto |
| `DATABASE_POOL_SIZE`, `DATABASE_MIN_IDLE` | 10 / 1 por defecto; revisar con carga medida |

Los nombres técnicos `gestudio_crm`, `gestudio` y el volumen `production_postgres` se conservan para no cambiar bases existentes por accidente. El proyecto Compose exclusivo crea su propio volumen; no comparte la instalación de uso local ni otros CRM del VPS.

El override Hostinger fija PostgreSQL `17.11-alpine` por digest de manifiesto. La base de smoke conserva su selección histórica PostgreSQL 17. Las imágenes CRM deben estar cargadas o descargadas antes del preflight; si una etiqueta fue reutilizada o su ID difiere del inventario aprobado, el chequeo falla. Cuando existe label OCI `org.opencontainers.image.revision`, también debe coincidir con el commit.

Las variables opcionales de operación se guardan en `/srv/apps/crm/config/operations.conf`, basadas en `deploy/hostinger/operations.conf.example`. La alerta externa permanece deshabilitada; no hay destino inventado ni notificaciones enviadas por preparar estos archivos.
