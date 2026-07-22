#!/usr/bin/env sh
set -eu

[ "$#" -eq 3 ] && [ "$3" = "--confirm-destructive-restore" ] || { printf 'Usage: %s BACKUP_FILE crm_restore_NAME --confirm-destructive-restore\n' "$0" >&2; exit 1; }
backup_file=$1
target=$2
case "$target" in crm_restore_[a-z0-9_]*) ;; *) printf 'Target must use crm_restore_ prefix.\n' >&2; exit 1 ;; esac
[ -f "$backup_file" ] || { printf 'Backup not found.\n' >&2; exit 1; }
if [ -f "${backup_file}.sha256" ]; then (cd "$(dirname "$backup_file")" && sha256sum -c "$(basename "$backup_file").sha256"); fi
container=$(docker compose ps -q postgres)
[ -n "$container" ] || { printf 'The project PostgreSQL container is not running.\n' >&2; exit 1; }
database_user=${DATABASE_USER:-$(docker exec "$container" printenv POSTGRES_USER)}
primary=${POSTGRES_DB:-$(docker exec "$container" printenv POSTGRES_DB)}
[ "$target" != "$primary" ] || { printf 'Primary database restore is prohibited.\n' >&2; exit 1; }
container_file="/tmp/$(basename "$backup_file")"
trap 'docker exec "$container" rm -f "$container_file" >/dev/null 2>&1 || true' EXIT
docker cp "$backup_file" "${container}:${container_file}"
docker exec "$container" dropdb -U "$database_user" --if-exists "$target"
docker exec "$container" createdb -U "$database_user" "$target"
docker exec "$container" pg_restore -U "$database_user" -d "$target" --no-owner --no-privileges --exit-on-error "$container_file"
printf 'Restore completed into isolated database: %s\n' "$target"
