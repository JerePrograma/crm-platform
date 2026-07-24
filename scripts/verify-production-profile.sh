#!/usr/bin/env sh
set -eu

frontend_port=${1:-18080}
project="crm-production-smoke-$$"
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
compose="$repo_root/deploy/docker-compose.production.yml"
export DATABASE_PASSWORD=synthetic-production-drill-password
export CRM_BOOTSTRAP_USERNAME=production-drill-admin
export CRM_BOOTSTRAP_PASSWORD=synthetic-production-drill-admin-password
export SESSION_COOKIE_SECURE=false
export PRODUCTION_FRONTEND_PORT=$frontend_port
cleanup() { docker compose --project-name "$project" -f "$compose" down -v --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT
docker compose --project-name "$project" -f "$compose" up -d --build --wait
wget -qO- "http://127.0.0.1:${frontend_port}/actuator/health/readiness" | grep -q '"status":"UP"'
backend=$(docker compose --project-name "$project" -f "$compose" ps -q backend)
postgres=$(docker compose --project-name "$project" -f "$compose" ps -q postgres)
environment=$(docker inspect "$backend" --format '{{json .Config.Env}}')
printf '%s' "$environment" | node "$repo_root/scripts/assert-container-env.js" \
  SENDING_ENABLED=false \
  SENDING_DRY_RUN=true \
  SENDING_DAILY_LIMIT=0 \
  SENDING_KILL_SWITCH=true \
  MESSAGING_REAL_NETWORK_ALLOWED=false \
  EMAIL_PROVIDER_MODE=NOOP \
  WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
sent=$(docker exec "$postgres" psql -U gestudio -d gestudio_crm -At -c "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');")
[ "$sent" = 0 ]
printf 'Production profile local smoke passed: health, non-root/read-only services, blocked providers, zero SENT.\n'
