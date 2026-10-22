#!/usr/bin/env sh

# Sourced by database operations. Explicit options override Compose's normal
# COMPOSE_FILE / COMPOSE_PROJECT_NAME environment without evaluating strings.
normalize_database_directory() (
  # Resolve symlinks and dot components without requiring missing parents or
  # GNU realpath -m (which is absent on standard macOS installations).
  [ -n "$1" ] || { printf 'A backup directory is required.\n' >&2; exit 1; }
  case "$1" in /*) remaining=${1#/} ;; *) remaining="$(pwd -P)/$1"; remaining=${remaining#/} ;; esac
  resolved=/
  links=0
  while [ -n "$remaining" ]; do
    component=${remaining%%/*}
    case "$remaining" in */*) remaining=${remaining#*/} ;; *) remaining= ;; esac
    case "$component" in
      ''|.) ;;
      ..) resolved=${resolved%/*}; [ -n "$resolved" ] || resolved=/ ;;
      *)
        candidate="${resolved%/}/$component"
        if [ -L "$candidate" ]; then
          links=$((links + 1))
          [ "$links" -le 40 ] || { printf 'Too many symbolic links in backup directory.\n' >&2; exit 1; }
          link=$(readlink "$candidate") || exit 1
          case "$link" in /*) resolved=/; link=${link#/} ;; esac
          remaining="$link${remaining:+/$remaining}"
        elif [ -d "$candidate" ]; then
          resolved=$(CDPATH= cd -- "$candidate" && pwd -P) || exit 1
        else
          resolved=$candidate
        fi ;;
    esac
  done
  printf '%s\n' "$resolved"
)

read_database_compose_options() {
  db_compose_file=
  db_compose_project=
  db_compose_env_file=
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --compose-file|--project-name|--env-file)
        [ "$#" -ge 2 ] && [ -n "$2" ] || { printf 'A value is required for %s.\n' "$1" >&2; return 1; }
        case "$1" in
          --compose-file) db_compose_file=$2 ;;
          --project-name) db_compose_project=$2 ;;
          --env-file) db_compose_env_file=$2 ;;
        esac
        shift 2 ;;
      *) printf 'Unknown database operation option: %s\n' "$1" >&2; return 1 ;;
    esac
  done
  [ -z "$db_compose_file" ] || [ -f "$db_compose_file" ] || { printf 'Compose file not found.\n' >&2; return 1; }
  [ -z "$db_compose_env_file" ] || [ -f "$db_compose_env_file" ] || { printf 'Compose environment file not found.\n' >&2; return 1; }
  if [ -n "$db_compose_project" ]; then
    case "$db_compose_project" in
      *[!a-z0-9_-]*|[-_]*) printf 'Invalid Compose project name.\n' >&2; return 1 ;;
    esac
  fi
}

get_database_container() {
  set -- compose
  [ -z "$db_compose_file" ] || set -- "$@" --file "$db_compose_file"
  [ -z "$db_compose_project" ] || set -- "$@" --project-name "$db_compose_project"
  [ -z "$db_compose_env_file" ] || set -- "$@" --env-file "$db_compose_env_file"
  db_container=$(docker "$@" ps -q postgres) || { printf 'Unable to select PostgreSQL from the requested Compose project.\n' >&2; return 1; }
  case "$db_container" in
    ''|*[!a-zA-Z0-9_.-]*) printf 'Expected exactly one running PostgreSQL container in the requested Compose project.\n' >&2; return 1 ;;
  esac
  printf '%s\n' "$db_container"
}
