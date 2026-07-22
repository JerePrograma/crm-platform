# Seguridad

Actualizado: 2026-07-22

## Modelo de amenazas

Activos: credenciales, sesiones, PII de contactos, exclusiones/no-contacto,
historial comercial, auditoría, mensajes, backups y configuración de envío.
Actores: usuario anónimo, usuario autenticado, usuario de otro tenant, operador,
administrador, proveedor externo y atacante que controla una carga o webhook.
Fronteras: navegador/frontend, HTTP backend, sesión/CSRF, PostgreSQL, importación
CSV/XLSX, webhook fake, runtime Docker y futuros providers.

| Amenaza | Mitigación ejecutable |
|---|---|
| acceso anónimo o escalada | deny-by-default, sesión rotada, RBAC backend, 401/403 probados |
| fuga cross-tenant | `organization_id` derivado del principal en cada consulta/mutación y pruebas PostgreSQL |
| CSRF/session fixation | token CSRF, cookie HttpOnly/SameSite, rotación al login, invalidación al logout/cambio |
| XSS/inyección | React escaping, sin HTML arbitrario, SQL parametrizado, JSON/DTO estricto |
| CSV formula injection | neutralización de `= + - @ tab CR` y tests |
| archivos hostiles | 10 MiB, 10.000 filas, 100 columnas, 10.000 chars/celda, magic XLSX, NUL/JSON/MIME estricto |
| webhook forjado/replay | HMAC constant-time, timestamp, ventana, event ID, tamaño/content-type y receipt único |
| envío real accidental | cuatro guardas de entorno, kill switch DB, provider/red desconectados y endpoint send ausente |
| payload/secret leakage | payloads minimizados, hashes, MDC sanitizado, sin bodies/tokens/cookies en INFO |
| supply chain/runtime | lockfiles, npm audit, imagen runtime mínima fijada y Grype High/Critical |
| pérdida de datos | backups externos con checksum y restore drill aislado |

Riesgos residuales: el socket Docker usado por Testcontainers/Grype tiene control
elevado sobre el daemon y solo se usa con código propio revisado; TLS, WAF,
secret manager, rotación externa y controles de infraestructura dependen del
entorno de despliegue todavía no autorizado.

## Autenticación y autorización

El bootstrap crea el primer administrador solo si no existe otro y se proveen
`CRM_BOOTSTRAP_USERNAME` y `CRM_BOOTSTRAP_PASSWORD`. Las contraseñas se hashean
con el encoder delegado. La sesión es server-side, expira, rota y se invalida al
logout, cambio de contraseña o desactivación. En producción TLS se exige
`SESSION_COOKIE_SECURE=true`.

Roles base: `ADMIN`, `MANAGER`, `SALES`, `VIEWER`; la autoridad final siempre es
el backend. `REPORT_READ`, `SETTINGS_MANAGE` y permisos operativos protegen
reportes, configuración, outbox y quarantine. Ocultar un control en React no
otorga autorización.

Rutas:

- `/actuator/health/**`: pública para probes mínimos;
- `/actuator/metrics` y otros endpoints operativos: autenticados;
- `/api/**`, OpenAPI y Swagger: autenticados, además de permisos de método;
- resto: denegado.

No existe CORS global permisivo; el frontend usa reverse proxy same-origin. Las
mutaciones exigen CSRF. Nginx agrega CSP, HSTS bajo TLS, frame denial,
`nosniff`, referrer policy y permissions policy.

## Bloqueo de comunicaciones

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
system_setting sending.kill-switch=true
```

El entorno domina cualquier preferencia de organización. La API de settings
rechaza y audita intentos permisivos. Cada worker reevalúa configuración,
campaña, exclusiones, consentimiento, elegibilidad, cliente/no-contacto,
aprobación, provider e idempotencia. Los estados producidos son draft,
simulated, blocked, cancelled o noop; una regresión SQL exige cero
`SENT|DELIVERED|READ`. `/api/v1/messages/send` no existe.

Gmail y WhatsApp tienen adaptadores y contracts, pero están
`IMPLEMENTED_NOT_CONNECTED`; no se cargaron credenciales ni se probó red real.

## Webhook e inbound

`FAKE_INBOUND` es el único provider inbound ejecutable. El secreto viene del
entorno de prueba y no se versiona. El endpoint exige POST, JSON, límite de
tamaño, HMAC, timestamp y external event ID. La comparación es constant-time,
el replay es idempotente y una asociación ambigua queda en quarantine. No se
confirma PII en la respuesta y no se envía contestación automática.

## Datos, PII y retención

La descripción técnica está en `docs/privacy-and-retention.md`. Los datos reales
no ingresan en Git, CI, imágenes, fixtures ni evidencia. El XLSX real solo puede
usarse para preview agregado fuera del repositorio. Auditoría, exclusiones y
evidencia de no-contacto no se borran silenciosamente. Las retenciones y
solicitudes de supresión requieren revisión legal local; esta documentación no
es asesoramiento legal.

## Dependencias, imágenes y evidencia

- Maven/Java y npm usan versiones/lockfiles controlados.
- PostgreSQL JDBC está en `42.7.12`; Jackson 3/2 en `3.1.5`/`2.21.5`.
- Backend runtime: JRE mínima no-root fijada por digest; healthcheck Java sin
  shell ni `curl`.
- Grype está fijado por digest y falla en High/Critical; npm audit falla en High.
- OWASP Dependency-Check no se usa como evidencia local porque la descarga NVD
  sin API key quedó `BLOCKED_EXTERNAL_NVD_RATE_LIMIT`, no PASS.
- `validation-output/`, `.env`, browser artifacts, logs y XLSX están ignorados y
  prohibidos como archivos versionados por repository safety.

Los transcripts deben revisarse antes de compartir. Nunca deben contener
contraseñas, tokens, cookies, filas reales, cuerpos completos o secretos webhook.

## Validación canónica

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-complete-crm.ps1 `
  -PostgresPort 25432 -BackendPort 8080 -FrontendPort 5173
```

Unix:

```bash
make validate-complete-crm
```

El validador exige rama/árbol limpio, pruebas backend/frontend, migraciones,
smoke/E2E, bloqueos, cero SENT, Grype/npm audit, backup/restore, perfil
productivo local y repository safety final. El socket Docker solo debe montarse
sobre código confiable.
