# SEG-001 — Ejecución real, corrección y cierre

## Estado

La implementación fue endurecida mediante revisión estática y pruebas de regresión versionadas. La validación ejecutada continúa bloqueada por el entorno: no hay Maven, Docker, cachés ni acceso de red, y el conector no expone `workflow_dispatch` ni checks visibles.

## Objetivo del próximo `continuar`

Conseguir una ejecución real de la matriz técnica, corregir todos los fallos observados y cerrar `SEG-001`. No añadir módulos comerciales, Gmail, campañas ni RBAC antes de estabilizar el árbol.

## No repetir

Ya fueron revisados y corregidos:

- exclusiones importadas retroactivas y auditadas;
- revisiones ambiguas del preview;
- referencias de duplicados exactos;
- límites multipart y HTTP 413;
- validación de correo y recuperación por fila;
- CSV `,`/`;`, comillas y encabezados duplicados;
- fechas Excel UTC;
- preview con exclusiones;
- métrica `excludedRows`;
- saneamiento de nombre de archivo;
- orden hoja/fila;
- Basic Auth UTF-8.

Reabrir estos puntos solo si una ejecución real demuestra un fallo.

## Orden obligatorio

1. resolver un checkout de la rama en un entorno con red o usar CI;
2. registrar commit exacto antes de ejecutar;
3. ejecutar Maven/Spotless/tests;
4. corregir primero errores de compilación;
5. confirmar Flyway V1–V5, Hibernate y Testcontainers;
6. instalar frontend y generar `package-lock.json`;
7. ejecutar TypeScript/Vite;
8. validar Compose;
9. construir imagen Docker;
10. ejecutar escaneo local de secretos y datos reales;
11. registrar cada comando, salida y fecha en `docs/validation/SEG-001.md`;
12. repetir la matriz después de cada corrección;
13. cerrar `SEG-001` solo si todo queda verde.

## Comandos Linux/macOS

```bash
git fetch origin
git switch feat/seg-001-prospect-vertical-slice
git pull --ff-only

git rev-parse HEAD
sh ./mvnw -B -f backend/pom.xml verify

cd frontend
npm install
npm run typecheck
npm run build
cd ..

docker compose config
docker build -t gestudio-crm:seg-001 .
```

## Comandos Windows PowerShell

```powershell
git fetch origin
git switch feat/seg-001-prospect-vertical-slice
git pull --ff-only

git rev-parse HEAD
.\mvnw.cmd -B -f backend\pom.xml verify

Push-Location frontend
npm install
npm run typecheck
npm run build
Pop-Location

docker compose config
docker build -t gestudio-crm:seg-001 .
```

## Escaneo mínimo

```bash
git grep -n -I -E 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|client_secret|refresh_token|api[_-]?key'
git ls-files | grep -E '\.(xlsx|xls|csv|env|pem|key|p12|pfx)$' || true
git diff --check main...HEAD
```

## Correcciones permitidas

- compilación Java/Spring Boot 4;
- formato Spotless;
- mapeos JPA/Flyway;
- aislamiento/limpieza de tests;
- tipos TypeScript;
- lockfile;
- configuración Vite;
- rutas Docker/Compose;
- fallos de las pruebas de regresión existentes;
- documentación de evidencia.

## Mejoras permitidas después de verde

- prueba HTTP de confirmación de importación y 413;
- mostrar `excludedRows` en UI;
- resolución auditada de DuplicateReview;
- retry explícito de ImportJob fallido;
- frontend containerizado o publicación estática;
- accesibilidad básica.

## Criterios de cierre

- Maven, Spotless y tests: PASS;
- Flyway V1–V5 y Hibernate: PASS;
- Testcontainers: PASS;
- frontend y lockfile: PASS;
- Compose e imagen: PASS;
- secretos/datos reales: PASS;
- resultados documentados;
- `SEG-001` marcado `COMPLETE`;
- `SEG-002` marcado `ACTIVE`;
- nuevo `docs/next-step.md` para identidad, organizaciones y RBAC.

## Restricciones

- no abrir PR;
- no fusionar a `main`;
- no desplegar;
- no habilitar envíos;
- no importar el XLSX real en CI;
- no declarar éxito sin salida ejecutada.
