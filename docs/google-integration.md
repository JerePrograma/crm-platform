# Integración Google

## Estado

No implementada en SEG-001. No existen tokens, clientes ni llamadas Google en el código actual.

## Desarrollo

Usar OAuth 2.0 con cuenta personal:

- consentimiento explícito;
- scopes mínimos;
- credenciales fuera de Git;
- refresh token cifrado o en Secret Manager;
- callback restringido;
- revocación documentada;
- ambiente Google Cloud separado de producción.

Scopes se añadirán de forma incremental. No solicitar Gmail, Sheets y Drive completos si el caso de uso requiere permisos más estrechos.

## Producción

Usar Google Workspace con cuenta de servicio y delegación en todo el dominio únicamente después de:

- aprobación administrativa;
- allowlist de usuario delegado;
- scopes revisados;
- IAM mínimo;
- auditoría de impersonación;
- rotación de credenciales;
- staging E2E.

La clave JSON de cuenta de servicio no debe descargarse si Workload Identity puede reemplazarla.

## Puertos previstos

```text
GoogleAuthorizationPort
GmailSendPort
GmailReadPort
GmailWatchPort
GoogleSheetsPort
GoogleDrivePort
```

Los casos de uso dependen de puertos; adaptadores Google viven en infraestructura.

## Gmail

Futuro:

- enviar mensajes individuales;
- MIME multipart/alternative y mixed;
- etiquetas;
- message/thread/RFC IDs;
- lectura y watch;
- Pub/Sub;
- reconciliación;
- rebotes, respuestas y bajas.

## Sheets

PostgreSQL domina. Sheets puede editar únicamente campos autorizados:

- aprobación;
- prioridad;
- segmento;
- notas;
- exclusión manual;
- propietario;
- próxima acción.

Sincronización requiere:

- versión/timestamp;
- preview;
- conflictos;
- filas rechazadas;
- auditoría;
- idempotencia.

## Drive

Usos previstos:

- adjuntos comerciales aprobados;
- cotizaciones/exportaciones;
- evidencia de archivos utilizados.

Cada archivo debe conservar ID, nombre, MIME, tamaño, SHA-256 y versión/revisión relevante.

## Seguridad

- Secret Manager;
- scopes mínimos;
- tokens nunca en logs;
- refresh tokens cifrados;
- revocación;
- protección SSRF;
- límites y timeouts;
- circuit breaker;
- health separado por integración;
- no degradar fail-open.

## Apps Script

No es motor principal. Puede documentarse como contingencia manual limitada, sin reemplazar idempotencia, auditoría ni PostgreSQL.
