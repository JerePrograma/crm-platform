# Estado actual

## Repositorio

- Rama real de trabajo: `feat/seg-001-prospect-vertical-slice`.
- `main` contiene únicamente la inicialización mínima.
- Primer lote de `SEG-001` publicado: backend base, Maven, configuración, Flyway, PostgreSQL y Docker.

## Seguridad

- Envío real: inexistente y bloqueado.
- Valores activos por defecto: `sending.enabled=false`, `sending.dry-run=true`, `sending.daily-limit=0` y kill switch ambiental activo.
- Flyway inicializa además un kill switch persistente y los valores de envío cerrados.
- Los lotes XLSX reales y los datos de contacto no se publican porque el repositorio es público.

## Segmentos

- `SEG-000`: continuidad documental y rama real — completado.
- `SEG-001`: publicación y cierre del vertical slice de prospectos — activo.

## Lotes de SEG-001

1. Backend base, configuración fail-closed, esquema y contenedores — publicado; validación CI pendiente.
2. Dominio de instituciones, contactos, prospectos y exclusiones — pendiente.
3. Importaciones persistentes, deduplicación y revisión — pendiente.
4. API, frontend, pruebas de integración y cierre del segmento — pendiente.

## Riesgos activos

1. CI debe confirmar compilación, formato, prueba de configuración, Compose e imagen Docker.
2. Maven Wrapper todavía no está versionado; se incorporará antes del cierre de `SEG-001`.
3. La persistencia de `ImportJob`, `ImportRow` y `DuplicateReview` sigue pendiente.
4. Los cuatro contactos previos deben registrarse como exclusiones cuando existan canales verificados.
5. No existe todavía una prueba Testcontainers que valide Flyway y JPA contra PostgreSQL real.
