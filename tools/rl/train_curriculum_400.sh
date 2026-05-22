#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

export RL_TRAIN_LOG="${RL_TRAIN_LOG:-logs/rl-curriculum-400-route-cars62-1024x2-v1.log}"
export RL_CURRICULUM_CHECKPOINT_DIR="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-400-route-cars62-1024x2-v1}"
export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
export RL_CURRICULUM_TARGET_EASY_ITERATIONS="${RL_CURRICULUM_TARGET_EASY_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_HARD_ITERATIONS="${RL_CURRICULUM_TARGET_HARD_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_2_ITERATIONS="${RL_CURRICULUM_TARGET_2_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_4_ITERATIONS="${RL_CURRICULUM_TARGET_4_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_8_ITERATIONS="${RL_CURRICULUM_TARGET_8_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_16_ITERATIONS="${RL_CURRICULUM_TARGET_16_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_32_ITERATIONS="${RL_CURRICULUM_TARGET_32_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_CROWD_ITERATIONS="${RL_CURRICULUM_TARGET_CROWD_ITERATIONS:-400}"
export RL_CURRICULUM_TARGET_CROWD_MAX_CYCLES="${RL_CURRICULUM_TARGET_CROWD_MAX_CYCLES:-0}"

if [[ "${1:-}" == "--detach" ]]; then
  exec bash "${script_dir}/train_for_hours.sh" --detach curriculum
fi
if [[ "${1:-}" == "--foreground" ]]; then
  shift
fi

exec bash "${script_dir}/train_for_hours.sh" curriculum
