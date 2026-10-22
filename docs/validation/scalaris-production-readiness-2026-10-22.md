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
| Backend, integración y formato | EXECUTED_PASS | 126/126 en cada integral final limpia, sin fallos/errores/omitidos |
| Frontend typecheck/unit/build/audit | EXECUTED_PASS | Vite 8.1.6, Vitest 4.1.12; 21 unitarias y audit cero vulnerabilidades |
| Regresiones de agenda >200 y ficha | EXECUTED_PASS | 2 integración PostgreSQL y 4 E2E de tareas; además, ficha ocupada cubierta con backend real en el recorrido CRM |
| Proxy y configuración productiva | EXECUTED_PASS | 73 aserciones reales nginx/Caddy TLS, cookies, límites, spoofing, logs y sustitución del backend sin reiniciar frontend |
| Backup/restore y destinos Compose | EXECUTED_PASS | 29 aserciones PS5.1, paridad Unix (rutas sin padres/symlink/cleanup propiedad) y restore PostgreSQL17 real aislado |
| E2E Marketing Scalaris/importación grande | EXECUTED_PASS | 7/7 en cada integral Windows PS5.1, cero omitidos/inesperados/flaky, ~1.28MiB accepted2/dryRun, guardas y cleanup |
| Perfil productivo local | EXECUTED_PASS_LOCALLY | 7/7 en cada integral Windows PS5.1; readiness, IDs exactos, guardas, cero enviados y cleanup |
| Recorrido general y Gmail simulado | EXECUTED_PASS | general 8/8 y Gmail canónico 10/10 fases en cada integral; proveedor falso, manual/render y seguridad de evidencia |
| Compatibilidad de limpieza/probe Gmail | EXECUTED_PASS | 23 aserciones PS5.1/PS7, carpeta existente, stderr de versión y fallo real de render |
| Política de release Hostinger | EXECUTED_PASS | 7 regresiones: commit/referencias/IDs verificados, PostgreSQL por digest, puertos privados y cookie segura |
| Migraciones V11/V13/empty a V14 | EXECUTED_PASS | ambas integrales finales de 4b556a5: vacío→V14, V11→V12→V13→V14, V13→V14 y Hibernate validate |
| Grype de las imágenes elegidas | EXECUTED_PASS | mismos IDs del perfil productivo final: high/critical cero, backend medium15/low11 y frontend medium3; DB válida y cero exclusiones |
| Validador integral corridas 1 y 2 | EXECUTED_PASS | 2/2 limpias consecutivas, 23/23 fases cada una, fuente funcional 4b556a5; evidencia detallada debajo |
| Publicación y CI de la fuente funcional | NOT_RUN | pendientes de autorización final: publicar primero 4b556a5 en main y exigir 23 jobs con headSha exacto; cierre documental por separado |
| Producción real | NOT_DEPLOYED | destino elegido; activación y secretos operativos pendientes |

## Pareja final e identidad de los artefactos

La fuente funcional es `4b556a5c6e91e42325ed12c393c8003d7914d48a`. Ambas corridas canónicas usaron Windows PowerShell 5.1, comenzaron y terminaron con el árbol versionado limpio y aprobaron 23/23 fases. Cada una pasó backend 126/126, frontend unitario 21/21, typecheck/build, npm audit cero, migraciones vacío/V11/V13→V14, recuperación real, proxy 73 aserciones, general 8/8, Gmail 10 fases, Scalaris 7/7 y producción local 7/7. No hubo E2E omitidos, inesperados ni flaky; guardas antes/después, cero estados enviados y cleanup propio pasaron.

| Corrida del 2026-10-22 (UTC) | Duración | Resumen preservado | SHA-256 del resumen |
|---|---|---|---|
| 17:14:53–17:32:35 | 1.061,617 s | `4b556a5-run1-preserved/complete-crm-20261022-171453.json` | `1f71b2d61c30e990dc7c72a7d6be712cf8634fce3c08c06e9604f51a1d59d07a` |
| 17:33:32–17:53:18 | 1.185,842 s | `4b556a5-run2-preserved/complete-crm-20261022-173332.json` | `22e134a211cf6bb531e2a95850f8da2fdf26a1a3a8b11feb80a10f020c0556b4` |

Los transcripts homónimos `.log` tienen SHA-256 `940f29af4f2ec3bb06b4ddf569840faf28b75fb7e478c67f0b4b60a28dc2c36c` y `9a11409e132df2ccf3c5376b8393e595d568816eafee25c26c358d84c71d2d9a`. El consolidado `validation-output/canonical-pair-4b556a5-proof-2026-10-22.json`, SHA-256 `dcc0c3a417fbde9c15c41a3f2e413a6a1d41638deae56c964ada312f5e2e6d9c`, conserva los hashes de reportes y manifiestos de ambas corridas.

Después de la pareja se escanearon de forma controlada los IDs exactos del perfil productivo de la segunda corrida. Los scans principales y los productivos construyen imágenes propias: no se deduce igualdad por compartir commit o tag. `validation-output/hostinger-release/artifact-proof.json`, SHA-256 `ab6707b9a8e17637b65e59bff435c4483b89b9dc017378009d4cf90ae4d5ad10`, vincula los seis JSON íntegros: dos reportes Grype, sus dos metadatos y resumen/reporte Playwright productivos. Esta prueba posterior cierra el pendiente de identidad registrado en el consolidado original, que se conserva sin reescribir.

| Imagen productiva elegida | ID exacto | Capas verificadas | Grype controlado |
|---|---|---|---|
| Backend | `sha256:1ebe81a3a98970ae6d6b1044e285193f74e4f8f6844f38c6621bd4761a0ee1a9` | 8 | 0 high/critical, 15 medium, 11 low |
| Frontend | `sha256:0c9a7c74f607e5f8bd2e87cd3b6b7fe47a0e0955aabb79b78ed851bd317d3abe` | 12 | 0 high/critical, 3 medium |

Ambos scans terminaron exit0 con DB Grype válida, `ignore: []`, `match-upstream-kernel-headers: true` y cero matches ignorados. El gate operativo ignorado verifica la cadena OCI índice→manifest→config por bytes, hashes y tamaños, y cada capa comprimida y su `diff_id` descomprimido; así vincula el ID de Docker con la representación normalizada de Grype sin confiar sólo en tags. Sus regresiones aprobaron 10/10 de identidad, 12/12 de artefactos y 11/11 de orquestación. Este ajuste pertenece al empaquetado operativo ignorado y no cambia la fuente funcional ni sustituye la pareja canónica.

El export `images.tar` tiene 174.558.720 bytes y SHA-256 `593e3c204bab8e90447dfc94c22c6826e75668958e03b98888bc90d0bfb74413`: `images-export-proof.json` verifica las mismas 8+12 capas e IDs. El archivo fuente de `4b556a5` conserva SHA-256 `1236673430b7f7c3f78dd96c8f6bdce55adf0b6bb5aeecd36075d02ad68eb513`. El manifiesto aceptado de despliegue sigue pendiente de CI y aprobación humana.

Este cierre documental se registra como descendiente separado, con diff exclusivamente documental. Fuente, archivo fuente, tags y aprobaciones de artefactos conservan el SHA funcional `4b556a5`. Tras la autorización final, publicar primero `4b556a5c6e91e42325ed12c393c8003d7914d48a` en `main` y esperar los 23 jobs CI con ese `headSha` exacto antes de aceptar el bundle. Publicar el descendiente documental por separado: una CI disparada sólo sobre ese descendiente no acredita la CI de `4b556a5`. Publicación/push, CI y autorización de producción siguen pendientes. No se activaron DNS/TLS, secretos, timers ni copia real del VPS; `SCALARIS-DEPLOY-001` no está `COMPLETE`.

## Antecedentes focalizados

Los resultados siguientes documentan el desarrollo previo a la pareja final. Sus pendientes y futuros se refieren a esa etapa; los resultados vigentes son los de las dos secciones anteriores. El PASS productivo Bash previo cubre 5/5 en Git Bash sobre Windows y no acredita una integral Unix nativa.

Las pruebas focalizadas de infraestructura se registran en `validation-output/scalaris-deployment-infrastructure-2026-10-22.json`. El gate Marketing aprobado es `crm-scalaris-scalaris-e2e-1792678333794-57af9ab7.json` con su reporte Playwright. Se ejecutó sobre un working tree en desarrollo, no sustituye las dos corridas finales limpias.

El perfil productivo focalizado aprobado corresponde a `crm-scalaris-production-e2e-1792678865151-e65c69dd.json` y su reporte Playwright (5/5 en 39,9 s; 357,574 s contando primera carga de caché Maven). Las ejecuciones previas fallidas se conservaron: fixture CSV sin canales de contacto, callback de route.fetch pendiente al cerrar y acción intentada mientras seguía visible «Guardando cambios…». Se corrigió el harness esperando estados/callbacks observables, sin cambiar timeouts ni producto para forzar PASS.

La revisión independiente encontró y corrigió tres riesgos: imágenes Hostinger sin inventario exacto, normalización Bash de destinos de respaldo con padres inexistentes y cleanup de drills sin propiedad confirmada. Las regresiones nuevas comprueban rechazos, UUID/PID y conservación de bases ajenas. La política exige commit completo, referencias de release/digest e IDs reales; PostgreSQL Hostinger queda fijado por digest.

El primer análisis npm actualizado encontró Vite 8.1.5 high y Vitest/mocker moderate. Se aplicaron las correcciones de patch Vite 8.1.6 y Vitest 4.1.12; el nuevo audit reporta cero. El primer Grype de nginx actualizado encontró curl 8.22.0-r0 con CVE-2026-92392 high sin fix disponible. Se retiraron curl/libcurl del runtime porque nginx y su healthcheck busybox no los requieren; se exige reconstruir y volver a escanear, sin excepciones.

El scan repetido después de retirarlos terminó con exit0 y high/critical cero (`frontend-grype-final-2026-10-22.json`); el backend actualizado también exit0 (`backend-grype-2026-10-22.json`). La DB Grype validada corresponde al 22 de octubre. Se corrigió la caché a la ruta efectiva de Grype y se fijó explícitamente `GRYPE_DB_CACHE_DIR`; la primera DB fría requirió extracción pesada y no se consideró PASS antes de terminar.

La primera suite de desarrollo se conserva como fallo: 124 casos contabilizados, un fallo y tres errores. Dos eran del nuevo fixture JPA/JDBC (faltaba flush), uno fue un timeout de arranque PostgreSQL durante concurrencia de builds/scans, y el fallo de seguridad ejecutó producción compilada antes del cambio de su test. Después de sincronizar el fixture y recompilar, las tres clases afectadas aprobaron 18/18. Este rerun focalizado no reemplaza las dos corridas integrales del commit limpio.

El build Maven usa caché de dependencias descargadas, no de clases/JAR: `mvn clean package` se ejecuta en cada reconstrucción sin caché de capas. Los gates integrales siguen exigiendo build limpio y pruebas, y el Dockerfile sigue sin sustituir Maven verify.

## Intentos integrales conservados y corrección de la ficha

Los intentos `complete-crm-20261022-143156.json` y `complete-crm-20261022-145115.json` ejecutaron la fuente limpia `a809216e53325fb4bb6ecca813053a3c32fa7ae8`. Ninguno integra una pareja aprobada.

El primero terminó `EXECUTED_FAIL` porque faltaba `.env`, requisito local documentado. Se creó el archivo ignorado desde `.env.example`, sin credenciales operativas ni cambios de fuente, antes de repetir. SHA-256 del resumen: `7fa735ad0516e0e22cf6385b383a0c91fa6874fc37151db46834e8f6c6d7cf4e`.

El segundo duró 1.688,911 s. Pasó backend 126/126, interfaz 21/21, typecheck/build, reconstrucción sin caché de capas, arranque/smoke, npm audit cero, Grype de ambos runtimes sin high/critical y las tres rutas de migración. Falló después en el recorrido general al esperar «Nota registrada.»; los otros dos casos de navegador pasaron. SHA-256 del resumen: `b129322fe1e7ef71b30c8db80f348326c5570019f0322241135a142b865c543e`; transcript: `f6c1541831c35787c16c3daa0e5eec09c9a6c4dc2c4112c4948a3892f5ac7732`.

El trace confirma tres cambios de estado HTTP200 y ninguna solicitud POST de nota. Los cuatro botones de contacto, tarea, actividad y nota seguían activos mientras `run()` descartaba nuevas operaciones por `busyRef`; la actualización anterior aún no terminaba. Se agrega `disabled={busy}` a esos botones y una regresión que retiene la actualización, comprueba bloqueo y conservación del texto, libera la respuesta y exige una sola nota persistida. No se aumentan timeouts ni se habilitan reintentos. La corrección requiere un nuevo commit y reiniciar ambas validaciones integrales; el resultado focal y la pareja definitiva se registrarán al ejecutarse.

El recorrido focal corregido aprobó 6/6 casos generales, responsive, NOOP, paginación y ficha ocupada, sin omitidos ni flaky (`focused-gates-1792683461912-389e6a7d/general-playwright.json`). El fixture inicial de la regresión se conserva como fallo de desarrollo: creaba un prospecto sin localidad/canal que quedaba `NEEDS_DATA`, por lo que no existía la transición elegida; se completaron sus datos sintéticos. No se modificó el CSV histórico: el perfil del recorrido general es `LEGACY`, comprobado en el trace, y su importación pasó. SQL y API confirmaron guardas cerradas y límites efectivos/persistidos cero antes y después.

El siguiente gate focal Gmail pasó NOOP y detectó HTTP502 tras recrear el backend mientras el frontend continuaba en ejecución. El proxy resolvía `backend:8080` sólo al iniciar. Se prepara resolución dinámica mediante el DNS interno de Docker y una regresión con IP nueva y el mismo frontend, antes de repetir Gmail y el perfil productivo. Nginx requiere un upstream con memoria compartida y resolución configurada para seguir esos cambios; esa capacidad está disponible en la versión fijada, según su [documentación oficial](https://nginx.org/en/docs/http/ngx_http_upstream_module.html#server). Este fallo no se convierte en PASS reiniciando el frontend desde la prueba.

El repro negativo confirmó backend `172.21.0.3→172.21.0.5`, acceso directo HTTP200 y proxy HTTP502 con el mismo frontend (`crm-gmail-fake-dns-repro-1792683981549.json`). La imagen corregida `sha256:2940013173d8cb0652f7506962c1fcb483790e64c5821df833951c7fd6a9c00a` aprobó 73 aserciones del proxy con su configuración empaquetada, reservando la IP antigua con un servidor HTTP503. Ambas rutas `/api` y `/actuator` recuperaron HTTP200 dentro de 15 segundos, conservando ID, hora de inicio y contador de reinicios del frontend. Esa regresión queda integrada en los validadores completos Windows y Unix.

La repetición focal detectó además una carrera del test Gmail: tras cerrar sesión volvía a aceptar el panel de la sesión anterior antes de completar el logout. El harness ahora espera «Ingresar» y verifica `/auth/me` contra el usuario solicitado, sin reintentos ni mayores timeouts. El fallo queda conservado en `focused-gates-1792684164440-6fde4882/gmail-logout-diagnosis.json`.

La repetición final `focused-gates-1792684758906-29bbf760/summary.json` aprobó Gmail completo (NOOP 1/1, OAuth/campaña simulada 1/1 en 38,634 s, permisos, revocación, cifrado y comprobaciones de base) y producción Scalaris 6/6 en 28,4 s. El recorrido general 6/6 había pasado sobre la misma imagen frontend, sin cambios posteriores de aplicación. Los IDs exactos backend/frontend, guardas cerradas, cero enviados y cleanup se verificaron; el consolidado `final-focused-release-summary-2026-10-22.json` tiene SHA-256 `6b61606d6cc403fe005ffd435a9a53f66777275d574c297a10b5c304ed6b726e`. Son ejecuciones focalizadas sobre fuente en desarrollo: faltan el scan de la imagen nueva y las dos corridas integrales consecutivas sobre el siguiente commit limpio.

La primera integral limpia de `b04129948a60267f55d66d91d43e96de44d21a21`, `complete-crm-20261022-160534.json`, falló a los 652,341 s al iniciar el wrapper Gmail bajo Windows PowerShell 5.1: `System.IO.Path.GetRelativePath` no existe en .NET Framework y la rama de limpieza se activó al encontrar screenshots anteriores. La prueba focal previa había usado PowerShell 7 y un destino nuevo; no cubría esa combinación. Pasaron backend 126/126, frontend 21/21, typecheck/build, reconstrucción, smoke, npm cero, scans de ambas imágenes sin high/critical, migraciones y recorrido general 3/3. No se ejecutaron los gates posteriores ni se inició una segunda corrida sobre B. Se conserva el resumen con SHA-256 `e09b64a7db3b51c69062a69334954527aa3b34b08a2d243da713639e7f7a73a8` y transcript `0d4c7da08c66670314b40e130fa5bdf93a4091da4199140ab6383d7bf5c8a383` en `b041299-run1-preserved/`. La corrección deberá probar la carpeta existente en PS5.1 y reiniciar la pareja limpia; los puertos elegidos para el stack principal deben diferir de los reservados por Gmail.

La revisión de aislamiento detectó que `COMPOSE_ENV_FILES` heredado seguía activo aunque el runner Scalaris deshabilitara el `.env` por defecto. Un archivo sintético inexistente hizo fallar `compose config --quiet` en ambos perfiles. Se elimina esa variable junto con `COMPOSE_FILE`: la regresión Node aprobó 4/4 y ambos perfiles pasaron la misma reproducción de configuración, sin iniciar servicios (`focused-compose-env-isolation-2026-10-22.json`). Las 29 aserciones de backup con Docker simulado también pasaron bajo PS5.1; no sustituyen el restore real del siguiente gate integral.

El helper de limpieza reemplaza la API ausente por validación de rutas absolutas, descendencia estricta y rechazo de reparse points antes de borrar. Sus 17 aserciones pasaron en PS5.1 y PS7, incluidas carpetas existentes y junctions con un archivo externo preservado. El wrapper real PS5.1 superó esa limpieza, NOOP, Gmail, proveedor y seguridad de base, pero conservó otro fallo en `gmail-live-fake-20261022-162029.json` (SHA-256 `5366de5fbbba403e31f2c064a67d019e8ba5a1c2fba2650583c36603906b9bd0`): `pdftoppm -v` escribe su versión por stderr y `ErrorActionPreference=Stop` la convertía en excepción. El manual HTML/PDF/ZIP y sus 32 capturas se habían generado; falta corregir y probar el probe de versión sin ocultar errores del render real, y volver a ejecutar el wrapper completo.

El probe corregido contiene `ErrorActionPreference=Continue` únicamente durante la consulta de versión, restaura su valor en `finally` y exige exit0; el render real conserva su gate estricto. La regresión ampliada pasó 23/23 en PS5.1 y PS7, incluidos stderr exitoso, exit7 y fallo real del render. El wrapper canónico repetido en Windows PowerShell 5.1, con carpeta de screenshots previa, terminó `FUNCTIONAL_PASS` en sus diez fases (`gmail-live-fake-20261022-162504.json`, SHA-256 `1caacaf865fbec75a43146e26be0414004f5d1445c2774d38b70b262542787f0`): incluye manual/render y seguridad de evidencia. Sintaxis de los 33 scripts y repository safety también pasaron. La regresión entra en la fase Windows del validador completo y en el job CI Windows existente. Esta fuente debe fijarse en un nuevo commit limpio antes de reiniciar las dos corridas integrales.

El intento limpio de `eb2a5ea2c0f8bbaabd0714ea8f3ddb0628a29ee8`, `complete-crm-20261022-162819.json`, duró 893,060 s y terminó `EXECUTED_FAIL` en el recorrido general. Después de configurar la secuencia, «Congelar audiencia» permanecía activo antes de terminar el refresco de la campaña; la confirmación usó una versión anterior y recibió el conflicto de edición. La UI mostró «El registro fue modificado por otra persona» aunque era la propia operación anterior. Backend 126/126, frontend 21/21, build/arranque, scans y migraciones pasaron; Gmail y gates posteriores no se ejecutaron. Resumen SHA-256 `5bf3900d9edafe1371160f48d8307d5e32aee37827115ef14d7492280bf4e97e`; transcript `2adbed739b30203c552c38c3646db7bb0d0573e2d8774567b814cf2137f63ed6`. Se conserva el fallo y se requiere corregir la concurrencia de acciones de campaña con una regresión de refresco retenido antes de reiniciar la pareja.

La revisión sistemática agregó bloqueo síncrono y botones deshabilitados en campañas/configuración hasta terminar cada actualización. Las confirmaciones reservan el bloqueo antes de abrir el diálogo y cancelarlas lo libera y recupera el foco. Configuración conserva la versión recibida aunque falle la recarga y el texto/color de una etiqueta cuyo POST falló. La agenda mantiene un conjunto de tareas en curso: finalizar A no habilita B mientras su POST sigue pendiente.

El foco `focused-gates-1792688860421-b27f864a/summary.json` aprobó general 12/12 en 32,349 s, Gmail canónico PS5.1 en sus diez fases y producción Scalaris 7/7 en 27,0 s. Guardas antes/después, cero enviados y cleanup pasaron. Resumen SHA-256 `8c02778ab876f39f10b1d17057bde60df160787e343140fcb1eefa88bda705e8`; Gmail `gmail-live-fake-20261022-170905.json`, hash `cd16289cacbd633e1c63f1acccd7e1f38293264a4ea22d807fa5c851dd2f7a79`; producción `crm-scalaris-production-e2e-1792689073496-e77055bf.json`, hash `2f421e5fd130cd5d9d2da1ee780c662dd99a49ec11ee59d04c678433f48c8936`. Settings y paginación/concurrencia usan respuestas sintéticas controladas; el recorrido CRM, campaña, ficha ocupada y Marketing usan el backend real aislado. El intento focal anterior 9/12 falló por un selector exacto de label y dos expectativas de errores internos frente al mensaje humanizado; se conserva en `focused-gates-1792688180988-f595f241`, sin cambios de producto, timeouts ni retries para corregir esos fixtures.

General/producción fijaron backend `sha256:b588912e41b2bec3654b7e425f1ef8d2f19caa71b04d75a63b085125fc50747b` y frontend `sha256:b0818ff0db5fc2144c373056306aeeeb6a9d440aa227f6ea740822078d8c1a9c`. Gmail canónico construyó sus propios artefactos: frontend runtime `sha256:b877f1011c2ce0c8b9aefbe7ed34f8e5fb77eacd696565d2ebe243e5e1609909`, backend NOOP `sha256:030d9af996bad49d7a2703effb741af21b3384ea6144370756020d34d1a32d91` y fake `sha256:6592d699b4d5969fcd5042ebf2d01231b3cac26a065bba92432044b1bec0ad66`, comprobados por inspect y no por el tag reconstruido. Esta evidencia es focal sobre fuente modificada; todavía faltan dos corridas limpias y scans de los IDs finales elegidos. La entrega debe vincular por ID exacto scan y E2E productivo, además del commit y la CI.

## Copia cifrada elegida por el usuario

El destino externo será esta PC. El helper está instalado fuera del repositorio en `%LOCALAPPDATA%\ScalarisCRM\hostinger-backup`: AES-256-GCM streaming, metadatos autenticados, verificación MAC/SHA-256 y clave DPAPI CurrentUser con ACL privada. Aprobó 12/12 pruebas sintéticas y 7/7 controles de ACL; recuperar las copias depende de conservar el perfil Windows y `backup-key.dpapi`. No se exportó la clave en texto plano.

La tarea diaria de las 04:15 de Buenos Aires está preparada, sin registrar ni habilitar: usuario actual interactivo, sin elevación, sin solapamiento, ejecución al volver a estar disponible y límite de 30 minutos. Sus siete regresiones pasaron, incluidos fallos SSH, copia alterada y verificación de copias ya existentes. El VPS tendrá el respaldo diario de las 03:15 según el runbook preparado. Todavía no hay copias operativas descargadas ni conexión SSH del helper; instalarlo y probarlo con datos sintéticos no acredita un respaldo externo del VPS.

La preparación PC está vinculada al SHA funcional `4b556a5`; su reporte es `validation-output/offsite-backup/task-preparation/rebind-4b556a5/validation-summary.json`, SHA-256 `1827034f507d1a62dd22b8f3c0679396077da0f964d647d0290b165734354d9a`. El wrapper sigue preparado, sin instalar. La tarea requiere esta PC encendida y el usuario con sesión iniciada; no envía alertas externas. Las copias cifradas protegen ante pérdida del VPS, pero no se acredita recuperación ante pérdida de la PC/perfil. Retención, RPO/RTO y monitoreo independiente siguen pendientes.

## Límites

El rechazo 413 previsto por el [default nginx de 1m](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size) quedó corregido y cubierto por la importación E2E de ~1,28 MiB a través de nginx: accepted2/dryRun en ambos perfiles finales. Se mantienen los límites backend de 10 MB de archivo y 11 MB de request; esta prueba no acredita todos los tamaños límite posibles.

Evidencia local, logs y reportes se guardan en `validation-output/`, ignorado por Git, y se revisan antes de compartir. Las pruebas no usan contactos ni credenciales operativas. Una vulnerabilidad o un scanner bloqueado se registra como fallo o `BLOCKED_EXTERNAL`, nunca como PASS.
