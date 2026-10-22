#!/usr/bin/env sh
set -eu
repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
temporary=$(mktemp -d "${TMPDIR:-/tmp}/crm-database-test.XXXXXX")
cleanup() {
  case "$temporary" in "${TMPDIR:-/tmp}"/crm-database-test.*) rm -rf "$temporary" ;; esac
}
trap cleanup EXIT
mkdir -p "$temporary/bin" "$temporary/backup files"
export CRM_DATABASE_TEST_ROOT=$temporary
export PATH="$temporary/bin:$PATH"
unset POSTGRES_DB DATABASE_USER
cat > "$temporary/bin/docker" <<'MOCK'
#!/usr/bin/env sh
set -eu
case "$1" in
  compose) printf '%s\n' "$@" > "$CRM_DATABASE_TEST_ROOT/compose-arguments"; printf 'synthetic-db\n' ;;
  cp)
    case "$2" in synthetic-db:*)
      [ ! -f "$CRM_DATABASE_TEST_ROOT/fail-copy" ] || exit 1
      printf 'PGDMP-synthetic-not-a-real-backup' > "$3" ;;
    esac ;;
  exec)
    [ "$2" = synthetic-db ] || exit 9
    shift 2
    case "$1" in
      printenv) case "$2" in POSTGRES_DB) printf 'synthetic_crm\n' ;; *) printf 'synthetic_user\n' ;; esac ;;
      dropdb) printf '%s\n' "$@" >> "$CRM_DATABASE_TEST_ROOT/dropped-databases" ;;
      createdb)
        for argument in "$@"; do name=$argument; done
        printf '%s\n' "$name" >> "$CRM_DATABASE_TEST_ROOT/created-databases"
        if [ -f "$CRM_DATABASE_TEST_ROOT/fail-createdb" ]; then
          stage=$(cat "$CRM_DATABASE_TEST_ROOT/fail-createdb")
          case "$stage:$name" in source:crm_restore_source_*|target:crm_restore_target_*) exit 1 ;; esac
        fi ;;
      psql)
        for argument in "$@"; do
          case "$argument" in 'SELECT value'*) printf 'SYNTHETIC_BACKUP_RESTORE_PROBE\n' ;; 'SELECT version'*) printf '14\n' ;; esac
        done ;;
    esac ;;
esac
MOCK
chmod +x "$temporary/bin/docker"
compose="$temporary/selected compose.yml"
env_file="$temporary/private synthetic.env"
printf 'services: {}\n' > "$compose"
printf '# synthetic only\n' > "$env_file"
backup_dir="$temporary/backup files"
sh "$repo_root/scripts/backup-database.sh" "$backup_dir" --compose-file "$compose" --project-name selected-synthetic-project --env-file "$env_file"
printf '%s\n' compose --file "$compose" --project-name selected-synthetic-project --env-file "$env_file" ps -q postgres > "$temporary/expected-arguments"
cmp "$temporary/expected-arguments" "$temporary/compose-arguments"
# Missing parents must not bypass the repository boundary, including dot and
# symlink paths. These checks must fail before mkdir or Docker selection.
for forbidden in "$repo_root/new-private-parent-$$/nested backups" "$repo_root/new-private-parent-$$/../another-new-parent-$$/backups"; do
  if output=$(sh "$repo_root/scripts/backup-database.sh" "$forbidden" 2>&1); then printf 'An in-repository backup path was accepted.\n' >&2; exit 1; fi
  printf '%s' "$output" | grep -q 'outside the repository'
done
ln -s "$repo_root" "$temporary/repository link"
if output=$(sh "$repo_root/scripts/backup-database.sh" "$temporary/repository link/new-parent/backups" 2>&1); then printf 'A symlink into the repository was accepted.\n' >&2; exit 1; fi
printf '%s' "$output" | grep -q 'outside the repository'
[ ! -e "$repo_root/new-private-parent-$$" ]
[ ! -e "$repo_root/another-new-parent-$$" ]
set -- "$backup_dir"/*.dump
backup=$1
[ "$#" -eq 1 ] && [ -s "$backup" ] && [ -s "$backup.sha256" ]
grep -q '"restoreTested":false' "$backup.json"
touch "$temporary/fail-copy"
if sh "$repo_root/scripts/backup-database.sh" "$backup_dir" --compose-file "$compose" --project-name selected-synthetic-project --env-file "$env_file"; then printf 'Failed copy was accepted.\n' >&2; exit 1; fi
set -- "$backup_dir"/*.dump
[ "$#" -eq 1 ]
set -- "$backup_dir"/*.partial
[ ! -e "$1" ]
rm "$temporary/fail-copy"
new_outside="$temporary/nonexistent parent/deeper private backups"
sh "$repo_root/scripts/backup-database.sh" "$new_outside" --compose-file "$compose" --project-name selected-synthetic-project --env-file "$env_file"
set -- "$new_outside"/*.dump
[ "$#" -eq 1 ] && [ -s "$1" ]
mv "$backup.sha256" "$temporary/saved.sha256"
if sh "$repo_root/scripts/restore-database.sh" "$backup" crm_restore_test --confirm-destructive-restore; then printf 'Missing checksum was accepted.\n' >&2; exit 1; fi
printf '%064d\n' 0 > "$backup.sha256"
if sh "$repo_root/scripts/restore-database.sh" "$backup" crm_restore_test --confirm-destructive-restore; then printf 'Invalid checksum was accepted.\n' >&2; exit 1; fi
mv "$temporary/saved.sha256" "$backup.sha256"
sh "$repo_root/scripts/restore-database.sh" "$backup" crm_restore_test --confirm-destructive-restore --compose-file "$compose" --project-name selected-synthetic-project --env-file "$env_file"
grep -qx crm_restore_test "$temporary/dropped-databases"
if sh "$repo_root/scripts/restore-database.sh" "$backup" 'crm_restore_test;invalid' --confirm-destructive-restore; then printf 'Unsafe target was accepted.\n' >&2; exit 1; fi
sh "$repo_root/scripts/verify-backup-restore.sh"
printf '%s\n' compose ps -q postgres > "$temporary/expected-arguments"
cmp "$temporary/expected-arguments" "$temporary/compose-arguments"
first_source=$(grep '^crm_restore_source_' "$temporary/created-databases" | tail -1)
printf '%s\n' "$first_source" | grep -Eq '^crm_restore_source_[0-9]+_[a-f0-9]{32}$'
[ "${#first_source}" -le 63 ]
sh "$repo_root/scripts/verify-backup-restore.sh"
second_source=$(grep '^crm_restore_source_' "$temporary/created-databases" | tail -1)
[ "$first_source" != "$second_source" ]
rm "$temporary/dropped-databases"
printf 'source\n' > "$temporary/fail-createdb"
if sh "$repo_root/scripts/verify-backup-restore.sh"; then printf 'Failed source creation was accepted.\n' >&2; exit 1; fi
[ ! -e "$temporary/dropped-databases" ]
printf 'target\n' > "$temporary/fail-createdb"
if sh "$repo_root/scripts/verify-backup-restore.sh"; then printf 'Failed target creation was accepted.\n' >&2; exit 1; fi
owned_source=$(grep '^crm_restore_source_' "$temporary/created-databases" | tail -1)
grep -qxF "$owned_source" "$temporary/dropped-databases"
if grep -q '^crm_restore_target_' "$temporary/dropped-databases"; then printf 'Cleanup attempted an unowned target.\n' >&2; exit 1; fi
printf 'Database operation regressions passed: explicit project, canonical paths without parents, spaces, incomplete copy, checksum, unique drill names and ownership-aware cleanup; Docker mocked.\n'
