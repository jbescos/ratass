#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

cd "${repo_root}"

export RL_TRAIN_LOG="${RL_TRAIN_LOG:-logs/rl-race-f1-single.log}"
export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-rl-checkpoints-race-f1-single-v1}"
export RL_FOREVER_ITERATIONS="${RL_FOREVER_ITERATIONS:-80}"
export RL_MAX_CYCLES="${RL_MAX_CYCLES:-1}"
export RL_FRESH_START="${RL_FRESH_START:-1}"
export RL_WORKERS="${RL_WORKERS:-4}"
export RL_RAY_NUM_CPUS="${RL_RAY_NUM_CPUS:-6}"
export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"
export RL_HIDDEN_SIZE="${RL_HIDDEN_SIZE:-1024}"
export RL_HIDDEN_LAYERS="${RL_HIDDEN_LAYERS:-2}"
export RL_CHECKPOINT_EVERY="${RL_CHECKPOINT_EVERY:-10}"
export RL_RANDOM_RACE_SPAWNS="${RL_RANDOM_RACE_SPAWNS:-1}"

bash "${script_dir}/train_forever.sh" race-single
