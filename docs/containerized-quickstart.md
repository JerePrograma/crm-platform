# Inicio rápido completamente contenedorizado

## Objetivo

Esta ruta levanta PostgreSQL, backend y frontend con Docker Compose. En el host solo requiere Git y Docker con Compose v2.

No habilita comunicaciones. El backend continúa sin adaptadores Gmail o SMTP y las cuatro guardas de envío deben permanecer cerradas.

## 1. Obtener el repositorio

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
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

Editar `.env` y definir al menos:

```dotenv
POSTGRES_DB=gestudio_crm
DATABASE_URL=jdbc:postgresql://localhost:5432/gestudio_crm
DATABASE_USER=gestudio
DATABASE_PASSWORD=gestudio_local_only
CRM_BOOTSTRAP_USERNAME=gestudio-admin
CRM_BOOTSTRAP_PASSWORD=una-clave-local-segura
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
PORT=8080
```

Para el contenedor backend, Compose reemplaza internamente el host de `DATABASE_URL` por `postgres`. El valor de `.env` continúa siendo útil para ejecutar el backend directamente desde el host.

## 3. Ejecutar preflight contenedorizado

Linux/macOS:

```bash
sh scripts/preflight.sh --container-only
```

Con Make:

```bash
make preflight-container
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
```

El preflight comprueba Git, Docker, Compose, `.env`, PostgreSQL, credenciales bootstrap y guardas de envío. No requiere Java, Node ni npm en el host.

## 4. Construir e iniciar el stack persistente

```bash
docker compose --profile app up -d --build
```

Con Make:

```bash
make app-up
```

Servicios:

| Servicio | Puerto host | Función |
|---|---:|---|
| PostgreSQL | `127.0.0.1:5432` | fuente de verdad |
| Backend | `127.0.0.1:8080` | API, Flyway y seguridad |
| Frontend | `127.0.0.1:5173` | SPA y proxy hacia backend |

Los servicios se inician en orden:

1. PostgreSQL saludable;
2. backend aplica Flyway y responde health;
3. frontend inicia después del backend saludable.

## 5. Comprobar estado

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

## 6. Abrir el sistema

```text
http://localhost:5173
```

Ingresar con:

```text
CRM_BOOTSTRAP_USERNAME
CRM_BOOTSTRAP_PASSWORD
```

Health directo:

```text
http://localhost:8080/actuator/health
```

Swagger autenticado:

```text
http://localhost:8080/swagger-ui/index.html
```

## 7. Smoke test contra stack activo

Linux/macOS:

```bash
sh scripts/smoke-test.sh
```

Con Make:

```bash
make smoke
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

Comprueba health, API autenticada y documento raíz del frontend. No crea datos.

## 8. Smoke test efímero completamente contenedorizado

Esta modalidad construye el stack, espera health checks, ejecuta un contenedor `smoke` y retira los contenedores al finalizar.

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

Después del comando directo, retirar contenedores conservando el volumen:

```bash
docker compose --profile app --profile smoke down --remove-orphans
```

El servicio `smoke` verifica desde la red interna de Compose:

- health del backend;
- autenticación Basic contra prospectos;
- entrega del frontend mediante Nginx.

Este es también el recorrido preparado para CI.

## 9. Flujo funcional

1. ingresar al dashboard;
2. registrar exclusiones conocidas;
3. preparar CSV o XLSX de hasta 10 MB;
4. ejecutar preview;
5. revisar `EXCLUDED`, `REJECTED`, `DUPLICATE` y `REVIEW_REQUIRED`;
6. corregir el archivo cuando corresponda;
7. ejecutar importación confirmada;
8. revisar prospectos y elegibilidad;
9. revisar auditoría.

La descripción completa está en `docs/local-development-and-usage.md`.

## 10. Detener conservando datos

```bash
docker compose --profile app down
```

Con Make:

```bash
make app-down
```

El volumen `crm_postgres` se conserva.

## 11. Eliminar también la base local

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
```

Con Make:

```bash
make reset-db
```

Esta operación es destructiva.

## 12. Reconstruir después de cambios

```bash
git pull --ff-only
docker compose --profile app up -d --build
```

Forzar reconstrucción sin caché:

```bash
docker compose --profile app build --no-cache
docker compose --profile app up -d
```

## 13. Diagnóstico

### Backend no queda saludable

```bash
docker compose logs backend
```

Revisar:

- autenticación PostgreSQL;
- migraciones Flyway;
- validación Hibernate;
- memoria disponible para Docker.

### Frontend devuelve 502

```bash
docker compose --profile app ps
docker compose logs backend
docker compose logs frontend
```

El frontend Nginx necesita resolver `backend:8080` dentro de la red Compose.

### Smoke falla

```bash
docker compose --profile app --profile smoke logs --no-color
```

Revisar:

- credenciales bootstrap no vacías;
- backend y frontend saludables;
- respuesta de `/api/v1/prospects`;
- documento raíz del frontend.

### Cambio de contraseña PostgreSQL

La contraseña se fija al crear el volumen. Para un entorno descartable:

```bash
docker compose --profile app --profile smoke down -v --remove-orphans
docker compose --profile app up -d --build
```

### Puertos ocupados

Los puertos predeterminados son 5432, 8080 y 5173. Cambiar mapeos y URLs relacionadas de manera consistente.

## 14. Seguridad

- todos los puertos se publican solo en `127.0.0.1`;
- `.env` no debe versionarse;
- las credenciales bootstrap son temporales y locales;
- las cuatro guardas de envío deben conservar valores seguros;
- no incorporar XLSX real al repositorio o imágenes;
- `.dockerignore` excluye secretos, planillas, claves y cachés;
- el servicio smoke solo realiza lecturas;
- no utilizar este stack como despliegue de producción.

## Limitación actual

El frontend todavía no posee `package-lock.json`; la imagen utiliza `npm install`. Generar y versionar el lockfile continúa siendo un bloqueante de reproducibilidad para cerrar SEG-001.
