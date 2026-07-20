# SEG-001 — Validación ejecutada y cierre desde `main`

## Estado

Producto, hardening, documentación y automatización están consolidados en `main`.

El cierre depende exclusivamente de ejecutar la matriz en un entorno con red y Docker, corregir fallos reales y registrar evidencia. Los commits del conector continúan sin checks visibles.

## Objetivo del próximo `continuar`

Trabajar solo desde `main`, ejecutar la matriz completa y cerrar SEG-001.

No iniciar identidad/RBAC, campañas, Gmail, Sheets, workers o cloud antes de estabilizar el árbol.

## Fuentes canónicas

```text
branch: main
Docker-only: docs/containerized-quickstart.md
procesos separados: docs/local-development-and-usage.md
automatización: scripts/README.md
estado: docs/status.md
validación: docs/validation/SEG-001.md
backlog: docs/backlog.md
```

## Ya implementado — no repetir sin fallo real

- vertical slice backend/frontend;
- importación, deduplicación, exclusiones y auditoría;
- hardening de parser, preview, recuperación y métricas;
- consolidación en `main`;
- configuración DB coherente;
- perfil Compose `app`;
- perfil Compose `smoke`;
- imágenes backend/frontend;
- health checks y Nginx;
- preflight y smoke multiplataforma;
- Makefile con `smoke-container`;
- CI con arranque y smoke E2E;
- `.dockerignore`/`.gitattributes`;
- documentación completa;
- evidencia estática de YAML, shell y Make.

## Orden obligatorio

1. clonar/actualizar `main`;
2. registrar SHA;
3. crear `.env`;
4. configurar credenciales bootstrap;
5. mantener guardas de envío cerradas;
6. ejecutar preflight;
7. ejecutar Maven/Spotless/tests;
8. confirmar Flyway/Hibernate/Testcontainers;
9. instalar frontend y generar lockfile;
10. ejecutar typecheck/build;
11. migrar a `npm ci` después del lockfile;
12. validar Compose app/smoke;
13. construir ambas imágenes;
14. ejecutar smoke contenedorizado;
15. escanear secretos/datos;
16. corregir fallos con regresión;
17. repetir la matriz;
18. registrar fecha, SHA, comandos y salida;
19. cerrar SEG-001 solo con controles principales en PASS.

## Ruta recomendada — Docker-only

### Linux/macOS

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

cp .env.example .env
# Editar credenciales bootstrap.
# Mantener las cuatro variables SENDING_* cerradas.

sh scripts/preflight.sh --container-only
make smoke-container
```

Sin Make:

```bash
docker compose --profile app --profile smoke config
docker compose --profile app --profile smoke up \
  --build \
  --abort-on-container-exit \
  --exit-code-from smoke \
  smoke
docker compose --profile app --profile smoke down --remove-orphans
```

### Windows PowerShell

```powershell
git clone https://github.com/JerePrograma/crm-platform.git
Set-Location crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

Copy-Item .env.example .env
# Editar credenciales bootstrap.

powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
docker compose --profile app --profile smoke config
docker compose --profile app --profile smoke up --build --abort-on-container-exit --exit-code-from smoke smoke
docker compose --profile app --profile smoke down --remove-orphans
```

## Matriz completa Linux/macOS

```bash
sh scripts/preflight.sh --local
sh ./mvnw -B -f backend/pom.xml verify

cd frontend
npm install
npm run typecheck
npm run build
cd ..

docker compose --profile app --profile smoke config
docker build -t gestudio-crm:seg-001 .
docker build -f frontend/Dockerfile -t gestudio-crm-frontend:seg-001 frontend
make smoke-container
```

## Matriz completa Windows

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
.\mvnw.cmd -B -f backend\pom.xml verify

Push-Location frontend
npm install
npm run typecheck
npm run build
Pop-Location

docker compose --profile app --profile smoke config
docker build -t gestudio-crm:seg-001 .
docker build -f frontend/Dockerfile -t gestudio-crm-frontend:seg-001 frontend
docker compose --profile app --profile smoke up --build --abort-on-container-exit --exit-code-from smoke smoke
docker compose --profile app --profile smoke down --remove-orphans
```

## Lockfile

Después de `npm install`:

```bash
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

Luego:

- reemplazar `npm install` por `npm ci` en CI e imagen frontend;
- habilitar caché npm usando `frontend/package-lock.json`;
- repetir frontend, imágenes y smoke.

## Evidencia a registrar

En `docs/validation/SEG-001.md`:

- fecha y SHA;
- versión de Java, Node, npm, Docker y Compose;
- resultado Maven/Spotless/tests;
- Flyway/Hibernate/Testcontainers;
- frontend install/typecheck/build;
- `docker compose config`;
- builds de ambas imágenes;
- salida de smoke;
- escaneo de datos/secretos;
- correcciones realizadas;
- repetición final verde.

## Correcciones autorizadas

- Java/Spring Boot;
- Spotless/tests;
- JPA/Flyway/Testcontainers;
- TypeScript/Vite/Nginx;
- lockfile/npm ci;
- Dockerfiles/Compose/smoke;
- scripts/CI;
- `excludedRows` en UI;
- documentación de evidencia;
- cualquier fallo demostrado.

## Criterios de cierre

- Maven/Spotless/tests: PASS;
- Flyway/Hibernate/Testcontainers: PASS;
- frontend + lockfile: PASS;
- scripts/preflight: PASS;
- Compose app/smoke: PASS;
- ambas imágenes: PASS;
- smoke E2E: PASS;
- secretos/datos: PASS;
- evidencia registrada;
- SEG-001 COMPLETE;
- SEG-002 ACTIVE.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar XLSX real a Git, CI o imágenes;
- no declarar éxito sin salida ejecutada;
- no iniciar SEG-002 con bloqueantes;
- no tratar Compose local como producción.
