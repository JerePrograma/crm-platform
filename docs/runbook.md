# Runbook operativo

## Prioridades

- P0: envío no autorizado, exposición de datos, acceso no autorizado, corrupción masiva;
- P1: indisponibilidad total, migración fallida, importaciones corruptas;
- P2: función degradada, filas rechazadas, UI parcial;
- P3: defecto menor o mejora.

## P0 — Envío no autorizado

Estado actual: el adaptador Gmail existe, pero los defaults versionados lo
bloquean y Google real no está conectado.

1. establecer `SENDING_KILL_SWITCH=true`;
2. establecer `SENDING_ENABLED=false`,
   `MESSAGING_REAL_NETWORK_ALLOWED=false` y `EMAIL_PROVIDER_MODE=NOOP`;
3. activar el kill switch persistente;
4. pausar campañas y worker;
5. revocar localmente las cuentas y ejecutar revocación remota best-effort;
6. preservar logs/auditoría sin copiar tokens, MIME ni destinatarios;
7. identificar comunicaciones afectadas por correlation/provider ID;
8. bloquear despliegues e iniciar incidente de seguridad;
9. no reanudar sin causa raíz, regresión y aprobación formal.

## P0 — Exposición de datos

1. aislar servicio o credencial;
2. rotar secretos;
3. revocar sesiones/tokens;
4. determinar alcance por auditoría y logs;
5. preservar evidencia;
6. corregir y probar;
7. evaluar notificaciones legales/contractuales;
8. redactar postmortem.

## P1 — Migración fallida

1. detener nuevas instancias;
2. capturar versión Flyway y error;
3. no usar `repair` sin comprender el estado;
4. verificar si hubo cambios parciales;
5. crear forward-fix o restaurar backup según impacto;
6. probar en clon/staging;
7. documentar comandos exactos.

## P1 — Importación incorrecta

1. identificar `import_job`;
2. detener operaciones dependientes;
3. revisar filas y auditoría;
4. no borrar evidencia;
5. si no hubo comunicación, preparar reversión transaccional específica;
6. agregar exclusiones si existe riesgo de contacto;
7. corregir fuente y pruebas;
8. reimportar solo mediante flujo soportado.

## P2 — UI inaccesible

1. comprobar backend health;
2. probar API con credenciales locales;
3. revisar build/proxy;
4. revisar consola sin copiar secretos;
5. operar temporalmente por API si está autorizado.

## P2 — API no autentica

1. verificar variables bootstrap;
2. confirmar reinicio de proceso;
3. comprobar reloj y entorno;
4. no agregar credenciales hardcoded;
5. revisar eventos de seguridad.

## Información mínima de incidente

- fecha/hora UTC y local;
- ambiente;
- commit/imagen;
- actor que detectó;
- impacto;
- entidades afectadas;
- logs y correlation IDs;
- acciones tomadas;
- estado de kill switches;
- próxima decisión y responsable.

## Cierre

Todo P0/P1 requiere:

- causa raíz;
- acciones correctivas;
- pruebas de regresión;
- actualización de runbook;
- revisión de seguridad;
- aprobación antes de reactivar operaciones críticas.
