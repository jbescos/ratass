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
requested_version_code=$9
version_name=${10}

resolve_release_version_code() {
    local requested=$1
    local maximum_version_code=2100000000
    local version_code

    if [[ "$requested" == "auto" ]]; then
        local epoch_2020=1577836800
        local timestamp_code=$(( $(date +%s) - epoch_2020 ))
        local state_dir=${XDG_STATE_HOME:-"$HOME/.local/state"}/rogue-circuit
        local state_file="$state_dir/android-version-code"
        local previous_code=0
        if [[ -f "$state_file" ]]; then
            read -r previous_code < "$state_file"
            if [[ ! "$previous_code" =~ ^[0-9]+$ ]]; then
                echo "Invalid saved Android version code: $state_file" >&2
                exit 1
            fi
        fi

        version_code=$timestamp_code
        if (( previous_code >= version_code )); then
            version_code=$((previous_code + 1))
        fi
        if (( version_code > maximum_version_code )); then
            echo "Generated Android version code exceeds $maximum_version_code." >&2
            exit 1
        fi

        umask 077
        mkdir -p "$state_dir"
        local temporary_state_file="$state_file.tmp.$$"
        printf '%s\n' "$version_code" > "$temporary_state_file"
        mv "$temporary_state_file" "$state_file"
    else
        if [[ ! "$requested" =~ ^[1-9][0-9]*$ ]]; then
            echo "Android release version code must be 'auto' or a positive integer: $requested" >&2
            exit 1
        fi
        version_code=$((10#$requested))
        if (( version_code > maximum_version_code )); then
            echo "Android release version code exceeds $maximum_version_code: $version_code" >&2
            exit 1
        fi
    fi

    printf '%s\n' "$version_code"
}

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
version_code=$(resolve_release_version_code "$requested_version_code")
echo "Android release versionCode: $version_code"
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
echo "Built signed Android release AAB: $target_dir/${final_name}-release.aab (versionCode=$version_code)"
