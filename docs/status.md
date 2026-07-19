# Estado actual

## Repositorio

- Rama real de trabajo: `feat/seg-001-prospect-vertical-slice`.
- `main` contiene únicamente la inicialización mínima.
- El árbol de código recuperado se encuentra preparado fuera de Git y debe publicarse por lotes verificables.

## Seguridad

- Envío real: inexistente y bloqueado.
- Valores obligatorios: `sending.enabled=false`, `dry-run=true`, `daily-limit=0`.
- Los lotes XLSX reales y los datos de contacto no se publican porque el repositorio es público.

## Segmentos

- `SEG-000`: continuidad documental y rama real — completado.
- `SEG-001`: publicación y cierre del vertical slice de prospectos — activo.

## Artefacto recuperado

El artefacto local contiene backend Java 21/Spring Boot, frontend React/Vite, Flyway, PostgreSQL, Docker, CI y la base inicial de prospectos, exclusiones, campañas y plantillas. Antes de marcarlo como funcional debe publicarse y ejecutarse CI.

## Riesgos activos

1. El código completo todavía no está en la rama remota.
2. No existe validación remota de Maven, frontend, Compose ni Docker.
3. La persistencia de `ImportJob`, `ImportRow` y `DuplicateReview` sigue pendiente.
4. Los cuatro contactos previos deben registrarse como exclusiones cuando existan canales verificados.
