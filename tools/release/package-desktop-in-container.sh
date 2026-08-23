#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
project_version=${PROJECT_VERSION:-1.0}
platforms=",${RELEASE_PLATFORMS:-linux,windows},"
jar_path="$repo_dir/desktop/target/ratass-desktop-$project_version.jar"

mkdir -p "$HOME"
mvn -f "$repo_dir/pom.xml" -pl desktop -am package -DskipTests

if [[ "$platforms" == *,linux,* ]]; then
    SKIP_BUILD=1 PROJECT_VERSION="$project_version" \
        "$repo_dir/tools/release/package-desktop.sh"
fi

if [[ "$platforms" == *,windows,* ]]; then
    : "${PACKR_JAR:?PACKR_JAR is required for a Windows cross-build}"
    : "${WINDOWS_JRE_ARCHIVE:?WINDOWS_JRE_ARCHIVE is required for a Windows cross-build}"
    : "${RCEDIT_EXE:?RCEDIT_EXE is required for a Windows cross-build}"

    package_dir="$repo_dir/dist/desktop/windows-x86_64/RogueCircuit"
    rm -rf "$package_dir"
    mkdir -p "$(dirname "$package_dir")"

    java -jar "$PACKR_JAR" \
        --platform windows64 \
        --jdk "$WINDOWS_JRE_ARCHIVE" \
        --executable RogueCircuit \
        --classpath "$jar_path" \
        --mainclass com.github.jbescos.DesktopLauncher \
        --vmargs Dfile.encoding=UTF-8 \
        --output "$package_dir"

    export WINEDEBUG=-all
    export WINEPREFIX="$HOME/.wine-release"
    wine_bin=${WINE_BIN:-$(command -v wine64 || command -v wine || true)}
    if [[ -z "$wine_bin" && -x /usr/lib/wine/wine64 ]]; then
        wine_bin=/usr/lib/wine/wine64
    fi
    if [[ -z "$wine_bin" ]]; then
        echo "Wine was not found; the Windows executable icon cannot be embedded." >&2
        exit 1
    fi
    "$wine_bin" "$RCEDIT_EXE" "$package_dir/RogueCircuit.exe" \
        --set-icon "$repo_dir/assets/branding/rogue-circuit.ico" \
        --set-version-string ProductName "Rogue Circuit" \
        --set-version-string FileDescription "Rogue Circuit" \
        --set-version-string CompanyName "jbescos" \
        --set-file-version "$project_version" \
        --set-product-version "$project_version"

    cp "$repo_dir/LICENSE" "$package_dir/LICENSE.txt"
    file "$package_dir/RogueCircuit.exe" | grep -q 'PE32+'
    test -f "$package_dir/jre/bin/java.exe"
    jar tf "$package_dir/ratass-desktop-$project_version.jar" \
        | grep -q '^com/github/jbescos/DesktopLauncher.class$'

    if [[ ${WINDOWS_WINE_SMOKE_TEST:-1} == 1 ]]; then
        "$wine_bin" "$package_dir/jre/bin/java.exe" -version
        echo "Windows bundled runtime passed its Wine smoke test."
    fi
    echo "Packaged Rogue Circuit for Windows: $package_dir"
fi
