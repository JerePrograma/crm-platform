#!/usr/bin/env sh
set -eu

port=${1:-55432}
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_path="$repo_root/.env"

[ -f "$env_path" ] || {
  printf 'ERROR: .env is missing. Copy .env.example to .env first.\n' >&2
  exit 1
}

backend_port=$(awk -F= '/^BACKEND_HOST_PORT=/{print substr($0, index($0, "=") + 1); exit}' "$env_path")
frontend_port=$(awk -F= '/^FRONTEND_HOST_PORT=/{print substr($0, index($0, "=") + 1); exit}' "$env_path")
backend_port=${backend_port:-8080}
frontend_port=${frontend_port:-5173}

exec "$repo_root/scripts/set-local-host-ports.sh" "$port" "$backend_port" "$frontend_port"
