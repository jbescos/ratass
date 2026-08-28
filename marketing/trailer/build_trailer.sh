#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RAW_DIR="${SCRIPT_DIR}/raw/current"
OUTPUT_DIR="${SCRIPT_DIR}/output"
GT3_CAPTURE="${RAW_DIR}/gt3-current.mp4"
HALLOWEEN_CAPTURE="${RAW_DIR}/halloween-current.mp4"
MUSIC="${REPO_ROOT}/assets/theme/gt3/audio/music.ogg"
END_SLATE="${REPO_ROOT}/assets/game_menu.png"
FILTER="${SCRIPT_DIR}/trailer.filter"
MASTER="${OUTPUT_DIR}/rogue-circuit-steam-gameplay-trailer.mp4"
POSTER="${OUTPUT_DIR}/rogue-circuit-trailer-poster.png"

for command in ffmpeg ffprobe; do
    if ! command -v "${command}" >/dev/null 2>&1; then
        printf 'Missing required command: %s\n' "${command}" >&2
        exit 1
    fi
done

for input in "${GT3_CAPTURE}" "${HALLOWEEN_CAPTURE}" "${MUSIC}" "${END_SLATE}" "${FILTER}"; do
    if [[ ! -f "${input}" ]]; then
        printf 'Missing trailer input: %s\n' "${input}" >&2
        exit 1
    fi
done

mkdir -p "${OUTPUT_DIR}"

ffmpeg -y -hide_banner -loglevel warning \
    -ss 20 -t 4 -i "${GT3_CAPTURE}" \
    -ss 5 -t 4 -i "${GT3_CAPTURE}" \
    -ss 35 -t 4 -i "${GT3_CAPTURE}" \
    -ss 30 -t 4 -i "${HALLOWEEN_CAPTURE}" \
    -ss 90 -t 4 -i "${GT3_CAPTURE}" \
    -ss 105 -t 4 -i "${GT3_CAPTURE}" \
    -ss 5 -t 4 -i "${HALLOWEEN_CAPTURE}" \
    -ss 110 -t 4 -i "${GT3_CAPTURE}" \
    -ss 100 -t 4 -i "${HALLOWEEN_CAPTURE}" \
    -ss 115 -t 5 -i "${GT3_CAPTURE}" \
    -stream_loop -1 -i "${MUSIC}" \
    -loop 1 -framerate 60 -t 5 -i "${END_SLATE}" \
    -filter_complex_script "${FILTER}" \
    -map '[outv]' -map '[outa]' \
    -c:v libx264 -preset medium -b:v 20M -maxrate 24M -bufsize 40M \
    -r 60 -pix_fmt yuv420p -profile:v high -level:v 4.2 \
    -c:a aac -b:a 192k -ar 48000 -ac 2 \
    -movflags +faststart -metadata title='Rogue Circuit - Gameplay Trailer' \
    "${MASTER}"

ffmpeg -y -hide_banner -loglevel warning \
    -ss 4.5 -i "${MASTER}" -frames:v 1 -update 1 "${POSTER}"

ffprobe -v error \
    -show_entries format=duration,size,bit_rate:stream=codec_name,width,height,r_frame_rate,sample_rate,channels \
    -of default=noprint_wrappers=1 "${MASTER}"

printf 'Trailer: %s\nPoster:  %s\n' "${MASTER}" "${POSTER}"
