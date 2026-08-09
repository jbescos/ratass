#!/usr/bin/env bash
set -euo pipefail

output_path=${1:-"$HOME/.config/rogue-circuit/android-upload.p12"}
key_alias=${2:-upload}

if ! command -v keytool >/dev/null 2>&1; then
    echo "keytool was not found. Install a JDK and make keytool available in PATH." >&2
    exit 1
fi
if [[ -e "$output_path" ]]; then
    echo "Refusing to overwrite existing keystore: $output_path" >&2
    exit 1
fi

umask 077
mkdir -p "$(dirname "$output_path")"

echo "Creating the Rogue Circuit Google Play upload key."
echo "Choose a strong password and preserve this file and password in secure backups."
keytool \
    -genkeypair \
    -v \
    -keystore "$output_path" \
    -storetype PKCS12 \
    -alias "$key_alias" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 36500

echo
echo "Created upload keystore: $output_path"
echo "Key alias: $key_alias"
echo "Keep the keystore and its password private and backed up."
