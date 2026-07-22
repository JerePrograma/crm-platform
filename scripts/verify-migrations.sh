#!/usr/bin/env sh
set -eu

backend_image=${1:-$(docker compose images -q backend)}
[ -n "$backend_image" ] || { printf 'Build the backend image before migration verification.\n' >&2; exit 1; }
suffix="$$-$(date -u +%Y%m%d%H%M%S)"
network="crm-migration-$suffix"
postgres="crm-migration-postgres-$suffix"
upgrade11="crm-migration-v11-$suffix"
upgrade_latest="crm-migration-latest-$suffix"
empty_latest="crm-migration-empty-$suffix"
password=synthetic-migration-password
cleanup() {
  for name in "$empty_latest" "$upgrade_latest" "$upgrade11" "$postgres"; do docker rm -f "$name" >/dev/null 2>&1 || true; done
  docker network rm "$network" >/dev/null 2>&1 || true
}
trap cleanup EXIT
wait_container() {
  name=$1
  attempt=0
  while [ "$attempt" -lt 90 ]; do
    state=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || true)
    [ "$state" = healthy ] && return 0
    case "$state" in unhealthy|exited|dead) docker logs "$name"; return 1 ;; esac
    sleep 2; attempt=$((attempt + 1))
  done
  return 1
}
start_backend() {
  name=$1 database=$2 target=${3:-}
  target_args=''
  [ -z "$target" ] || target_args="--env SPRING_FLYWAY_TARGET=$target"
  # shellcheck disable=SC2086
  docker run -d --name "$name" --network "$network" \
    --env "DATABASE_URL=jdbc:postgresql://${postgres}:5432/${database}" --env DATABASE_USER=gestudio --env "DATABASE_PASSWORD=$password" \
    --env CRM_BOOTSTRAP_USERNAME=migration-admin --env CRM_BOOTSTRAP_PASSWORD=synthetic-migration-admin-password \
    --env SENDING_ENABLED=false --env SENDING_DRY_RUN=true --env SENDING_DAILY_LIMIT=0 --env SENDING_KILL_SWITCH=true \
    --env MESSAGING_REAL_NETWORK_ALLOWED=false --env OUTBOX_WORKER_ENABLED=false --env FAKE_INBOUND_ENABLED=false \
    $target_args "$backend_image" >/dev/null
  wait_container "$name"
}
docker network create "$network" >/dev/null
docker run -d --name "$postgres" --network "$network" --env POSTGRES_DB=upgrade --env POSTGRES_USER=gestudio --env "POSTGRES_PASSWORD=$password" --health-cmd='pg_isready -U gestudio -d upgrade' --health-interval=2s --health-timeout=2s --health-retries=30 postgres:17-alpine >/dev/null
wait_container "$postgres"
docker exec "$postgres" createdb -h 127.0.0.1 -U gestudio empty
start_backend "$upgrade11" upgrade 11
[ "$(docker exec "$postgres" psql -h 127.0.0.1 -U gestudio -d upgrade -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;')" = 11 ]
docker rm -f "$upgrade11" >/dev/null
start_backend "$upgrade_latest" upgrade
[ "$(docker exec "$postgres" psql -h 127.0.0.1 -U gestudio -d upgrade -At -c "SELECT string_agg(version, ',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success AND installed_rank > 11;")" = '12,13' ]
start_backend "$empty_latest" empty
[ "$(docker exec "$postgres" psql -h 127.0.0.1 -U gestudio -d empty -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1;')" = 13 ]
printf 'Migration verification passed: empty -> V13, V11 -> V12 -> V13, Hibernate validate on both latest schemas.\n'
