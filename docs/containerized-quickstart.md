# Inicio rápido completamente contenedorizado

## Objetivo

Esta ruta levanta PostgreSQL, backend y frontend con Docker Compose. En el host solo requiere Git y Docker con Compose v2.

No habilita comunicaciones. El backend no contiene adaptadores Gmail o SMTP y las cuatro guardas de envío deben permanecer cerradas.

## 1. Obtener el repositorio

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
```

En un checkout existente:

```bash
git status
git diff -- mvnw.cmd
git pull --ff-only
```

Restaurar `mvnw.cmd` solo si su modificación local no fue intencional:

```bash
git restore -- mvnw.cmd
```

## 2. Crear el entorno local

Linux/macOS:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

No volver a copiar el archivo si `.env` ya contiene credenciales elegidas.

Configuración recomendada:

```dotenv
POSTGRES_DB=gestudio_crm
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
DATABASE_POOL_SIZE=10
DATABASE_MIN_IDLE=1
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

`POSTGRES_HOST_PORT` controla únicamente el puerto publicado en el host. Dentro de Compose, el backend siempre conecta a `postgres:5432`.

El valor `55432` evita conflictos comunes con PostgreSQL local, otros contenedores y reservas de Windows.

## 3. Ejecutar preflight

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Con Make:

```bash
make preflight-container
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

El preflight comprueba:

- Git, Docker y Compose;
- `.env`;
- base, usuario y contraseña;
- `POSTGRES_HOST_PORT` entre 1 y 65535;
- coincidencia del puerto con `DATABASE_URL`;
- credenciales bootstrap;
- cuatro guardas de envío cerradas;
- parseo de Compose.

## 4. Construir e iniciar el stack

Ruta habitual:

```bash
docker compose --profile app up -d --build
```

Con Make:

```bash
make app-up
```

Servicios:

| Servicio | Puerto host predeterminado | Puerto interno | Función |
|---|---:|---:|---|
| PostgreSQL | `127.0.0.1:55432` | `5432` | fuente de verdad |
| Backend | `127.0.0.1:8080` | `8080` | API, Flyway y seguridad |
| Frontend | `127.0.0.1:5173` | `8080` | SPA y proxy al backend |

Orden de inicio:

1. PostgreSQL saludable;
2. backend aplica Flyway y responde health;
3. frontend inicia después del backend saludable.

## 5. Build limpio para validación

Una salida completamente `CACHED` confirma que una imagen puede exportarse, pero no demuestra que el código actual compile desde cero.

Frontend:

```bash
docker compose --progress plain --profile app build --no-cache frontend
```

Backend:

```bash
docker compose --progress plain --profile app build --no-cache backend
```

`--progress` es una opción global y debe ir antes de `build`.

El Dockerfile backend usa `-DskipTests`; las pruebas completas se ejecutan después con Maven Wrapper.

## 6. Comprobar estado

```bash
docker compose --profile app ps
```

Los tres servicios deben aparecer activos y saludables.

Logs combinados:

```bash
docker compose --profile app logs -f
```

Logs individuales:

```bash
docker compose logs -f postgres
docker compose logs -f backend
docker compose logs -f frontend
```

## 7. Abrir el sistema

Frontend:

```text
http://localhost:5173
```

Ingresar con:

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

Health:

```text
http://localhost:8080/actuator/health
```

Swagger autenticado:

```text
http://localhost:8080/swagger-ui/index.html
```

## 8. Smoke contra stack activo

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Con Make:

```bash
make smoke
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba health, API autenticada y frontend. No crea datos.

## 9. Smoke efímero contenedorizado

Con Make:

```bash
make smoke-container
```

Comando directo:

```bash
docker compose --profile app --profile smoke up \
  --build \
  --abort-on-container-exit \
  --exit-code-from smoke \
  smoke
```

Retirar después:

```bash
docker compose --profile app --profile smoke down --remove-orphans
```

El contenedor smoke verifica desde la red interna:

- backend health;
- Basic Auth contra prospectos;
- documento raíz de Nginx.

## 10. Flujo funcional

1. ingresar al dashboard;
2. comprobar que los envíos estén bloqueados;
3. registrar exclusiones conocidas;
4. preparar CSV o XLSX de hasta 10 MB;
5. ejecutar preview;
6. revisar `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
7. corregir el archivo;
8. ejecutar importación confirmada;
9. revisar prospectos y elegibilidad;
10. revisar auditoría.

La descripción completa está en `docs/local-development-and-usage.md`.

## 11. Generar package-lock sin Node local

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-frontend-lock.ps1
```

Linux/macOS:

```bash
sh scripts/generate-frontend-lock.sh
```

Con Make:

```bash
make frontend-lock
```

Después:

```bash
git status --short
```

El lockfile debe versionarse antes de migrar Docker y CI a `npm ci`.

## 12. Pruebas backend completas

Windows:

```powershell
.\mvnw.cmd -B -f backend\pom.xml verify
```

Linux/macOS:

```bash
sh ./mvnw -B -f backend/pom.xml verify
```

Requieren Docker para Testcontainers.

## 13. Detener conservando datos

```bash
docker compose --profile app down
```

Con Make:

```bash
make app-down
```

El volumen `crm_postgres` se conserva.

## 14. Eliminar la base local

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

Con Make:

```bash
make reset-db
```

Esta operación es destructiva.

## 15. Diagnóstico

### Puerto PostgreSQL ocupado

Error típico:

```text
listen tcp4 127.0.0.1:5432: bind ...
```

Solución recomendada en `.env`:

```dotenv
POSTGRES_HOST_PORT=55432
DATABASE_URL=jdbc:postgresql://localhost:55432/gestudio_crm
```

Windows, identificar proceso:

```powershell
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Puertos reservados:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

No es necesario detener otro PostgreSQL si se usa un puerto host distinto.

### Backend no saludable

```bash
docker compose logs backend
```

Revisar:

- PostgreSQL saludable;
- credenciales;
- Flyway;
- Hibernate validate;
- memoria Docker.

### Frontend devuelve 502

```bash
docker compose --profile app ps
docker compose logs backend
docker compose logs frontend
```

Nginx necesita resolver `backend:8080`.

### Smoke falla

```bash
docker compose --profile app --profile smoke logs --no-color
```

Revisar credenciales, health y respuesta de prospectos.

### Contraseña PostgreSQL cambiada

La contraseña queda fijada en el volumen. Para un entorno descartable:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
docker compose --profile app up -d --build
```

## 16. Seguridad

- puertos solo en `127.0.0.1`;
- `.env` fuera de Git;
- credenciales bootstrap locales;
- guardas de envío cerradas;
- XLSX real fuera del repositorio e imágenes;
- contextos Docker sin claves, planillas o cachés;
- smoke solo lectura;
- stack no apto para producción.

## Limitaciones actuales

- falta versionar `package-lock.json`;
- imagen frontend y CI usan `npm install` hasta el lockfile;
- HTTP Basic es temporal;
- SEG-001 requiere build limpio, pruebas, migraciones y smoke verdes.
