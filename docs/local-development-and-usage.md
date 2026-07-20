# Desarrollo local y flujo de uso

## Propósito

Este documento describe cómo levantar Gestudio CRM desde `main`, comprobar que cada componente funciona y utilizar el vertical slice disponible. Es la guía operativa principal para desarrollo local.

El sistema actual administra prospectos, exclusiones, importaciones y auditoría. No existe ningún adaptador capaz de enviar correos.

## Arquitectura local

Los procesos locales son:

1. PostgreSQL 17 en Docker, puerto `5432` limitado a `127.0.0.1`;
2. backend Spring Boot, puerto `8080`;
3. frontend Vite, puerto `5173`;
4. proxy de Vite desde `/api` y `/actuator` hacia el backend.

PostgreSQL es la fuente de verdad. El frontend no guarda las credenciales en `localStorage` ni `sessionStorage`.

## Requisitos

Instalar:

- Git;
- Docker Desktop o Docker Engine con Compose v2;
- Java 21;
- Node.js 22 y npm;
- `curl` o `wget` y una herramienta SHA-512 para el primer uso del Maven Wrapper.

Maven no necesita instalarse globalmente. `mvnw` y `mvnw.cmd` descargan Maven 3.9.16 y validan su SHA-512.

Comprobar versiones:

```bash
git --version
docker --version
docker compose version
java -version
node --version
npm --version
```

## Obtener el código

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

Verificar la rama:

```bash
git status
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
```

La rama esperada es `main`.

## Configurar variables

### Linux y macOS

```bash
cp .env.example .env
```

### Windows PowerShell

```powershell
Copy-Item .env.example .env
```

Editar `.env` antes de iniciar. Los valores mínimos son:

```dotenv
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:5432/gestudio_crm
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

Cambiar `CRM_BOOTSTRAP_PASSWORD`. También puede cambiarse `DATABASE_PASSWORD`; Docker Compose y el backend consumen la misma variable.

No modificar las cuatro guardas de envío. El segmento actual exige:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

`.env` está ignorado por Git y no debe versionarse.

## Levantar PostgreSQL

Docker Compose lee `.env` automáticamente.

```bash
docker compose up -d postgres
docker compose ps
```

Esperar hasta que `postgres` figure como `healthy`.

Consultar logs si no inicia:

```bash
docker compose logs -f postgres
```

La primera creación del volumen toma la contraseña disponible en ese momento. Si se cambia `DATABASE_PASSWORD` después de haber creado el volumen y el backend informa error de autenticación, debe recrearse el volumen local:

```bash
docker compose down -v
docker compose up -d postgres
```

`down -v` elimina todos los datos locales. No usarlo sobre un ambiente con información que deba conservarse.

## Cargar variables en la terminal

### Linux y macOS

```bash
set -a
. ./.env
set +a
```

Comprobar sin imprimir secretos:

```bash
printf 'DB=%s USER=%s PORT=%s\n' "$DATABASE_URL" "$DATABASE_USER" "$PORT"
```

### Windows PowerShell

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
```

Comprobar:

```powershell
$env:DATABASE_URL
$env:DATABASE_USER
$env:PORT
```

## Levantar el backend

### Linux y macOS

```bash
sh ./mvnw -f backend/pom.xml spring-boot:run
```

### Windows PowerShell

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Durante el arranque:

- Flyway aplica V1–V5;
- Hibernate valida el esquema;
- Spring Security crea el usuario bootstrap únicamente si ambas credenciales están configuradas;
- el backend queda en `http://localhost:8080`.

No cerrar esta terminal.

## Comprobar el backend

Health público:

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

API autenticada:

```bash
curl -u "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  "http://localhost:8080/api/v1/prospects?size=5"
```

En PowerShell:

```powershell
$pair = "$($env:CRM_BOOTSTRAP_USERNAME):$($env:CRM_BOOTSTRAP_PASSWORD)"
$encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
Invoke-RestMethod \
  -Uri 'http://localhost:8080/api/v1/prospects?size=5' \
  -Headers @{ Authorization = "Basic $encoded" }
```

OpenAPI, con autenticación Basic:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

Sin credenciales bootstrap, `/api/**`, Swagger y OpenAPI deben responder `401`. Solo health es público.

## Levantar el frontend

Abrir otra terminal.

### Linux, macOS y Windows

```bash
cd frontend
npm install
npm run dev
```

Abrir:

```text
http://localhost:5173
```

Vite redirige `/api` y `/actuator` hacia `http://localhost:8080`.

Ingresar con:

- usuario: valor de `CRM_BOOTSTRAP_USERNAME`;
- contraseña: valor de `CRM_BOOTSTRAP_PASSWORD`.

Las credenciales permanecen solo en memoria. Al recargar o cerrar sesión deben ingresarse nuevamente.

## Flujo funcional recomendado

### 1. Verificar el Dashboard

Al ingresar, comprobar:

- cantidad de prospectos visibles;
- prospectos en interés o pipeline;
- contactos bloqueados;
- exclusiones;
- revisiones ambiguas pendientes;
- panel que muestra envíos bloqueados.

La pantalla puede estar vacía en una base nueva.

### 2. Registrar exclusiones conocidas

Antes de incorporar un lote, abrir `Exclusiones` y registrar canales que no deban contactarse.

Canales disponibles:

- `EMAIL`;
- `PHONE`;
- `WHATSAPP`;
- `WEBSITE`;
- `SOCIAL`.

Motivos disponibles:

- exclusión manual;
- pedido de baja;
- respuesta negativa;
- rebote permanente;
- contacto inválido;
- cliente existente;
- conversación existente;
- institución no pertinente.

Una exclusión es dominante. Si el canal pertenece a un prospecto existente, el prospecto queda no elegible y pasa a `DO_NOT_CONTACT`.

Teléfono y WhatsApp se consideran equivalentes cuando normalizan al mismo número.

### 3. Preparar el archivo

Formatos admitidos:

- `.csv`, separado por coma o punto y coma;
- `.xlsx`, máximo 10 MB.

CSV exige al menos la columna `Institución`.

XLSX admite:

- hoja `Prospectos`;
- hoja opcional `Exclusiones`.

El parser utiliza encabezados normalizados, no posiciones rígidas. Consultar `docs/import-existing-data.md` y `docs/import-hardening.md` para aliases y reglas completas.

No versionar el archivo real dentro del repositorio.

### 4. Ejecutar preview

Abrir `Importaciones`, seleccionar el archivo y pulsar `Ejecutar preview`.

El preview:

- persiste `ImportJob`, `ImportRow` y evidencia de revisión;
- calcula SHA-256 e idempotencia;
- valida formato, correos, dominios y filas;
- detecta duplicados exactos;
- genera revisiones ambiguas;
- aplica exclusiones existentes;
- no crea instituciones, contactos, prospectos ni exclusiones del archivo.

Revisar los estados por fila:

- `ACCEPTED`: apta para ejecución;
- `EXCLUDED`: bloqueada por una exclusión;
- `REJECTED`: inválida, con motivo visible;
- `DUPLICATE`: coincidencia exacta;
- `REVIEW_REQUIRED`: coincidencia ambigua que no se fusiona automáticamente.

No ejecutar el lote hasta entender los rechazos, bloqueados y revisiones.

### 5. Revisar duplicados ambiguos

La tabla `Duplicados ambiguos pendientes` muestra:

- hoja y fila;
- tipo de coincidencia;
- confianza;
- nota.

El segmento actual no ofrece todavía una acción de resolución. Esas filas deben revisarse manualmente y corregirse en el archivo antes de una nueva importación.

### 6. Ejecutar la importación

Seleccionar el mismo archivo y pulsar `Importar con confirmación`.

La interfaz solicita confirmación. La API exige además:

```text
X-Import-Confirmation: EXECUTE_PROSPECT_IMPORT
```

La ejecución:

- procesa cada fila en una transacción independiente;
- crea prospectos elegibles;
- conserva filas bloqueadas sin convertirlas en elegibles;
- importa exclusiones y deshabilita prospectos previos cuando corresponde;
- no fusiona coincidencias ambiguas;
- registra auditoría.

El mismo archivo y modo son idempotentes: repetirlo devuelve el trabajo existente en lugar de duplicar resultados.

### 7. Revisar Prospectos

Abrir `Prospectos` para:

- filtrar por estado;
- ver institución y localidad;
- comprobar elegibilidad;
- abrir la ficha integral;
- revisar prioridad, puntuación, alumnos estimados, fuente y propietario.

El segmento actual no incluye edición completa de la ficha ni transición de estados desde la UI.

### 8. Revisar Auditoría

Abrir `Auditoría` para confirmar eventos como:

- `PROSPECT_CREATED`;
- `EXCLUSION_CREATED`;
- `IMPORT_STARTED`;
- `IMPORT_COMPLETED`;
- `IMPORT_FAILED`.

La auditoría evita copiar canales completos en el payload de exclusión.

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

## Validar el proyecto

Backend, formato y pruebas:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

En Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Las pruebas de integración requieren Docker porque utilizan Testcontainers y PostgreSQL real.

Frontend:

```bash
cd frontend
npm install
npm run typecheck
npm run build
cd ..
```

Infraestructura local:

```bash
docker compose config
docker build -t gestudio-crm:local .
```

## Detener el sistema

Detener frontend y backend con `Ctrl+C` en sus terminales.

Detener PostgreSQL conservando datos:

```bash
docker compose stop postgres
```

Detener y retirar el contenedor conservando el volumen:

```bash
docker compose down
```

Eliminar también la base local:

```bash
docker compose down -v
```

## Arranque posterior

Cuando `.env`, dependencias y volumen ya existen:

Terminal 1:

```bash
docker compose up -d postgres
set -a && . ./.env && set +a
sh ./mvnw -f backend/pom.xml spring-boot:run
```

Terminal 2:

```bash
cd frontend
npm run dev
```

## Problemas frecuentes

### `401 Unauthorized`

- comprobar que usuario y contraseña bootstrap no estén vacíos;
- reiniciar backend después de modificar `.env`;
- comprobar que la terminal del backend cargó las variables;
- usar exactamente las mismas credenciales en la UI.

### Error de contraseña PostgreSQL

Si la contraseña cambió después de crear el volumen:

```bash
docker compose down -v
docker compose up -d postgres
```

Esto destruye la base local anterior.

### Puerto `5432` ocupado

Detener otro PostgreSQL local o cambiar el mapeo de puerto y `DATABASE_URL` de forma consistente.

### Puerto `8080` ocupado

Cambiar `PORT` en `.env`. El proxy de Vite debe actualizarse si el backend deja de usar `8080`.

### Puerto `5173` ocupado

Vite puede seleccionar otro puerto. Abrir la URL que muestra la terminal.

### Maven Wrapper no descarga

El primer uso requiere acceso de red a la distribución Maven. Verificar proxy, DNS, `curl`/`wget` y herramientas SHA-512.

### Testcontainers falla

Confirmar que Docker está iniciado y que el usuario puede ejecutar `docker ps`.

### Importación rechazada con 413

El archivo supera 10 MB. Dividir el lote; no aumentar el límite sin una decisión de arquitectura y seguridad documentada.

### CSV interpretado incorrectamente

- guardar en UTF-8;
- usar coma o punto y coma;
- cerrar correctamente todos los campos entre comillas;
- evitar encabezados duplicados después de quitar tildes y puntuación.

## Seguridad y límites actuales

- no existe envío de correo;
- no existen Gmail, SMTP, Cloud Tasks ni campañas;
- HTTP Basic es temporal para desarrollo;
- no existe RBAC persistente;
- no se deben usar datos reales en pruebas o CI;
- no se debe exponer PostgreSQL fuera de localhost;
- no se debe desplegar esta rama sin completar `docs/validation/SEG-001.md`.

## Documentación relacionada

- `README.md`;
- `docs/status.md`;
- `docs/next-step.md`;
- `docs/import-existing-data.md`;
- `docs/import-hardening.md`;
- `docs/manual-operations.md`;
- `docs/testing.md`;
- `docs/security.md`;
- `docs/validation/SEG-001.md`.
