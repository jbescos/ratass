#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RAW_DIR="${SCRIPT_DIR}/raw/current"
STILL_DIR="${SCRIPT_DIR}/stills/current"
GENERATED_DIR="${SCRIPT_DIR}/stills/generated"
OUTPUT_DIR="${SCRIPT_DIR}/output"
GT3_CAPTURE="${RAW_DIR}/gt3-current.mp4"
HALLOWEEN_CAPTURE="${RAW_DIR}/halloween-current.mp4"
HUNTER_STORM_CAPTURE="${RAW_DIR}/hunter-storm-current.mkv"
GT3_MUSIC="${REPO_ROOT}/assets/theme/gt3/audio/music.ogg"
HALLOWEEN_MUSIC="${REPO_ROOT}/assets/theme/halloween/audio/music.ogg"
FILTER="${SCRIPT_DIR}/trailer.filter"
MASTER="${OUTPUT_DIR}/rogue-circuit-steam-gameplay-trailer.mp4"
POSTER="${OUTPUT_DIR}/rogue-circuit-trailer-poster.png"

for command in ffmpeg ffprobe python3; do
    if ! command -v "${command}" >/dev/null 2>&1; then
        printf 'Missing required command: %s\n' "${command}" >&2
        exit 1
    fi
done

"${SCRIPT_DIR}/build_visual_assets.py"

inputs=(
    "${GT3_CAPTURE}"
    "${HALLOWEEN_CAPTURE}"
    "${HUNTER_STORM_CAPTURE}"
    "${GT3_MUSIC}"
    "${HALLOWEEN_MUSIC}"
    "${STILL_DIR}/menu3.png"
    "${STILL_DIR}/halloween-menu.png"
    "${GENERATED_DIR}/card-driver.png"
    "${GENERATED_DIR}/card-tuning.png"
    "${GENERATED_DIR}/card-technique.png"
    "${GENERATED_DIR}/card-powerup.png"
    "${GENERATED_DIR}/card-revenge.png"
    "${GENERATED_DIR}/card-set.png"
    "${GENERATED_DIR}/card-wall-wide.png"
    "${GENERATED_DIR}/synergy.png"
    "${GENERATED_DIR}/end-slate.png"
    "${FILTER}"
)
for input in "${inputs[@]}"; do
    if [[ ! -f "${input}" ]]; then
        printf 'Missing trailer input: %s\n' "${input}" >&2
        exit 1
    fi
done

mkdir -p "${OUTPUT_DIR}"

ffmpeg -y -hide_banner -loglevel warning \
    -ss 108 -t 3 -i "${GT3_CAPTURE}" \
    -ss 20 -t 5 -i "${GT3_CAPTURE}" \
    -ss 78 -t 4 -i "${GT3_CAPTURE}" \
    -ss 0 -t 3 -i "${GT3_CAPTURE}" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-driver.png" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-tuning.png" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-technique.png" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-powerup.png" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-revenge.png" \
    -loop 1 -framerate 60 -t 2 -i "${GENERATED_DIR}/card-set.png" \
    -loop 1 -framerate 60 -t 4 -i "${GENERATED_DIR}/card-wall-wide.png" \
    -loop 1 -framerate 60 -t 3 -i "${GENERATED_DIR}/synergy.png" \
    -ss 72 -t 6 -i "${HUNTER_STORM_CAPTURE}" \
    -loop 1 -framerate 60 -t 2 -i "${STILL_DIR}/menu3.png" \
    -loop 1 -framerate 60 -t 2 -i "${STILL_DIR}/halloween-menu.png" \
    -ss 25 -t 4 -i "${HALLOWEEN_CAPTURE}" \
    -loop 1 -framerate 60 -t 4 -i "${GENERATED_DIR}/end-slate.png" \
    -i "${GT3_MUSIC}" \
    -i "${HALLOWEEN_MUSIC}" \
    -filter_complex_script "${FILTER}" \
    -map '[outv]' -map '[outa]' \
    -c:v libx264 -preset medium -b:v 18M -maxrate 22M -bufsize 36M \
    -r 60 -pix_fmt yuv420p -profile:v high -level:v 4.2 \
    -c:a aac -b:a 192k -ar 48000 -ac 2 \
    -movflags +faststart -metadata title='Rogue Circuit - Racing Roguelite Trailer' \
    "${MASTER}"

ffmpeg -y -hide_banner -loglevel warning \
    -ss 37.1 -i "${MASTER}" -frames:v 1 -update 1 "${POSTER}"

ffprobe -v error \
    -show_entries format=duration,size,bit_rate:stream=codec_name,width,height,r_frame_rate,sample_rate,channels \
    -of default=noprint_wrappers=1 "${MASTER}"

printf 'Trailer: %s\nPoster:  %s\n' "${MASTER}" "${POSTER}"
