#!/usr/bin/env bash
set -euo pipefail

module_dir=$1
target_dir=$2
program_jar=$3
final_name=$4
sdk_root=$5
build_tools_version=$6
compile_sdk=$7
min_sdk=$8
target_sdk=$9
version_code=${10}
version_name=${11}

platform_jar="$sdk_root/platforms/android-$compile_sdk/android.jar"
build_tools_dir="$sdk_root/build-tools/$build_tools_version"
aapt2_bin="$build_tools_dir/aapt2"
zipalign_bin="$build_tools_dir/zipalign"
apksigner_bin="$build_tools_dir/apksigner"
d8_bin="$build_tools_dir/d8"
if [[ ! -x "$d8_bin" ]]; then
    d8_bin="$sdk_root/cmdline-tools/bin/d8"
fi

runtime_libs_dir="$target_dir/android-runtime-libs"
native_jars_dir="$target_dir/android-native-jars"
assets_dir="$module_dir/../assets"
res_dir="$module_dir/res"
manifest_file="$module_dir/AndroidManifest.xml"

staging_dir="$target_dir/android-build"
compiled_res_zip="$staging_dir/compiled-res.zip"
resource_apk="$staging_dir/resources.apk"
apk_root_dir="$staging_dir/apk-root"
dex_dir="$staging_dir/dex"
native_libs_dir="$staging_dir/native-libs"
native_extract_dir="$staging_dir/native-extract"
unaligned_apk="$target_dir/${final_name}-debug-unaligned.apk"
aligned_apk="$target_dir/${final_name}-debug-aligned.apk"
final_apk="$target_dir/${final_name}-debug.apk"
debug_keystore="$target_dir/debug.keystore"

zip_bin=$(command -v zip)
unzip_bin=$(command -v unzip)
keytool_bin=$(command -v keytool)
jarsigner_bin=$(command -v jarsigner)

require_file() {
    local path=$1
    if [[ ! -f "$path" ]]; then
        echo "Required file not found: $path" >&2
        exit 1
    fi
}

require_executable() {
    local path=$1
    if [[ ! -x "$path" ]]; then
        echo "Required executable not found: $path" >&2
        exit 1
    fi
}

require_file "$platform_jar"
require_file "$manifest_file"
require_file "$program_jar"
require_executable "$aapt2_bin"
require_executable "$zipalign_bin"
require_executable "$d8_bin"
require_executable "$zip_bin"
require_executable "$unzip_bin"
require_executable "$keytool_bin"
require_executable "$jarsigner_bin"
if [[ ! -d "$runtime_libs_dir" ]]; then
    echo "Runtime library directory not found: $runtime_libs_dir" >&2
    exit 1
fi
if [[ ! -d "$native_jars_dir" ]]; then
    echo "Native library directory not found: $native_jars_dir" >&2
    exit 1
fi

rm -rf "$staging_dir"
rm -f "$compiled_res_zip" "$resource_apk" "$unaligned_apk" "$aligned_apk" "$final_apk"
mkdir -p "$staging_dir" "$apk_root_dir" "$dex_dir" "$native_libs_dir" "$native_extract_dir"

"$aapt2_bin" compile --dir "$res_dir" -o "$compiled_res_zip"
"$aapt2_bin" link \
    --manifest "$manifest_file" \
    --min-sdk-version "$min_sdk" \
    --target-sdk-version "$target_sdk" \
    --version-code "$version_code" \
    --version-name "$version_name" \
    --auto-add-overlay \
    -I "$platform_jar" \
    -A "$assets_dir" \
    -o "$resource_apk" \
    "$compiled_res_zip"

"$unzip_bin" -oq "$resource_apk" -d "$apk_root_dir"

mapfile -t runtime_jars < <(find "$runtime_libs_dir" -maxdepth 1 -type f -name '*.jar' | sort)
if [[ ${#runtime_jars[@]} -eq 0 ]]; then
    echo "No runtime jars were copied for Android dexing." >&2
    exit 1
fi

"$d8_bin" \
    --lib "$platform_jar" \
    --min-api "$min_sdk" \
    --output "$dex_dir" \
    "$program_jar" \
    "${runtime_jars[@]}"

find "$dex_dir" -maxdepth 1 -type f -name 'classes*.dex' -exec cp {} "$apk_root_dir/" \;

mapfile -t native_jars < <(find "$native_jars_dir" -maxdepth 1 -type f -name '*.jar' | sort)
if [[ ${#native_jars[@]} -eq 0 ]]; then
    echo "No native jars were copied for Android packaging." >&2
    exit 1
fi

for native_jar in "${native_jars[@]}"; do
    abi_dir=
    case "$(basename "$native_jar")" in
        *natives-armeabi-v7a.jar) abi_dir="$native_libs_dir/lib/armeabi-v7a" ;;
        *natives-arm64-v8a.jar) abi_dir="$native_libs_dir/lib/arm64-v8a" ;;
        *natives-x86.jar) abi_dir="$native_libs_dir/lib/x86" ;;
        *natives-x86_64.jar) abi_dir="$native_libs_dir/lib/x86_64" ;;
    esac
    if [[ -z "$abi_dir" ]]; then
        continue
    fi
    mkdir -p "$abi_dir"
    jar_extract_dir="$native_extract_dir/$(basename "$native_jar" .jar)"
    rm -rf "$jar_extract_dir"
    mkdir -p "$jar_extract_dir"
    "$unzip_bin" -oq "$native_jar" -d "$jar_extract_dir"
    while IFS= read -r native_so; do
        cp "$native_so" "$abi_dir/$(basename "$native_so")"
    done < <(find "$jar_extract_dir" -type f -name '*.so' | sort)
done

if [[ -d "$native_libs_dir/lib" ]]; then
    mkdir -p "$apk_root_dir/lib"
    cp -R "$native_libs_dir/lib/." "$apk_root_dir/lib/"
fi

(
    cd "$apk_root_dir"
    "$zip_bin" -qr "$unaligned_apk" .
)

"$zipalign_bin" -f -p 4 "$unaligned_apk" "$aligned_apk"

if [[ ! -f "$debug_keystore" ]]; then
    "$keytool_bin" -genkeypair \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storetype PKCS12 \
        -keystore "$debug_keystore" \
        -storepass android \
        -keypass android \
        -dname "CN=Android Debug,O=Android,C=US" \
        -noprompt
fi

if [[ -x "$apksigner_bin" ]]; then
    "$apksigner_bin" sign \
        --ks "$debug_keystore" \
        --ks-key-alias androiddebugkey \
        --ks-pass pass:android \
        --key-pass pass:android \
        --out "$final_apk" \
        "$aligned_apk"
    "$apksigner_bin" verify "$final_apk"
else
    cp "$aligned_apk" "$final_apk"
    "$jarsigner_bin" \
        -keystore "$debug_keystore" \
        -storepass android \
        -keypass android \
        "$final_apk" \
        androiddebugkey
fi

echo "Built Android debug APK: $final_apk"
