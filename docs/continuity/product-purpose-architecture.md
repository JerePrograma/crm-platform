# Propósito, producto y arquitectura

Actualizado: 2026-10-14

## Distinción esencial

### Negocio actual: Scalaris

Scalaris ofrece servicio técnico, desarrollo y venta a pedido. Marketing trabaja con particulares, comercios, empresas y profesionales, registra consultas, intereses, contactos y próximas acciones, y administra oportunidades y campañas preparadas o simuladas.

Referencias vigentes: WhatsApp **11 7278-9289**, **Zona Oeste, CABA y Mar del Plata**. Retiro y entrega a coordinar según servicio, localidad y caso; diagnóstico, alcance, presupuesto, disponibilidad y plazos se confirman en cada consulta. El CRM no debe inventar precios, garantías ni resultados comerciales.

El perfil `CRM_BUSINESS_PROFILE=SCALARIS` usa organización propia, referencias, etiquetas y plantillas iniciales. La cuenta `MARKETING` tiene permisos comerciales, sin administración de usuarios/configuración ni `MESSAGE_SEND`. El bootstrap conserva las cuentas existentes y no añade contactos reales.

### Perfil histórico de compatibilidad: Gestudio

Gestudio es una plataforma web para academias, estudios, escuelas e instituciones con alumnos y clases recurrentes. Su propuesta comercial incluye administración de alumnos, inscripciones, profesores, disciplinas, horarios, asistencias, mensualidades, pagos, deuda, caja, recibos y reportes desde PC o celular.

### Aplicación de este repositorio

Este repositorio implementa el CRM local para el trabajo comercial de Scalaris y conserva el perfil `LEGACY` para compatibilidad con el CRM de Gestudio. Administra:

- personas, negocios, empresas y prospectos;
- contactos, canales y contactabilidad;
- actividades, tareas y timeline;
- importaciones CSV/XLSX;
- deduplicación y exclusiones;
- oportunidades, pipeline y forecast;
- campañas, audiencias y simulación;
- cuentas Gmail OAuth y campañas LIVE controladas exclusivamente por outbox;
- borradores y enlaces manuales;
- outbox e inbound de prueba;
- reportes, auditoría, usuarios, roles, settings y etiquetas.

La capacidad LIVE existe pero permanece fail-closed y sin Google real. No debe
confundirse una ejecución contra el proveedor falso con correo real.

## Usuarios previstos

- persona responsable de Marketing;
- propietario o responsable comercial;
- administración;
- operador de prospección;
- supervisor;
- auditor interno;
- futuro equipo de soporte u operaciones.

## Resultado esperado

Centralizar el ciclo comercial de Scalaris, facilitar la instalación local por doble clic y conservar trazabilidad, aislamiento por organización, deduplicación y guardas que impidan contactos indebidos o envíos accidentales.

## Arquitectura

### Backend

- Java 21;
- Spring Boot 4.1;
- PostgreSQL 17 en Docker; PostgreSQL 18 en el paquete Windows portable validado localmente;
- Flyway V1–V14;
- Spring Security, sesión HttpOnly same-origin y CSRF;
- RBAC y tenant isolation;
- REST, OpenAPI y Problem Details;
- auditoría JSONB;
- Micrometer, health, readiness y métricas;
- Maven Verify, Testcontainers y ArchUnit.

### Frontend

- React;
- TypeScript strict;
- Vite;
- Vitest;
- Playwright con Chromium y smoke crítico en Firefox/WebKit;
- CSS y componentes propios;
- navegación desktop y drawer móvil;
- diálogos accesibles, focus trap y retorno de foco;
- filtros, paginación, estados vacíos y feedback de operaciones lentas.

### Infraestructura

- Dockerfiles multi-stage;
- paquete Windows portable con Java 21, PostgreSQL 18 y Node 24, servidor web loopback e iniciadores de apertura/parada/respaldo;
- datos y configuración privados fuera del paquete y del repositorio;
- Docker Compose para desarrollo, validación y perfil productivo local;
- Compose Scalaris independiente, con base privada y único puerto de pantalla en `127.0.0.1`;
- imágenes runtime no-root;
- PostgreSQL privado en el perfil productivo;
- frontend nginx no-root;
- backend JRE Chainguard fijada por digest;
- GitHub Actions para gates de backend, frontend, migraciones, seguridad y E2E.

## Límites de dominio

- PostgreSQL es la fuente de verdad.
- Una exclusión domina cualquier intención de contacto.
- La audiencia de campaña se congela antes de simular.
- Outbox ofrece entrega at-least-once; la idempotencia evita efectos duplicados.
- El inbound real no está conectado.
- La búsqueda, reporting y paginación deben ser tenant-scoped.
- No se cargan todas las páginas para calcular métricas agregadas.
- Los contratos públicos deben conservar compatibilidad hacia atrás salvo autorización explícita.

## Estado de la edición Scalaris

SCALARIS-001 tiene operación portable local aprobada y publicación GitHub `AUTHORIZED_IN_PROGRESS`, con CI `PENDING_POST_PUSH`: 18 comprobaciones PowerShell 5.1 de apertura, cuentas/permisos, persistencia, respaldo y recuperación en base nueva. Backend aprobó 62 unitarias y 6 de integración focalizada; frontend, 16 unitarias, typecheck/build/audit sin vulnerabilidades y E2E conjunto 4/4 sin omitidos, inesperados ni flaky. Manual desktop/móvil revisado a 1440/390/320 px. La fase Git autorizada se registra en [publicación GitHub Scalaris](../validation/scalaris-github-publication-2026-10-14.md); no se afirma push exitoso antes de verificarlo.

Docker real y el validador integral histórico siguen bloqueados por ausencia de Docker. La fuente local está congelada y el ZIP definitivo quedó revisado con manifiesto/checksum coincidentes, 2.243 entradas y cero archivos privados. La instalación de uso está activa en loopback 8787, sin prospectos de prueba, con 18 etiquetas y 7 plantillas; QA detenido. El siguiente paso es uso/piloto local. Consultar `docs/status.md` y la matriz para la evidencia de octubre; los PASS históricos de julio no cubren estos cambios.

## Fuera de alcance actual

- despliegue productivo autorizado;
- conexiones reales de Gmail, SMTP o WhatsApp Cloud;
- secretos productivos;
- infraestructura cloud elegida;
- dominio y TLS productivos;
- uso del XLSX real dentro de Git, CI, imágenes o fixtures;
- habilitación automática de campañas LIVE o envío fuera del outbox;
- migraciones destructivas;
- refactorización amplia de arquitectura.
