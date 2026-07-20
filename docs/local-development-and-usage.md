# Desarrollo local y flujo de uso

## Propósito

Guía para levantar Gestudio CRM desde `main`, comprobar componentes y utilizar el vertical slice disponible.

El sistema administra prospectos, exclusiones, importaciones y auditoría. No existe adaptador de envío.

## Arquitectura local

Procesos:

1. PostgreSQL 17 en Docker, publicado por defecto en `127.0.0.1:55432`;
2. backend Spring Boot en `8080`;
3. frontend Vite en `5173`;
4. proxy Vite de `/api` y `/actuator` al backend.

PostgreSQL es la fuente de verdad. El frontend no guarda credenciales en almacenamiento persistente del navegador.

Dentro de Compose, PostgreSQL conserva el puerto `5432`. `POSTGRES_HOST_PORT` solo controla el puerto publicado en el host.

## Requisitos

- Git;
- Docker con Compose v2;
- Java 21;
- Node.js 22 y npm;
- curl o wget y SHA-512 para Maven Wrapper.

Maven global no es necesario. `mvnw` y `mvnw.cmd` descargan Maven 3.9.16 y verifican SHA-512.

## Obtener el código

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

Comprobar:

```bash
git status
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
```

Si `mvnw.cmd` aparece modificado sin una edición intencional:

```bash
git diff -- mvnw.cmd
git restore -- mvnw.cmd
```

## Configurar variables

Linux/macOS:

```bash
cp .env.example .env
```

Windows:

```powershell
Copy-Item .env.example .env
```

No sobrescribir un `.env` ya configurado.

Valores mínimos:

```dotenv
POSTGRES_DB=gestudio_crm
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
DATABASE_POOL_SIZE=10
DATABASE_MIN_IDLE=1
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=change-this-local-password
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

Cambiar `CRM_BOOTSTRAP_PASSWORD`. `POSTGRES_HOST_PORT` y el puerto de `DATABASE_URL` deben coincidir.

No modificar:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

`.env` está ignorado por Git.

## Preflight

Linux/macOS:

```bash
sh scripts/preflight.sh --local
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

Valida herramientas, puerto PostgreSQL, URL, credenciales, Compose y guardas.

## Levantar PostgreSQL

```bash
docker compose up -d postgres
docker compose ps
```

Esperar `healthy`.

Logs:

```bash
docker compose logs -f postgres
```

Puerto host predeterminado:

```text
127.0.0.1:55432
```

El contenedor escucha internamente en `5432`.

Si se cambia la contraseña después de crear el volumen:

```bash
docker compose down -v
docker compose up -d postgres
```

`down -v` elimina los datos locales.

## Cargar variables

Linux/macOS:

```bash
set -a
. ./.env
set +a
```

Windows PowerShell:

```powershell
Get-Content .env | ForEach-Object {
  $line = $_.TrimStart([char]0xFEFF)
  if ($line -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
```

Comprobar sin imprimir secretos:

```powershell
$env:POSTGRES_HOST_PORT
$env:DATABASE_URL
$env:DATABASE_USER
$env:PORT
```

## Levantar backend

Linux/macOS:

```bash
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Durante el arranque:

- Flyway aplica V1–V5;
- Hibernate valida el esquema;
- Spring Security crea el usuario bootstrap si ambas credenciales existen;
- backend queda en `http://localhost:8080`.

## Comprobar backend

Health:

```bash
curl http://localhost:8080/actuator/health
```

Esperado:

```json
{"status":"UP"}
```

API autenticada:

```bash
curl -u "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  "http://localhost:8080/api/v1/prospects?size=5"
```

PowerShell:

```powershell
$pair = "$($env:CRM_BOOTSTRAP_USERNAME):$($env:CRM_BOOTSTRAP_PASSWORD)"
$encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
Invoke-RestMethod `
  -Uri 'http://localhost:8080/api/v1/prospects?size=5' `
  -Headers @{ Authorization = "Basic $encoded" }
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

Sin credenciales, API, Swagger y OpenAPI deben responder 401. Health permanece público.

## Levantar frontend

```bash
cd frontend
npm install
npm run dev
```

Abrir:

```text
http://localhost:5173
```

Vite redirige `/api` y `/actuator` a `http://localhost:8080`.

## Build frontend limpio

```bash
npm run typecheck
npm run build
```

Mediante Docker:

```bash
docker compose --progress plain --profile app build --no-cache frontend
```

## Flujo funcional

### 1. Dashboard

Comprobar:

- prospectos visibles;
- interés o pipeline;
- contactos bloqueados;
- exclusiones;
- revisiones pendientes;
- envíos bloqueados.

### 2. Exclusiones

Canales:

- EMAIL;
- PHONE;
- WHATSAPP;
- WEBSITE;
- SOCIAL.

Motivos:

- manual;
- baja;
- respuesta negativa;
- rebote permanente;
- contacto inválido;
- cliente existente;
- conversación existente;
- institución no pertinente.

Una exclusión domina la elegibilidad. Teléfono y WhatsApp son equivalentes al normalizar al mismo número.

### 3. Archivo

Admite:

- CSV con coma o punto y coma;
- XLSX hasta 10 MB;
- hoja `Prospectos`;
- hoja opcional `Exclusiones`.

CSV requiere `Institución`. El parser usa encabezados normalizados.

No versionar datos reales.

### 4. Preview

El preview:

- persiste ImportJob, ImportRow y evidencia;
- calcula SHA-256;
- valida filas;
- detecta duplicados;
- genera revisiones ambiguas;
- aplica exclusiones;
- no crea dominio desde el archivo.

Estados:

- ACCEPTED;
- EXCLUDED;
- REJECTED;
- DUPLICATE;
- REVIEW_REQUIRED.

La UI muestra Aceptadas, Bloqueadas, Rechazadas, Duplicadas y A revisión.

### 5. Duplicados ambiguos

No existe resolución automática. Corregir o revisar el archivo antes de ejecutar.

### 6. Ejecución

Pulsar `Importar con confirmación`.

Cabecera exigida:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

La ejecución procesa cada fila de forma independiente, crea elegibles, conserva bloqueados, importa exclusiones y audita.

### 7. Prospectos

Revisar estado, elegibilidad, prioridad, puntuación, alumnos estimados, fuente y propietario.

### 8. Auditoría

Eventos principales:

- PROSPECT_CREATED;
- EXCLUSION_CREATED;
- IMPORT_STARTED;
- IMPORT_COMPLETED;
- IMPORT_FAILED.

## API principal

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

## Validar proyecto

Backend:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Testcontainers requiere Docker.

Frontend:

```bash
cd frontend
npm install
npm run typecheck
npm run build
cd ..
```

Infraestructura:

```bash
docker compose --profile app --profile smoke config
docker compose --progress plain --profile app build --no-cache frontend
docker compose --progress plain --profile app build --no-cache backend
```

## Generar lockfile

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Linux/macOS:

```bash
sh scripts/generate-frontend-lock.sh
```

## Detener

Backend/frontend: Ctrl+C.

PostgreSQL conservando datos:

```bash
docker compose stop postgres
```

Retirar contenedor conservando volumen:

```bash
docker compose down
```

Eliminar base:

```bash
docker compose down -v
```

## Problemas frecuentes

### 401

- credenciales bootstrap no vacías;
- backend reiniciado tras modificar `.env`;
- terminal con variables cargadas;
- credenciales UI idénticas.

### Contraseña PostgreSQL

Recrear volumen solo en entorno descartable:

```bash
docker compose down -v
docker compose up -d postgres
```

### Puerto PostgreSQL ocupado

Usar:

```dotenv
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Windows:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

No es necesario detener otro PostgreSQL si se usa un puerto host alternativo.

### Puerto 8080

Cambiar PORT y actualizar proxy si corresponde.

### Puerto 5173

Abrir la URL informada por Vite.

### Maven Wrapper

El primer uso requiere red, curl/wget y SHA-512.

### Testcontainers

Confirmar Docker y `docker ps`.

### HTTP 413

El archivo supera 10 MB. Dividir el lote.

### CSV incorrecto

- UTF-8;
- coma o punto y coma;
- comillas cerradas;
- encabezados no duplicados tras normalización.

## Seguridad y límites

- sin envío;
- sin Gmail, SMTP, campañas o cloud;
- HTTP Basic temporal;
- sin RBAC persistente;
- datos reales fuera de pruebas/CI;
- PostgreSQL solo en localhost;
- no desplegar sin cerrar SEG-001.

## Documentación relacionada

- `README.md`;
- `docs/status.md`;
- `docs/next-step.md`;
- `docs/containerized-quickstart.md`;
- `docs/import-existing-data.md`;
- `docs/import-hardening.md`;
- `docs/validation/SEG-001.md`.
