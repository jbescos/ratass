#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export RL_OBJECTIVE="${RL_OBJECTIVE:-navigation}"
export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-rl-checkpoints-navigation-route}"
export RL_CONTROLLED_AGENTS="${RL_CONTROLLED_AGENTS:-1}"
export RL_FIELD_SIZE="${RL_FIELD_SIZE:-1}"
export RL_MAX_ACTION_STEPS="${RL_MAX_ACTION_STEPS:-1200}"

exec "${script_dir}/train_docker.sh" "$@"
