#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

[ -f .env ] || fail ".env is missing"
command -v curl >/dev/null 2>&1 || fail "curl is required"

set -a
# shellcheck disable=SC1091
. ./.env
set +a

BACKEND_URL=${BACKEND_URL:-http://localhost:8080}
FRONTEND_URL=${FRONTEND_URL:-http://localhost:5173}

[ -n "${CRM_BOOTSTRAP_USERNAME:-}" ] || fail "CRM_BOOTSTRAP_USERNAME is required"
[ -n "${CRM_BOOTSTRAP_PASSWORD:-}" ] || fail "CRM_BOOTSTRAP_PASSWORD is required"

health_response=$(curl --fail --silent --show-error "$BACKEND_URL/actuator/health")
printf '%s' "$health_response" | grep -q '"status":"UP"' \
  || fail "Backend health response is not UP: $health_response"

prospects_response=$(curl --fail --silent --show-error \
  --user "$CRM_BOOTSTRAP_USERNAME:$CRM_BOOTSTRAP_PASSWORD" \
  "$BACKEND_URL/api/v1/prospects?size=1")
printf '%s' "$prospects_response" | grep -q '"content"' \
  || fail "Authenticated prospects response does not contain a page"

frontend_response=$(curl --fail --silent --show-error "$FRONTEND_URL/")
printf '%s' "$frontend_response" | grep -qi '<div id="root"></div>' \
  || fail "Frontend root document was not served"

printf 'Smoke test passed.\n'
printf 'Backend health: UP\n'
printf 'Authenticated API: reachable\n'
printf 'Frontend: reachable\n'
printf 'Sending controls remain configuration-only; this test performs no communications.\n'
