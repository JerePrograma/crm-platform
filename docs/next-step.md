# SEG-001 — Validación ejecutada y cierre desde `main`

## Estado

Todo el código, hardening, documentación y automatización local están consolidados en `main`.

La única brecha bloqueante es ejecutar la matriz técnica en un entorno con red y Docker, corregir fallos reales y registrar evidencia. Los commits del conector continúan sin checks o runs visibles.

## Objetivo del próximo `continuar`

Trabajar exclusivamente desde `main`, ejecutar la matriz completa, corregir cualquier fallo real y cerrar `SEG-001`.

No iniciar identidad/RBAC, campañas, Gmail, Sheets, workers o infraestructura cloud antes de estabilizar el árbol.

## Fuente canónica

```text
branch: main
container quickstart: docs/containerized-quickstart.md
local tools: docs/local-development-and-usage.md
automation: scripts/README.md
status: docs/status.md
validation: docs/validation/SEG-001.md
backlog: docs/backlog.md
```

## No repetir sin evidencia de fallo

Ya están implementados y revisados:

- dominio, API y frontend del vertical slice;
- exclusiones importadas retroactivas y auditadas;
- revisiones ambiguas en preview;
- referencias de duplicados exactos;
- límites multipart y HTTP 413;
- validación de correo y recuperación por fila;
- CSV con coma o punto y coma, comillas y encabezados duplicados;
- fechas Excel UTC;
- preview con exclusiones;
- métrica `excludedRows`;
- saneamiento de archivos;
- Basic Auth UTF-8;
- configuración DB compartida;
- consolidación en `main`;
- perfil Compose `app` con los tres servicios;
- imágenes backend y frontend;
- preflight y smoke tests multiplataforma;
- Makefile;
- CI ampliado;
- contextos Docker minimizados;
- guías de arranque y uso.

Reabrir estos puntos solo si una ejecución real demuestra un defecto.

## Orden obligatorio

1. clonar o actualizar `main` en un entorno con red y Docker;
2. registrar el SHA exacto;
3. copiar `.env.example` a `.env`;
4. configurar credenciales bootstrap locales;
5. verificar que las cuatro variables de envío siguen cerradas;
6. ejecutar preflight;
7. ejecutar Maven, Spotless y pruebas;
8. confirmar Flyway V1–V5, Hibernate y Testcontainers;
9. instalar dependencias frontend;
10. generar y versionar `package-lock.json`;
11. ejecutar typecheck y build;
12. validar perfil Compose completo;
13. construir imágenes backend y frontend;
14. levantar el stack completo;
15. ejecutar smoke test;
16. realizar escaneo local de secretos y datos reales;
17. corregir cada fallo con prueba de regresión cuando corresponda;
18. repetir toda la matriz;
19. registrar fecha, SHA, comandos, jobs y resultados;
20. cerrar SEG-001 solo cuando los controles principales estén en `PASS`.

## Ruta rápida contenedorizada

### Linux/macOS

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

cp .env.example .env
# Editar .env: establecer credenciales bootstrap.
# Mantener SENDING_ENABLED=false, SENDING_DRY_RUN=true,
# SENDING_DAILY_LIMIT=0 y SENDING_KILL_SWITCH=true.

sh scripts/preflight.sh --container-only
docker compose --profile app config
docker compose --profile app up -d --build
docker compose --profile app ps
sh scripts/smoke-test.sh
```

### Windows PowerShell

```powershell
git clone https://github.com/JerePrograma/crm-platform.git
Set-Location crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

Copy-Item .env.example .env
# Editar .env y establecer credenciales bootstrap.

powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
docker compose --profile app config
docker compose --profile app up -d --build
docker compose --profile app ps
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
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

docker compose --profile app config
docker build -t gestudio-crm:seg-001 .
docker build -f frontend/Dockerfile -t gestudio-crm-frontend:seg-001 frontend
docker compose --profile app up -d --build
sh scripts/smoke-test.sh
```

Con Make:

```bash
make preflight
make verify
make app-up
make smoke
```

## Matriz completa Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1
.\mvnw.cmd -B -f backend\pom.xml verify

Push-Location frontend
npm install
npm run typecheck
npm run build
Pop-Location

docker compose --profile app config
docker build -t gestudio-crm:seg-001 .
docker build -f frontend/Dockerfile -t gestudio-crm-frontend:seg-001 frontend
docker compose --profile app up -d --build
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

## Lockfile frontend

Después de `npm install`:

```bash
git status --short frontend/package-lock.json
git add frontend/package-lock.json
git commit -m "build: lock frontend dependencies"
git push origin main
```

Después de versionarlo, cambiar CI e imagen frontend de `npm install` a `npm ci` y habilitar caché npm con el lockfile.

## Inspección del stack

```bash
docker compose --profile app ps
docker compose --profile app logs backend
docker compose --profile app logs frontend
docker compose --profile app logs postgres
curl http://localhost:8080/actuator/health
```

UI:

```text
http://localhost:5173
```

## Escaneo mínimo

```bash
git diff --check
git status --short
git ls-files | grep -E '\.(xlsx|xls|csv|env|pem|key|p12|pfx)$' || true
```

Revisar además patrones de secretos con una herramienta local adecuada. El resultado no debe incluir `.env`, lotes reales, claves ni tokens.

## Correcciones autorizadas dentro de SEG-001

- compilación Java/Spring Boot;
- formato Spotless;
- mapeos JPA/Flyway;
- tests y aislamiento de datos;
- compatibilidad Testcontainers;
- tipos TypeScript;
- lockfile y `npm ci`;
- configuración Vite/Nginx;
- Dockerfiles y Compose;
- scripts y CI;
- representación de `excludedRows`;
- documentación de evidencia;
- cualquier fallo demostrado por la matriz.

## Mejoras permitidas después de verde

- mostrar `excludedRows` como tarjeta independiente;
- pruebas HTTP para confirmación y 413;
- resolución auditada de `DuplicateReview`;
- retry explícito de trabajos fallidos;
- accesibilidad básica;
- identidad persistente y RBAC en SEG-002.

## Criterios de cierre

- Maven y Spotless: `PASS`;
- unit tests y ArchUnit: `PASS`;
- Testcontainers: `PASS`;
- Flyway V1–V5: `PASS`;
- Hibernate validate: `PASS`;
- frontend, typecheck y lockfile: `PASS`;
- scripts/preflight: `PASS`;
- Compose completo: `PASS`;
- imágenes backend/frontend: `PASS`;
- stack y smoke test: `PASS`;
- escaneo de secretos/datos: `PASS`;
- resultados documentados con SHA y fecha;
- `SEG-001` marcado `COMPLETE`;
- `SEG-002` marcado `ACTIVE`;
- `docs/next-step.md` reemplazado por identidad, organizaciones y RBAC.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real al repositorio, CI o imágenes;
- no declarar éxito sin salida ejecutada;
- no iniciar SEG-002 con controles bloqueantes pendientes;
- no utilizar el stack Compose local como despliegue productivo.
