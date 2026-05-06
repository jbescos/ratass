#!/usr/bin/env bash
set -euo pipefail

python_bin="${PYTHON_BIN:-.venv-rl/bin/python}"
workers="${RL_WORKERS:-0}"
controlled_agents="${RL_CONTROLLED_AGENTS:-6}"
field_size="${RL_FIELD_SIZE:-10}"
max_action_steps="${RL_MAX_ACTION_STEPS:-420}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-20}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-rl-checkpoints-tactical}"
map001_iterations="${RL_MAP001_ITERATIONS:-240}"
hard_iterations="${RL_HARD_ITERATIONS:-220}"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --max-action-steps "${max_action_steps}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --checkpoint-every "${checkpoint_every}"
)

mvn -pl desktop -am package
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --map-ids map004 --iterations "${RL_WARMUP_ITERATIONS:-40}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --map-ids map001 --iterations "${map001_iterations}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --map-ids map001,map003,map006 --iterations "${hard_iterations}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --iterations "${RL_MIXED_ITERATIONS:-160}"
"${python_bin}" tools/rl/export_policy.py --checkpoint-dir "${checkpoint_dir}"
mvn -pl desktop -am package
