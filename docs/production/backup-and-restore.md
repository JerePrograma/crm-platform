# Respaldo y recuperación

Los scripts PowerShell/Unix generan archivos PostgreSQL custom comprimidos fuera de Git, comprueban que el archivo se pueda listar antes de copiarlo, publican la copia solo cuando está completa y escriben SHA-256 más metadata. El destino Unix se normaliza completo, incluyendo componentes aún inexistentes y enlaces simbólicos, antes de crear directorios y rechaza rutas dentro del repositorio. `restoreTested=false` distingue un archivo legible de una restauración comprobada. La restauración exige checksum, confirmación destructiva y un destino `crm_restore_...`; rechaza la base principal. No elimina ni corta hacia la base de uso.

Seleccionar explícitamente archivo Compose, proyecto y configuración privada evita operar sobre otra aplicación. Se conserva la selección predeterminada para validadores históricos que ya aíslan su proyecto mediante el entorno.

```powershell
.\scripts\backup-database.ps1 -BackupDirectory 'C:\RutaPrivada\Respaldos' -ComposeFile deploy/docker-compose.production.yml -ComposeProjectName scalaris-crm -EnvFile 'C:\RutaPrivada\production.env'
.\scripts\restore-database.ps1 -BackupFile 'C:\RutaPrivada\Respaldos\copia.dump' -TargetDatabase crm_restore_review -ConfirmDestructiveRestore -ComposeFile deploy/docker-compose.production.yml -ComposeProjectName scalaris-crm -EnvFile 'C:\RutaPrivada\production.env'
```

```sh
sh scripts/backup-database.sh /srv/apps/crm/backups --compose-file deploy/docker-compose.production.yml --project-name scalaris-crm --env-file /srv/apps/crm/config/production.env
sh scripts/restore-database.sh /srv/apps/crm/backups/copia.dump crm_restore_review --confirm-destructive-restore --compose-file deploy/docker-compose.production.yml --project-name scalaris-crm --env-file /srv/apps/crm/config/production.env
```

Para el VPS se prepararon `deploy/hostinger/backup.sh` y unidades systemd: copia diaria aproximadamente a las 03:15 de Argentina (`America/Argentina/Buenos_Aires`), directorio privado, lock contra solapamiento y proyecto fijo. No se instalan ni activan automáticamente. No hay borrado de copias hasta aprobar retención y respaldo externo cifrado. Un backup en el mismo VPS no cubre pérdida del host.

Antes de aceptar operación: definir responsable, RPO/RTO, almacenamiento externo cifrado, acceso mínimo, retención y alertas de antigüedad/error. Preservar configuración y claves de instalación por separado. No mezclar PostgreSQL 18 portable con PostgreSQL 17 Docker sin migración revisada.

Ensayo real de recuperación: preservar la base afectada, verificar checksum/versiones, restaurar en aislamiento, comprobar Flyway V14, conteos, tenant/roles, exclusiones, login y guardas con una aplicación aislada. Obtener autorización del operador antes de cambiar el datasource. `verify-backup-restore` usa copias y una sonda sintética; sus nombres incluyen PID y 128 bits aleatorios, y su cleanup elimina únicamente bases cuya creación confirmó esa corrida. Un `createdb` fallido nunca autoriza borrar ese nombre. Ejecutarlo solamente contra datos sintéticos autorizados porque clona la base seleccionada.
