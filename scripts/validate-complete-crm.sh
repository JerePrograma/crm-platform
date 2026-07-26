#!/usr/bin/env bash
set -Eeuo pipefail

postgres_port=25432
backend_port=8080
frontend_port=5173
production_frontend_port=18080
while (($#)); do
  case "$1" in
    --postgres-port) postgres_port=$2; shift 2 ;;
    --backend-port) backend_port=$2; shift 2 ;;
    --frontend-port) frontend_port=$2; shift 2 ;;
    --production-frontend-port) production_frontend_port=$2; shift 2 ;;
    *) printf 'Unknown argument: %s\n' "$1" >&2; exit 1 ;;
  esac
done
repo_root=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"
output="validation-output"
mkdir -p "$output"
stamp=$(date -u +%Y%m%d-%H%M%S)
export COMPOSE_PROJECT_NAME="crm-complete-unix-$$-$stamp"
log="$output/complete-crm-unix-$stamp.log"
json="$output/complete-crm-unix-$stamp.json"
status=EXECUTED_FAIL
phase=preflight
cleanup() {
  code=$?
  docker compose --profile app --profile smoke down -v --remove-orphans >/dev/null 2>&1 || true
  finished=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  printf '{"schemaVersion":1,"validation":"COMPLETE CRM Unix functional validation","status":"%s","phase":"%s","commit":"%s","finishedAtUtc":"%s","transcript":"%s"}\n' "$status" "$phase" "$(git rev-parse HEAD 2>/dev/null || true)" "$finished" "$log" > "$json"
  exit "$code"
}
trap cleanup EXIT
exec > >(tee "$log") 2>&1

[ "$(git branch --show-current)" = main ]
[ -z "$(git status --porcelain)" ]
for command in git docker node npm mvn bash; do command -v "$command" >/dev/null; done
phase=host-port-preflight
node scripts/check-host-ports.js \
  "PostgreSQL=$postgres_port" \
  "Backend=$backend_port" \
  "Frontend=$frontend_port" \
  "Production frontend=$production_frontend_port"
phase=repository-safety
bash scripts/check-repository-safety.sh
phase=script-syntax
for script in scripts/*.sh; do bash -n "$script"; done
node scripts/test-container-env-assertions.js
node scripts/test-check-host-ports.js
phase=backend
bash scripts/verify-backend-container.sh
phase=frontend
(cd frontend && npm ci --no-audit --no-fund && npm run typecheck && npm run test:unit && npm run build)
export POSTGRES_HOST_PORT=$postgres_port BACKEND_HOST_PORT=$backend_port FRONTEND_HOST_PORT=$frontend_port
export CRM_BOOTSTRAP_USERNAME=complete-admin CRM_BOOTSTRAP_PASSWORD=complete-admin-password
export SENDING_ENABLED=false SENDING_DRY_RUN=true SENDING_DAILY_LIMIT=0 SENDING_KILL_SWITCH=true MESSAGING_REAL_NETWORK_ALLOWED=false
export EMAIL_PROVIDER_MODE=NOOP WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY OUTBOX_WORKER_ENABLED=false FAKE_INBOUND_ENABLED=true
export FAKE_INBOUND_WEBHOOK_SECRET=synthetic-complete-crm-inbound-secret
phase=compose-smoke
bash scripts/set-local-host-ports.sh "$postgres_port" "$backend_port" "$frontend_port"
docker compose --profile app build --no-cache backend frontend
docker compose --profile app up -d --wait
bash scripts/smoke-test.sh
backend_container=$(docker compose --profile app ps -q backend)
backend_image=$(docker inspect "$backend_container" --format '{{.Config.Image}}')
environment=$(docker inspect "$backend_container" --format '{{json .Config.Env}}')
printf '%s' "$environment" | node scripts/assert-container-env.js \
  SENDING_ENABLED=false \
  SENDING_DRY_RUN=true \
  SENDING_DAILY_LIMIT=0 \
  SENDING_KILL_SWITCH=true \
  MESSAGING_REAL_NETWORK_ALLOWED=false \
  EMAIL_PROVIDER_MODE=NOOP \
  WHATSAPP_PROVIDER_MODE=DEEPLINK_ONLY
phase=dependency-scan
(cd frontend && npm audit --audit-level=high)
docker volume create crm_grype_cache >/dev/null
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v crm_grype_cache:/root/.cache/grype \
  anchore/grype@sha256:fd4ab4d1042b522c896e73bdf09ab8bf384fa417df99d6dd0d6e1008c7e7c821 \
  "$backend_image" --fail-on high
phase=migrations
bash scripts/verify-migrations.sh "$backend_image"
phase=e2e
(cd frontend && CRM_E2E_BASE_URL="http://127.0.0.1:$frontend_port" CRM_E2E_USERNAME=complete-admin CRM_E2E_PASSWORD=complete-admin-password CRM_E2E_INBOUND_SECRET=synthetic-complete-crm-inbound-secret npm run test:e2e)
phase=zero-sent
postgres=$(docker compose ps -q postgres)
database=$(docker exec "$postgres" printenv POSTGRES_DB)
database_user=$(docker exec "$postgres" printenv POSTGRES_USER)
[ "$(docker exec "$postgres" psql -U "$database_user" -d "$database" -At -c "SELECT count(*) FROM message_record WHERE status IN ('SENT','DELIVERED','READ');")" = 0 ]
phase=backup-restore
bash scripts/verify-backup-restore.sh
phase=production-profile
bash scripts/verify-production-profile.sh "$production_frontend_port"
phase=final-safety
bash scripts/check-repository-safety.sh
[ -z "$(git status --porcelain)" ]
status=FUNCTIONAL_PASS
