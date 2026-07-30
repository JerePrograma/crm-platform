# Integración Gmail — OAuth y campañas controladas

Estado: `IMPLEMENTED_NOT_CONNECTED`

## Implementado

- Authorization Code backend con acceso offline, state hasheado, ligado a
  sesión/usuario/tenant, expiración, un solo uso y replay protection;
- scope de identidad mínimo más `gmail.send`;
- refresh token AES-256-GCM con nonce único, AAD, versión de clave y rotación;
- cuentas remitentes tenant-scoped `CONNECTED|REAUTH_REQUIRED|REVOKED|ERROR`,
  verificación, default, reconexión y revocación;
- provider de campaña con `users.messages.send`, un destinatario por request;
- MIME UTF-8 `multipart/alternative` codificado base64url, Reply-To validado,
  Message-ID, baja visible y cabeceras RFC 8058;
- asociación por provider message ID/thread ID y persistencia de HTTP,
  categoría, intentos, correlación e idempotencia;
- 400/401/403/429/5xx, `Retry-After`, `invalid_grant`, timeout y respuesta
  ambigua tipados; un ambiguo no se reintenta automáticamente;
- URL HTTPS obligatoria, salvo loopback en contract tests;
- NOOP sin secretos y Gmail incompleto fail-closed;
- contract, PostgreSQL y Playwright contra Google falso sin llamada a Internet.

Documentación oficial consultada el 2026-07-21:

- [OAuth 2.0 para aplicaciones web](https://developers.google.com/identity/protocols/oauth2/web-server)
- [Crear borradores](https://developers.google.com/workspace/gmail/api/guides/drafts)
- [Enviar email](https://developers.google.com/workspace/gmail/api/guides/sending)
- [Sincronización incremental](https://developers.google.com/workspace/gmail/api/guides/sync)

## Modo seguro actual

```dotenv
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
EMAIL_PROVIDER_MODE=NOOP
MESSAGING_REAL_NETWORK_ALLOWED=false
```

No se versiona client secret, refresh token ni archivo OAuth. El frontend no
recibe tokens, state ni code. No existe `/api/v1/messages/send`; el único camino
LIVE es campaña aprobada y confirmada → mensaje individual → outbox → validación
final → Gmail.

## Activación manual pendiente

Requiere recursos externos y revisión operativa:

1. registrar una aplicación OAuth y redirect URI HTTPS;
2. completar la verificación de `gmail.send` que Google exija;
3. provisionar client secret y keyring AES fuera de Git;
4. verificar TLS, SPF, DKIM, DMARC, dominio, Reply-To y URL pública de baja;
5. conectar una cuenta no operativa autorizada y ejecutar un smoke manual con
   un único destinatario sintético autorizado;
6. mantener hard cap 10/día, concurrencia 1 y observar métricas;
7. habilitar cada guarda solo mediante autorización separada.

Lectura de buzón, Pub/Sub, tracking, rebotes automáticos y reconciliación de
respuestas permanecen fuera de alcance. Aceptación por Gmail no equivale a
entrega al buzón.
