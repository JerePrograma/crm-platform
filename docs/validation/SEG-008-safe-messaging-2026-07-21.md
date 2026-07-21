# SEG-008 — Evidencia de mensajería segura

Fecha: 2026-07-21

Rama: `feat/complete-crm-platform`

Commit inicial: `cf3e8ed4a5c78ceecffdb91cf9487ae0537d5b06`

## Resultado

```text
SEG_008=EXECUTED_PASS
EMAIL_NOOP=EXECUTED_PASS
EMAIL_FAKE=EXECUTED_PASS
EMAIL_REAL_ADAPTER=IMPLEMENTED_NOT_CONNECTED
WHATSAPP_NOOP=EXECUTED_PASS
WHATSAPP_FAKE=EXECUTED_PASS
WHATSAPP_REAL_ADAPTER=IMPLEMENTED_NOT_CONNECTED
REAL_SENDING=DISABLED_BY_POLICY
REAL_NETWORK_CALLS_DURING_TESTS=0
SEND_ENDPOINT_AVAILABLE=false
PERSISTED_SENT_ROWS=0
```

## Contrato ejecutado

- V11 crea `integration_connection`, `message_record` y
  `message_provider_attempt` con tenant, constraints, índices e idempotencia;
- policy valida prospecto/contacto/canal dentro de la organización y aplica
  elegibilidad, consentimiento y exclusiones;
- entorno y PostgreSQL deben permitir simultáneamente un envío; la configuración
  vigente bloquea ambos;
- `NOOP` crea un borrador local con razón de bloqueo;
- `FAKE` produce IDs determinísticos y actividad `SIMULATED` sin red;
- Gmail y WhatsApp se ejercitan solo contra `HttpServer` loopback;
- la API expone safety/draft/simulation/manual-link y no expone send;
- la UI muestra providers, red bloqueada y endpoint inexistente.

## Validaciones

### Focalizadas

```text
mvn -Dtest=MessagingIntegrationTest,ProviderContractTest test
PASS — 6/6 — 40.785 s

mvn -Dtest=MessagingIntegrationTest,ProviderContractTest,DuplicateResolutionIntegrationTest,SecurityAuthorizationIntegrationTest test
PASS — 20/20 — 01:19 min
```

Las pruebas demostraron:

- draft idempotente `DRAFT_CREATED`/`NOOP`;
- simulación `SIMULATED`/`FAKE` y actividad persistida;
- exclusión bloqueada antes de persistir el cuerpo;
- defaults `NOOP` y `DEEPLINK_ONLY` con red falsa;
- inicialización real rechazada con red falsa;
- Gmail draft-only y WhatsApp HTTP mediante loopback;
- 429 tipado como reintentable;
- viewer sin permiso de mutación;
- administrador autenticado recibe 404 en `/api/v1/messages/send`;
- merge conserva el mensaje y remapea contacto/referencias.

### Suite completa

```text
mvn verify
PASS — 57/57 — 03:04 min — Java 21.0.7 — PostgreSQL 17 Testcontainers
Spotless — 119/119 limpios
ArchUnit — PASS

npm run build
PASS — TypeScript strict + Vite — 17 módulos

docker compose --profile app build --no-cache backend frontend
PASS — frontend npm ci/build + backend Java 21 package

docker compose --profile app up -d --wait postgres backend frontend
PASS — PostgreSQL/backend/frontend healthy — 25432/8080/5173

powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
PASS — health, sesión, API y frontend

powershell -ExecutionPolicy Bypass -File scripts/check-repository-safety.ps1
PASS
```

Flyway migró el volumen sintético local de V10 a V11 y Testcontainers ejecutó
V1→V11 desde vacío. Hibernate validate pasó como parte de cada contexto.

### Recorrido visual

Playwright con Chrome del sistema ejecutó:

```text
login
→ Mensajes e integraciones
→ provider Email NOOP
→ red real BLOQUEADA
→ endpoint de envío INEXISTENTE
→ seleccionar prospecto/contacto sintético
→ simular con fake
→ SIMULATED mediante FAKE
→ BLOCKED_BY_KILL_SWITCH
```

Resultado: sin errores de consola. Captura local ignorada por Git:
`validation-output/seg008-messaging-ui-20260721.png`.

## Verificación de datos y configuración

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
flyway_schema_history=11
message_record.status_SENT=0
```

## Advertencias

- Mockito avisa que el self-attach dinámico cambiará en un JDK futuro;
- Hikari registra warnings transitorios cuando algunos Testcontainers ya fueron
  cerrados entre contextos; no hubo fallo ni impacto de datos;
- la documentación Meta no estuvo plenamente accesible desde el entorno;
  versión/payload deben verificarse nuevamente antes de conectar;
- Gmail OAuth y WhatsApp Cloud requieren credenciales/cuentas externas y no se
  probaron contra servicios reales.
