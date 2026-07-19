# Gestudio CRM Platform

CRM comercial especializado en la prospección, revisión, importación y gestión segura de prospectos para Gestudio.

El diseño objetivo es un monolito modular que permita separar workers e integraciones sin reescribir el dominio. PostgreSQL es la fuente de verdad. Google Sheets será una interfaz auxiliar en segmentos posteriores.

## Estado actual

La rama activa `feat/seg-001-prospect-vertical-slice` contiene:

- backend Java 21 y Spring Boot;
- PostgreSQL y migraciones Flyway;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview y ejecución con confirmación explícita;
- cola de revisión humana de duplicados;
- auditoría estructurada;
- API REST y RFC 7807;
- interfaz React + TypeScript + Vite;
- Docker, Docker Compose y GitHub Actions;
- pruebas unitarias y de integración con Testcontainers.

El segmento sigue abierto hasta obtener una ejecución CI verde y corregir cualquier fallo real de compilación o migración.

## Seguridad de envío

No existe ningún adaptador de envío real. La configuración obligatoria es:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL inicializa además un kill switch persistente. Ningún cambio de este segmento puede habilitar correo.

## Requisitos locales

- Java 21;
- Docker con Docker Compose;
- Node.js 22 para el frontend;
- `curl` o `wget`, `unzip` y una herramienta SHA-512 si Maven no está instalado.

El repositorio incluye lanzadores Maven que descargan Maven 3.9.16 y verifican su SHA-512 antes de ejecutarlo.

## Inicio local — Linux/macOS

```bash
cp .env.example .env
# Editar .env y definir credenciales locales no compartidas.

docker compose up -d postgres
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`.

## Inicio local — Windows PowerShell

```powershell
Copy-Item .env.example .env
# Editar .env y definir credenciales locales no compartidas.

docker compose up -d postgres
Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

En otra terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

## Validaciones

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
(cd frontend && npm install && npm run build)
docker compose config
docker build -t gestudio-crm:local .
```

Windows PowerShell:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
Push-Location frontend
npm install
npm run build
Pop-Location
docker compose config
docker build -t gestudio-crm:local .
```

Las pruebas de integración requieren Docker porque utilizan PostgreSQL mediante Testcontainers.

## API implementada

```text
GET  /actuator/health
GET  /api/v1/prospects
POST /api/v1/prospects
GET  /api/v1/prospects/{id}

POST /api/v1/imports/prospects/preview
POST /api/v1/imports/prospects/execute
GET  /api/v1/imports/prospects/{jobId}
GET  /api/v1/imports/prospects/{jobId}/rows
GET  /api/v1/imports/prospects/duplicate-reviews/pending

GET  /api/v1/exclusions
POST /api/v1/exclusions
GET  /api/v1/exclusions/{id}

GET  /api/v1/audit
```

`POST /api/v1/imports/prospects/execute` exige:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

## Datos operativos

El repositorio es público. No se versionan:

- lotes reales de prospectos;
- correos o teléfonos comerciales;
- exportaciones de Gmail o Sheets;
- tokens OAuth;
- claves privadas;
- cuentas de servicio;
- secretos de infraestructura.

Las pruebas generan un workbook ficticio con la misma estructura de encabezados y los mismos conteos de referencia: 100 prospectos y 16 exclusiones.

## Continuidad

Antes de modificar el proyecto, leer:

1. `AGENTS.md`;
2. `docs/status.md`;
3. `docs/next-step.md`;
4. `docs/backlog.md`;
5. `docs/segments/SEG-001.md`;
6. ADR y documentación del módulo afectado.

La instrucción `continuar` ejecuta el trabajo indicado en `docs/next-step.md` y actualiza el estado al terminar.
