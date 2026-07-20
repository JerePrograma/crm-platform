#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "Docker is required"

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
frontend_path="$repository_root/frontend"
package_json="$frontend_path/package.json"
lockfile="$frontend_path/package-lock.json"

[ -f "$package_json" ] || fail "package.json not found at $package_json"

docker run --rm \
  -v "$frontend_path:/workspace/frontend" \
  -w /workspace/frontend \
  node:22-alpine \
  npm install --no-audit --no-fund

[ -f "$lockfile" ] || fail "package-lock.json was not generated"

printf 'Frontend lockfile generated: %s\n' "$lockfile"
printf 'Review it with git diff before committing.\n'
