# SEG-001 — Validación ejecutada y cierre desde `main`

## Estado

Todo el código y la documentación están consolidados en `main`. El hardening estático y la documentación de arranque están completos.

La única brecha bloqueante es obtener una ejecución técnica real y corregir cualquier fallo observado. Los commits del conector no muestran checks visibles y el entorno actual no dispone de Maven, Docker, cachés ni acceso a registros externos.

## Objetivo del próximo `continuar`

Trabajar exclusivamente desde `main`, ejecutar la matriz completa, corregir todos los fallos reales y cerrar `SEG-001`.

No iniciar identidad/RBAC, campañas, Gmail, Sheets o infraestructura cloud antes de estabilizar el árbol.

## Fuente canónica

```text
branch: main
startup: docs/local-development-and-usage.md
status: docs/status.md
validation: docs/validation/SEG-001.md
backlog: docs/backlog.md
```

## No repetir sin evidencia de fallo

Ya están implementados y revisados:

- exclusiones importadas retroactivas y auditadas;
- revisiones ambiguas persistidas en preview;
- referencias de duplicados exactos;
- límites multipart y HTTP 413;
- validación de correo y recuperación por fila;
- CSV con coma o punto y coma;
- comillas y encabezados duplicados;
- fechas Excel UTC;
- preview con exclusiones;
- métrica `excludedRows`;
- saneamiento del nombre de archivo;
- orden por hoja y fila;
- Basic Auth UTF-8;
- configuración local compartida entre Compose y backend;
- guía de arranque y uso.

Reabrir estos puntos solo cuando una ejecución real demuestre un defecto.

## Orden obligatorio

1. clonar o actualizar `main` en un entorno con red;
2. registrar el commit exacto;
3. copiar `.env.example` a `.env` y mantener envíos bloqueados;
4. ejecutar `docker compose config`;
5. levantar PostgreSQL;
6. ejecutar Maven, Spotless y todas las pruebas;
7. corregir primero errores de compilación;
8. confirmar Flyway V1–V5, Hibernate y Testcontainers;
9. instalar dependencias frontend;
10. generar y versionar `package-lock.json`;
11. ejecutar typecheck y build;
12. construir la imagen Docker;
13. realizar escaneo local de secretos y datos reales;
14. registrar comandos, fecha, commit y salida en `docs/validation/SEG-001.md`;
15. repetir toda la matriz después de cada corrección;
16. cerrar SEG-001 únicamente cuando todos los controles principales estén en `PASS`.

## Preparación Linux/macOS

```bash
git clone https://github.com/JerePrograma/crm-platform.git
cd crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

cp .env.example .env
# Editar .env y cambiar CRM_BOOTSTRAP_PASSWORD.
# Mantener todas las variables SENDING_* cerradas.

set -a
. ./.env
set +a
```

## Preparación Windows PowerShell

```powershell
git clone https://github.com/JerePrograma/crm-platform.git
Set-Location crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

Copy-Item .env.example .env
# Editar .env y cambiar CRM_BOOTSTRAP_PASSWORD.

Get-Content .env | ForEach-Object {
  if ($_ -match '^(?<name>[^#][^=]*)=(?<value>.*)$') {
    [Environment]::SetEnvironmentVariable($matches.name.Trim(), $matches.value, 'Process')
  }
}
```

## Matriz Linux/macOS

```bash
docker compose config
docker compose up -d postgres
docker compose ps

sh ./mvnw -B -f backend/pom.xml verify

cd frontend
npm install
npm run typecheck
npm run build
cd ..

docker build -t gestudio-crm:seg-001 .
```

## Matriz Windows PowerShell

```powershell
docker compose config
docker compose up -d postgres
docker compose ps

.\mvnw.cmd -B -f backend\pom.xml verify

Push-Location frontend
npm install
npm run typecheck
npm run build
Pop-Location

docker build -t gestudio-crm:seg-001 .
```

## Comprobación manual mínima

Backend:

```bash
sh ./mvnw -f backend/pom.xml spring-boot:run
```

En otra terminal:

```bash
curl http://localhost:8080/actuator/health
curl -u "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  "http://localhost:8080/api/v1/prospects?size=5"
```

Frontend:

```bash
cd frontend
npm run dev
```

Abrir `http://localhost:5173`, ingresar y completar el flujo descrito en `docs/local-development-and-usage.md`.

## Escaneo mínimo

```bash
git grep -n -I -E 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|client_secret|refresh_token|api[_-]?key'
git ls-files | grep -E '\.(xlsx|xls|csv|env|pem|key|p12|pfx)$' || true
git diff --check
git status --short
```

El resultado esperado no debe incluir `.env`, archivos reales de prospectos, claves ni tokens.

## Correcciones autorizadas dentro de SEG-001

- compilación Java/Spring Boot;
- formato Spotless;
- mapeos JPA/Flyway;
- tests y limpieza de datos;
- compatibilidad Testcontainers;
- tipos TypeScript;
- lockfile;
- configuración Vite;
- Docker y Compose;
- representación de `excludedRows`;
- documentación de evidencia;
- fallos demostrados por la matriz.

## Mejoras permitidas después de verde

- mostrar `excludedRows` como tarjeta independiente;
- pruebas HTTP para confirmación y 413;
- resolución auditada de `DuplicateReview`;
- retry explícito de `ImportJob` fallido;
- accesibilidad básica;
- estrategia de publicación del frontend.

## Criterios de cierre

- Maven y Spotless: `PASS`;
- unit tests: `PASS`;
- Testcontainers: `PASS`;
- Flyway V1–V5: `PASS`;
- Hibernate validate: `PASS`;
- frontend, typecheck y lockfile: `PASS`;
- Compose: `PASS`;
- imagen Docker: `PASS`;
- escaneo de secretos/datos: `PASS`;
- arranque y smoke test: `PASS`;
- resultados documentados con commit y fecha;
- `SEG-001` marcado `COMPLETE`;
- `SEG-002` marcado `ACTIVE`;
- `docs/next-step.md` reemplazado por el plan de identidad, organizaciones y RBAC.

## Restricciones

- no desplegar producción;
- no habilitar envíos;
- no incorporar el XLSX real al repositorio o CI;
- no declarar éxito sin salida ejecutada;
- no iniciar SEG-002 con controles bloqueantes pendientes.
