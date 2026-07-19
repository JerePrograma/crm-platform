# SEG-001 — Lote 2: dominio de prospectos y exclusiones

## Estado del lote anterior

El backend base, la configuración fail-closed, Flyway, PostgreSQL, Docker y la prueba de propiedades de envío ya están publicados. Antes de desarrollar este lote se debe revisar el resultado de CI y corregir cualquier fallo real.

## Objetivo

Implementar el dominio persistente de instituciones, contactos, canales, prospectos y exclusiones sobre el esquema ya versionado.

## Alcance

- Entidades JPA separadas para institución, contacto, canal y prospecto.
- Enumeraciones explícitas para estados y tipos de canal.
- Repositorios con consultas normalizadas.
- Servicio de normalización de correo, dominio, teléfono y nombre institucional.
- Exclusiones con precedencia sobre elegibilidad.
- Servicio de elegibilidad sin ningún camino de envío.
- API REST inicial de listado y ficha de prospectos.
- RFC 7807 para errores de validación y recursos inexistentes.
- Testcontainers para migración Flyway y validación JPA.
- Pruebas unitarias de normalización y exclusión.

## Criterios de aceptación

- CI del lote anterior está verde o sus fallos quedan corregidos y documentados.
- `mvn -B -f backend/pom.xml verify` finaliza correctamente.
- Flyway crea el esquema en PostgreSQL limpio.
- Hibernate valida que las entidades coinciden con Flyway.
- Una exclusión por correo o teléfono fuerza `contactEligible=false`.
- No se fusionan instituciones ambiguas.
- No existe ningún endpoint de envío.

## Fuera de alcance

- Gmail, Google Sheets, Cloud Tasks y automatizaciones.
- Envíos reales o simulados.
- Importación persistente; corresponde al lote 3.
- Datos comerciales reales.
