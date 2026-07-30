#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
: "${STEAM_APP_ID:?Set STEAM_APP_ID}"
: "${STEAM_DEPOT_ID:?Set STEAM_DEPOT_ID for this platform}"
: "${STEAM_USERNAME:?Set STEAM_USERNAME}"
: "${STEAM_CONTENT_DIR:?Set STEAM_CONTENT_DIR to the packaged platform directory}"

steamcmd_bin=${STEAMCMD_BIN:-$(command -v steamcmd || true)}
if [[ -z "$steamcmd_bin" || ! -x "$steamcmd_bin" ]]; then
    echo "steamcmd was not found. Set STEAMCMD_BIN." >&2
    exit 1
fi
if [[ ! -d "$STEAM_CONTENT_DIR" ]]; then
    echo "Steam content directory does not exist: $STEAM_CONTENT_DIR" >&2
    exit 1
fi

build_dir="$repo_dir/target/steam"
output_dir="$build_dir/output"
depot_vdf="$build_dir/depot-$STEAM_DEPOT_ID.vdf"
app_vdf="$build_dir/app-$STEAM_APP_ID.vdf"
mkdir -p "$build_dir" "$output_dir"

cat > "$depot_vdf" <<EOF
"DepotBuildConfig"
{
    "DepotID" "$STEAM_DEPOT_ID"
    "ContentRoot" "$STEAM_CONTENT_DIR"
    "FileMapping"
    {
        "LocalPath" "*"
        "DepotPath" "."
        "recursive" "1"
    }
}
EOF

cat > "$app_vdf" <<EOF
"AppBuild"
{
    "AppID" "$STEAM_APP_ID"
    "Desc" "${STEAM_BUILD_DESCRIPTION:-Rogue Circuit build}"
    "BuildOutput" "$output_dir"
    "ContentRoot" "$STEAM_CONTENT_DIR"
    "SetLive" "${STEAM_BRANCH:-}"
    "Depots"
    {
        "$STEAM_DEPOT_ID" "$depot_vdf"
    }
}
EOF

"$steamcmd_bin" \
    +login "$STEAM_USERNAME" \
    +run_app_build "$app_vdf" \
    +quit
