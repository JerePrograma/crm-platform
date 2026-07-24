# Configuración, entornos y clasificación de datos

Actualizado: 2026-07-24

## Principio general

La configuración versionada define contratos y valores seguros. Las credenciales, secretos y datos reales se inyectan fuera de Git. Todo ambiente no autorizado debe permanecer fail-closed.

## Matriz de entornos

| Entorno | Estado | Datos | Comunicaciones | Persistencia |
|---|---|---|---|---|
| Tests unitarios/integración | disponible | sintéticos | bloqueadas | efímera/Testcontainers |
| Desarrollo local | disponible | sintéticos o autorizados fuera de Git | bloqueadas | PostgreSQL local |
| Demo/evaluación | disponible con datos ficticios | ficticios | bloqueadas | entorno controlado |
| Perfil productivo local | validación técnica, no despliegue | sintéticos | bloqueadas | volumen efímero de smoke |
| Staging real | no provisionado | pendiente de política | no autorizado | pendiente |
| Producción real | no desplegada | no autorizados | no autorizadas | pendiente |

## Guardas obligatorias

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

Estas variables deben dominar cualquier preferencia almacenada en base de datos.

## Configuración real

Se considera real y canónica:

- repositorio `JerePrograma/crm-platform`;
- rama `main`;
- Java, Spring Boot, React, PostgreSQL y versiones declaradas en el repositorio;
- migraciones Flyway versionadas;
- contratos de Compose, Dockerfiles y scripts;
- tablas, endpoints, DTOs, permisos y estados implementados;
- documentos y ADR versionados.

## Configuración sintética o temporal

Se considera sintética o efímera:

- usuarios bootstrap de validación;
- contraseñas generadas para smoke;
- nombres de proyectos Compose;
- puertos alternativos seleccionados para evitar colisiones;
- IDs y tags de imágenes generadas en una corrida;
- bases y volúmenes creados para pruebas;
- fake inbound;
- lotes de prospectos sintéticos;
- archivos `.env` temporales creados por validadores;
- commits temporales de reconstrucción usados solo para comprobar trees.

Los valores concretos de una evidencia no deben convertirse en configuración permanente.

## Configuración implementada pero no conectada

- Gmail;
- SMTP;
- WhatsApp Cloud;
- proveedores de red real;
- secretos de proveedores;
- infraestructura cloud;
- dominio, TLS y reverse proxy productivos;
- observabilidad y alertas externas.

Estado correcto: `IMPLEMENTED_NOT_CONNECTED` o `NOT_PROVISIONED`, nunca `PASS`.

## Datos prohibidos en Git

- `.env`;
- tokens, cookies, claves privadas o secretos;
- contraseñas reales;
- direcciones de correo, teléfonos o datos operativos de clientes;
- dumps de base;
- archivos de evidencia sin sanitizar;
- XLSX real de prospectos;
- exports o capturas con datos sensibles.

## Archivos locales

`.env` se crea desde `.env.example` y debe permanecer ignorado. Los validadores pueden crear uno temporal, usar valores sintéticos y eliminarlo al terminar.

El XLSX real puede evaluarse únicamente fuera de Git y con autorización. No debe copiarse a fixtures, imágenes, CI ni documentación.

## Puertos

Los puertos predeterminados se documentan en `docs/local-development-and-usage.md` y `docs/containerized-quickstart.md`. Si están ocupados:

- seleccionar otros puertos libres;
- no detener servicios ajenos;
- mantener `DATABASE_URL` y los mapeos consistentes;
- registrar el puerto usado en la evidencia;
- no convertir el puerto elegido en un contrato global.

## Secretos productivos

Cuando exista un entorno autorizado, como mínimo serán secretos:

- `DATABASE_PASSWORD`;
- `CRM_BOOTSTRAP_USERNAME`;
- `CRM_BOOTSTRAP_PASSWORD`;
- credenciales de proveedores aprobados.

Deben residir en el gestor de secretos del proveedor, nunca en Dockerfile, Compose versionado, logs, tickets o documentación.
