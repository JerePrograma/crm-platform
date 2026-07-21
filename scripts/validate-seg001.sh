#!/usr/bin/env bash
set -Eeuo pipefail

postgres_port=55432
backend_port=8080
frontend_port=5173
keep_running=false
use_build_cache=false

usage() {
  cat <<'EOF'
Usage: bash scripts/validate-seg001.sh [options]

Options:
  --postgres-port PORT   PostgreSQL host port (default: 55432)
  --backend-port PORT    Backend host port (default: 8080)
  --frontend-port PORT   Frontend host port (default: 5173)
  --keep-running         Leave the Compose app stack running
  --use-build-cache      Permit cached image builds; not valid as closure evidence
  -h, --help             Show this help
EOF
}

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

validate_port() {
  local name=$1
  local value=$2
  [[ $value =~ ^[0-9]+$ ]] || fail "$name must be an integer"
  (( value >= 1 && value <= 65535 )) || fail "$name must be between 1 and 65535"
}

while (($# > 0)); do
  case "$1" in
    --postgres-port)
      (($# >= 2)) || fail '--postgres-port requires a value'
      postgres_port=$2
      shift 2
      ;;
    --backend-port)
      (($# >= 2)) || fail '--backend-port requires a value'
      backend_port=$2
      shift 2
      ;;
    --frontend-port)
      (($# >= 2)) || fail '--frontend-port requires a value'
      frontend_port=$2
      shift 2
      ;;
    --keep-running)
      keep_running=true
      shift
      ;;
    --use-build-cache)
      use_build_cache=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

validate_port POSTGRES_HOST_PORT "$postgres_port"
validate_port BACKEND_HOST_PORT "$backend_port"
validate_port FRONTEND_HOST_PORT "$frontend_port"
[[ $postgres_port != "$backend_port" && $postgres_port != "$frontend_port" && $backend_port != "$frontend_port" ]] \
  || fail 'PostgreSQL, backend and frontend host ports must be different'

for command_name in git docker tee date; do
  command -v "$command_name" >/dev/null 2>&1 || fail "Required command not found: $command_name"
done

docker info >/dev/null 2>&1 || fail 'Docker daemon is not reachable'
docker compose version >/dev/null 2>&1 || fail 'Docker Compose v2 is required'

repo_root=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
output_directory="$repo_root/validation-output"
mkdir -p "$output_directory"
timestamp=$(date -u +%Y%m%d-%H%M%S)
transcript_path="$output_directory/seg001-complete-$timestamp.log"
summary_path="$output_directory/seg001-complete-$timestamp.json"

status=RUNNING
started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
finished_at=''
commit=''
branch=''
failure_line=''
failure_command=''
lockfile_sha256=''
stack_kept_running=false
phase_tree_clean=NOT_RUN
phase_docker_stack=NOT_RUN
phase_backend_verify=NOT_RUN
phase_lockfile=NOT_RUN
phase_frontend_npm_ci=NOT_RUN
phase_final_smoke_host=NOT_RUN
phase_final_smoke_container=NOT_RUN
phase_repository_safety=NOT_RUN

json_escape() {
  local value=${1-}
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  value=${value//$'\n'/\\n}
  value=${value//$'\r'/\\r}
  value=${value//$'\t'/\\t}
  printf '%s' "$value"
}

write_summary() {
  local clean_builds=true
  [[ $use_build_cache == false ]] || clean_builds=false
  cat >"$summary_path" <<EOF
{
  "schemaVersion": 1,
  "validation": "SEG-001 complete local validation",
  "status": "$(json_escape "$status")",
  "startedAtUtc": "$(json_escape "$started_at")",
  "finishedAtUtc": "$(json_escape "$finished_at")",
  "commit": "$(json_escape "$commit")",
  "branch": "$(json_escape "$branch")",
  "ports": {
    "postgres": $postgres_port,
    "backend": $backend_port,
    "frontend": $frontend_port
  },
  "cleanBuilds": $clean_builds,
  "phases": {
    "trackedTreeClean": "$(json_escape "$phase_tree_clean")",
    "dockerStack": "$(json_escape "$phase_docker_stack")",
    "backendMavenVerify": "$(json_escape "$phase_backend_verify")",
    "lockfileGeneration": "$(json_escape "$phase_lockfile")",
    "frontendNpmCiBuild": "$(json_escape "$phase_frontend_npm_ci")",
    "finalSmokeHost": "$(json_escape "$phase_final_smoke_host")",
    "finalSmokeContainer": "$(json_escape "$phase_final_smoke_container")",
    "repositorySafety": "$(json_escape "$phase_repository_safety")"
  },
  "lockfile": {
    "path": "frontend/package-lock.json",
    "sha256": "$(json_escape "$lockfile_sha256")"
  },
  "transcript": "$(json_escape "$transcript_path")",
  "stackKeptRunning": $stack_kept_running,
  "error": {
    "line": "$(json_escape "$failure_line")",
    "command": "$(json_escape "$failure_command")"
  }
}
EOF
}

wait_service_health() {
  local service=$1
  local attempts=${2:-60}
  local attempt container_id health_state
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    container_id=$(docker compose --profile app ps -q "$service")
    if [[ -n $container_id ]]; then
      health_state=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")
      printf '%s health: %s\n' "$service" "$health_state"
      case "$health_state" in
        healthy)
          return 0
          ;;
        exited|dead|unhealthy)
          fail "$service entered terminal state: $health_state"
          ;;
      esac
    fi
    sleep 5
  done
  fail "Timed out waiting for $service to become healthy"
}

calculate_sha256() {
  local file=$1
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  else
    fail 'sha256sum or shasum is required to hash the lockfile'
  fi
}

on_error() {
  local exit_code=$?
  failure_line=${BASH_LINENO[0]:-${LINENO}}
  failure_command=${BASH_COMMAND:-unknown}
  status=FAIL
  return "$exit_code"
}

on_exit() {
  local exit_code=$?
  trap - ERR EXIT

  if ((exit_code != 0)); then
    status=FAIL
    printf 'Complete SEG-001 validation failed.\n' >&2
    docker compose --profile app --profile smoke ps || true
    docker compose --profile app --profile smoke logs --no-color || true
  fi

  if [[ $keep_running == true ]]; then
    stack_kept_running=true
    printf 'Stack left running because --keep-running was specified.\n'
  else
    docker compose --profile app --profile smoke down --remove-orphans || true
  fi

  finished_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  write_summary
  printf 'Structured evidence written: %s\n' "$summary_path"
  printf 'Transcript written: %s\n' "$transcript_path"
  popd >/dev/null || true
  exit "$exit_code"
}

pushd "$repo_root" >/dev/null
exec > >(tee -a "$transcript_path") 2>&1
trap on_error ERR
trap on_exit EXIT

commit=$(git rev-parse HEAD)
branch=$(git rev-parse --abbrev-ref HEAD)
[[ $branch == main ]] || fail "Validation must run from main; current branch is $branch"

unexpected_before=$(git status --porcelain --untracked-files=all | grep -Ev '^\?\? frontend/package-lock\.json$' || true)
[[ -z $unexpected_before ]] || fail "Working tree contains unexpected changes before validation:\n$unexpected_before"
phase_tree_clean=PASS

printf 'Complete SEG-001 validation started.\n'
printf 'Commit: %s\n' "$commit"
printf 'Transcript: %s\n' "$transcript_path"
printf 'Structured evidence: %s\n' "$summary_path"

sh scripts/set-local-host-ports.sh "$postgres_port" "$backend_port" "$frontend_port"
sh scripts/preflight.sh --container-only
docker compose --profile app --profile smoke config --quiet
docker compose --profile app --profile smoke down --remove-orphans

build_arguments=(compose --progress plain --profile app build)
if [[ $use_build_cache == false ]]; then
  build_arguments+=(--no-cache)
fi

docker "${build_arguments[@]}" frontend
docker "${build_arguments[@]}" backend
docker compose --profile app up -d
wait_service_health postgres
wait_service_health backend
wait_service_health frontend
docker compose --profile app ps
sh scripts/smoke-test.sh
docker compose --profile app --profile smoke run --rm smoke
phase_docker_stack=PASS

sh scripts/verify-backend-container.sh
phase_backend_verify=PASS

sh scripts/generate-frontend-lock.sh
lockfile_path="$repo_root/frontend/package-lock.json"
[[ -f $lockfile_path ]] || fail 'frontend/package-lock.json was not generated'
lockfile_sha256=$(calculate_sha256 "$lockfile_path")
phase_lockfile=PASS

docker "${build_arguments[@]}" frontend
phase_frontend_npm_ci=PASS

docker compose --profile app up -d --no-deps --force-recreate frontend
wait_service_health frontend
sh scripts/smoke-test.sh
phase_final_smoke_host=PASS
docker compose --profile app --profile smoke run --rm smoke
phase_final_smoke_container=PASS

sh scripts/check-repository-safety.sh
unexpected_after=$(git status --porcelain --untracked-files=all | grep -Ev '^(\?\?| M|M ) frontend/package-lock\.json$' || true)
[[ -z $unexpected_after ]] || fail "Unexpected changes after validation:\n$unexpected_after"
phase_repository_safety=PASS

status=PASS
printf 'Complete SEG-001 validation passed.\n'
printf 'The generated package-lock.json remains uncommitted for review.\n'
