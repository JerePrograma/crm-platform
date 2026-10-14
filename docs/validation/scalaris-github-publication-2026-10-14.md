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
publicación: AUTHORIZED_IN_PROGRESS
CI del nuevo contenido: PENDING_POST_PUSH
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

Commit, push, identidad remota y CI pendientes de ejecución y registro. El estado anterior de documentos que limitaba la misión a cambios locales corresponde a la fase de preparación; esta publicación está autorizada por la solicitud humana posterior.
