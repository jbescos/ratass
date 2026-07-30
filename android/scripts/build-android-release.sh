#!/usr/bin/env bash
set -euo pipefail

module_dir=$1
target_dir=$2
final_name=$3
gdx_version=$4
compile_sdk=$5
target_sdk=$6
min_sdk=$7
build_tools_version=$8
version_code=$9
version_name=${10}

resolve_android_sdk() {
    local candidates=(
        "${ANDROID_SDK_ROOT:-}"
        "${ANDROID_HOME:-}"
        "${ANDROID_HOME:-}/.."
    )
    local candidate
    for candidate in "${candidates[@]}"; do
        if [[ -n "$candidate" && -f "$candidate/platforms/android-$compile_sdk/android.jar" ]]; then
            ANDROID_HOME=$(cd "$candidate" && pwd)
            ANDROID_SDK_ROOT=$ANDROID_HOME
            export ANDROID_HOME ANDROID_SDK_ROOT
            return
        fi
    done
    echo "Android SDK platform $compile_sdk was not found. Set ANDROID_SDK_ROOT to the SDK root." >&2
    exit 1
}

required_variables=(
    ANDROID_KEYSTORE
    ANDROID_KEY_ALIAS
    ANDROID_KEYSTORE_PASSWORD
    ANDROID_KEY_PASSWORD
)
for variable in "${required_variables[@]}"; do
    if [[ -z "${!variable:-}" ]]; then
        echo "Required release signing variable is not set: $variable" >&2
        exit 1
    fi
done

if [[ ! -f "$ANDROID_KEYSTORE" ]]; then
    echo "Android release keystore was not found: $ANDROID_KEYSTORE" >&2
    exit 1
fi

resolve_android_sdk
mkdir -p "$target_dir"

"$module_dir/gradlew" \
    --no-daemon \
    --console=plain \
    -p "$module_dir" \
    -PgdxVersion="$gdx_version" \
    -PcompileSdk="$compile_sdk" \
    -PtargetSdk="$target_sdk" \
    -PminSdk="$min_sdk" \
    -PbuildToolsVersion="$build_tools_version" \
    -PappVersionCode="$version_code" \
    -PappVersionName="$version_name" \
    bundleRelease

mapfile -t bundles < <(find "$module_dir/build/outputs/bundle/release" -maxdepth 1 -type f -name '*-release.aab' | sort)
if [[ ${#bundles[@]} -ne 1 ]]; then
    echo "Expected one Android release bundle, found ${#bundles[@]}." >&2
    exit 1
fi

cp "${bundles[0]}" "$target_dir/${final_name}-release.aab"
echo "Built signed Android release AAB: $target_dir/${final_name}-release.aab"
