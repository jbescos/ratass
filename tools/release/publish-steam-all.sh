#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
: "${STEAM_APP_ID:?Set STEAM_APP_ID}"
: "${STEAM_LINUX_DEPOT_ID:?Set STEAM_LINUX_DEPOT_ID}"
: "${STEAM_WINDOWS_DEPOT_ID:?Set STEAM_WINDOWS_DEPOT_ID}"
for steam_id in "$STEAM_APP_ID" "$STEAM_LINUX_DEPOT_ID" "$STEAM_WINDOWS_DEPOT_ID"; do
    if [[ ! "$steam_id" =~ ^[0-9]+$ ]]; then
        echo "Steam app and depot IDs must contain only digits: $steam_id" >&2
        exit 1
    fi
done

linux_content=${STEAM_LINUX_CONTENT_DIR:-"$repo_dir/dist/desktop/linux-x86_64/RogueCircuit"}
windows_content=${STEAM_WINDOWS_CONTENT_DIR:-"$repo_dir/dist/desktop/windows-x86_64/RogueCircuit"}

if [[ ! -x "$linux_content/bin/RogueCircuit" ]]; then
    echo "Linux Steam executable is missing: $linux_content/bin/RogueCircuit" >&2
    exit 1
fi
if [[ ! -f "$windows_content/RogueCircuit.exe" ]]; then
    echo "Windows Steam executable is missing: $windows_content/RogueCircuit.exe" >&2
    exit 1
fi

build_dir="$repo_dir/target/steam"
output_dir="$build_dir/output"
linux_vdf="$build_dir/depot-$STEAM_LINUX_DEPOT_ID.vdf"
windows_vdf="$build_dir/depot-$STEAM_WINDOWS_DEPOT_ID.vdf"
app_vdf="$build_dir/app-$STEAM_APP_ID.vdf"
mkdir -p "$build_dir" "$output_dir"

write_depot() {
    local depot_id=$1
    local content_root=$2
    local output_file=$3
    cat > "$output_file" <<EOF
"DepotBuildConfig"
{
    "DepotID" "$depot_id"
    "ContentRoot" "$content_root"
    "FileMapping"
    {
        "LocalPath" "*"
        "DepotPath" "."
        "recursive" "1"
    }
}
EOF
}

write_depot "$STEAM_LINUX_DEPOT_ID" "$linux_content" "$linux_vdf"
write_depot "$STEAM_WINDOWS_DEPOT_ID" "$windows_content" "$windows_vdf"

set_live_line=
if [[ -n ${STEAM_BRANCH:-} ]]; then
    set_live_line="    \"SetLive\" \"$STEAM_BRANCH\""
fi

cat > "$app_vdf" <<EOF
"AppBuild"
{
    "AppID" "$STEAM_APP_ID"
    "Desc" "${STEAM_BUILD_DESCRIPTION:-Rogue Circuit multiplatform build}"
    "BuildOutput" "$output_dir"
    "ContentRoot" "$repo_dir"
$set_live_line
    "Depots"
    {
        "$STEAM_LINUX_DEPOT_ID" "$linux_vdf"
        "$STEAM_WINDOWS_DEPOT_ID" "$windows_vdf"
    }
}
EOF

echo "Prepared Steam build configuration: $app_vdf"
if [[ ${STEAM_DRY_RUN:-0} == 1 ]]; then
    exit 0
fi

: "${STEAM_USERNAME:?Set STEAM_USERNAME}"
default_sdk="$HOME/programs/steamSDK/sdk/tools/ContentBuilder/builder_linux/steamcmd.sh"
steamcmd_bin=${STEAMCMD_BIN:-$(command -v steamcmd || true)}
if [[ -z "$steamcmd_bin" && -f "$default_sdk" ]]; then
    staged_steamcmd="$build_dir/steamcmd"
    if [[ ! -f "$staged_steamcmd/steamcmd.sh" ]]; then
        mkdir -p "$staged_steamcmd"
        cp -a "$(dirname "$default_sdk")/." "$staged_steamcmd/"
    fi
    chmod +x \
        "$staged_steamcmd/steamcmd.sh" \
        "$staged_steamcmd/linux32/steamcmd" \
        "$staged_steamcmd/linux32/steamerrorreporter"
    steamcmd_bin="$staged_steamcmd/steamcmd.sh"
fi
if [[ -z "$steamcmd_bin" || ! -x "$steamcmd_bin" ]]; then
    echo "steamcmd was not found. Set STEAMCMD_BIN." >&2
    exit 1
fi

"$steamcmd_bin" \
    +login "$STEAM_USERNAME" \
    +run_app_build "$app_vdf" \
    +quit
