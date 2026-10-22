# Preparación productiva Scalaris — 2026-10-22

## Procedencia

- Base auditada: `2a22e4bf3227a87d120c22bdfc8aa05615a40a6d`, `main`, sin diferencias con `origin/main` al inicio.
- Última CI remota observada de esa base: [38602265455](https://github.com/JerePrograma/crm-platform/actions/runs/38602265455), 21/22 jobs success, fallo real de [Grype](https://github.com/JerePrograma/crm-platform/actions/runs/38602265455/job/115984193896). Ese fallo se conserva; una reparación local no cambia su resultado histórico.
- Docker Desktop se inició durante esta misión: cliente/motor Linux 29.8.2, API 1.56. El bloqueo histórico «Docker no instalado» dejó de describir este host.

## Dependencias y runtime

Spring Boot `4.1.2` se verificó en [Maven Central](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom). Su BOM resuelve Spring Framework `7.0.10`, Tomcat `11.0.27`, Jackson 3 `3.1.7`, Jackson 2 `2.21.7` y PostgreSQL JDBC `42.7.14`. Se retiraron los pins antiguos de PostgreSQL/Jackson y el BOM Jackson 2 duplicado.

El tag público `cgr.dev/chainguard/jre:latest` resuelve actualmente a Java 27.0.1 y los tags Java 21 probados no son accesibles. Para conservar Java 21 se usa la imagen oficial [Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin), fijada por índice `sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088`, Java `21.0.12.1`, Alpine 3.24. El contenedor continúa ejecutándose como UID/GID `65532`.

## Matriz de esta misión

| Control | Estado | Alcance |
|---|---|---|
| Sincronización Git inicial | EXECUTED_PASS | main y origin/main coincidentes; árbol limpio |
| Repository safety inicial | EXECUTED_PASS | git diff --check y rutas privadas |
| Sintaxis PowerShell inicial | EXECUTED_PASS | 28 scripts de la base auditada |
| Resolución del BOM actualizado | EXECUTED_PASS | dependency:tree; evidencia local en validation-output |
| Backend focalizado, integración y formato | EXECUTED_PASS | rerun de las tres clases afectadas aprobó 18/18; falta gate integral final limpio |
| Frontend typecheck/unit/build/audit | EXECUTED_PASS | Vite 8.1.6, Vitest 4.1.12; 21 unitarias y audit cero vulnerabilidades |
| Regresiones de agenda >200 y ficha | EXECUTED_PASS | 2 integración PostgreSQL y 2 recorridos de navegador; incluye respuestas tardías y retorno tras completar última tarea |
| Proxy y configuración productiva | EXECUTED_PASS | 57 aserciones reales nginx/Caddy TLS, login/cookies, límites, spoofing, privacidad de logs y Compose Hostinger |
| Backup/restore y destinos Compose | EXECUTED_PASS | 29 aserciones PS5.1, paridad Unix (rutas sin padres/symlink/cleanup propiedad) y restore PostgreSQL17 real aislado |
| E2E Marketing Scalaris/importación grande | EXECUTED_PASS | wrapper Windows PS5.1, 5/5 sin omitidos/flaky, ~1.28MiB accepted2/dryRun, guardas y cleanup |
| Perfil productivo local por Bash | EXECUTED_PASS | 5/5, readiness/runtime/guardas/cero enviados/cleanup; Git Bash en Windows, no host Unix nativo |
| Política de release Hostinger | EXECUTED_PASS | 7 regresiones: commit/referencias/IDs verificados, PostgreSQL por digest, puertos privados y cookie segura |
| Migraciones V11/V13/empty a V14 | NOT_RUN | pendiente |
| Grype backend/frontend | EXECUTED_PASS | ambos high/critical cero; backend medium15/low11 y frontend medium3; falta confirmar las imágenes de las corridas finales |
| Validador integral corrida 1/2 | NOT_RUN | pendiente de commit limpio definitivo |
| CI del código modificado | NOT_RUN | no publicado |
| Producción real | NOT_DEPLOYED | destino elegido; activación y secretos operativos pendientes |

Las pruebas focalizadas de infraestructura se registran en `validation-output/scalaris-deployment-infrastructure-2026-10-22.json`. El gate Marketing aprobado es `crm-scalaris-scalaris-e2e-1792678333794-57af9ab7.json` con su reporte Playwright. Se ejecutó sobre un working tree en desarrollo, no sustituye las dos corridas finales limpias.

El perfil productivo focalizado aprobado corresponde a `crm-scalaris-production-e2e-1792678865151-e65c69dd.json` y su reporte Playwright (5/5 en 39,9 s; 357,574 s contando primera carga de caché Maven). Las ejecuciones previas fallidas se conservaron: fixture CSV sin canales de contacto, callback de route.fetch pendiente al cerrar y acción intentada mientras seguía visible «Guardando cambios…». Se corrigió el harness esperando estados/callbacks observables, sin cambiar timeouts ni producto para forzar PASS.

La revisión independiente encontró y corrigió tres riesgos: imágenes Hostinger sin inventario exacto, normalización Bash de destinos de respaldo con padres inexistentes y cleanup de drills sin propiedad confirmada. Las regresiones nuevas comprueban rechazos, UUID/PID y conservación de bases ajenas. La política exige commit completo, referencias de release/digest e IDs reales; PostgreSQL Hostinger queda fijado por digest.

El primer análisis npm actualizado encontró Vite 8.1.5 high y Vitest/mocker moderate. Se aplicaron las correcciones de patch Vite 8.1.6 y Vitest 4.1.12; el nuevo audit reporta cero. El primer Grype de nginx actualizado encontró curl 8.22.0-r0 con CVE-2026-92392 high sin fix disponible. Se retiraron curl/libcurl del runtime porque nginx y su healthcheck busybox no los requieren; se exige reconstruir y volver a escanear, sin excepciones.

El scan repetido después de retirarlos terminó con exit0 y high/critical cero (`frontend-grype-final-2026-10-22.json`); el backend actualizado también exit0 (`backend-grype-2026-10-22.json`). La DB Grype validada corresponde al 22 de octubre. Se corrigió la caché a la ruta efectiva de Grype y se fijó explícitamente `GRYPE_DB_CACHE_DIR`; la primera DB fría requirió extracción pesada y no se consideró PASS antes de terminar.

La primera suite de desarrollo se conserva como fallo: 124 casos contabilizados, un fallo y tres errores. Dos eran del nuevo fixture JPA/JDBC (faltaba flush), uno fue un timeout de arranque PostgreSQL durante concurrencia de builds/scans, y el fallo de seguridad ejecutó producción compilada antes del cambio de su test. Después de sincronizar el fixture y recompilar, las tres clases afectadas aprobaron 18/18. Este rerun focalizado no reemplaza las dos corridas integrales del commit limpio.

El build Maven usa caché de dependencias descargadas, no de clases/JAR: `mvn clean package` se ejecuta en cada reconstrucción sin caché de capas. Los gates integrales siguen exigiendo build limpio y pruebas, y el Dockerfile sigue sin sustituir Maven verify.

## Límites

La inspección estática del proxy predijo el rechazo 413 para archivos mayores de 1 MiB: el [default nginx es 1m](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size), frente a 10 MB de archivo y 11 MB de request en backend. La corrección requiere una ejecución E2E a través de nginx; leer el archivo no cuenta como PASS.

Evidencia local, logs y reportes se guardan en `validation-output/`, ignorado por Git, y se revisan antes de compartir. Las pruebas no usan contactos ni credenciales operativas. Una vulnerabilidad o un scanner bloqueado se registra como fallo o `BLOCKED_EXTERNAL`, nunca como PASS.
