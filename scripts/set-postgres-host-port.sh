#!/usr/bin/env sh
set -eu

port=${1:-55432}
case "$port" in
  *[!0-9]*|'') printf 'ERROR: port must be an integer\n' >&2; exit 1 ;;
esac
[ "$port" -ge 1 ] && [ "$port" -le 65535 ] || {
  printf 'ERROR: port must be between 1 and 65535\n' >&2
  exit 1
}

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_path="$repo_root/.env"
[ -f "$env_path" ] || {
  printf 'ERROR: .env is missing. Copy .env.example to .env first.\n' >&2
  exit 1
}

db_name=$(sed -n 's/^POSTGRES_DB=//p' "$env_path" | head -n 1)
[ -n "$db_name" ] || db_name=gestudio_crm

tmp_path="$env_path.tmp.$$"
awk -v port="$port" -v db="$db_name" '
BEGIN { found_port=0; found_url=0 }
/^POSTGRES_HOST_PORT=/ {
  print "POSTGRES_HOST_PORT=" port
  found_port=1
  next
}
/^DATABASE_URL=/ {
  print "DATABASE_URL=jdbc:postgresql://localhost:" port "/" db
  found_url=1
  next
}
{ print }
END {
  if (!found_port) print "POSTGRES_HOST_PORT=" port
  if (!found_url) print "DATABASE_URL=jdbc:postgresql://localhost:" port "/" db
}
' "$env_path" > "$tmp_path"

mv "$tmp_path" "$env_path"

printf 'Updated .env safely.\n'
printf 'PostgreSQL host port: %s\n' "$port"
printf 'Database URL: jdbc:postgresql://localhost:%s/%s\n' "$port" "$db_name"
printf 'Existing passwords and sending controls were preserved.\n'
