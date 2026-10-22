#!/usr/bin/env sh
set -eu

script_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec node "$script_root/validate-scalaris-e2e.js" --frontend-port "${1:-18787}" --stack scalaris
