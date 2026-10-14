# SCALARIS-001 — publicación Git autorizada

Fecha: 14 de octubre de 2026, Argentina.

## Autorización y alcance

El titular solicitó explícitamente: «Sube todo a github por favor, add, commit, diff y push».

Esta autorización habilita revisar y versionar el trabajo de Scalaris CRM y publicarlo en `origin/main`. No habilita despliegue productivo, comunicaciones reales, datos operativos, credenciales ni reescritura de historia.

```text
repositorio: https://github.com/JerePrograma/crm-platform
rama: main
base local y remota al iniciar: 75f37a023a0e69d991fe7640f6ed24df98d6ca18
divergencia inicial HEAD...origin/main: 0 0
publicación: PUBLISHED_FAST_FORWARD
commit de implementación: 331b3e7be5ad52b93b285a0ff9654abedce79b7d
diff de implementación: 64 archivos, 4126 inserciones, 252 eliminaciones
HEAD = origin/main = ls-remote refs/heads/main: 331b3e7be5ad52b93b285a0ff9654abedce79b7d
divergencia después del push: 0 0
working tree después del push, antes del cierre documental: CLEAN
CI del commit de implementación: FAILED_DEPENDENCY_SCAN
CI run: 38600419532
resultado CI final: completed/failure, 21/22 jobs success, solo dependency-scan failure
documentación: POST_PUSH_RESULTS_RECORDED
```

## Contenido revisado

- Perfil y organización Scalaris, usuarios/roles, catálogo, etiquetas y plantillas.
- Segmentación de campañas por etiqueta propia activa y métricas globales del negocio.
- Interfaz, agenda, seguimientos, oportunidades y guía de Marketing.
- Iniciadores, Compose Scalaris, servidor local y empaquetado Windows.
- Pruebas automatizadas y documentación canónica de la operación local.

La revisión del diff se distribuyó por backend, frontend y scripts. Las migraciones V1–V14 permanecen inmutables. Las credenciales de pruebas son sintéticas; teléfono y enlaces de Scalaris son referencias comerciales públicas confirmadas.

El contenido generado en `validation-output/`, los runtimes, los ZIP, las bases, los respaldos y las claves locales no forman parte del commit. Se conservan fuera de Git conforme a la política del repositorio. La entrega Windows del recorrido anterior mantiene su checksum y su evidencia local; no se atribuye automáticamente a un nuevo commit ni a la CI remota.

## Ajustes detectados antes de publicar

- Alta de usuarios: el rol predeterminado y la opción Marketing deben seguir el perfil de organización. `LEGACY` utiliza `SALES`; `SCALARIS` utiliza `MARKETING`. La selección del operador se conserva cuando termina de cargar el perfil.
- Proxy local: una respuesta interrumpida por el backend después de recibir headers debe cerrar la respuesta al navegador, sin dejar la petición pendiente ni interrumpir solicitudes posteriores.

Ambos ajustes se verificaron antes del stage y commit: frontend con typecheck/build, 16/16 unitarias y 5/5 recorridos de navegador en QA privado (32,2 s; cero fallos y omisiones), y proxy con 2/2 pruebas Node, incluida la interrupción posterior a headers y la siguiente petición normal. La evidencia funcional anterior de Scalaris se conserva en `docs/validation/scalaris-marketing-local-2026-10-14.md`.

```text
frontend de la fuente a publicar: index-DwmPZe8F.js / index-rXXdIgLR.css
JavaScript SHA-256: F9DEBEA2069353D9EC3524C3F4B3A3E4292A2741476C9F9779452A23E4002445
CSS SHA-256: 5C59A5FAEBEFB7ACD6AE76884CBBDEDCE1FBFE24227E70BB75B6EA53751962D0
parser PowerShell 5.1: 28 scripts aprobados
posibles secretos detectados en candidatos: 0
```

La instalación de uso y el ZIP Windows previo no se reiniciaron ni se modificaron durante esta publicación. Contienen el servidor y frontend de la validación local anterior; el repositorio incorpora estas dos correcciones adicionales. Un paquete posterior debe generarse desde la nueva fuente y registrar su propia procedencia y checksum.

## Verificación Git y controles

1. Revisar estado, rama, remoto y comparación tras `fetch`.
2. Revisar el diff completo y los archivos nuevos del alcance.
3. Comprobar sintaxis, whitespace, archivos prohibidos y posibles secretos sin imprimir valores sensibles.
4. Agregar rutas explícitas del alcance revisado y revisar el diff del índice.
5. Crear un commit de implementación y publicar mediante fast-forward.
6. Confirmar SHA local, tracking remoto y referencia remota real.
7. Consultar la CI del SHA publicado y registrar su resultado sin sustituirlo por las pruebas locales.

No se realiza force push, reset, clean ni eliminación de datos. La falta de Docker en este host continúa limitando el validador local integral y la suite histórica de Testcontainers; no bloquea por sí sola la operación Git autorizada ni convierte esa fase en PASS.

## Resultado

La implementación se publicó por fast-forward en [331b3e7](https://github.com/JerePrograma/crm-platform/commit/331b3e7be5ad52b93b285a0ff9654abedce79b7d): 64 archivos, 4.126 inserciones y 252 eliminaciones. HEAD, `origin/main` y la referencia real `refs/heads/main` consultada con `ls-remote` coincidieron; divergencia `0 0`. El working tree estaba limpio al terminar esa verificación, antes de este cierre documental.

La [CI 38600419532](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532) inició queued y después detectó un fallo real en [dependency-scan 115977926787](https://github.com/JerePrograma/crm-platform/actions/runs/38600419532/job/115977926787). Grype terminó con exit 2 por vulnerabilidades high/critical en dependencias Java y la imagen runtime. npm audit terminó sin vulnerabilidades y el build Docker pasó; esos resultados no convierten el scan fallido en PASS. Resultado final: run completed/failure, 21/22 jobs success y solo dependency-scan failure. frontend-e2e y Gmail fake Google terminaron SUCCESS. Estado `FAILED_DEPENDENCY_SCAN`, sin afirmar PASS integral ni tratarlo como bloqueo externo.

El log señala Spring Web MVC 7.0.8 → 7.0.9, Tomcat 11.0.22 → 11.0.25, Jackson databind/core 2.21.5 y 3.1.5 → 2.21.7 y 3.1.7, y hallazgos en libcrypto/glibc/zlib de la base Chainguard. POM y Dockerfile son idénticos a la base `75f37a023a0e69d991fe7640f6ed24df98d6ca18`: los hallazgos son heredados y siguen sin remediar. La reparación queda en `SCALARIS-SEC-001`; no se cambiaron POM ni runtime durante esta publicación.

También queda `SCALARIS-AGENDA-001`: el límite inicial de 200 tareas se aplica antes del filtro de pendientes de Marketing y puede omitir seguimientos con muchas tareas cerradas. Se registró sin corregir el producto en este cierre.

Este cierre documental registra la publicación de la implementación y sus resultados post-push. Su propio commit se consulta en Git, sin autorreferencia de SHA. La publicación Git no equivale a producción ni a CI verde; las diferencias del ZIP/instalación anteriores y los límites de comunicaciones/datos permanecen vigentes.
