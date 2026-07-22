#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backup_dir=${1:-"$(dirname "$repo_root")/crm-backups"}
case "$(cd "$(dirname "$backup_dir")" 2>/dev/null && pwd)/$(basename "$backup_dir")" in
  "$repo_root"|"$repo_root"/*) printf 'Backup directory must be outside the repository.\n' >&2; exit 1 ;;
esac
mkdir -p "$backup_dir"
container=$(docker compose ps -q postgres)
[ -n "$container" ] || { printf 'The project PostgreSQL container is not running.\n' >&2; exit 1; }
database=${POSTGRES_DB:-$(docker exec "$container" printenv POSTGRES_DB)}
database_user=${DATABASE_USER:-$(docker exec "$container" printenv POSTGRES_USER)}
stamp=$(date -u +%Y%m%dT%H%M%SZ)
base="crm-${database}-${stamp}"
container_file="/tmp/${base}.dump"
backup_file="${backup_dir}/${base}.dump"
trap 'docker exec "$container" rm -f "$container_file" >/dev/null 2>&1 || true' EXIT
docker exec "$container" pg_dump -U "$database_user" -d "$database" -Fc --compress=9 --no-owner --no-privileges -f "$container_file"
docker cp "${container}:${container_file}" "$backup_file"
checksum=$(sha256sum "$backup_file" | awk '{print $1}')
printf '%s  %s\n' "$checksum" "$(basename "$backup_file")" > "${backup_file}.sha256"
printf '{"createdAtUtc":"%s","database":"%s","format":"postgres-custom-compressed","sha256":"%s","sizeBytes":%s}\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$database" "$checksum" "$(wc -c < "$backup_file")" > "${backup_file}.json"
printf 'Backup created outside Git: %s\n' "$backup_file"
