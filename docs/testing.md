# Estrategia de pruebas

## Regla de evidencia

Un test versionado no equivale a un test ejecutado. El estado debe distinguir:

- `IMPLEMENTED`: existe código de prueba;
- `EXECUTED_PASS`: se ejecutó y finalizó correctamente;
- `EXECUTED_FAIL`: se ejecutó y falló;
- `BLOCKED`: no pudo ejecutarse por entorno;
- `NOT_IMPLEMENTED`.

## Comando principal

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

## Pruebas implementadas

### Unitarias

- `SendingPropertiesTest`
  - configuración cerrada permite construir propiedades;
  - no sustituye pruebas del safety gate futuro.
- `NormalizationServiceTest`
  - nombres, diacríticos y puntuación;
  - email;
  - teléfono;
  - dominio;
  - dispatch por tipo de canal.
- `NameSimilarityServiceTest`
  - error tipográfico probable;
  - sufijos numéricos distintos no superan el umbral.
- `ProspectImportFileParserTest`
  - fixture anónima con 100 prospectos y 16 exclusiones;
  - reconocimiento por encabezados;
  - marcadores no publicados.

### Integración con PostgreSQL/Testcontainers

- `ProspectPersistenceIntegrationTest`
  - Flyway y validación JPA;
  - persistencia normalizada;
  - exclusión dominante al crear;
  - ID externo repetido.
- `ProspectImportIntegrationTest`
  - 100 prospectos + 16 exclusiones;
  - conteos de trabajo;
  - reimportación idempotente;
  - dry-run sin escrituras de dominio.
- `ProspectDeduplicationIntegrationTest`
  - coincidencia nominal ambigua;
  - revisión humana;
  - ausencia de fusión automática.
- `ExclusionIntegrationTest`
  - exclusión retroactiva;
  - transición a `DO_NOT_CONTACT`;
  - auditoría sin canal completo.
- `SecurityAuthorizationIntegrationTest`
  - health público;
  - API anónima rechazada;
  - bootstrap explícito autorizado.

## Validaciones de build configuradas

Maven `verify` ejecuta:

- compilación;
- JUnit;
- Testcontainers;
- Spotless check.

GitHub Actions configura:

- backend Maven;
- frontend TypeScript/Vite;
- `docker compose config`;
- build de imagen backend.

## Estado de ejecución actual

Las pruebas están implementadas, pero todavía no existe evidencia consultable de una ejecución CI para el último commit de la rama. El entorno de la sesión no dispone de acceso Git directo ni Docker/Maven local suficiente para afirmar un resultado.

Hasta observar CI:

- estado global: `IMPLEMENTED / EXECUTION_PENDING`;
- no marcar SEG-001 como completo;
- cualquier error de compilación encontrado por CI tiene prioridad sobre nuevas funciones.

## Pruebas pendientes de SEG-001

- API RFC 7807 con MockMvc;
- POST de importación con y sin confirmación;
- equivalencia PHONE/WHATSAPP en exclusión;
- auditoría de fallo de importación;
- concurrencia de la clave idempotente;
- archivo mayor a 10 MB;
- archivo corrupto;
- encabezado obligatorio ausente;
- CSV con comillas, comas y saltos de línea;
- paginación y filtro de estado;
- build frontend con lockfile.

## Pruebas futuras

- autorización por rol;
- safety gate acumulativo;
- MIME multipart;
- idempotencia de envío ante caída después de Gmail;
- reintentos Cloud Tasks;
- reconciliación Gmail;
- conflictos Sheets;
- follow-ups en días hábiles;
- kill switch concurrente;
- carga y rate limiting;
- E2E staging.
