# Backup and restore

`scripts/backup-database.ps1` and `.sh` use PostgreSQL custom compressed format, write outside Git by default, and produce SHA-256 plus sanitized metadata. Backups require encrypted, access-controlled storage and a documented retention schedule.

`scripts/restore-database.ps1` and `.sh` refuse the primary database and require an explicit destructive confirmation plus a `crm_restore_` target. `scripts/verify-backup-restore.ps1` and `.sh` clone only the current test database into isolated databases, add a synthetic integrity probe, back it up, restore it, verify the probe and Flyway history, and remove only resources with their unique drill prefix.

An untested backup is not accepted. Before any operational restore, preserve the failed database, verify checksum and PostgreSQL compatibility, restore to isolation, run integrity/application checks, and obtain operator approval for cutover.
