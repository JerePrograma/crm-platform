# Validación de contactabilidad y paginación — 2026-07-23

## Alcance validado

- un prospecto sin correo, teléfono ni WhatsApp utilizable queda pendiente de completar datos;
- una web puede aportar evidencia y exclusiones, pero no convierte por sí sola al prospecto en contactable;
- la contactabilidad se recalcula al crear o eliminar contactos y al agregar, modificar o eliminar canales;
- las exclusiones continúan dominando el estado comercial;
- la vista previa de importación aplica el mismo criterio que la ejecución;
- la lista de prospectos utiliza la paginación real de la API y conserva búsqueda, estado y página en la URL.

## Validaciones ejecutadas

- Spotless aplicado y verificado;
- pruebas específicas de prospectos operativos, importaciones, reportes, campañas y oportunidades;
- `./mvnw -B -f backend/pom.xml verify`;
- `npm ci --no-audit --no-fund`;
- `npm run typecheck`;
- `npm run test:unit`;
- `npm run build`;
- Docker Compose con configuración fail-closed;
- `npm run test:e2e` con Playwright Chromium;
- `bash scripts/check-repository-safety.sh`;
- `git diff --check`.

## Seguridad

Se mantuvieron los envíos deshabilitados, el modo de simulación activo, el límite diario en cero, la protección de emergencia activa y la red real deshabilitada.

Ejecución: `30036648327` sobre `42d1574e7558f544e5894aef1a69a72a7e08da6f`.
