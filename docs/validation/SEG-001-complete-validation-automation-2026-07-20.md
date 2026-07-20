# Automatización de validación completa — SEG-001

## Fecha

2026-07-20

## Estado

`IMPLEMENTED — STATIC_CHECKS_PARTIAL_PASS — REAL_EXECUTION_PENDING`

Este documento registra la automatización añadida para convertir los controles separados de SEG-001 en un recorrido local reproducible.

No constituye evidencia de ejecución verde. Los scripts todavía deben ejecutarse en Windows/Docker y sus resultados deben incorporarse a `docs/validation/SEG-001.md`.

## Objetivo

Cerrar las siguientes brechas operativas:

1. Maven verify dependía de Java instalado en el host;
2. Docker, Maven/Testcontainers, lockfile y npm ci se ejecutaban por separado;
3. la evidencia era principalmente texto libre;
4. el escaneo de archivos prohibidos estaba duplicado;
5. el generador de lockfile ejecutaba una instalación completa;
6. no existía una comprobación final de cambios inesperados en el checkout.

## Archivos añadidos

```text
scripts/verify-backend-container.ps1
scripts/verify-backend-container.sh
scripts/validate-seg001.ps1
scripts/check-repository-safety.ps1
scripts/check-repository-safety.sh
```

## Archivos modificados

```text
scripts/validate-docker-stack.ps1
scripts/generate-frontend-lock.ps1
scripts/generate-frontend-lock.sh
.github/workflows/ci.yml
Makefile
```

## Verificación backend contenedorizada

### Comandos

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-backend-container.ps1
```

Unix:

```bash
sh scripts/verify-backend-container.sh
```

### Comportamiento

- usa `maven:3.9.16-eclipse-temurin-21`;
- ejecuta `mvn -B -f backend/pom.xml verify`;
- monta el repositorio en solo lectura;
- monta un volumen efímero en `backend/target`;
- conserva una caché Maven en `crm_maven_cache`;
- monta `/var/run/docker.sock` para Testcontainers;
- configura `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`;
- elimina contenedor y volumen target al finalizar;
- no modifica fuentes ni realiza commits.

### Cobertura esperada

- compilación Java;
- Maven Enforcer;
- Spotless;
- unit tests;
- ArchUnit;
- Testcontainers PostgreSQL;
- pruebas de Flyway/Hibernate incorporadas a la suite.

### Consideración de seguridad

Montar el socket Docker concede al contenedor capacidad elevada sobre el daemon local. Ejecutar este control únicamente sobre código propio y revisado desde `main`. No ejecutar sobre ramas o dependencias no confiables.

## Generación segura de package-lock

Los scripts de lockfile ahora ejecutan:

```text
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
```

Garantías:

- genera o actualiza solamente `frontend/package-lock.json`;
- no ejecuta scripts lifecycle de paquetes;
- no debe crear `frontend/node_modules`;
- falla si `node_modules` aparece inesperadamente;
- no realiza commits.

## Validador Docker estructurado

Archivo:

```text
scripts/validate-docker-stack.ps1
```

Nuevas capacidades:

- parámetro `-NoTranscript` para composición desde otro orquestador;
- evidencia JSON además del transcript;
- commit y versiones Docker/Compose;
- puertos usados;
- distinción build limpio/cacheado;
- health por servicio;
- estado de smoke host y contenedor;
- error capturado sin incluir credenciales;
- cleanup final configurable.

Archivos generados:

```text
validation-output/seg001-docker-YYYYMMDD-HHMMSS.log
validation-output/seg001-docker-YYYYMMDD-HHMMSS.json
```

## Validador integral

Archivo:

```text
scripts/validate-seg001.ps1
```

### Requisitos

- Windows PowerShell;
- Git;
- Docker Desktop con contenedores Linux;
- Compose v2;
- rama `main`;
- archivos rastreados sin modificaciones;
- `.env` local con credenciales bootstrap y guardas cerradas.

No requiere Java, Maven, Node o npm instalados en el host.

### Recorrido

1. registra commit y rama;
2. exige `main`;
3. exige árbol rastreado limpio;
4. ejecuta `validate-docker-stack.ps1` con builds limpios;
5. confirma Compose, imágenes, health y smoke inicial;
6. ejecuta Maven verify/Testcontainers dentro de Docker;
7. genera `frontend/package-lock.json` sin lifecycle scripts;
8. calcula SHA-256 del lockfile;
9. reconstruye frontend desde cero;
10. el Dockerfile detecta el lockfile y ejecuta `npm ci`;
11. recrea y espera health del frontend;
12. repite smoke host y contenedor;
13. ejecuta el escaneo centralizado del repositorio;
14. permite únicamente el cambio esperado de package-lock;
15. escribe transcript y resumen JSON;
16. conserva o retira el stack según `-KeepRunning`.

### Comando canónico

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 8080 `
  -FrontendPort 5173 `
  -KeepRunning
```

Puertos alternativos:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-seg001.ps1 `
  -PostgresPort 55432 `
  -BackendPort 18080 `
  -FrontendPort 15173 `
  -KeepRunning
```

No usar `-UseBuildCache` como evidencia de cierre.

### Evidencia generada

```text
validation-output/seg001-complete-YYYYMMDD-HHMMSS.log
validation-output/seg001-complete-YYYYMMDD-HHMMSS.json
```

Resumen JSON:

- commit y rama;
- timestamps UTC;
- puertos;
- estado de árbol limpio;
- Docker stack;
- Maven verify;
- lockfile y SHA-256;
- build npm ci;
- smoke final;
- seguridad del repositorio;
- estado final y error;
- indicador de stack conservado.

## Escaneo centralizado de seguridad

Archivos:

```text
scripts/check-repository-safety.ps1
scripts/check-repository-safety.sh
```

Bloquean archivos rastreados como:

- `.env` en cualquier directorio;
- `validation-output/`;
- datos privados de importación salvo su README;
- datos privados de exportación;
- `gestudio_lote_*_prospectos.xlsx` en cualquier directorio;
- claves y certificados `.pem`, `.key`, `.p12`, `.pfx`, `.jks`;
- JSON con nombres de credenciales, service account o client secret.

También ejecutan `git diff --check`.

Este control no sustituye un escáner de secretos por contenido; reduce errores evidentes por ruta y extensión.

## Makefile

Targets añadidos:

```text
repository-safety
backend-verify-container
verify-container
```

`verify-container` ejecuta:

1. preflight container-only;
2. Maven verify/Testcontainers contenedorizado;
3. generación segura del lockfile;
4. Compose config;
5. build limpio frontend con npm ci;
6. build limpio backend;
7. smoke contenedorizado;
8. escaneo de seguridad.

## CI

El job `scripts` ahora valida:

- sintaxis de todos los scripts shell;
- parser de todos los scripts PowerShell;
- escaneo centralizado de seguridad;
- preflight fail-closed.

El backend continúa ejecutando Maven verify directamente en el runner. El job E2E continúa validando imágenes, stack y smoke.

## Controles estáticos ejecutados

Entorno disponible sin red, Docker ni PowerShell:

```text
verify-backend-container.sh: PASS_SYNTAX
generate-frontend-lock.sh: PASS_SYNTAX
Make backend-verify-container: PASS_PARSE
Make verify-container: PASS_PARSE
CI YAML: PASS_PARSE
jobs CI esperados: PASS_STRUCTURE
control de seguridad CI: PASS_STRUCTURE
```

La primera prueba de Make se lanzó fuera de la raíz temporal y la invocación recursiva no encontró el Makefile. Repetida desde la raíz correcta, todos los targets expandieron correctamente. Esto fue un error del harness estático, no del Makefile versionado.

## Controles pendientes

- parser PowerShell real de los scripts nuevos;
- ejecución de `check-repository-safety.ps1`;
- ejecución de `validate-docker-stack.ps1` actualizado;
- ejecución de `verify-backend-container.ps1`;
- ejecución de `validate-seg001.ps1`;
- acceso Testcontainers mediante Docker socket;
- Maven verify real;
- generación real del lockfile;
- build real con npm ci;
- resumen JSON real;
- revisión del transcript;
- commit del lockfile;
- CI visible verde.

## Reglas de evidencia

- una revisión de código no es una ejecución;
- un script parseado no es un PASS funcional;
- un build cacheado no es un build limpio;
- un JSON con estado PASS debe corresponder a un proceso con código de salida cero;
- el transcript debe revisarse antes de compartirlo porque puede contener logs de aplicación;
- no versionar `validation-output/`;
- resumir resultados relevantes en `docs/validation/SEG-001.md`;
- no marcar SEG-001 COMPLETE hasta que la matriz principal esté verde.

## Seguridad preservada

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
```

Ningún script despliega, importa el XLSX real, envía mensajes o modifica el kill switch persistente.
