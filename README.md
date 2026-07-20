# Gestudio CRM Platform

CRM comercial para importar, revisar y administrar prospectos de Gestudio con PostgreSQL como fuente de verdad y controles fail-closed.

## Estado actual

Todo el código y la documentación vigentes están consolidados en `main`.

`SEG-001 — Vertical slice persistente de prospectos` está funcionalmente implementado y endurecido mediante revisión estática. Continúa abierto hasta obtener evidencia ejecutada de Maven, Testcontainers, Flyway, Hibernate, frontend, Compose e imagen Docker.

El sistema incluye:

- backend Java 21 y Spring Boot;
- PostgreSQL 17, Flyway V1–V5 y JPA `validate`;
- instituciones, contactos, canales, prospectos y exclusiones;
- normalización, elegibilidad y deduplicación exacta/ambigua;
- importaciones CSV/XLSX persistentes e idempotentes;
- preview con paridad de validación y ejecución confirmada;
- evidencia por fila y cola de revisión humana;
- auditoría JSONB;
- API REST, OpenAPI y RFC 7807;
- interfaz React, TypeScript y Vite;
- Docker, Docker Compose, GitHub Actions y Testcontainers.

Estado detallado: `docs/status.md`.

## Seguridad de envío

No existe ningún adaptador de correo, Gmail o SMTP. Los valores obligatorios son:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

PostgreSQL inicializa además un kill switch persistente. Ninguna operación disponible puede enviar mensajes.

## Inicio rápido

### Requisitos

- Git;
- Java 21;
- Docker con Compose v2;
- Node.js 22 y npm;
- `curl` o `wget`, `unzip` y SHA-512 para el primer uso del Maven Wrapper.

### 1. Clonar `main`

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

### 2. Crear configuración local

Linux/macOS:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Editar `.env` y cambiar al menos `CRM_BOOTSTRAP_PASSWORD`. Mantener cerradas las cuatro variables de envío.

### 3. Levantar PostgreSQL

```bash
docker compose up -d postgres
docker compose ps
```

Compose y el backend consumen el mismo `DATABASE_USER`, `DATABASE_PASSWORD` y `POSTGRES_DB` desde `.env`.

### 4. Levantar backend

Linux/macOS:

```bash
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Windows PowerShell:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Health:

```text
http://localhost:8080/actuator/health
```

Swagger, con autenticación Basic:

```text
http://localhost:8080/swagger-ui/index.html
```

### 5. Levantar frontend

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Abrir:

```text
http://localhost:5173
```

Ingresar con `CRM_BOOTSTRAP_USERNAME` y `CRM_BOOTSTRAP_PASSWORD`.

La guía completa, incluyendo comprobaciones, troubleshooting, detención y flujo funcional, está en:

```text
docs/local-development-and-usage.md
```

## Flujo operativo recomendado

1. ingresar al Dashboard y comprobar que los envíos figuran bloqueados;
2. registrar en `Exclusiones` los canales que no deben contactarse;
3. preparar un CSV o XLSX de hasta 10 MB;
4. ejecutar `Preview` desde `Importaciones`;
5. revisar filas `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
6. corregir el archivo cuando corresponda;
7. ejecutar `Importar con confirmación`;
8. revisar los prospectos creados y su elegibilidad;
9. comprobar los eventos en `Auditoría`.

El preview persiste evidencia, pero no crea prospectos ni exclusiones procedentes del archivo. La ejecución requiere confirmación en la UI y la cabecera:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

## Formatos de importación

- CSV UTF-8 separado por coma o punto y coma;
- XLSX con hoja `Prospectos` y hoja opcional `Exclusiones`;
- tamaño máximo: 10 MB;
- CSV requiere al menos el encabezado `Institución`;
- el parser trabaja por encabezados normalizados, no por posiciones fijas.

Contrato completo:

- `docs/import-existing-data.md`;
- `docs/import-hardening.md`.

Los datos operativos reales no se versionan. Las pruebas generan fixtures con dominios `.test`.

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

## Validación

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify

cd frontend
npm install
npm run typecheck
npm run build
cd ..

docker compose config
docker build -t gestudio-crm:local .
```

Windows PowerShell:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify

Push-Location frontend
npm install
npm run typecheck
npm run build
Pop-Location

docker compose config
docker build -t gestudio-crm:local .
```

Las pruebas de integración requieren Docker porque utilizan PostgreSQL mediante Testcontainers.

La evidencia real se registra exclusivamente en `docs/validation/SEG-001.md`.

## Detener el entorno

Detener backend y frontend con `Ctrl+C`.

Conservar base local:

```bash
docker compose down
```

Eliminar también la base local:

```bash
docker compose down -v
```

`down -v` destruye los datos del volumen.

## Limitaciones actuales

- `SEG-001` no debe declararse completo sin CI o validación local verde;
- falta `package-lock.json` hasta ejecutar `npm install` en un entorno con registro disponible;
- HTTP Basic es temporal;
- no existen usuarios persistentes ni RBAC;
- no existe resolución desde UI de `DuplicateReview`;
- no existe retry explícito de trabajos fallidos;
- no existen campañas, Gmail, Sheets, workers ni infraestructura cloud.

## Continuidad

Antes de modificar el proyecto, leer:

1. `AGENTS.md`;
2. `docs/status.md`;
3. `docs/next-step.md`;
4. `docs/backlog.md`;
5. `docs/segments/SEG-001.md`;
6. `docs/validation/SEG-001.md`;
7. ADR y documentación del módulo afectado.

La instrucción `continuar` ejecuta el trabajo indicado en `docs/next-step.md` y actualiza toda la documentación canónica al finalizar.
