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

if ! command -v keytool >/dev/null 2>&1; then
    echo "keytool is required to validate the Android release certificate." >&2
    exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
    echo "openssl is required to validate the Android release certificate." >&2
    exit 1
fi

certificate_file=$(mktemp)
cleanup_certificate() {
    rm -f "$certificate_file"
}
trap cleanup_certificate EXIT

if ! keytool \
        -exportcert \
        -rfc \
        -keystore "$ANDROID_KEYSTORE" \
        -alias "$ANDROID_KEY_ALIAS" \
        -storepass:env ANDROID_KEYSTORE_PASSWORD \
        -file "$certificate_file" >/dev/null 2>&1; then
    echo "Could not read Android release key '$ANDROID_KEY_ALIAS' from $ANDROID_KEYSTORE." >&2
    echo "Check the alias and keystore password." >&2
    exit 1
fi

# Google Play requires signing certificates to remain valid after 22 October 2033.
play_certificate_cutoff_epoch=2013638400
seconds_until_cutoff=$((play_certificate_cutoff_epoch - $(date +%s)))
if (( seconds_until_cutoff > 0 )) \
        && ! openssl x509 -in "$certificate_file" -noout -checkend "$seconds_until_cutoff" \
            >/dev/null 2>&1; then
    certificate_expiry=$(openssl x509 -in "$certificate_file" -noout -enddate | cut -d= -f2-)
    echo "Android release certificate expires too soon: $certificate_expiry" >&2
    echo "Google Play requires it to remain valid after 22 October 2033." >&2
    echo "Create a permanent upload key with tools/release/create-android-upload-key.sh." >&2
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
