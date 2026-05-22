#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
log_file="${RL_TRAIN_LOG:-logs/rl-curriculum-route-cars62-v1.log}"

resolve_log_path() {
  if [[ "${log_file}" == /* ]]; then
    printf '%s\n' "${log_file}"
  else
    printf '%s/%s\n' "${repo_root}" "${log_file}"
  fi
}

if [[ "${1:-}" == "--detach" ]]; then
  preset="${2:-${RL_PRESET:-curriculum}}"
  log_path="$(resolve_log_path)"
  mkdir -p "$(dirname "${log_path}")"
  (
    cd "${repo_root}"
    setsid nohup env RL_DETACH=0 RL_PRESET="${preset}" bash "${script_dir}/train_for_hours.sh" >> "${log_path}" 2>&1 < /dev/null &
    printf '%s\n' "$!" > "${log_path}.pid"
  )
  printf 'pid=%s log=%s\n' "$(cat "${log_path}.pid")" "${log_path}"
  exit 0
fi

cd "${repo_root}"
preset="${1:-${RL_PRESET:-curriculum}}"

export RL_CURRICULUM_CHECKPOINT_DIR="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-route-cars62-v1}"
export RL_CURRICULUM_TARGET_EASY_ITERATIONS="${RL_CURRICULUM_TARGET_EASY_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_HARD_ITERATIONS="${RL_CURRICULUM_TARGET_HARD_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_2_ITERATIONS="${RL_CURRICULUM_TARGET_2_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_4_ITERATIONS="${RL_CURRICULUM_TARGET_4_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_8_ITERATIONS="${RL_CURRICULUM_TARGET_8_ITERATIONS:-500}"
export RL_CURRICULUM_TARGET_16_ITERATIONS="${RL_CURRICULUM_TARGET_16_ITERATIONS:-600}"
export RL_CURRICULUM_TARGET_32_ITERATIONS="${RL_CURRICULUM_TARGET_32_ITERATIONS:-700}"
export RL_CURRICULUM_TARGET_CROWD_ITERATIONS="${RL_CURRICULUM_TARGET_CROWD_ITERATIONS:-800}"
export RL_CURRICULUM_TARGET_CROWD_MAX_CYCLES="${RL_CURRICULUM_TARGET_CROWD_MAX_CYCLES:-0}"
export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-${RL_CURRICULUM_CHECKPOINT_DIR}}"
export RL_FOREVER_ITERATIONS="${RL_FOREVER_ITERATIONS:-${RL_CURRICULUM_TARGET_4_ITERATIONS}}"
export RL_WORKERS="${RL_WORKERS:-7}"
export RL_RAY_NUM_CPUS="${RL_RAY_NUM_CPUS:-8}"
export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"
export RL_HIDDEN_SIZE="${RL_HIDDEN_SIZE:-1024}"
export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
export RL_CHECKPOINT_EVERY="${RL_CHECKPOINT_EVERY:-10}"
export RL_BEST_EVAL_EPISODES_PER_MAP="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
export RL_PACKAGE_EVERY_CYCLES="${RL_PACKAGE_EVERY_CYCLES:-0}"
export RL_FRESH_START="${RL_FRESH_START:-1}"

if [[ -n "${RL_INIT_POLICY:-}" && ! -f "${RL_INIT_POLICY}" ]]; then
  echo "init_policy_missing=${RL_INIT_POLICY}; starting from scratch"
  unset RL_INIT_POLICY
fi

exec bash "${script_dir}/train_forever.sh" "${preset}"
