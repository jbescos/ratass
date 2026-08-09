#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
project_version=${PROJECT_VERSION:-1.0}
output_root=${DESKTOP_PACKAGE_OUTPUT:-"$repo_dir/dist/desktop"}
jpackage_bin=${JPACKAGE_BIN:-$(command -v jpackage || true)}
jdeps_bin=${JDEPS_BIN:-$(command -v jdeps || true)}

if [[ -z "$jpackage_bin" || ! -x "$jpackage_bin" ]]; then
    echo "jpackage was not found. Use a current full JDK or set JPACKAGE_BIN." >&2
    exit 1
fi
if [[ -z "$jdeps_bin" || ! -x "$jdeps_bin" ]]; then
    echo "jdeps was not found. Use a current full JDK or set JDEPS_BIN." >&2
    exit 1
fi

if [[ ${SKIP_BUILD:-0} != 1 ]]; then
    mvn -f "$repo_dir/pom.xml" -pl desktop -am package -DskipTests
fi

jar_path="$repo_dir/desktop/target/ratass-desktop-$project_version.jar"
if [[ ! -f "$jar_path" ]]; then
    echo "Desktop application jar not found: $jar_path" >&2
    exit 1
fi

os_name=$(uname -s | tr '[:upper:]' '[:lower:]')
arch_name=$(uname -m)
case "$os_name" in
    darwin) platform=macos ;;
    linux) platform=linux ;;
    mingw*|msys*|cygwin*) platform=windows ;;
    *)
        echo "Unsupported desktop packaging host: $os_name" >&2
        exit 1
        ;;
esac

case "$platform" in
    linux) default_icon="$repo_dir/assets/branding/rogue-circuit-icon.png" ;;
    windows) default_icon="$repo_dir/assets/branding/rogue-circuit.ico" ;;
    macos) default_icon="$repo_dir/assets/branding/rogue-circuit.icns" ;;
esac
icon_path=${JPACKAGE_ICON:-$default_icon}

package_dir="$output_root/$platform-$arch_name"
input_dir="$repo_dir/desktop/target/jpackage-input"
rm -rf "$package_dir" "$input_dir"
mkdir -p "$package_dir" "$input_dir"
cp "$jar_path" "$input_dir/"

runtime_modules=${JPACKAGE_MODULES:-$(
    "$jdeps_bin" \
        --ignore-missing-deps \
        --multi-release base \
        --print-module-deps \
        "$jar_path"
)}
if [[ -z "$runtime_modules" ]]; then
    echo "Could not determine the Java modules required by the desktop game." >&2
    exit 1
fi

jpackage_args=(
    --type app-image
    --name RogueCircuit
    --description "Roguelite circuit racing"
    --vendor "jbescos"
    --app-version "$project_version"
    --input "$input_dir"
    --main-jar "$(basename "$jar_path")"
    --main-class com.github.jbescos.DesktopLauncher
    --dest "$package_dir"
    --add-modules "$runtime_modules"
    --java-options "-Dfile.encoding=UTF-8"
)

if [[ "$platform" == macos ]]; then
    jpackage_args+=(--java-options "-XstartOnFirstThread")
fi
if [[ ! -f "$icon_path" ]]; then
    echo "Desktop package icon does not exist: $icon_path" >&2
    exit 1
fi
jpackage_args+=(--icon "$icon_path")

"$jpackage_bin" "${jpackage_args[@]}"

echo "Packaged Rogue Circuit with a bundled runtime: $package_dir/RogueCircuit"
