# SEG-001 — Gmail y ejecución real de campañas

Registro canónico vivo de la implementación recuperada y completada entre el
2026-07-28 y el 2026-07-30. Este
documento no reemplaza `docs/segments/SEG-001.md`, que conserva la trazabilidad
de la vertical slice histórica. Los estados permitidos en este registro son
`EXECUTED_PASS`, `EXECUTED_FAIL`, `NOT_RUN` y `BLOCKED_EXTERNAL`.

## Parte 0 — Estado y Git

### Preflight y recuperación ejecutados

```text
fecha local de recuperación: 2026-07-30
repositorio: C:/laburo/crm-platform
remoto: origin https://github.com/JerePrograma/crm-platform
rama: main
commit inicial: 12421c53375deabebe8f48f17af3ae95af95893b
origin/main cacheado inicial: 12421c53375deabebe8f48f17af3ae95af95893b
origin/main remoto y posterior al fetch: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
working tree preservado: 23 archivos rastreados y 45 no rastreados
unión del snapshot: 68 archivos, 68 hashes SHA-256 verificados
diff rastreado inicial: 23 archivos, +1730/-82
staged inicial: ninguno
relación: HEAD local es ancestro de origin/main, exit 0
merge-base: 12421c53375deabebe8f48f17af3ae95af95893b
fast-forward: EXECUTED_PASS a 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
integridad posterior: 68/68 hashes idénticos y 45/45 no rastreados presentes
```

Se creó y verificó un snapshot externo, fuera del repositorio, con patches
binarios de working tree e index, manifiesto CSV, archivos preservados,
eliminaciones/renombres y hashes SHA-256. La ubicación privada completa no se
versiona; su nombre lógico es `crm-platform-seg001-recovery-20260730-015431`.

La única referencia retirada fue:

```text
refs/codex/turn-diffs/captures/1785355393050/0a385f69-64b6-4589-ad54-1e6abdddb027/base
```

Era un loose ref de 41 bytes NUL, no estaba en `packed-refs`, no contenía un
object ID válido y `git cat-file -e` devolvió 128. Se respaldaron contenido,
timestamp, ruta y SHA-256 `9e1736...`. `git update-ref -d` devolvió exit 1, por
lo que se movió exclusivamente ese archivo exacto al backup. Después,
`git show-ref` y `git for-each-ref` resolvieron todas las referencias;
`git fsck --full --no-reflogs` devolvió exit 0 y solo informó un commit remoto
dangling no alcanzable. `git fetch --prune origin` y el fast-forward posterior
pasaron sin sobrescritura.

### Arquitectura encontrada

- monolito modular Java 21 y Spring Boot 4.1;
- PostgreSQL 17 como fuente de verdad, Flyway V1–V13 y Hibernate `validate`;
- sesión same-origin, cookie HttpOnly, CSRF, RBAC y tenant isolation;
- frontend React 19, TypeScript strict, Vite, Vitest y Playwright;
- campañas con plantillas versionadas, audiencias congeladas, aprobación y
  simulación;
- mensajería `NOOP`/`FAKE`, adapters de red desconectados y outbox PostgreSQL
  at-least-once con `SKIP LOCKED`, leases, CAS, retry y dead-letter;
- validadores Windows/Unix, Testcontainers, Docker Compose, backup/restore,
  npm audit, Grype y repository safety.

### Componentes reutilizados

| Componente real | Símbolo/contrato reutilizado |
|---|---|
| `campaign` | `CampaignService`, `CampaignController`, `CampaignState`, V10 |
| `messaging` | `EmailProvider`, `GmailEmailProvider`, `MessagePolicy`, `MessageDispatcherService`, V11 |
| `outbox` | `OutboxPublisher`, `OutboxWorkerService`, `OutboxEventProcessor`, V12 |
| identidad/tenant | `CurrentActor`, `CrmPrincipal`, `MESSAGE_SEND` |
| seguridad | `SecurityConfig`, sesión, CSRF y `ApiExceptionHandler` |
| exclusiones | `exclusion`, `DefaultMessagePolicy`, `ExclusionApplicationService` |
| auditoría | `AuditEventWriter` |
| configuración | `SendingProperties`, `OrganizationSettingsService`, `MessagingProperties` |
| frontend | `App.tsx`, `api.ts`, `types.ts`, `uiLabels.ts`, `decisionDialog.ts` |
| validación | `validate-complete-crm.ps1/.sh`, `verify-migrations.ps1/.sh`, Playwright |

### Limitaciones iniciales corregidas

- `EMAIL_PROVIDER_MODE=NOOP`, red real bloqueada, límite cero y kill switch
  activo son los valores versionados predeterminados;
- no existe `/api/v1/messages/send` y debe seguir sin existir;
- `GmailEmailProvider` solo es seleccionable como `GMAIL_DRAFT_ONLY` y consume
  un `GMAIL_ACCESS_TOKEN` estático no apto para producción;
- la implementación recuperada incorporó OAuth offline, refresh/revocación,
  múltiples cuentas por tenant, one-click unsubscribe y ejecución LIVE
  exclusivamente por campaña/outbox;
- MIME pasó a UTF-8 `multipart/alternative` con cabeceras de baja y manejo
  explícito de resultados ambiguos;
- V14 amplió aditivamente `integration_connection`, campañas, mensajes,
  intentos, tokens de baja, ledger y supresión;
- se agregó generador reproducible HTML/PDF/PNG/JSON/ZIP;
- Gmail real y todos los envíos reales continúan deshabilitados por defecto.

### Archivos y símbolos reconciliados

La matriz de reconstrucción se cerró contra el diff y los consumidores:

```text
V14 + GmailSenderAccountRepository/Service/Controller
GmailOAuthStateService + GoogleOAuthClient + GmailTokenCipher
GmailMimeBuilder + GmailApiClient + CampaignOnlyGmailEmailProvider
CampaignDeliveryService + CampaignMessageDeliveryService
UnsubscribeTokenService/Service/RateLimiter/Controller
CampaignService/Controller + outbox/messaging/settings/security
GmailSenderAccountsPanel + App/api/types/uiLabels/gmailCampaignUi
fake-google-server + docker-compose.gmail-fake.yml
validate-gmail-live-fake.ps1 + Playwright + generador de manual
```

No quedaron símbolos sin consumidor, endpoints sin UI, mocks en producción,
`TODO`, `FIXME`, `UnsupportedOperationException`, métodos vacíos ni retornos
temporales en el alcance SEG-001.

### Checklist

- [x] ruta Git real confirmada;
- [x] rama, remoto, HEAD y working tree relacionado confirmados desde cero;
- [x] snapshot externo creado y verificado;
- [x] referencia exacta respaldada y retirada sin tocar otras referencias;
- [x] `fsck`, fetch, ancestría y fast-forward ejecutados;
- [x] integridad 68/68 posterior al fast-forward verificada;
- [x] documentación de continuidad y fuentes canónicas leídas;
- [x] arquitectura, migraciones, permisos, campañas, mensajería, outbox,
  frontend, pruebas y validadores inspeccionados;
- [x] consumidores de `GMAIL_ACCESS_TOKEN`, `MESSAGE_SEND` y `/messages/send`
  localizados;
- [x] inventario final de archivos y símbolos reconciliado con el diff.

## Parte 1 — Alcance y decisiones de arquitectura

### Objetivo e incluido

Permitir que un administrador conecte una cuenta Gmail por OAuth 2.0 y que un
usuario con `MESSAGE_SEND` apruebe y programe campañas individuales, dosificadas
y trazables mediante el outbox existente. Incluye cuentas remitentes tenant,
OAuth offline, cifrado autenticado, Gmail `users.messages.send`, control de
campaña, baja inmediata, UI, auditoría, pruebas falsas y operación fail-closed.

### Fuera de alcance

Lectura o sincronización del buzón, Gmail Pub/Sub, clasificación de respuestas,
rebotes automáticos, SMTP, App Passwords, service accounts, domain-wide
delegation, tracking, rotación evasiva, WhatsApp real, datos reales y despliegue
productivo.

### Flujo textual

```text
ADMIN + SETTINGS_MANAGE
  -> POST inicio OAuth con CSRF
  -> state aleatorio, tenant/usuario/sesión/expiración y uso único
  -> Google falso/real autorizado por configuración externa
  -> callback backend + code exchange + userinfo
  -> refresh token AES-256-GCM en PostgreSQL
  -> sender account CONNECTED

campaña LIVE + sender + plantilla inmutable
  -> audiencia congelada y explicada
  -> aprobación CAMPAIGN_APPROVE
  -> confirmación SEND_LIVE_CAMPAIGN + MESSAGE_SEND
  -> message_record por destinatario + outbox por destinatario
  -> claim corto / lease / validación final
  -> límites, ventana, exclusión, consentimiento e idempotencia
  -> refresh access token en memoria
  -> POST Gmail users.messages.send
  -> GMAIL_ACCEPTED | RETRY | AMBIGUOUS | PERMANENT_FAILURE
  -> progreso de campaña y auditoría sanitizada
```

### Decisiones

- extender V10–V12 y no crear una segunda cola;
- ampliar aditivamente `integration_connection` para admitir múltiples cuentas
  Gmail por tenant sin duplicar el modelo de integración;
- reutilizar `MESSAGE_SEND`; no crear un permiso redundante;
- usar `java.net.http.HttpClient`, MIME/base64url y AES-GCM de Java 21; no sumar
  SDK Google ni dependencia MIME;
- conservar `/api/v1/messages/send` inexistente; solo campaña/outbox puede
  solicitar ejecución;
- validar OAuth/Gmail en un stack sintético separado del stack NOOP canónico;
- considerar una aceptación de Gmail como aceptación del proveedor, nunca como
  entrega al buzón;
- no reintentar automáticamente un resultado HTTP ambiguo.

### Alternativas rechazadas

SMTP/App Password, BCC, SDK Google completo, broker externo, token en frontend,
token estático productivo, transacción SQL abierta durante HTTP, endpoint
genérico de envío y modificación destructiva de V11.

### Estrategia fail-closed

Los flags de entorno dominan PostgreSQL. NOOP inicia sin secretos. El modo Gmail
exige configuración OAuth, cifrado y URLs válidas al arrancar, pero aun así no
envía sin todas las guardas, límites positivos, cuenta conectada, aprobación,
permiso, confirmación y kill switch desactivado.

### Checklist

- [x] objetivo, incluido y fuera de alcance delimitados;
- [x] diagrama textual definido sobre componentes reales;
- [x] alternativas rechazadas documentadas;
- [x] estrategia fail-closed definida;
- [x] arquitectura ejecutada y verificada end-to-end contra Google falso.

## Parte 2 — Modelo de datos y migraciones

### Diseño V14

- `integration_connection`: amplía el modelo existente con email normalizado,
  nombre, estado `CONNECTED|REAUTH_REQUIRED|REVOKED|ERROR`, default, scopes,
  ciphertext/nonce/tag implícito GCM, key version, actor, conexión/verificación/
  revocación/error sanitizado y optimistic locking;
- `gmail_oauth_state`: hash de state, organización, usuario, hash de sesión,
  sender de reconexión, expiración y consumo único;
- extensiones aditivas de `campaign`: modo `SIMULATION|LIVE`, sender, Reply-To,
  zona, ventana, días, límite, intervalo, reintentos, stop config, schedule,
  aprobador/ejecutor y timestamps;
- extensiones aditivas de `message_record`: estado individual, sender, categoría,
  HTTP, intentos, next attempt, aceptación, correlation e idempotencia;
- `unsubscribe_token`: hash opaco tenant-safe ligado a mensaje/campaña/canal;
- `delivery_daily_ledger`: reservas y aceptaciones por sender y fecha
  local;
- `global_contact_suppression`: hash de canal global tenant-safe para que una
  baja prevalezca aun frente a nuevas campañas;
- constraints, FK e índices tenant-first; único default parcial por organización.

### Compatibilidad y reversión

Los registros históricos se backfillean como `SIMULATION`; no se borran ni
renombran columnas. El rollback operativo es volver a NOOP, activar kill switch,
pausar workers, revocar cuentas y desplegar la imagen anterior compatible. Las
tablas V14 permanecen sin uso; su eliminación requeriría una migración futura y
backup, nunca un down destructivo.

### Checklist

- [x] V14 aditiva creada;
- [x] esquema vacío V1→V14 ejecutado;
- [x] upgrade V13→V14 ejecutado;
- [x] upgrade V11→V14 preservado en validadores;
- [x] constraints/default/tenant/optimistic locking probados;
- [x] impacto histórico y rollback verificados.

## Parte 3 — OAuth y cuentas remitentes

### Contrato

Authorization Code server-side con `response_type=code`, `access_type=offline`,
`prompt=consent`, scopes `openid email profile gmail.send`, redirect exacta,
state de 256 bits hasheado y ligado a usuario/sesión/organización con expiración
y consumo único. El code solo se intercambia en backend y no se persiste. El
access token vive en memoria durante cada operación. El refresh token se cifra
con AES-256-GCM y AAD tenant/cuenta.

La reconexión conserva el refresh token anterior cuando el token response no
trae uno nuevo. `invalid_grant` cambia la cuenta a `REAUTH_REQUIRED`. La
revocación remota es best-effort y la revocación local es obligatoria. Todos los
queries incluyen `organization_id`.

### Checklist

- [x] URL OAuth y scopes mínimos implementados;
- [x] state aleatorio, expiración, sesión, tenant, single-use y replay cubiertos;
- [x] callback e intercambio backend implementados;
- [x] refresh token cifrado y access token no persistido;
- [x] conectar, listar, verificar, default, reconectar y revocar implementados;
- [x] `invalid_grant` y errores sanitizados probados;
- [x] aislamiento tenant y permisos probados.

## Parte 4 — Adaptador Gmail

### Contrato

Cliente REST con timeouts explícitos y URL configurable validada. Envío único:

```text
POST /gmail/v1/users/me/messages/send
Authorization: Bearer <access token efímero>
Content-Type: application/json
{"raw":"<RFC 5322 base64url>"}
```

El MIME será UTF-8 `multipart/alternative`, con `From`, `To`, `Reply-To`,
`Subject`, `Message-ID`, `Date`, `MIME-Version`, texto, HTML seguro,
`List-Unsubscribe`, `List-Unsubscribe-Post` y baja visible. Se rechazan CR/LF,
emails o longitudes inválidas. No se registra el cuerpo.

Categorías: validación permanente, reauth/revocación, scope, cuota/rate limit,
429 con `Retry-After`, 5xx, timeout de conexión reintentable, timeout/corte
ambiguo no reintentable, MIME, respuesta inválida y destinatario inválido.

### Checklist

- [x] MIME completo y base64url implementados;
- [x] HTML/texto/unicode/header injection probados;
- [x] cliente `users.messages.send` implementado;
- [x] 200/400/401/403/429/5xx/timeout/malformed/corte clasificados;
- [x] provider message/thread ID, HTTP y aceptación persistidos;
- [x] resultado ambiguo no reintentado;
- [x] UI/textos no afirman entrega.

## Parte 5 — Ejecución de campañas y outbox

### Estados y ejecución

Se reutilizan `DRAFT`, `READY_FOR_REVIEW`, `APPROVED`, `SCHEDULED`, `RUNNING`,
`PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED` y `SIMULATED`. `delivery_mode`
distingue inequívocamente simulación y real. Cualquier cambio material antes de
aprobar debe incrementar versión e invalidar aprobación.

La programación crea un mensaje y evento outbox por destinatario. Claim/lease,
recovery, backoff, dead-letter y CAS permanecen en V12. No hay HTTP dentro de la
transacción de claim. Un lease vencido durante una solicitud Gmail se marca
ambiguo y no se reintenta. Pausa/reanuda/cancela actualizan campaña, mensajes y
eventos tenant-scoped.

### Límites

Capas acumulativas: hard cap ambiental 10, límite ambiental activo, organización,
sender y campaña; contador por fecha en zona de campaña; intervalo mínimo;
concurrencia predeterminada 1; lunes a viernes; 09:30–17:30;
`America/Argentina/Buenos_Aires`. Defaults versionados continúan deshabilitados.

### Checklist

- [x] modo real/simulación y configuración material persistidos;
- [x] aprobación real e invalidación probadas;
- [x] confirmación `SEND_LIVE_CAMPAIGN` backend/frontend implementada;
- [x] programación, inicio, pausa, reanudación y cancelación implementados;
- [x] un message/outbox por destinatario y sin duplicados;
- [x] límites, ventana, día, intervalo y ledger probados;
- [x] lease/restart/retry/dead/ambiguous probados;
- [x] progreso y estados individuales persistidos.

## Parte 6 — Exclusiones, bajas y entregabilidad

### Validación final

Antes de cada envío se reevalúan tenant, campaña, sender, permiso, asociación,
correo, contacto, consentimiento (`DENIED` siempre bloquea), elegibilidad,
exclusiones, baja, rebote permanente conocido, pausas, flags, provider, límite,
intervalo, ventana, día, idempotencia, envío previo y render. Un fallo persiste
la razón sin PII innecesaria.

### Baja

Token opaco de alta entropía; solo su hash se persiste. GET muestra una página
genérica y POST RFC 8058 ejecuta una baja idempotente sin login, crea la exclusión
inmediata y cancela mensajes/seguimientos pendientes del canal. El endpoint usa
rate limit y no revela datos del prospecto.

### Entregabilidad

Antes de activación real se exige operación con TLS, SPF/DKIM, DMARC/alineación,
reputación y métricas. Gmail aceptar un mensaje no prueba entrega, apertura,
lectura ni respuesta. No se implementan tracking ni evasión.

### Checklist

- [x] reevaluación final completa implementada;
- [x] exclusión y `DENIED` nunca alcanzan al fake provider;
- [x] GET/POST one-click, hash, idempotencia y rate limit implementados;
- [x] baja cancela pendientes y seguimientos;
- [x] carrera baja/envío serializada y probada;
- [x] SPF/DKIM/DMARC, límites y pausas documentados.

## Parte 7 — API, permisos y auditoría

### API implementada

```text
GET  /api/v1/sender-accounts
GET  /api/v1/sender-accounts/configuration
POST /api/v1/sender-accounts/gmail/oauth/start
GET  /api/v1/sender-accounts/gmail/oauth/callback
POST /api/v1/sender-accounts/{id}/verify
POST /api/v1/sender-accounts/{id}/default
POST /api/v1/sender-accounts/{id}/reconnect
POST /api/v1/sender-accounts/{id}/revoke
PUT  /api/v1/campaigns/{id}/delivery
POST /api/v1/campaigns/{id}/schedule
POST /api/v1/campaigns/{id}/start
POST /api/v1/campaigns/{id}/pause
POST /api/v1/campaigns/{id}/resume
POST /api/v1/campaigns/{id}/cancel
GET  /api/v1/campaigns/{id}/progress
GET  /api/v1/campaigns/{id}/results
GET  /api/v1/campaigns/{id}/results.csv
GET  /api/v1/unsubscribe/{token}
POST /api/v1/unsubscribe/{token}
```

`SETTINGS_MANAGE` protege conexión/revocación; `CAMPAIGN_APPROVE` aprobación;
`MESSAGE_SEND` ejecución/pausa; `CAMPAIGN_READ`/`REPORT_READ` consulta. ADMIN ya
posee `MESSAGE_SEND`; MANAGER, SALES y VIEWER no.

Auditoría sanitizada cubrirá OAuth, ciclo de sender, campaña real, resultados,
retry/dead/ambiguo, baja y kill switch. No incluirá tokens, secrets, codes,
cuerpos, HTML, URL de baja ni correo completo.

Problem Details añadirá códigos estables sin stack ni response Google completa.
Inicio OAuth y mutaciones exigen CSRF; callback usa state; unsubscribe público
es el único POST externo CSRF-exempt y está acotado por token/rate limit.

### Checklist

- [x] endpoints/DTO reales implementados y documentados;
- [x] RBAC y tenant isolation por endpoint probados;
- [x] CSRF/state/public unsubscribe probados;
- [x] eventos auditados sanitizados;
- [x] Problem Details y códigos negativos probados;
- [x] `/api/v1/messages/send` continúa ausente.

## Parte 8 — Interfaz de usuario

### Cuentas remitentes

Configuración mostrará estado OAuth ausente/conectado/reauth/revocado/error,
email, nombre, scopes, conexión, verificación, default y error sanitizado, con
acciones conectar, verificar, default, reconectar y revocar según permisos.

### Campañas y mensajería

Campañas permitirá elegir modo, sender, Reply-To, audiencia, límites, fecha,
ventana e intervalo; aprobar, confirmar, programar/iniciar, pausar, reanudar,
cancelar y ver progreso/resultados/retries/permanentes/ambiguos. Badges,
advertencias y diálogo reforzado separarán real de simulación. Mensajes e
integraciones conservará borrador/simulación/manual y mostrará provider, red,
kill switch, límites, uso y reauth; no incorporará envío directo.

### Accesibilidad

Se reutilizan labels, botones semánticos, alerts `role=status|alert`, diálogo con
focus trap/retorno de foco, controles nativos fecha/hora y disabled states por
permiso/configuración.

### Checklist

- [x] UI de sender accounts completa;
- [x] callback vuelve sin tokens y muestra resultado;
- [x] UI de campaña real/simulación y configuración completa;
- [x] confirmación reforzada y disabled states probados;
- [x] progreso, pausas, resultados y errores visibles;
- [x] Mensajes e integraciones refleja estado real sin send directo;
- [x] accesibilidad básica ejecutada.

## Parte 9 — Configuración y operación

### Variables semánticas

OAuth client ID/secret, redirect URI, retorno frontend, URL pública de baja,
clave AES y versión, authorize/token/userinfo/revoke/Gmail base URLs, scopes,
timeouts, hard cap, intervalos y modo test falso serán externos. Ningún secreto
se versiona. Las URLs de red serán HTTPS salvo opt-in de test aislado; no serán
controlables por request.

NOOP no exige secretos. Gmail configurado exige todos los campos y clave de 32
bytes. `GMAIL_ACCESS_TOKEN` queda solo como compatibilidad del adapter
draft-only sintético y se documenta como no productivo.

### Operación y rollback

Activación futura: provisionar secretos, verificar redirect/TLS/DNS, ejecutar
smoke manual con cuenta y destinatario no operativos autorizados, mantener hard
cap 10 y observar métricas. Desactivación: kill switch, pausa worker, modo NOOP,
revocación local/remota. Rotación: agregar versión de clave, re-cifrar y retirar
la anterior después de verificar. Rollback sin pérdida: flags + imagen anterior.

### Checklist

- [x] configuración validada y documentada;
- [x] startup NOOP sin secretos probado;
- [x] startup Gmail incompleto falla cerrado;
- [x] URLs test sobrescribibles y SSRF/open redirect probados;
- [x] Docker/local/runbook/rotación/revocación/rollback actualizados;
- [x] defaults versionados permanecen deshabilitados.

## Parte 10 — Pruebas y seguridad

### Matriz ejecutada antes del cierre Git

| Gate | Estado | Evidencia |
|---|---|---|
| backend clean verify | EXECUTED_PASS | Java 21, 112 tests, Spotless, ArchUnit, V1→V14 |
| cifrado/tamper/key version/redacción | EXECUTED_PASS | `GmailTokenCipherTest`, 3/3 |
| OAuth/state/replay/reconnect/revoke | EXECUTED_PASS | unitarios, PostgreSQL y E2E fake |
| MIME/Gmail/error mapping | EXECUTED_PASS | `GmailMimeBuilderTest` y `GmailApiClientTest`, 7/7 |
| PostgreSQL/tenant/constraints/outbox/limits | EXECUTED_PASS | Testcontainers + E2E fake |
| unsubscribe y concurrencia | EXECUTED_PASS | hash/idempotencia/cancelación/seguimiento y E2E |
| RBAC/CSRF/SSRF/open redirect/CRLF/XSS | EXECUTED_PASS | backend, fake y Playwright |
| frontend install/typecheck/unit/build | EXECUTED_PASS | `npm ci`, 13/13, Vite build |
| E2E Gmail/OAuth falso | EXECUTED_PASS | `validate-gmail-live-fake.ps1`, 29.2 s |
| compatibilidad NOOP/simulación | EXECUTED_PASS | NOOP sin secretos + suite previa |
| migración empty y upgrade | EXECUTED_PASS | V1→V14 y V11→V14 |
| manual/screenshot/PDF visual | EXECUTED_PASS | 32 PNG, PDF 34 páginas revisado |
| backup/restore/profile smoke | EXECUTED_PASS | V14/probe; perfil non-root/read-only/zero SENT |
| npm audit/Grype/secret scan | EXECUTED_PASS | 0 vulnerabilidades; sin secretos ni artefactos prohibidos |
| validador integral corrida 1 | EXECUTED_PASS | `complete-crm-20260730-090155.json`, `d6bfe642`, 620,955 s, SHA-256 `5a8c1e6e28bcfdc2c5721fab9a87b47b001e44ec2d78435a323cb91d646e52c7` |
| validador integral corrida 2 | EXECUTED_PASS | `complete-crm-20260730-091224.json`, `d6bfe642`, 640,545 s, SHA-256 `0f789a354f64d95dfc75c85b79d1b1184d8bd2528f563effb3cf7ac092cbba16` |

La primera CI remota posterior, run `30528538137` sobre `61885e2`, terminó
21/22: `frontend-e2e` descubría el spec Gmail LIVE dentro del stack general y
abortaba antes de tocar datos porque faltaba `CRM_E2E_COMPOSE_PROJECT`. Se
alineó el workflow con el validador canónico: 3 escenarios general/NOOP y el
escenario Gmail LIVE en su stack `crm-gmail-fake-*`, sin omitir pruebas.
El run definitivo `30530515993` sobre `d6bfe642` terminó 22/22 verde, incluido
el paso Gmail LIVE contra el proveedor falso aislado.

### Referencias oficiales verificadas

Consulta: 2026-07-28.

- [Gmail server-side OAuth](https://developers.google.com/workspace/gmail/api/auth/web-server)
- [OAuth 2.0 web server](https://developers.google.com/identity/protocols/oauth2/web-server)
- [OAuth security best practices](https://developers.google.com/identity/protocols/oauth2/resources/best-practices)
- [Gmail users.messages.send](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/send)
- [Gmail sender guidelines](https://support.google.com/mail/answer/81126)
- [Gmail sender guidelines FAQ](https://support.google.com/mail/answer/14229414)
- [Gmail sending limits](https://support.google.com/mail/answer/22839)

La documentación confirma redirect exacta, offline access, state, cifrado y
revocación; `gmail.send` como scope mínimo; Message.raw RFC/base64url; y one-click
además de baja visible. El límite interno de 10/día es deliberadamente más
estricto que los límites Gmail publicados.

### Checklist

- [x] referencias oficiales consultadas y fechadas;
- [x] matriz Gmail automatizada implementada sin Internet;
- [x] tests negativos de seguridad ejecutados;
- [x] scans precommit ejecutados;
- [x] dos validadores integrales limpios ejecutados;
- [x] comandos, tiempos y resultados Gmail factuales incorporados.

## Parte 11 — Manual de usuario y screenshots

### Artefactos generados y revisados

Se versionará el generador y se producirán artefactos ignorados bajo:

```text
validation-output/gmail-campaign-manual/SEG-001-gmail-campaign-user-manual.html
validation-output/gmail-campaign-manual/SEG-001-gmail-campaign-user-manual.pdf
validation-output/gmail-campaign-manual/*.png
validation-output/gmail-campaign-manual/index.json
validation-output/gmail-campaign-manual/SEG-001-gmail-campaign-user-manual.zip
```

Las capturas usarán tenant, usuarios, cuentas y destinatarios exclusivamente
sintéticos `.test`, Gmail/OAuth falso y valores no sensibles. El índice incluirá
SHA-256. No se versionarán PNG/PDF/ZIP ni `validation-output/`.

El manual cubre configuración ausente, conexión/callback/cuenta/default/
verificación/reconexión/revocación, mensajería, plantilla/preview, campaña y
sender, modo real/simulación, filtros/audiencia/excluidos, aprobación/
confirmación/programación/ejecución/pausa/reanudación/completado, resultados,
retry/permanente/ambiguo, baja, auditoría, kill switch y falta de permisos.

Evidencia definitiva del 2026-07-30:

| Artefacto | Bytes | SHA-256 |
|---|---:|---|
| HTML | 21025 | `ad78cc4f6964db629b4038fa527e842b6ecb168a13b75fab06714bea801ca472` |
| PDF | 3865910 | `646c346b939afae0c6a4a0361ce0a38d2073b60a2eabbda7b8e7b01b4b3cf1dc` |
| index.json | 11647 | `bac71398ecd40598fa8eaafe7f537b47b2e59b40ea0757bf482c30920c7a483e` |
| ZIP | 9038006 | `c50e78481edfa46c9f7860a8fc62eec1d4bcca822f55807a2b4553e1df313289` |

El PDF tiene 34 páginas (portada, guía y 32 pantallas). Se renderizaron las 34
páginas y se inspeccionaron el contacto completo y páginas representativas,
sin overflow ni recorte. Los artefactos son ignorados; solo se versiona el
generador.

### Checklist

- [x] generador versionado;
- [x] E2E sintético captura pantallas reales;
- [x] HTML generado;
- [x] PDF generado y revisado;
- [x] 32 PNG e índice JSON generados;
- [x] ZIP generado;
- [x] hashes y ubicaciones finales registrados;
- [x] artefactos revisados sin secretos/PII/state/code.

## Parte 12 — Criterios de aceptación y cierre

### Checklist funcional

- [x] cuenta remitente OAuth completa, cifrada, tenant-safe y revocable;
- [x] Gmail send individual con MIME/baja/error mapping y aceptación persistida;
- [x] campaña real aprobada/confirmada/programada por `MESSAGE_SEND`;
- [x] audiencia congelada y reevaluación por destinatario;
- [x] outbox/lease/retry/dead/ambiguous/idempotencia sin transacción HTTP larga;
- [x] límites, ventanas, pausas, cancelación y kill switch efectivos;
- [x] baja inmediata gana sobre futuros mensajes y seguimientos;
- [x] UI completa y manual/capturas sintéticos;
- [x] NOOP/simulación/funciones previas continúan pasando;
- [x] ninguna prueba llama a Google ni envía correo real;
- [x] defaults y perfil productivo siguen fail-closed;
- [x] documentación canónica reconciliada;
- [x] dos validaciones integrales limpias sobre `d6bfe642`;
- [x] diff precommit relacionado, sin secretos ni artefactos prohibidos;
- [x] commits funcional y de integración del validador creados;
- [x] push fast-forward a `origin/main` confirmado.

### Resultados finales

```text
estado: FUNCTIONAL_PASS_PUBLISHED
base sincronizada: 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
commit implementación: df2c79a77e27203f7eb63325f3e526cf38d1820a
commit integración del validador: d724b80a2d1eecbe2f4994366571ecd009342343
commit validado/publicado: d6bfe64283cd3bab8dce441e4b01e348f9a55d5e
push: EXECUTED_PASS 9995b3e..d6bfe64
verificación remota: HEAD=origin/main=ls-remote, conteo 0 0
CI del SHA publicado: EXECUTED_PASS, run 30530515993, 22/22
producción: NOT_DEPLOYED
Google real smoke: OUT_OF_SCOPE_MANUAL_POSTERIOR
```

### Registro final de archivos

El registro final se obtiene con `git diff --name-status`, `git diff --stat`,
revisión del diff completo y stage explícito. Los grupos funcionales son V14;
OAuth/cifrado/Gmail/unsubscribe; campaña/outbox; seguridad/configuración;
frontend; fake Google/E2E/manual; validadores y documentación.

### Riesgos y pendientes fuera de alcance

- aceptación por Gmail no prueba entrega, apertura, lectura ni respuesta;
- SPF/DKIM/DMARC, reputación, secretos y verificación OAuth dependen del futuro
  entorno autorizado;
- no hay lectura de buzón, sincronización de rebotes ni Pub/Sub;
- producción y cuenta Gmail real permanecen fuera de esta validación.

### Checklist

- [x] checklist de aceptación reconciliada con evidencia real disponible;
- [x] comandos integrales y resultados finales registrados;
- [x] commit y hashes locales registrados;
- [x] push registrado tras verificar `origin/main`;
- [x] riesgos residuales y desviaciones declarados.
