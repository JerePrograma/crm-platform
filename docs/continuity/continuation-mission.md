# Misión de continuidad para la próxima sesión

Actualizado: 2026-07-24

## Objetivo inmediato

Cerrar el candidato post-hardening de forma segura, integrarlo sobre el `main` actual y publicarlo únicamente después de obtener evidencia verde sobre el contenido exacto que se enviará.

## Primer paso obligatorio

Leer todo `docs/continuity/` y luego inspeccionar:

- `AGENTS.md`;
- `docs/status.md`;
- `docs/next-step.md`;
- `docs/backlog.md`;
- `docs/estado-integral-y-roadmap.md`;
- `docs/validation/COMPLETE-CRM-matrix.md`;
- scripts de validación;
- configuración Docker/Compose;
- diff real del candidato.

No asumir rutas, nombres, comandos ni estado remoto.

## Investigación del fallo pendiente

La última ejecución levantó correctamente el perfil productivo y mostró `SENDING_ENABLED=false`, pero la aserción del harness la reportó como ausente.

Localizar:

- función que captura `docker inspect ... {{json .Config.Env}}`;
- tipo de dato devuelto;
- comprobación de membresía;
- serialización o saltos de línea;
- pruebas existentes del script;
- diferencias PowerShell 5.1/7.

Corregir el parser o la aserción, no debilitar el gate.

## Integración

El candidato histórico parte de `83e181c`, pero la documentación de continuidad habrá adelantado `main`.

Procedimiento:

1. confirmar `git status --short`, rama y remotos;
2. detenerse ante cambios locales ajenos;
3. `git fetch origin`;
4. `git switch main`;
5. `git pull --ff-only origin main`;
6. conservar el commit documental actual;
7. reconstruir o aplicar los cambios del candidato sobre ese `main`;
8. resolver únicamente conflictos reales revisados;
9. calcular el nuevo tree final;
10. revisar diff completo contra el `main` documental.

No usar force push, rebase de commits publicados ni reset destructivo.

## Validación mínima de reanudación

- prueba específica de la aserción de entorno;
- `productionProfileSmoke`;
- `finalTreeClean`;
- `git diff --check`;
- repository safety;
- status y diff final.

## Validación de cierre

Antes de publicar, ejecutar el contrato canónico requerido por el repositorio sobre el commit exacto. Si la documentación o integración alteró archivos cubiertos, no reutilizar evidencia antigua para esas fases.

El cierre histórico exige dos corridas limpias consecutivas del validador integral. Confirmar si esa regla sigue vigente en `AGENTS.md` y cumplirla, salvo instrucción explícita del usuario que cambie el contrato.

## Documentación a actualizar al cerrar

- `docs/status.md`;
- `docs/next-step.md`;
- `docs/backlog.md`;
- `docs/estado-integral-y-roadmap.md`;
- `docs/execution/complete-crm-platform-progress.md`;
- `docs/validation/COMPLETE-CRM-matrix.md`;
- documento de evidencia nuevo;
- `CHANGELOG.md`;
- `README.md`;
- estos archivos de continuidad si el estado cambia.

## Condiciones de parada

Detenerse sin commit ni push ante:

- cambios locales previos no relacionados;
- divergencia o conflicto remoto;
- evidencia inconsistente;
- secretos o datos reales;
- fallo que no pueda demostrarse como previo;
- necesidad de migración destructiva;
- incompatibilidad del candidato con el `main` actual;
- necesidad de force push;
- validaciones requeridas fallidas.

## Informe final requerido

1. resumen implementado;
2. archivos modificados;
3. comandos ejecutados;
4. resultado de cada validación;
5. commit y hash;
6. resultado del push a `main`;
7. estado de CI;
8. riesgos, limitaciones y pendientes;
9. desviaciones del alcance.

No afirmar ejecución sin salida de comandos.
