#!/usr/bin/env sh
set -eu

container=$(docker compose ps -q postgres)
[ -n "$container" ] || { printf 'The project PostgreSQL container is not running.\n' >&2; exit 1; }
database=${POSTGRES_DB:-$(docker exec "$container" printenv POSTGRES_DB)}
database_user=${DATABASE_USER:-$(docker exec "$container" printenv POSTGRES_USER)}
suffix=$(date -u +%Y%m%d%H%M%S)
source_db="crm_restore_source_${suffix}"
target_db="crm_restore_target_${suffix}"
source_dump="/tmp/${source_db}.dump"
target_dump="/tmp/${target_db}.dump"
cleanup() {
  docker exec "$container" dropdb -U "$database_user" --if-exists "$target_db" >/dev/null 2>&1 || true
  docker exec "$container" dropdb -U "$database_user" --if-exists "$source_db" >/dev/null 2>&1 || true
  docker exec "$container" rm -f "$source_dump" "$target_dump" >/dev/null 2>&1 || true
}
trap cleanup EXIT
docker exec "$container" pg_dump -U "$database_user" -d "$database" -Fc --no-owner --no-privileges -f "$source_dump"
docker exec "$container" createdb -U "$database_user" "$source_db"
docker exec "$container" pg_restore -U "$database_user" -d "$source_db" --no-owner --no-privileges --exit-on-error "$source_dump"
docker exec "$container" psql -U "$database_user" -d "$source_db" -v ON_ERROR_STOP=1 -c "CREATE TABLE backup_restore_probe(id uuid PRIMARY KEY, value text NOT NULL); INSERT INTO backup_restore_probe VALUES ('00000000-0000-0000-0000-00000000b001','SYNTHETIC_BACKUP_RESTORE_PROBE');" >/dev/null
docker exec "$container" pg_dump -U "$database_user" -d "$source_db" -Fc --compress=9 --no-owner --no-privileges -f "$target_dump"
docker exec "$container" createdb -U "$database_user" "$target_db"
docker exec "$container" pg_restore -U "$database_user" -d "$target_db" --no-owner --no-privileges --exit-on-error "$target_dump"
probe=$(docker exec "$container" psql -U "$database_user" -d "$target_db" -At -v ON_ERROR_STOP=1 -c "SELECT value FROM backup_restore_probe WHERE id='00000000-0000-0000-0000-00000000b001';")
version=$(docker exec "$container" psql -U "$database_user" -d "$target_db" -At -v ON_ERROR_STOP=1 -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;')
[ "$probe" = 'SYNTHETIC_BACKUP_RESTORE_PROBE' ] && [ -n "$version" ]
printf 'Backup/restore drill passed: isolated schema V%s and synthetic probe verified.\n' "$version"
