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

BACKEND_HOST_PORT=${BACKEND_HOST_PORT:-8080}
FRONTEND_HOST_PORT=${FRONTEND_HOST_PORT:-5173}
BACKEND_URL=${BACKEND_URL:-http://localhost:$BACKEND_HOST_PORT}
FRONTEND_URL=${FRONTEND_URL:-http://localhost:$FRONTEND_HOST_PORT}

[ -n "${CRM_BOOTSTRAP_USERNAME:-}" ] || fail "CRM_BOOTSTRAP_USERNAME is required"
[ -n "${CRM_BOOTSTRAP_PASSWORD:-}" ] || fail "CRM_BOOTSTRAP_PASSWORD is required"

health_response=$(curl --fail --silent --show-error "$BACKEND_URL/actuator/health")
printf '%s' "$health_response" | grep -q '"status":"UP"' \
  || fail "Backend health response is not UP: $health_response"

cookie_jar=${TMPDIR:-/tmp}/gestudio-crm-smoke-cookies-$$
trap 'rm -f "$cookie_jar"' EXIT HUP INT TERM
csrf_response=$(curl --fail --silent --show-error --cookie-jar "$cookie_jar" \
  "$BACKEND_URL/api/v1/auth/csrf")
csrf_token=$(printf '%s' "$csrf_response" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
csrf_header=$(printf '%s' "$csrf_response" | sed -n 's/.*"headerName":"\([^"]*\)".*/\1/p')
[ -n "$csrf_token" ] || fail "CSRF endpoint did not return a token"
[ -n "$csrf_header" ] || fail "CSRF endpoint did not return a header name"
login_body=$(printf '{"username":"%s","password":"%s"}' \
  "$CRM_BOOTSTRAP_USERNAME" "$CRM_BOOTSTRAP_PASSWORD")
curl --fail --silent --show-error --cookie "$cookie_jar" --cookie-jar "$cookie_jar" \
  --header "$csrf_header: $csrf_token" --header 'Content-Type: application/json' \
  --data "$login_body" "$BACKEND_URL/api/v1/auth/login" >/dev/null
prospects_response=$(curl --fail --silent --show-error --cookie "$cookie_jar" \
  "$BACKEND_URL/api/v1/prospects?size=1")
printf '%s' "$prospects_response" | grep -q '"content"' \
  || fail "Authenticated prospects response does not contain a page"

frontend_response=$(curl --fail --silent --show-error "$FRONTEND_URL/")
printf '%s' "$frontend_response" | grep -qi '<div id="root"></div>' \
  || fail "Frontend root document was not served"

printf 'Smoke test passed.\n'
printf 'Backend URL: %s\n' "$BACKEND_URL"
printf 'Frontend URL: %s\n' "$FRONTEND_URL"
printf 'Backend health: UP\n'
printf 'Authenticated API: reachable\n'
printf 'Frontend: reachable\n'
printf 'Sending controls remain configuration-only; this test performs no communications.\n'
