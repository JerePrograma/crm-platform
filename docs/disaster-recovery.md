# Recuperación ante desastres

## Estado

Plan provisional. No existe staging ni producción todavía.

## Activos críticos

1. PostgreSQL;
2. secretos y credenciales de integración;
3. imágenes/versiones desplegadas;
4. Terraform state;
5. adjuntos comerciales futuros;
6. configuración DNS/entregabilidad;
7. auditoría y logs dentro de su retención.

## Objetivos provisionales

Antes de producción se deben aprobar:

- RPO objetivo;
- RTO objetivo;
- retención de backups;
- regiones;
- responsables;
- ejercicios periódicos.

Como punto inicial de diseño:

- RPO deseado: 24 horas o menor;
- RTO deseado: 4 horas o menor.

No son compromisos hasta probarlos.

## PostgreSQL

Producción deberá usar:

- backups automáticos Cloud SQL;
- point-in-time recovery;
- cifrado;
- acceso IAM mínimo;
- exportaciones periódicas verificadas;
- pruebas de restauración en proyecto/instancia aislada.

Un backup no se considera válido hasta restaurarlo y ejecutar verificaciones de integridad.

## Procedimiento de restauración

1. declarar incidente y congelar escrituras;
2. activar kill switches;
3. identificar punto de recuperación;
4. restaurar en instancia aislada;
5. ejecutar Flyway info/validate sin mutar;
6. verificar conteos, constraints y auditoría;
7. ejecutar pruebas de humo;
8. cambiar tráfico de forma controlada;
9. monitorear;
10. documentar pérdida de datos y acciones manuales.

## Secretos

Secret Manager no sustituye backups de configuración. Documentar nombres y propósito, nunca valores. Ante pérdida o compromiso:

- recrear o rotar;
- actualizar versiones activas;
- revocar credencial anterior;
- verificar integraciones;
- auditar acceso.

## Terraform state

- backend remoto con versionado y locking;
- acceso restringido;
- recuperación de versiones;
- no contener secretos cuando sea evitable;
- exportación/documentación de recursos críticos.

## Pruebas

Antes de producción:

- restauración de PostgreSQL;
- pérdida de instancia Cloud Run;
- pérdida/rotación de secreto;
- rollback de imagen;
- Pub/Sub redelivery;
- Cloud Tasks duplicate delivery;
- indisponibilidad Gmail;
- activación global de kill switch.

## Datos locales

Docker Compose no es backup. El volumen local puede eliminarse. Los archivos reales deben tener una ubicación operativa autorizada y una política de retención independiente del repositorio.
