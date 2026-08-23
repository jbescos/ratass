#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
image_name=${DESKTOP_RELEASE_IMAGE:-rogue-circuit-desktop-release:local}
maven_cache=${MAVEN_CACHE_DIR:-"$HOME/.m2"}

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker was not found." >&2
    exit 1
fi

mkdir -p "$maven_cache"
docker build \
    --platform linux/amd64 \
    --file "$repo_dir/tools/release/Dockerfile.desktop" \
    --tag "$image_name" \
    "$repo_dir"

docker run --rm \
    --platform linux/amd64 \
    --user "$(id -u):$(id -g)" \
    --env HOME=/tmp/rogue-circuit-release \
    --env MAVEN_OPTS=-Dmaven.repo.local=/maven-cache/repository \
    --env PROJECT_VERSION="${PROJECT_VERSION:-1.0}" \
    --env RELEASE_PLATFORMS="${RELEASE_PLATFORMS:-linux,windows}" \
    --env WINDOWS_WINE_SMOKE_TEST="${WINDOWS_WINE_SMOKE_TEST:-1}" \
    --volume "$repo_dir:/workspace" \
    --volume "$maven_cache:/maven-cache" \
    "$image_name"
