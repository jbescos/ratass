#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
python_bin="${repo_root}/.venv-rl/bin/python"
if [[ ! -x "${python_bin}" ]]; then
  python_bin="python3"
fi

profiles="all"
if [[ "$#" -gt 0 && "${1}" != -* ]]; then
  profiles="$1"
  shift
  while [[ "$#" -gt 0 && "${1}" != -* ]]; do
    profiles+=",${1}"
    shift
  done
fi

exec "${python_bin}" "${script_dir}/evaluate_lap_times.py" \
  --profiles "${profiles}" \
  --map-source all \
  --laps 5 \
  --steps 0 \
  --timeout-seconds "${RL_LAP_EVAL_TIMEOUT_SECONDS:-10}" \
  --include-missing \
  --group-by-map \
  --stream-map-tables \
  "$@"
