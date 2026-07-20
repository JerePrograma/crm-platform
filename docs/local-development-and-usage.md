# Desarrollo local y flujo de uso

## Propósito

Describe cómo levantar Gestudio CRM desde `main` con procesos separados, comprobar cada componente y utilizar el vertical slice disponible.

El sistema administra prospectos, exclusiones, importaciones y auditoría. No existe adaptador de envío.

## Arquitectura local

Procesos:

1. PostgreSQL 17 en Docker;
2. backend Spring Boot;
3. frontend Vite;
4. proxy Vite de `/api` y `/actuator` al backend.

Puertos predeterminados:

```text
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
PORT=8080
```

`PORT` se usa cuando el backend corre directamente en el host. `BACKEND_HOST_PORT` se usa cuando el backend corre mediante Compose.

## Requisitos

- Git;
- Docker Desktop o Docker Engine con Compose v2;
- Java 21;
- Node.js 22 y npm;
- curl o wget;
- herramienta SHA-512 para el primer uso del Maven Wrapper.

Maven global no es necesario.

## Obtener el código

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
git status
git rev-parse HEAD
```

## Configurar `.env`

Linux/macOS:

```bash
[ -f .env ] || cp .env.example .env
```

Windows:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Valores mínimos:

```dotenv
POSTGRES_HOST_PORT=55432
BACKEND_HOST_PORT=8080
FRONTEND_HOST_PORT=5173
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=change-this-local-password
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

Cambiar la contraseña bootstrap. No modificar `SENDING_*`.

Configurar puertos de forma segura:

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173
```

Unix:

```bash
sh scripts/set-local-host-ports.sh 55432 8080 5173
```

## Preflight local

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
```

Unix:

```bash
sh scripts/preflight.sh --local
```

Valida Java, Node, npm, Docker, tres puertos, credenciales y guardas.

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

PostgreSQL queda disponible en:

```text
localhost:${POSTGRES_HOST_PORT}
```

Con valor predeterminado:

```text
localhost:55432
```

## Cargar variables

Linux/macOS:

```bash
set -a
. ./.env
set +a
```

Windows:

```powershell
Get-Content .env | ForEach-Object {
  $line = $_.TrimStart([char]0xFEFF)
  if ($line -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
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
- Spring Security configura bootstrap si ambas credenciales existen;
- backend responde en `http://localhost:${PORT}`.

Con valor predeterminado:

```text
http://localhost:8080
```

## Comprobar backend

Health:

```text
http://localhost:8080/actuator/health
```

PowerShell autenticado:

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
```

Sin credenciales, API/Swagger/OpenAPI deben responder 401; health permanece público.

## Levantar frontend

```bash
cd frontend
if [ -f package-lock.json ]; then npm ci; else npm install; fi
npm run dev
```

Abrir:

```text
http://localhost:5173
```

Las credenciales permanecen solo en memoria.

## Generar package-lock

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Unix:

```bash
sh scripts/generate-frontend-lock.sh
```

Después, frontend, Dockerfile, Makefile y CI utilizarán `npm ci` automáticamente.

## Flujo funcional

### 1. Dashboard

Comprobar:

- prospectos visibles;
- contactos bloqueados;
- exclusiones;
- revisiones ambiguas;
- envíos bloqueados.

### 2. Exclusiones

Canales:

```text
EMAIL
PHONE
WHATSAPP
WEBSITE
SOCIAL
```

Motivos incluyen baja, respuesta negativa, bounce, contacto inválido, cliente existente y conversación existente.

Una exclusión es dominante. Si coincide con un prospecto, queda no elegible y pasa a `DO_NOT_CONTACT`.

### 3. Archivo

- CSV UTF-8 con coma o punto y coma;
- XLSX de hasta 10 MB;
- hoja `Prospectos`;
- hoja opcional `Exclusiones`;
- CSV requiere `Institución`.

No versionar datos reales.

### 4. Preview

Estados:

```text
ACCEPTED
EXCLUDED
REJECTED
DUPLICATE
REVIEW_REQUIRED
```

El preview persiste evidencia, pero no crea instituciones, contactos, prospectos ni exclusiones procedentes del archivo.

### 5. Revisiones ambiguas

No existe fusión automática ni resolución UI. Corregir el archivo antes de ejecutar.

### 6. Ejecución

Usar `Importar con confirmación`.

Cabecera API:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

La ejecución procesa por fila, crea prospectos elegibles, conserva bloqueados, importa exclusiones y registra auditoría.

### 7. Prospectos

Revisar institución, localidad, estado, elegibilidad, prioridad, puntuación, alumnos estimados, fuente y propietario.

### 8. Auditoría

Eventos principales:

```text
PROSPECT_CREATED
EXCLUSION_CREATED
IMPORT_STARTED
IMPORT_COMPLETED
IMPORT_FAILED
```

## Smoke local

Con procesos activos:

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Unix:

```bash
sh scripts/smoke-test.sh
```

Los URLs se derivan de `BACKEND_HOST_PORT` y `FRONTEND_HOST_PORT`, o de overrides explícitos.

## Validar proyecto

Backend:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Frontend:

```bash
cd frontend
if [ -f package-lock.json ]; then npm ci; else npm install; fi
npm run typecheck
npm run build
```

Testcontainers requiere Docker.

## Detener

Backend/frontend: `Ctrl+C`.

PostgreSQL conservando volumen:

```bash
docker compose stop postgres
```

Retirar contenedor conservando volumen:

```bash
docker compose down
```

Eliminar base local:

```bash
docker compose down -v
```

La última operación es destructiva.

## Problemas frecuentes

### Puerto ocupado

```powershell
powershell -ExecutionPolicy Bypass -File scripts/set-local-host-ports.ps1 `
  -PostgresPort 55433 `
  -BackendPort 18080 `
  -FrontendPort 15173
```

### 401

- comprobar credenciales no vacías;
- reiniciar backend después de cambiar `.env`;
- cargar variables en la terminal;
- usar las mismas credenciales en UI.

### Contraseña PostgreSQL

Si cambió después de crear el volumen:

```bash
docker compose down -v
docker compose up -d postgres
```

Esto destruye la base anterior.

### Maven Wrapper

El primer uso requiere red, curl/wget y SHA-512.

### Testcontainers

Confirmar Docker activo y `docker ps` funcional.

### HTTP 413

El archivo supera 10 MB. Dividir el lote.

### CSV incorrecto

- guardar UTF-8;
- usar coma o punto y coma;
- cerrar campos entre comillas;
- evitar encabezados duplicados normalizados.

## Seguridad y límites

- no existe envío de correo;
- no existen Gmail, SMTP, Cloud Tasks ni campañas;
- HTTP Basic es temporal;
- no existe RBAC persistente;
- no usar datos reales en pruebas o CI;
- no exponer servicios fuera de localhost;
- no desplegar sin cerrar `docs/validation/SEG-001.md`.

## Documentación relacionada

- `README.md`;
- `docs/status.md`;
- `docs/next-step.md`;
- `docs/containerized-quickstart.md`;
- `docs/import-existing-data.md`;
- `docs/import-hardening.md`;
- `docs/testing.md`;
- `docs/security.md`;
- `docs/validation/SEG-001.md`.
