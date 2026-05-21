#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

log_file="${RL_TRAIN_LOG:-logs/rl-curriculum-40-to-4-route-escape51-survival-v2.log}"
checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-40-to-4-route-escape51-survival-v2}"
iterations="${RL_CURRICULUM_ITERATIONS:-40}"

run_phase() {
  local phase="$1"
  local init_policy="$2"
  local best_eval_state="${checkpoint_dir}/best-eval/${phase}/best_policy.json"

  echo "curriculum_phase=${phase} iterations=${iterations} max_cycles=1 checkpoint_dir=${checkpoint_dir} best_eval_state=${best_eval_state} init_policy=${init_policy:-none}"
  env \
    RL_CHECKPOINT_DIR="${checkpoint_dir}" \
    RL_FOREVER_ITERATIONS="${iterations}" \
    RL_MAX_CYCLES=1 \
    RL_INIT_POLICY="${init_policy}" \
    RL_BEST_EVAL_STATE="${best_eval_state}" \
    bash "${script_dir}/train_forever.sh" "${phase}"
}

run_all() {
  export RL_WORKERS="${RL_WORKERS:-7}"
  export RL_RAY_NUM_CPUS="${RL_RAY_NUM_CPUS:-8}"
  export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"
  export RL_HIDDEN_SIZE="${RL_HIDDEN_SIZE:-1024}"
  export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
  export RL_CHECKPOINT_EVERY="${RL_CHECKPOINT_EVERY:-10}"
  export RL_BEST_EVAL_EPISODES_PER_MAP="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
  export RL_PACKAGE_EVERY_CYCLES="${RL_PACKAGE_EVERY_CYCLES:-0}"

  local init_policy="${RL_INIT_POLICY:-}"
  if [[ -n "${init_policy}" && ! -f "${init_policy}" ]]; then
    echo "init_policy_missing=${init_policy}; starting from scratch"
    init_policy=""
  fi

  run_phase "target-easy" "${init_policy}"
  run_phase "target-hard" ""
  run_phase "target-2" ""
  run_phase "target-4" ""
}

if [[ "${1:-}" == "--foreground" ]]; then
  run_all
  exit 0
fi

log_path="${log_file}"
if [[ "${log_path}" != /* ]]; then
  log_path="${repo_root}/${log_path}"
fi
mkdir -p "$(dirname "${log_path}")"
(
  cd "${repo_root}"
  setsid nohup env RL_DETACH=0 bash "${BASH_SOURCE[0]}" --foreground >> "${log_path}" 2>&1 < /dev/null &
  printf '%s\n' "$!" > "${log_path}.pid"
)
printf 'pid=%s log=%s\n' "$(cat "${log_path}.pid")" "${log_path}"
