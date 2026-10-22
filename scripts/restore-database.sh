#!/usr/bin/env sh
set -eu

[ "$#" -ge 3 ] && [ "$3" = "--confirm-destructive-restore" ] || { printf 'Usage: %s BACKUP_FILE crm_restore_NAME --confirm-destructive-restore [--compose-file FILE] [--project-name NAME] [--env-file FILE]\n' "$0" >&2; exit 1; }
backup_file=$1
target=$2
shift 3
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$script_dir/database-compose.sh"
read_database_compose_options "$@"
case "$target" in crm_restore_?*) ;; *) printf 'Target must use crm_restore_ prefix.\n' >&2; exit 1 ;; esac
case "$target" in *[!a-z0-9_]*) printf 'Target contains unsupported characters.\n' >&2; exit 1 ;; esac
[ -f "$backup_file" ] || { printf 'Backup not found.\n' >&2; exit 1; }
[ -f "${backup_file}.sha256" ] || { printf 'Backup checksum file is required.\n' >&2; exit 1; }
expected=$(awk 'NR==1 { print $1 }' "${backup_file}.sha256")
actual=$(sha256sum "$backup_file" | awk '{print $1}')
[ "$actual" = "$expected" ] || { printf 'Backup checksum does not match.\n' >&2; exit 1; }
container=$(get_database_container)
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
