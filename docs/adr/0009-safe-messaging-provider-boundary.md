# ADR 0009: frontera fail-closed para proveedores de mensajería

Fecha: 2026-07-21

Estado: aceptado

## Contexto

El CRM necesita crear borradores, simular comunicaciones y preparar integraciones
Gmail/WhatsApp sin habilitar comunicaciones reales. Un único flag no es una
frontera suficiente: una mala selección de bean, una ruta olvidada o una
configuración parcial podrían transformarse en una salida de red.

## Decisión

Se mantienen contratos internos pequeños (`EmailProvider`, `WhatsAppProvider`,
`MessageRenderer`, `MessagePolicy`, `MessageDispatcher`,
`InboundMessageProvider`, `DeliveryStatusProvider`) dentro del monolito modular.
No se agrega broker ni servicio externo.

La salida queda protegida por capas independientes:

1. la API pública expone únicamente borrador, simulación, enlace manual y estado
   de seguridad; deliberadamente no existe `/api/v1/messages/send`;
2. RBAC exige `MESSAGE_DRAFT` o `MESSAGE_SIMULATE`;
3. `MessagePolicy` valida organización, prospecto, contacto, canal,
   elegibilidad, consentimiento y exclusiones;
4. las cuatro variables de entorno permanecen bloqueadas;
5. `system_setting` conserva el kill switch, dry-run, enabled y límite diario
   por organización;
6. `MESSAGING_REAL_NETWORK_ALLOWED=false` impide inicializar beans reales;
7. los adapters Gmail se construyen en modo draft-only y WhatsApp recibe la
   decisión ambiental de envío; con la configuración canónica ambos quedan
   incapaces de enviar;
8. `NOOP`, `FAKE` y enlaces manuales son los únicos modos activos por defecto;
9. cada borrador/simulación conserva idempotencia, intento y auditoría sin
   registrar tokens.

El resultado operativo permitido es `DRAFT_CREATED`,
`PROVIDER_DRAFT_CREATED`, `SIMULATED` o un bloqueo tipado. `SENT` no forma parte
del estado persistible de `message_record` en V11.

## Proveedores reales

`GmailEmailProvider` implementa el contrato HTTP de borradores y conserva el
envío detrás de una capacidad de construcción que la configuración de la
aplicación nunca habilita. `WhatsAppCloudProvider` implementa el contrato HTTP
versionado, pero no se selecciona con red real bloqueada y tampoco puede enviar
si los controles ambientales están cerrados.

Los contract tests usan exclusivamente un servidor HTTP de loopback, tokens
sintéticos y versiones sintéticas. No realizan llamadas a Google o Meta.

## Consecuencias

- La operación local puede crear borradores, simulaciones determinísticas y
  deep-links sin red externa.
- Conectar OAuth, registrar webhooks y aprobar un envío controlado exige una
  decisión operativa posterior, credenciales externas y cambio explícito de
  política.
- La configuración parcial falla al iniciar en vez de degradar a envío real.
- SEG-009 puede reutilizar los contratos inbound/status sin modificar la
  frontera de salida.

## Alternativas descartadas

- SMTP o llamadas HTTP directas desde controllers: dispersan las políticas.
- Un motor genérico de plugins: no existe necesidad demostrada.
- Tokens en frontend/localStorage: rompe la frontera de secretos.
- Habilitar WhatsApp/Gmail para “probar”: está fuera de autorización.
