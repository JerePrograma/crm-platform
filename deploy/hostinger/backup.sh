#!/usr/bin/env sh
set -eu
umask 077
release_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
backup_dir=${CRM_BACKUP_DIRECTORY:-/srv/apps/crm/backups}
env_file=${CRM_ENV_FILE:-/srv/apps/crm/config/production.env}
[ -d "$backup_dir" ] && [ -r "$env_file" ] || { printf 'Private backup directory or production environment is missing.\n' >&2; exit 1; }
command -v flock >/dev/null || { printf 'flock is required.\n' >&2; exit 1; }
exec 9>"$backup_dir/.backup.lock"
flock -n 9 || { printf 'A CRM backup is already running.\n'; exit 0; }
# Explicit selection prevents backing up any other Compose application.
sh "$release_root/scripts/backup-database.sh" "$backup_dir" \
  --compose-file "$release_root/deploy/docker-compose.production.yml" \
  --project-name scalaris-crm --env-file "$env_file"
# No automatic deletion: offsite encryption/retention must be approved first.
