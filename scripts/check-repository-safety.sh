#!/usr/bin/env sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || fail "Git is required"

git diff --check

for path in $(git ls-files); do
  case "$path" in
    .env|*/.env)
      fail "Local environment file is tracked: $path"
      ;;
    validation-output/*)
      fail "Local validation evidence is tracked: $path"
      ;;
    data/import/private/*)
      [ "$path" = "data/import/private/README.md" ] \
        || fail "Private import data is tracked: $path"
      ;;
    data/export/private/*)
      fail "Private export data is tracked: $path"
      ;;
    gestudio_lote_*_prospectos.xlsx|*/gestudio_lote_*_prospectos.xlsx)
      fail "Operational prospect workbook is tracked: $path"
      ;;
    *.pem|*.key|*.p12|*.pfx|*.jks)
      fail "Key or certificate file is tracked: $path"
      ;;
    *credentials*.json|*service-account*.json|*client_secret*.json)
      fail "Credential JSON file is tracked: $path"
      ;;
  esac
done

printf 'Repository safety scan passed.\n'
