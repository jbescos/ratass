#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAW_DIR="${SCRIPT_DIR}/raw/current"
OUTPUT_DIR="${SCRIPT_DIR}/output/screenshots"

mkdir -p "${OUTPUT_DIR}"

capture() {
    local source="$1"
    local timestamp="$2"
    local output="$3"
    ffmpeg -y -hide_banner -loglevel error -ss "${timestamp}" -i "${source}" \
        -frames:v 1 -vf 'scale=1920:1080:flags=lanczos' "${OUTPUT_DIR}/${output}"
}

capture "${RAW_DIR}/gt3-current.mp4" 95 "01-gt3-level-25-race.png"
capture "${RAW_DIR}/gt3-current.mp4" 105 "02-gt3-powerup-battle.png"
capture "${RAW_DIR}/gt3-current.mp4" 92 "03-gt3-revenge-projectiles.png"
capture "${RAW_DIR}/gt3-current.mp4" 1 "04-gt3-card-offer.png"
capture "${RAW_DIR}/halloween-current.mp4" 10 "05-halloween-race.png"
capture "${RAW_DIR}/halloween-current.mp4" 50 "06-halloween-rain-battle.png"
capture "${RAW_DIR}/halloween-current.mp4" 90 "07-halloween-final-lap.png"
ffmpeg -y -hide_banner -loglevel error -i "${RAW_DIR}/card-types-current.png" \
    -frames:v 1 -vf 'scale=1920:1080:flags=lanczos' "${OUTPUT_DIR}/08-halloween-card-build.png"

printf 'Screenshots: %s\n' "${OUTPUT_DIR}"
