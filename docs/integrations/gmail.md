# Integración Gmail — contrato deshabilitado

Estado: `IMPLEMENTED_NOT_CONNECTED`

## Implementado

- provider de creación de borradores con `users.drafts.create`;
- MIME UTF-8 codificado base64url;
- asociación por draft ID/thread ID;
- scope mínimo `gmail.compose` para el modo disponible;
- constantes aisladas para `gmail.send` y `gmail.readonly`;
- timeouts, errores tipados, 429/5xx reintentables y respuestas validadas;
- URL HTTPS obligatoria, salvo loopback en contract tests;
- cursor persistible en `integration_connection.cursor_value` para history ID;
- credencial persistible solo como bytes cifrados más key ID;
- contract test local sin llamada a Google;
- revocación/desconexión representables por el estado de integración.

Documentación oficial consultada el 2026-07-21:

- [OAuth 2.0 para aplicaciones web](https://developers.google.com/identity/protocols/oauth2/web-server)
- [Crear borradores](https://developers.google.com/workspace/gmail/api/guides/drafts)
- [Enviar email](https://developers.google.com/workspace/gmail/api/guides/sending)
- [Sincronización incremental](https://developers.google.com/workspace/gmail/api/guides/sync)

## Modo seguro actual

```dotenv
EMAIL_PROVIDER_MODE=NOOP
MESSAGING_REAL_NETWORK_ALLOWED=false
GMAIL_ACCESS_TOKEN=
GMAIL_SCOPES=
```

No se versiona client secret, refresh token ni archivo OAuth. El frontend no
recibe tokens. El modo alternativo preparado es `GMAIL_DRAFT_ONLY`; no existe un
modo de envío seleccionable por la API del CRM.

## Activación manual pendiente

Requiere recursos externos y revisión operativa:

1. registrar una aplicación OAuth y redirect URI HTTPS;
2. elegir scopes mínimos y completar la verificación que Google exija;
3. proveer cifrado/key management externo para refresh tokens;
4. implementar y revisar la ceremonia de callback/revocación con credenciales
   de prueba de una cuenta no operativa;
5. seleccionar `GMAIL_DRAFT_ONLY`, mantener todos los switches de envío
   bloqueados y ejecutar contract/safety tests;
6. no habilitar envío sin autorización separada.

El polling incremental y la asociación de respuestas se implementan en SEG-009
sobre `InboundMessageProvider`; no son necesarios para crear un borrador seguro.
