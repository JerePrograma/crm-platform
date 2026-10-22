#!/usr/bin/env sh
set -eu
release_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file=${CRM_ENV_FILE:-/srv/apps/crm/config/production.env}
edge_file=${TRUSTED_EDGE_CONFIG_FILE:-/srv/apps/crm/config/nginx-trusted-edge.conf}
for command in docker curl stat python3; do command -v "$command" >/dev/null || { printf 'Missing required command: %s\n' "$command" >&2; exit 1; }; done
[ -r "$env_file" ] || { printf 'Private production environment is missing.\n' >&2; exit 1; }
case "$(stat -c '%a' "$env_file")" in 400|600) ;; *) printf 'Private production environment must have mode 400 or 600.\n' >&2; exit 1 ;; esac
docker info >/dev/null
docker network inspect proxy >/dev/null
edge_ip=$(docker inspect scalaris-proxy-caddy-1 --format '{{(index .NetworkSettings.Networks "proxy").IPAddress}}')
[ -n "$edge_ip" ] && grep -qxF "$edge_ip/32 1;" "$edge_file" || { printf 'Trusted edge file must allow only the reviewed current Caddy socket peer.\n' >&2; exit 1; }
if grep -Ev '^[[:space:]]*(#.*)?$' "$edge_file" | grep -vxF "$edge_ip/32 1;" >/dev/null; then
  printf 'The CRM edge allowlist contains additional, unreviewed peers.\n' >&2; exit 1
fi
export TRUSTED_EDGE_CONFIG_FILE=$edge_file
docker compose --env-file "$env_file" --project-name scalaris-crm \
  --file "$release_root/deploy/docker-compose.production.yml" \
  --file "$release_root/deploy/hostinger/compose.override.yml" config --quiet
# Parse the sensitive rendered model in memory; print only policy failures.
docker compose --env-file "$env_file" --project-name scalaris-crm \
  --file "$release_root/deploy/docker-compose.production.yml" \
  --file "$release_root/deploy/hostinger/compose.override.yml" config --format json | \
  python3 "$release_root/deploy/hostinger/verify-images.py"
port_owners=$(docker ps --filter publish=18095 --format '{{.Label "com.docker.compose.project"}}')
case "$port_owners" in ''|scalaris-crm) ;; *) printf 'Port 18095 is owned by another project.\n' >&2; exit 1 ;; esac
printf 'Hostinger preflight passed: project scalaris-crm, private database, loopback 18095 and reviewed Caddy peer. No services changed.\n'
