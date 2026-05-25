#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

log_file="${RL_TRAIN_LOG:-logs/rl-race-single-400-all-maps-physics68-v1.log}"
checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-race-single-400-all-maps-physics68-v1}"
iterations="${RL_CURRICULUM_ITERATIONS:-400}"

is_true() {
  [[ "${1:-}" == "1" || "${1:-}" == "true" || "${1:-}" == "yes" ]]
}

clean_checkpoint_dir_for_fresh_start() {
  if ! is_true "${RL_FRESH_START:-1}"; then
    return
  fi
  if [[ -z "${checkpoint_dir}" || "${checkpoint_dir}" == "/" ]]; then
    echo "fresh_start_invalid_checkpoint_dir=${checkpoint_dir}" >&2
    exit 2
  fi
  if [[ -e "${checkpoint_dir}" ]]; then
    echo "fresh_start_remove_checkpoint_dir=${checkpoint_dir}"
    rm -rf -- "${checkpoint_dir}"
  else
    echo "fresh_start_checkpoint_dir_absent=${checkpoint_dir}"
  fi
}

available_mask_map_ids() {
  local paths=()
  local ids=()
  local path
  local name

  mapfile -t paths < <(find assets/maps -maxdepth 1 -type f -name '*_mask.png' | sort)
  if [[ "${#paths[@]}" -eq 0 ]]; then
    echo "no_mask_maps_found=assets/maps/*_mask.png" >&2
    exit 2
  fi

  for path in "${paths[@]}"; do
    name="$(basename "${path}")"
    ids+=("${name%_mask.png}")
  done

  local joined="${ids[0]}"
  local index
  for ((index = 1; index < ${#ids[@]}; index++)); do
    joined+=",${ids[index]}"
  done
  printf '%s\n' "${joined}"
}

run_phase() {
  local phase="$1"
  local init_policy="$2"
  local phase_iterations="$3"
  local random_race_spawns="$4"
  local max_checkpoints="$5"
  local best_eval_state="${checkpoint_dir}/best-eval/${phase}/best_policy.json"
  local best_eval_min_checkpoints="${RL_BEST_EVAL_MIN_CHECKPOINTS:-}"
  local phase_best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"

  if [[ "${phase_iterations}" -le 0 ]]; then
    echo "curriculum_phase_skip=${phase} iterations=${phase_iterations}"
    return
  fi
  if [[ -z "${best_eval_min_checkpoints}" ]]; then
    if [[ "${max_checkpoints}" =~ ^[1-9][0-9]*$ ]]; then
      best_eval_min_checkpoints="${max_checkpoints}"
    else
      best_eval_min_checkpoints="1"
    fi
  fi

  echo "curriculum_phase=${phase} iterations=${phase_iterations} max_cycles=1 max_checkpoints=${max_checkpoints} checkpoint_dir=${checkpoint_dir} best_eval_state=${best_eval_state} best_output=${phase_best_output} init_policy=${init_policy:-none} random_race_spawns=${random_race_spawns}"
  env \
    RL_CHECKPOINT_DIR="${checkpoint_dir}" \
    RL_FOREVER_ITERATIONS="${phase_iterations}" \
    RL_MAX_CYCLES=1 \
    RL_INIT_POLICY="${init_policy}" \
    RL_BEST_EVAL_STATE="${best_eval_state}" \
    RL_BEST_EVAL_MIN_CHECKPOINTS="${best_eval_min_checkpoints}" \
    RL_BEST_OUTPUT="${phase_best_output}" \
    RL_BEST_EVAL_IGNORE_INSTALLED=1 \
    RL_RANDOM_RACE_SPAWNS="${random_race_spawns}" \
    RL_MAX_CHECKPOINTS="${max_checkpoints}" \
    RL_CHECKPOINT_STAGE_ACTIVE=1 \
    RL_FRESH_START=0 \
    bash "${script_dir}/train_forever.sh" "race-single"
}

checkpoint_stage_label() {
  case "$1" in
    -1) printf '%s\n' "lap" ;;
    *) printf 'cp%s\n' "$1" ;;
  esac
}

run_all() {
  export RL_WORKERS="${RL_WORKERS:-7}"
  export RL_RAY_NUM_CPUS="${RL_RAY_NUM_CPUS:-8}"
  export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"
  export RL_OBJECTIVE="${RL_OBJECTIVE:-race}"
  export RL_HIDDEN_SIZE="${RL_HIDDEN_SIZE:-1024}"
  export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
  export RL_CHECKPOINT_EVERY="${RL_CHECKPOINT_EVERY:-10}"
  export RL_BEST_EVAL_EPISODES_PER_MAP="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
  export RL_PACKAGE_EVERY_CYCLES="${RL_PACKAGE_EVERY_CYCLES:-0}"
  export RL_FORCE_EXPORT_ON_FINISH="${RL_FORCE_EXPORT_ON_FINISH:-1}"
  export RL_FRESH_START="${RL_FRESH_START:-1}"

  local detected_map_ids
  detected_map_ids="$(available_mask_map_ids)"
  export RL_MAP_IDS="${RL_MAP_IDS:-${detected_map_ids}}"
  export RL_BEST_EVAL_MAP_IDS="${RL_BEST_EVAL_MAP_IDS:-${RL_MAP_IDS}}"
  echo "curriculum_maps=${RL_MAP_IDS}"

  local init_policy="${RL_INIT_POLICY:-}"
  if [[ -n "${init_policy}" && ! -f "${init_policy}" ]]; then
    echo "init_policy_missing=${init_policy}; starting from scratch"
    init_policy=""
  fi

  clean_checkpoint_dir_for_fresh_start
  local base=$((iterations / 4))
  local remainder=$((iterations % 4))
  local checkpoints=(1 2 3 -1)
  echo "curriculum_schedule checkpoint_stages=1,2,3,lap total_iterations=${iterations}"
  local index
  for index in 0 1 2 3; do
    local phase_iterations="${base}"
    if [[ "${index}" -lt "${remainder}" ]]; then
      phase_iterations=$((phase_iterations + 1))
    fi
    local checkpoint_target="${checkpoints[index]}"
    local checkpoint_label
    checkpoint_label="$(checkpoint_stage_label "${checkpoint_target}")"
    run_phase "race-single-random-${checkpoint_label}" "${init_policy}" "${phase_iterations}" "1" "${checkpoint_target}"
    init_policy=""
  done
}

if [[ "${1:-}" == "--detach" ]]; then
  log_path="${log_file}"
  if [[ "${log_path}" != /* ]]; then
    log_path="${repo_root}/${log_path}"
  fi
  mkdir -p "$(dirname "${log_path}")"
  (
    cd "${repo_root}"
    setsid nohup env RL_DETACH=0 bash "${BASH_SOURCE[0]}" >> "${log_path}" 2>&1 < /dev/null &
    printf '%s\n' "$!" > "${log_path}.pid"
  )
  printf 'pid=%s log=%s\n' "$(cat "${log_path}.pid")" "${log_path}"
  exit 0
fi

run_all
