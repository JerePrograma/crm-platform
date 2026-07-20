# Evidencia de build contenedorizado — SEG-001

## Fecha

2026-07-20

## Entorno aportado

- sistema operativo: Windows con PowerShell;
- repositorio local: `C:\laburo\crm-platform`;
- Docker: `29.3.1`, build `c2be9cc`;
- modalidad: stack contenedorizado mediante perfil `app`;
- datos operativos reales: no importados;
- comunicaciones: no habilitadas.

## Comandos ejecutados

```powershell
Set-Location C:\laburo\crm-platform
Copy-Item .env.example .env
powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly
docker compose --profile app up -d --build
```

## Preflight

Resultado ejecutado:

```text
PASS
mode: container-only
Docker disponible
DATABASE_URL configurada
usuario bootstrap configurado
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

El primer intento de copiar `.env.example` informó que el archivo no existía. Una repetición posterior en el mismo directorio funcionó. Antes de reintentar debe ejecutarse `git pull --ff-only` y confirmarse la presencia de `.env.example`.

## Descarga y construcción

Evidencia observada:

- `postgres:17-alpine` descargada;
- metadata de Maven, Temurin, Node y Nginx resuelta;
- capas base de backend y frontend descargadas;
- contexto `.dockerignore` aplicado;
- `npm install --no-audit --no-fund` completado;
- 24 paquetes frontend instalados dentro del stage de build;
- Nginx base construida parcialmente;
- runtime backend instaló/verificó `curl`;
- build backend cancelado cuando falló el build frontend.

## Fallo observado

Comando que falló:

```text
npm run build
```

Errores TypeScript:

```text
src/App.tsx: credentials podía ser null en getProspect
src/App.tsx: credentials podía ser null en refresh
src/main.tsx: no existía declaración para import lateral de ./styles.css
```

Consecuencia:

- imagen frontend: `FAIL`;
- imagen backend: `CANCELED`, no evaluada;
- contenedores persistentes de aplicación: no creados;
- Flyway/Hibernate/smoke: no ejecutados.

## Causa raíz

### Credenciales

El componente comprobaba `if (!credentials) return ...`, pero utilizaba la variable de estado anulable dentro de funciones anidadas. TypeScript no conservó el refinamiento en esos cierres.

### CSS

El proyecto no incluía `vite-env.d.ts`, por lo que TypeScript 7 no reconocía el import lateral de `styles.css`.

## Correcciones aplicadas en `main`

### Commit `72f0421caaf4898ce04fd97724ecbad9a4ed6390`

- crea `activeCredentials` después del guard de autenticación;
- usa esa referencia no anulable en consultas, filtros, refrescos y paneles hijos;
- mantiene `strict: true` y no desactiva `strictNullChecks`;
- muestra `excludedRows` como `Bloqueadas` en el resumen de importación.

### Commit `31960db073d6df0cae683b02267a752b9538e08f`

- crea `frontend/src/vite-env.d.ts`;
- referencia los tipos oficiales de Vite;
- declara imports `*.css`.

## Estado después de corregir

```text
frontend source fix: COMMITTED
CSS/Vite declaration: COMMITTED
remote read-back: PASS
frontend build rerun: PENDING
backend build: PENDING
stack health: PENDING
smoke E2E: PENDING
```

## Reejecución requerida

```powershell
Set-Location C:\laburo\crm-platform
git switch main
git pull --ff-only
git rev-parse HEAD

powershell -ExecutionPolicy Bypass -File scripts/preflight.ps1 -ContainerOnly

docker compose --profile app build frontend

docker compose --profile app up -d --build

docker compose --profile app ps
```

Si el build falla nuevamente:

```powershell
docker compose --profile app build frontend --progress=plain
docker compose --profile app build backend --progress=plain
```

Cuando los tres servicios estén saludables:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

## Generación de lockfile sin Node instalado en el host

Después de que el build frontend sea verde, generar `package-lock.json` desde Docker:

```powershell
docker run --rm `
  -v "${PWD}\frontend:/workspace/frontend" `
  -w /workspace/frontend `
  node:22-alpine `
  npm install --no-audit --no-fund
```

Verificar:

```powershell
Test-Path frontend\package-lock.json
git status --short
```

El lockfile debe revisarse y versionarse antes de migrar Docker/CI a `npm ci`.

## Seguridad

Durante todo el intento:

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

No existió envío, importación real, despliegue ni modificación de producción.

## Conclusión

El intento convirtió tres riesgos estáticos en fallos reproducibles y corregidos. No demuestra todavía que el frontend, backend o stack completo sean verdes; la evidencia definitiva depende de la reejecución posterior a ambos commits.