#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f dist/mathpath.jar ]]; then bash scripts/build.sh; fi
java -jar dist/mathpath.jar "$@"
