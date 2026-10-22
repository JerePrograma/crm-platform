#!/usr/bin/env sh
set -eu
umask 077
state_dir=${CRM_STATE_DIRECTORY:-/srv/apps/crm/state}
health_url=${CRM_HEALTH_URL:-https://crm.scalaris.com.ar/actuator/health/readiness}
[ -d "$state_dir" ] || { printf 'CRM state directory is missing.\n' >&2; exit 1; }
case "$health_url" in https://*) ;; *) printf 'The operational health URL must use HTTPS.\n' >&2; exit 1 ;; esac
status=unhealthy
if response=$(curl --fail --silent --show-error --connect-timeout 5 --max-time 20 "$health_url") &&
  printf '%s' "$response" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
  status=healthy
fi
printf '{"service":"scalaris-crm","status":"%s","checkedAtUtc":"%s"}\n' "$status" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$state_dir/health.json.partial"
mv "$state_dir/health.json.partial" "$state_dir/health.json"
printf 'Scalaris CRM health: %s\n' "$status"
# Alert delivery is opt-in and remains disabled unless the operator explicitly
# configures an approved endpoint in the private operations file.
if [ "$status" = unhealthy ] && [ "${CRM_ALERTING_ENABLED:-false}" = true ]; then
  case "${CRM_ALERT_WEBHOOK_URL:-}" in https://*) ;; *) printf 'An approved HTTPS alert endpoint is required.\n' >&2; exit 1 ;; esac
  curl --fail --silent --show-error --connect-timeout 5 --max-time 15 \
    -H 'Content-Type: application/json' --data '{"service":"scalaris-crm","status":"unhealthy"}' \
    "$CRM_ALERT_WEBHOOK_URL" >/dev/null
fi
[ "$status" = healthy ]
