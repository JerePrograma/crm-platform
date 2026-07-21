#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "Docker is required"
command -v id >/dev/null 2>&1 || fail "id is required to preserve file ownership"

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
frontend_path="$repository_root/frontend"
package_json="$frontend_path/package.json"
lockfile="$frontend_path/package-lock.json"

[ -f "$package_json" ] || fail "package.json not found at $package_json"
[ ! -d "$frontend_path/node_modules" ] \
  || fail "frontend/node_modules already exists; remove it before generating a clean lockfile"

host_uid=$(id -u)
host_gid=$(id -g)

docker run --rm \
  --user "$host_uid:$host_gid" \
  --env HOME=/tmp/npm-home \
  --env npm_config_cache=/tmp/npm-cache \
  --mount "type=bind,source=$frontend_path,target=/workspace/frontend" \
  --workdir /workspace/frontend \
  node:22-alpine \
  npm install --package-lock-only --ignore-scripts --no-audit --no-fund

[ -f "$lockfile" ] || fail "package-lock.json was not generated"
[ ! -d "$frontend_path/node_modules" ] || fail "node_modules was unexpectedly created"
[ -w "$lockfile" ] || fail "package-lock.json is not writable by the current user"

printf 'Frontend lockfile generated: %s\n' "$lockfile"
printf 'Ownership preserved for UID:GID %s:%s.\n' "$host_uid" "$host_gid"
printf 'No package lifecycle scripts were executed and node_modules was not created.\n'
printf 'Review the lockfile before committing.\n'
