#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

profiles="${1:-all}"
if [[ "$#" -gt 0 ]]; then
  shift
fi

python_bin="${RL_PYTHON:-${repo_root}/.venv-rl/bin/python}"
jar="${RL_JAR:-${repo_root}/desktop/target/ratass-desktop-1.0.jar}"
if [[ ! -x "${python_bin}" ]]; then
  echo "driver_metadata_error=missing_python path=${python_bin}" >&2
  exit 2
fi
if [[ ! -f "${jar}" ]]; then
  echo "driver_metadata_error=missing_jar path=${jar} action=run_maven_package" >&2
  exit 2
fi

exec "${python_bin}" tools/rl/evaluate_lap_times.py \
  --jar "${jar}" \
  --profiles "${profiles}" \
  --map-source game \
  --cars default \
  --laps "${RL_DRIVER_BENCHMARK_LAPS:-3}" \
  --steps "${RL_DRIVER_BENCHMARK_STEPS:-6400}" \
  --action-repeat "${RL_ACTION_REPEAT:-4}" \
  --seed "${RL_DRIVER_BENCHMARK_SEED:-20260531}" \
  --quiet \
  --metadata-only \
  --write-driver-metadata \
  "$@"
