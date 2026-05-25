#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

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

export RL_TRAIN_LOG="${RL_TRAIN_LOG:-logs/rl-curriculum-400-race-physics68-1024x2-v1.log}"
export RL_CURRICULUM_CHECKPOINT_DIR="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-400-race-physics68-1024x2-v1}"
export RL_OBJECTIVE="${RL_OBJECTIVE:-race}"
export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
export RL_CURRICULUM_RACE_1_ITERATIONS="${RL_CURRICULUM_RACE_1_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_2_ITERATIONS="${RL_CURRICULUM_RACE_2_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_4_ITERATIONS="${RL_CURRICULUM_RACE_4_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_8_ITERATIONS="${RL_CURRICULUM_RACE_8_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_16_ITERATIONS="${RL_CURRICULUM_RACE_16_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_20_ITERATIONS="${RL_CURRICULUM_RACE_20_ITERATIONS:-400}"
export RL_CURRICULUM_RACE_20_MAX_CYCLES="${RL_CURRICULUM_RACE_20_MAX_CYCLES:-0}"
export RL_FORCE_EXPORT_ON_FINISH="${RL_FORCE_EXPORT_ON_FINISH:-1}"
export RL_RANDOM_RACE_SPAWNS="${RL_RANDOM_RACE_SPAWNS:-1}"

detected_map_ids="$(available_mask_map_ids)"
export RL_MAP_IDS="${RL_MAP_IDS:-${detected_map_ids}}"
export RL_BEST_EVAL_MAP_IDS="${RL_BEST_EVAL_MAP_IDS:-${RL_MAP_IDS}}"
echo "curriculum_maps=${RL_MAP_IDS}"

if [[ "${1:-}" == "--detach" ]]; then
  exec bash "${script_dir}/train_for_hours.sh" --detach curriculum
fi
if [[ "${1:-}" == "--foreground" ]]; then
  shift
fi

exec bash "${script_dir}/train_for_hours.sh" curriculum
