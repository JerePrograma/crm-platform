#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
. "$repo_root/scripts/database-compose.sh"
backup_dir="$(dirname "$repo_root")/crm-backups"
if [ "$#" -gt 0 ]; then
  case "$1" in --*) ;; *) backup_dir=$1; shift ;; esac
fi
read_database_compose_options "$@"
backup_dir=$(normalize_database_directory "$backup_dir")
case "$backup_dir" in
  "$repo_root"|"$repo_root"/*) printf 'Backup directory must be outside the repository.\n' >&2; exit 1 ;;
esac
mkdir -p "$backup_dir"
container=$(get_database_container)
database=${POSTGRES_DB:-$(docker exec "$container" printenv POSTGRES_DB)}
database_user=${DATABASE_USER:-$(docker exec "$container" printenv POSTGRES_USER)}
stamp=$(date -u +%Y%m%dT%H%M%SZ)
safe_database_name=$(printf '%s' "$database" | tr -c 'a-zA-Z0-9_.-' '_')
base="crm-${safe_database_name}-${stamp}-$$"
container_file="/tmp/${base}.dump"
backup_file="${backup_dir}/${base}.dump"
cleanup() {
  docker exec "$container" rm -f "$container_file" >/dev/null 2>&1 || true
  rm -f "${backup_file}.partial"
}
trap cleanup EXIT
docker exec "$container" pg_dump -U "$database_user" -d "$database" -Fc --compress=9 --no-owner --no-privileges -f "$container_file"
docker exec "$container" pg_restore --list "$container_file" >/dev/null
docker cp "${container}:${container_file}" "${backup_file}.partial"
[ -s "${backup_file}.partial" ] || { printf 'Backup archive is empty.\n' >&2; exit 1; }
mv "${backup_file}.partial" "$backup_file"
checksum=$(sha256sum "$backup_file" | awk '{print $1}')
printf '%s  %s\n' "$checksum" "$(basename "$backup_file")" > "${backup_file}.sha256"
printf '{"createdAtUtc":"%s","format":"postgres-custom-compressed","sha256":"%s","sizeBytes":%s,"restoreTested":false}\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$checksum" "$(wc -c < "$backup_file")" > "${backup_file}.json"
printf 'Backup created outside Git: %s\n' "$backup_file"
