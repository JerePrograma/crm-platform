# Integración WhatsApp Cloud — contrato deshabilitado

Estado: `IMPLEMENTED_NOT_CONNECTED`

## Implementado

- provider HTTP con versión Graph configurable y ruta por phone number ID;
- payload de texto limitado, destinatario normalizado y token Bearer;
- HTTPS obligatorio salvo loopback en contract tests;
- timeouts, validación de respuesta, errores tipados y 429/5xx reintentables;
- columnas para WABA/phone metadata, credencial cifrada y cursor;
- contratos separados para inbound y delivery status;
- modo `DEEPLINK_ONLY` manual y provider `NOOP`/`FAKE`;
- contract test local con versión, IDs y tokens exclusivamente sintéticos.

Referencia oficial intentada el 2026-07-21:

- [WhatsApp Cloud API — Send Messages](https://developers.facebook.com/docs/whatsapp/cloud-api/guides/send-messages)

La documentación de Meta devolvió una pantalla de autenticación y luego rate
limit desde este entorno. Por eso la versión Graph no se fija en código ni se
declara la integración conectada. Antes de conectar, la versión y los payloads
deben cotejarse nuevamente con documentación oficial accesible.

## Modo seguro actual

```dotenv
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
MESSAGING_REAL_NETWORK_ALLOWED=false
WHATSAPP_GRAPH_API_VERSION=
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_BUSINESS_ACCOUNT_ID=
WHATSAPP_ACCESS_TOKEN=
```

El deep-link solo se entrega al navegador luego de una acción explícita; el CRM
no lo abre, no registra `SENT` y no afirma delivery.

## Activación manual pendiente

Requiere cuenta WABA, app Meta, phone number ID, token, plantillas aprobadas,
dominio HTTPS y secreto de webhook. Luego corresponde:

1. verificar la versión Graph vigente y los contratos oficiales;
2. configurar credenciales mediante secretos externos;
3. registrar y validar webhook/firma/replay protection de SEG-009;
4. ejecutar contract tests contra un entorno de prueba controlado;
5. mantener `DEEPLINK_ONLY` o `NOOP` hasta autorización separada de envío.
