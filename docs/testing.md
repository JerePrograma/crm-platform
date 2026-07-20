# Estrategia de pruebas

## Regla de evidencia

Un test versionado no equivale a un test ejecutado.

Estados:

- `IMPLEMENTED`: existe código o automatización;
- `EXECUTED_PASS`: ejecución real con código de salida cero;
- `EXECUTED_FAIL`: ejecución real fallida;
- `PASS_FROM_CACHE`: imagen exportada sin recompilación limpia;
- `PASS_SYNTAX`: parser o sintaxis;
- `PASS_STRUCTURE`: estructura YAML/configuración;
- `BLOCKED`: entorno insuficiente;
- `NOT_RUN`: no ejecutado;
- `NOT_IMPLEMENTED`.

## Comando integral Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Este recorrido ejecuta:

- builds limpios frontend/backend;
- Compose config;
- health PostgreSQL/backend/frontend;
- Flyway/Hibernate durante arranque;
- smoke host/contenedor;
- Maven verify dentro de Docker;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers;
- package-lock-only;
- build frontend con npm ci;
- smoke final;
- seguridad del repositorio;
- JSON y transcript.

No requiere Java, Maven, Node o npm instalados en el host.

## Backend con herramientas locales

Unix:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

## Backend contenedorizado

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

Con Make:

```bash
make backend-verify-container
```

Características:

- Maven 3.9.16 y Java 21;
- repositorio montado en solo lectura;
- `backend/target` en volumen efímero;
- caché Maven en `crm_maven_cache`;
- socket Docker para Testcontainers;
- `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`;
- cleanup de contenedor y target.

Advertencia: el socket Docker concede privilegios elevados. Ejecutar solo sobre código propio y revisado.

## Frontend

Con lockfile:

```bash
cd frontend
npm ci
npm run typecheck
npm run build
```

Sin lockfile, únicamente durante transición:

```bash
cd frontend
npm install
npm run typecheck
npm run build
```

Dockerfile, CI y Makefile seleccionan automáticamente el comando correcto.

## Generación de package-lock

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

```bash
sh scripts/generate-frontend-lock.sh
```

Comando npm:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

No ejecuta lifecycle scripts ni debe crear node_modules.

## Pruebas implementadas

### Unitarias

- `SendingPropertiesTest`
  - configuración cerrada;
  - no sustituye safety gate futuro.
- `NormalizationServiceTest`
  - nombres, diacríticos y puntuación;
  - email;
  - teléfono;
  - dominio;
  - dispatch por canal.
- `NameSimilarityServiceTest`
  - error tipográfico probable;
  - protección de sufijos numéricos.
- `ProspectImportFileParserTest`
  - fixture ficticia 100/16;
  - reconocimiento por encabezados;
  - marcadores no publicados;
  - CSV delimitadores/comillas/headers.

### Integración PostgreSQL/Testcontainers

- `ProspectPersistenceIntegrationTest`
  - Flyway;
  - Hibernate/JPA validate;
  - persistencia normalizada;
  - exclusión dominante;
  - ID externo repetido.
- `ProspectImportIntegrationTest`
  - fixture 100/16;
  - conteos;
  - idempotencia;
  - preview sin escritura de dominio;
  - exclusiones importadas;
  - error aislado por fila.
- `ProspectDeduplicationIntegrationTest`
  - coincidencia ambigua;
  - revisión humana;
  - sin fusión automática;
  - duplicado exacto enlazado.
- `ExclusionIntegrationTest`
  - exclusión retroactiva;
  - `DO_NOT_CONTACT`;
  - auditoría sin canal completo.
- `SecurityAuthorizationIntegrationTest`
  - health público;
  - API anónima rechazada;
  - bootstrap autorizado.

### Arquitectura

- ArchUnit valida límites del monolito modular.

### Smoke

- health backend;
- API autenticada;
- frontend;
- smoke host;
- smoke contenedor;
- sin creación de datos.

## Maven verify

Ejecuta:

- Maven Enforcer;
- compilación;
- JUnit;
- Testcontainers;
- ArchUnit;
- Spotless check;
- packaging.

El Dockerfile backend usa `-DskipTests`; construir la imagen no sustituye Maven verify.

## Validación del stack

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-docker-stack.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Valida:

- Compose config;
- imágenes;
- health;
- Flyway/Hibernate durante arranque;
- smoke host;
- smoke contenedor;
- evidencia JSON/transcript.

## Makefile

```bash
make verify
make verify-container
make backend-verify-container
make smoke-container
make repository-safety
```

`verify-container` permite un recorrido Unix sin Java/Node local.

## CI

Jobs:

- backend Maven verify;
- frontend npm ci/install, typecheck y build;
- scripts shell/PowerShell, seguridad y preflight;
- imágenes, stack y smoke E2E.

GitHub no muestra runs visibles para los commits consultados. No declarar CI verde hasta observar resultados.

## Estado de ejecución actual

### Real

```text
preflight: PASS
npm install: PASS
frontend inicial: FAIL reproducido
correcciones: aplicadas
imágenes: PASS_FROM_CACHE
stack: FAIL por 5432
```

### Estático o aislado

```text
shell: PASS_SYNTAX
Make: PASS_PARSE
CI YAML: PASS_PARSE
configurador Unix: PASS_FUNCTIONAL_ISOLATED
backend verify Unix: PASS_SYNTAX
lockfile seguro Unix: PASS_SYNTAX
```

### Pendiente

- validador integral PowerShell;
- Maven verify real;
- Testcontainers real;
- Flyway/Hibernate real;
- clean builds;
- stack healthy;
- smoke real;
- package-lock;
- npm ci real;
- CI visible.

## Pruebas pendientes de SEG-001 no bloqueantes

- RFC 7807 con MockMvc más exhaustivo;
- endpoints importación con/sin confirmación;
- equivalencia PHONE/WHATSAPP adicional;
- auditoría de fallo;
- concurrencia de clave idempotente;
- archivo corrupto;
- paginación/filtros adicionales;
- accesibilidad frontend.

## Pruebas futuras

- autorización por rol;
- safety gate acumulativo;
- MIME multipart;
- idempotencia de envío;
- reintentos Cloud Tasks;
- reconciliación Gmail;
- conflictos Sheets;
- follow-ups hábiles;
- kill switch concurrente;
- carga/rate limiting;
- E2E staging.
