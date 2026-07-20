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

El preflight comprueba Git, Docker, Compose, `.env`, PostgreSQL, credenciales bootstrap y las guardas de envío. No requiere Java, Node ni npm instalados en el host.

## 4. Construir e iniciar el stack

```bash
docker compose --profile app up -d --build
```

Con Make:

```bash
make app-up
```

Servicios:

| Servicio | Contenedor | Puerto host | Función |
|---|---|---:|---|
| PostgreSQL | `postgres` | `127.0.0.1:5432` | fuente de verdad |
| Backend | `backend` | `127.0.0.1:8080` | API, Flyway y seguridad |
| Frontend | `frontend` | `127.0.0.1:5173` | interfaz y proxy hacia backend |

Los servicios se inician en orden:

1. PostgreSQL debe quedar saludable;
2. backend aplica Flyway y debe responder health;
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

Ingresar con los valores de:

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

## 7. Ejecutar smoke test

Linux/macOS con `curl`:

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

El smoke test solo lee health, una página vacía o existente de prospectos y el documento raíz del frontend. No crea datos.

## 8. Flujo funcional

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

## 9. Detener conservando datos

```bash
docker compose --profile app down
```

Con Make:

```bash
make app-down
```

El volumen `crm_postgres` se conserva.

## 10. Eliminar también la base local

```bash
docker compose --profile app down -v
```

Con Make:

```bash
make reset-db
```

Esta operación es destructiva.

## 11. Reconstruir después de cambios

```bash
git pull --ff-only
docker compose --profile app up -d --build
```

Forzar reconstrucción sin caché:

```bash
docker compose --profile app build --no-cache
docker compose --profile app up -d
```

## 12. Diagnóstico

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

El proxy no alcanza al backend. Comprobar:

```bash
docker compose --profile app ps
docker compose logs backend
docker compose logs frontend
```

### Cambio de contraseña PostgreSQL

La contraseña se fija al crear el volumen. Para un entorno local descartable:

```bash
docker compose --profile app down -v
docker compose --profile app up -d --build
```

### Puertos ocupados

Los puertos predeterminados son 5432, 8080 y 5173. Cambiar los mapeos y cualquier URL de host relacionada de manera consistente.

## 13. Seguridad

- todos los puertos se publican solo en `127.0.0.1`;
- `.env` no debe versionarse;
- las credenciales bootstrap son temporales y locales;
- las cuatro guardas de envío deben conservar sus valores seguros;
- no incorporar el XLSX real al repositorio ni a una imagen;
- no utilizar este stack como despliegue de producción.

## Limitación actual

El frontend todavía no posee `package-lock.json`; la imagen utiliza `npm install`. Generar y versionar el lockfile continúa siendo un bloqueante de reproducibilidad para cerrar SEG-001.
