#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

validate_port() {
  name=$1
  value=$2
  case "$value" in
    *[!0-9]*|'') fail "$name must be an integer" ;;
  esac
  [ "$value" -ge 1 ] && [ "$value" -le 65535 ] \
    || fail "$name must be between 1 and 65535"
}

postgres_port=${1:-55432}
backend_port=${2:-8080}
frontend_port=${3:-5173}

validate_port POSTGRES_HOST_PORT "$postgres_port"
validate_port BACKEND_HOST_PORT "$backend_port"
validate_port FRONTEND_HOST_PORT "$frontend_port"

[ "$postgres_port" != "$backend_port" ] \
  && [ "$postgres_port" != "$frontend_port" ] \
  && [ "$backend_port" != "$frontend_port" ] \
  || fail "All host ports must be different"

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_path="$repo_root/.env"
[ -f "$env_path" ] || fail ".env is missing. Copy .env.example to .env first"

postgres_db=$(awk -F= '/^POSTGRES_DB=/{print substr($0, index($0, "=") + 1); exit}' "$env_path")
postgres_db=${postgres_db:-gestudio_crm}
tmp_path="$env_path.tmp.$$"

awk -v pg="$postgres_port" -v be="$backend_port" -v fe="$frontend_port" -v db="$postgres_db" '
BEGIN { pg_seen=0; be_seen=0; fe_seen=0; url_seen=0 }
/^POSTGRES_HOST_PORT=/ { print "POSTGRES_HOST_PORT=" pg; pg_seen=1; next }
/^BACKEND_HOST_PORT=/ { print "BACKEND_HOST_PORT=" be; be_seen=1; next }
/^FRONTEND_HOST_PORT=/ { print "FRONTEND_HOST_PORT=" fe; fe_seen=1; next }
/^DATABASE_URL=/ { print "DATABASE_URL=jdbc:postgresql://localhost:" pg "/" db; url_seen=1; next }
{ print }
END {
  if (!pg_seen) print "POSTGRES_HOST_PORT=" pg
  if (!be_seen) print "BACKEND_HOST_PORT=" be
  if (!fe_seen) print "FRONTEND_HOST_PORT=" fe
  if (!url_seen) print "DATABASE_URL=jdbc:postgresql://localhost:" pg "/" db
}
' "$env_path" > "$tmp_path"

mv "$tmp_path" "$env_path"

printf 'Updated .env safely.\n'
printf 'PostgreSQL host port: %s\n' "$postgres_port"
printf 'Backend host port: %s\n' "$backend_port"
printf 'Frontend host port: %s\n' "$frontend_port"
printf 'Database URL: jdbc:postgresql://localhost:%s/%s\n' "$postgres_port" "$postgres_db"
printf 'Existing passwords and sending controls were preserved.\n'
