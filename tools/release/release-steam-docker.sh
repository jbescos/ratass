#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
: "${STEAM_APP_ID:?Set STEAM_APP_ID}"
: "${STEAM_LINUX_DEPOT_ID:?Set STEAM_LINUX_DEPOT_ID}"
: "${STEAM_WINDOWS_DEPOT_ID:?Set STEAM_WINDOWS_DEPOT_ID}"
if [[ ${STEAM_DRY_RUN:-0} != 1 ]]; then
    : "${STEAM_USERNAME:?Set STEAM_USERNAME}"
fi

if [[ ${STEAM_SKIP_BUILD:-0} != 1 ]]; then
    "$repo_dir/tools/release/package-desktop-docker.sh"
fi

"$repo_dir/tools/release/publish-steam-all.sh"
