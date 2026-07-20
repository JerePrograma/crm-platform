#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

for command_name in git docker java node npm; do
  require_command "$command_name"
done

if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
  fail "curl or wget is required"
fi

[ -f .env ] || fail ".env is missing. Copy .env.example to .env and edit it first"

set -a
# shellcheck disable=SC1091
. ./.env
set +a

[ -n "${DATABASE_URL:-}" ] || fail "DATABASE_URL is required"
[ -n "${DATABASE_USER:-}" ] || fail "DATABASE_USER is required"
[ -n "${DATABASE_PASSWORD:-}" ] || fail "DATABASE_PASSWORD is required"
[ -n "${CRM_BOOTSTRAP_USERNAME:-}" ] || fail "CRM_BOOTSTRAP_USERNAME is required for local UI access"
[ -n "${CRM_BOOTSTRAP_PASSWORD:-}" ] || fail "CRM_BOOTSTRAP_PASSWORD is required for local UI access"

[ "${SENDING_ENABLED:-}" = "false" ] || fail "SENDING_ENABLED must remain false"
[ "${SENDING_DRY_RUN:-}" = "true" ] || fail "SENDING_DRY_RUN must remain true"
[ "${SENDING_DAILY_LIMIT:-}" = "0" ] || fail "SENDING_DAILY_LIMIT must remain 0"
[ "${SENDING_KILL_SWITCH:-}" = "true" ] || fail "SENDING_KILL_SWITCH must remain true"

docker compose version >/dev/null
docker compose --profile app config >/dev/null

printf 'Preflight passed.\n'
printf 'Java: %s\n' "$(java -version 2>&1 | head -n 1)"
printf 'Node: %s\n' "$(node --version)"
printf 'npm: %s\n' "$(npm --version)"
printf 'Docker: %s\n' "$(docker --version)"
printf 'Database URL: %s\n' "$DATABASE_URL"
printf 'Bootstrap user configured: yes\n'
printf 'Sending controls: enabled=false dry-run=true daily-limit=0 kill-switch=true\n'
