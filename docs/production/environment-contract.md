# Production environment contract

Required secrets are injected at runtime: `DATABASE_PASSWORD`, `CRM_BOOTSTRAP_USERNAME`, and `CRM_BOOTSTRAP_PASSWORD`. They must never be placed in Git, images, Compose files, transcripts, or support tickets. Rotate the bootstrap credential after first use and follow the platform secret manager's rotation process.

The mandatory messaging boundary is `SENDING_ENABLED=false`, `SENDING_DRY_RUN=true`, `SENDING_DAILY_LIMIT=0`, `SENDING_KILL_SWITCH=true`, `MESSAGING_REAL_NETWORK_ALLOWED=false`, `EMAIL_PROVIDER_MODE=NOOP`, and `WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY`. Environment blocks dominate database preferences. Real provider credentials must be absent.

Set `SESSION_COOKIE_SECURE=true` behind TLS. Configure the external domain, restrictive origin policy at the edge, trusted forwarding headers, database pool size, JVM memory, and `PRODUCTION_FRONTEND_PORT` as deployment-specific values. PostgreSQL must only be reachable on the private application network.
