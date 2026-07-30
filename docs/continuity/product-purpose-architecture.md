# Propósito, producto y arquitectura

Actualizado: 2026-07-30

## Distinción esencial

### Producto comercializado

Gestudio es una plataforma web para academias, estudios, escuelas e instituciones con alumnos y clases recurrentes. Su propuesta comercial incluye administración de alumnos, inscripciones, profesores, disciplinas, horarios, asistencias, mensualidades, pagos, deuda, caja, recibos y reportes desde PC o celular.

### Aplicación de este repositorio

Este repositorio implementa el CRM de ventas y operaciones comerciales para vender Gestudio. Administra:

- instituciones y prospectos;
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

- propietario o responsable comercial;
- administración;
- operador de prospección;
- supervisor;
- auditor interno;
- futuro equipo de soporte u operaciones.

## Resultado esperado

Centralizar el ciclo comercial de Gestudio con trazabilidad, aislamiento por organización, deduplicación, seguridad y guardas que impidan contactos indebidos o envíos accidentales.

## Arquitectura

### Backend

- Java 21;
- Spring Boot 4.1;
- PostgreSQL 17;
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
- Docker Compose para desarrollo, validación y perfil productivo local;
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
