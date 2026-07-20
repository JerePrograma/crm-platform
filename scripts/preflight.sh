#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
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

mode=${1:-local}
case "$mode" in
  local|--local)
    container_only=false
    ;;
  container|--container-only)
    container_only=true
    ;;
  *)
    fail "Unknown preflight mode: $mode. Use --local or --container-only"
    ;;
esac

for command_name in git docker; do
  require_command "$command_name"
done

if [ "$container_only" = "false" ]; then
  for command_name in java node npm; do
    require_command "$command_name"
  done
  if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
    fail "curl or wget is required"
  fi
fi

[ -f .env ] || fail ".env is missing. Copy .env.example to .env and edit it first"

set -a
# shellcheck disable=SC1091
. ./.env
set +a

[ -n "${POSTGRES_DB:-}" ] || fail "POSTGRES_DB is required"
[ -n "${POSTGRES_HOST_PORT:-}" ] || fail "POSTGRES_HOST_PORT is required"
[ -n "${BACKEND_HOST_PORT:-}" ] || fail "BACKEND_HOST_PORT is required"
[ -n "${FRONTEND_HOST_PORT:-}" ] || fail "FRONTEND_HOST_PORT is required"
[ -n "${DATABASE_URL:-}" ] || fail "DATABASE_URL is required"
[ -n "${DATABASE_USER:-}" ] || fail "DATABASE_USER is required"
[ -n "${DATABASE_PASSWORD:-}" ] || fail "DATABASE_PASSWORD is required"
[ -n "${CRM_BOOTSTRAP_USERNAME:-}" ] || fail "CRM_BOOTSTRAP_USERNAME is required for local UI access"
[ -n "${CRM_BOOTSTRAP_PASSWORD:-}" ] || fail "CRM_BOOTSTRAP_PASSWORD is required for local UI access"

validate_port POSTGRES_HOST_PORT "$POSTGRES_HOST_PORT"
validate_port BACKEND_HOST_PORT "$BACKEND_HOST_PORT"
validate_port FRONTEND_HOST_PORT "$FRONTEND_HOST_PORT"

[ "$POSTGRES_HOST_PORT" != "$BACKEND_HOST_PORT" ] \
  && [ "$POSTGRES_HOST_PORT" != "$FRONTEND_HOST_PORT" ] \
  && [ "$BACKEND_HOST_PORT" != "$FRONTEND_HOST_PORT" ] \
  || fail "POSTGRES_HOST_PORT, BACKEND_HOST_PORT and FRONTEND_HOST_PORT must be different"

case "$DATABASE_URL" in
  *:"$POSTGRES_HOST_PORT"/*) ;;
  *) fail "DATABASE_URL must use the same port as POSTGRES_HOST_PORT for host-based development" ;;
esac

[ "${SENDING_ENABLED:-}" = "false" ] || fail "SENDING_ENABLED must remain false"
[ "${SENDING_DRY_RUN:-}" = "true" ] || fail "SENDING_DRY_RUN must remain true"
[ "${SENDING_DAILY_LIMIT:-}" = "0" ] || fail "SENDING_DAILY_LIMIT must remain 0"
[ "${SENDING_KILL_SWITCH:-}" = "true" ] || fail "SENDING_KILL_SWITCH must remain true"

docker compose version >/dev/null
docker compose --profile app --profile smoke config >/dev/null

printf 'Preflight passed.\n'
printf 'Mode: %s\n' "$( [ "$container_only" = "true" ] && printf 'container-only' || printf 'local-tools' )"
printf 'Docker: %s\n' "$(docker --version)"
if [ "$container_only" = "false" ]; then
  printf 'Java: %s\n' "$(java -version 2>&1 | head -n 1)"
  printf 'Node: %s\n' "$(node --version)"
  printf 'npm: %s\n' "$(npm --version)"
fi
printf 'PostgreSQL host port: %s\n' "$POSTGRES_HOST_PORT"
printf 'Backend host port: %s\n' "$BACKEND_HOST_PORT"
printf 'Frontend host port: %s\n' "$FRONTEND_HOST_PORT"
printf 'Database URL: %s\n' "$DATABASE_URL"
printf 'Bootstrap user configured: yes\n'
printf 'Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true\n'
