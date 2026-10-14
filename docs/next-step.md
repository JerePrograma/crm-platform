# Continuidad y próximos pasos

Actualizado: 2026-10-14

## Misión activa: SCALARIS-001

La edición Windows local de Marketing Scalaris está entregada y comprobada: apertura, cuentas/roles, persistencia, parada, respaldo, recuperación y ZIP definitivo. La fase actual es publicar en GitHub con autorización explícita del usuario; después continúa uso/piloto local siguiendo la [guía operativa](scalaris-marketing-local.md) y [SCALARIS-001](segments/SCALARIS-001.md).

```text
ACTIVE_SEGMENT SCALARIS-001
SCALARIS_IMPLEMENTATION AUTHORIZED_GITHUB_PUBLICATION_IN_PROGRESS
SCALARIS_STATE LOCAL_OPERATIONAL_PASS
BACKEND_UNIT EXECUTED_PASS_62
BACKEND_FOCUSED_INTEGRATION EXECUTED_PASS_6
DOCKER_LAUNCHER_FOCUSED EXECUTED_PASS_55_ASSERTIONS
DOCKER_REAL BLOCKED_EXTERNAL_NOT_INSTALLED
PORTABLE_WINDOWS EXECUTED_PASS_18_ASSERTIONS_PS51
FRONTEND_UNIT_TYPECHECK_BUILD EXECUTED_PASS_16_UNIT
FRONTEND_AUDIT EXECUTED_PASS_ZERO_VULNERABILITIES
FRONTEND_E2E EXECUTED_PASS_4_OF_4_ZERO_SKIPPED_UNEXPECTED_FLAKY
SOURCE_FOR_DELIVERY REVIEWED_LOCAL_SOURCE_PUBLICATION_IN_PROGRESS
FINAL_ZIP EXECUTED_PASS_MANIFEST_CHECKSUM_PRIVATE_FILES_ZERO
USE_INSTALLATION EXECUTED_PASS_LOOPBACK_8787_PROSPECTS_ZERO
USE_CATALOG TAGS_18_TEMPLATES_7
QA_PROCESSES STOPPED
CURRENT_EDITION_COMPLETE_VALIDATOR BLOCKED_EXTERNAL_DOCKER_NOT_INSTALLED
CURRENT_EDITION_REMOTE_PUBLICATION AUTHORIZED_IN_PROGRESS
CURRENT_EDITION_CI PENDING_POST_PUSH
REAL_COMMUNICATIONS DISABLED_BY_POLICY
REAL_CONTACTS NOT_IMPORTED
PRODUCTION NOT_DEPLOYED
NEXT SCALARIS_AUTHORIZED_GITHUB_PUBLICATION_THEN_LOCAL_PILOT
```

## Verificación funcional local completada

Backend: 62 pruebas unitarias y 6 de integración focalizada. Frontend: 16 unitarias, typecheck, build, audit sin vulnerabilidades y corrida E2E conjunta 4/4 aprobada, sin omitidos, inesperados ni flaky. El portable real aprobó 18 comprobaciones PowerShell 5.1, incluida persistencia y recuperación en base nueva con la anterior conservada. Manual revisado visualmente en desktop/móvil, sin overflow a 1440/390/320 px. Fuente local congelada para la entrega.

## Próximo paso: uso/piloto local

Antes de cerrar la fase Git, revisar el diff y los archivos incluidos, ejecutar los controles pertinentes, crear el commit autorizado y publicar a GitHub, preservando el estado remoto. Registrar commit, resultado del push y coincidencia local/remota; verificar después la CI de ese SHA. La autoridad es [publicación GitHub Scalaris](validation/scalaris-github-publication-2026-10-14.md). Publicación `AUTHORIZED_IN_PROGRESS`; CI `PENDING_POST_PUSH`. No afirmar éxito antes de comprobarlo.

El ZIP definitivo quedó revisado: manifiesto/checksum coincidentes, 2.243 entradas y cero archivos privados. La instalación de uso está activa en `http://127.0.0.1:8787` con cero prospectos, 18 etiquetas y 7 plantillas; perfil/rol correctos y todos los servicios en loopback. Los entornos QA quedaron detenidos.

1. Ingresar con Marketing, cambiar la clave inicial y seguir la guía para organizar la jornada.
2. Revisar consultas y seguimientos con la persona que usará el CRM; registrar dificultades de uso para una misión posterior.
3. Crear respaldos antes de actualizar y conservar copia privada/configuración según la guía.

No regenerar el paquete, importar contactos reales, conectar proveedores ni enviar automáticamente como continuación. La carga de datos operativos y nuevas capacidades requieren un alcance explícito. Docker real y la suite integral histórica permanecen bloqueados mientras no esté disponible Docker.

No trasladar el PASS de julio a los cambios actuales ni afirmar cierre integral remoto. El commit y push actuales sí están autorizados explícitamente; producción, PR y comunicaciones reales permanecen fuera de alcance.

## Historial remoto consolidado — 2026-07-30

```text
BRANCH main
VAL_001 FUNCTIONAL_PASS
VAL_002 COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX_003 COMPLETE_WITH_FOCUSED_VALIDATION_AND_CI_NO_CHECKS_REPORTED
UX_004 FUNCTIONAL_PASS
UX_006 COMPLETE_WITH_FUNCTIONAL_VALIDATION
SEG001_GMAIL COMPLETE_PUBLISHED
BASE 9995b3e71278d069e3d17afb1c36cdfc995a0bf2
IMPLEMENTATION_COMMIT df2c79a77e27203f7eb63325f3e526cf38d1820a
VALIDATED_COMMIT d6bfe64283cd3bab8dce441e4b01e348f9a55d5e
PUBLISHED_COMMIT d6bfe64283cd3bab8dce441e4b01e348f9a55d5e
COMPLETE_VALIDATOR 2_OF_2_FUNCTIONAL_PASS
CI EXECUTED_PASS_RUN_30530515993_22_OF_22
FAKE_GOOGLE_E2E EXECUTED_PASS
REAL_GOOGLE IMPLEMENTED_NOT_CONNECTED
PRODUCTION NOT_DEPLOYED
REAL_COMMUNICATIONS DISABLED_BY_POLICY
NEXT_AT_HISTORICAL_CHECKPOINT OPS-001
```

## Qué quedó resuelto

- `VAL-001`, `VAL-002`, `UX-003`, `UX-004` y `UX-006` permanecen cerrados;
- se creó y verificó el snapshot externo de los 68 archivos parciales;
- se retiró solo la referencia Codex rota y `git fsck` no mostró corrupción
  alcanzable;
- `main` avanzó por fast-forward de `12421c5` a `9995b3e` y los 68 hashes
  permanecieron idénticos;
- V14, OAuth/cifrado, Gmail REST, campaña/outbox, unsubscribe y UI quedaron
  integrados;
- backend, frontend y Google falso/Playwright pasaron con datos sintéticos;
- el manual HTML/PDF/32 PNG/JSON/ZIP fue generado y revisado.

## Qué permanece fuera de alcance

- Google real, credenciales reales, correo real y producción siguen fuera de
  alcance;
- Gmail aceptar un mensaje no prueba entrega al buzón;
- continúan en backlog `OPS-001`, `UX-007`, `TECH-001` y `PERF-001`.

## Backlog posterior al cierre Scalaris

SEG-001 Gmail quedó publicado. Los siguientes trabajos históricos permanecen en backlog; revisar su prioridad después del cierre local de SCALARIS-001:

1. `OPS-001` — outbox e inbound paginados;
2. `UX-007` — navegadores, foco y móvil;
3. `TECH-001` — modularización gradual;
4. `PERF-001` — escala representativa autorizada.

No reconstruir el candidato histórico `9e058d...` por descripción.

## Fuera de esta misión

No se conecta Google real, no se habilitan flags de red/envío, no se despliega,
no se incorpora el XLSX real y no se reconstruye el candidato histórico
`9e058d...`.

## Restricciones

```text
SENDING_ENABLED=false
SENDING_DRY_RUN=true
SENDING_DAILY_LIMIT=0
SENDING_KILL_SWITCH=true
MESSAGING_REAL_NETWORK_ALLOWED=false
EMAIL_PROVIDER_MODE=NOOP
WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
```

No desplegar, no enviar, no conectar proveedores, no incorporar datos reales y no usar force push.
