#!/usr/bin/env bash
set -euo pipefail

python_bin="${PYTHON_BIN:-.venv-rl/bin/python}"
objective="${RL_OBJECTIVE:-combat}"
if [[ "${objective}" == "navigation" ]]; then
  default_controlled_agents=1
  default_field_size=1
  default_max_action_steps=1200
  default_checkpoint_dir="rl-checkpoints-navigation-route"
  export_objective="navigation-route-safe-circle-v1"
else
  default_controlled_agents=6
  default_field_size=12
  default_max_action_steps=900
  default_checkpoint_dir="rl-checkpoints-direct-circle-route"
  export_objective="direct-route-safe-circle-v1"
fi
workers="${RL_WORKERS:-0}"
controlled_agents="${RL_CONTROLLED_AGENTS:-${default_controlled_agents}}"
field_size="${RL_FIELD_SIZE:-${default_field_size}}"
max_action_steps="${RL_MAX_ACTION_STEPS:-${default_max_action_steps}}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-20}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-${default_checkpoint_dir}}"
num_gpus="${RL_NUM_GPUS:-0}"
map001_iterations="${RL_MAP001_ITERATIONS:-240}"
hard_iterations="${RL_HARD_ITERATIONS:-220}"
opponent_count="${RL_OPPONENT_COUNT:-}"
no_reward_summary="${RL_NO_REWARD_SUMMARY:-0}"
ray_num_cpus="${RL_RAY_NUM_CPUS:-0}"
ray_temp_dir="${RL_RAY_TEMP_DIR:-}"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --max-action-steps "${max_action_steps}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --checkpoint-every "${checkpoint_every}"
  --num-gpus "${num_gpus}"
  --objective "${objective}"
)

if [[ -n "${opponent_count}" ]]; then
  common_args+=(--opponent-count "${opponent_count}")
fi
if [[ "${no_reward_summary}" == "1" || "${no_reward_summary}" == "true" ]]; then
  common_args+=(--no-reward-summary)
fi
if [[ "${ray_num_cpus}" != "0" ]]; then
  common_args+=(--ray-num-cpus "${ray_num_cpus}")
fi
if [[ -n "${ray_temp_dir}" ]]; then
  common_args+=(--ray-temp-dir "${ray_temp_dir}")
fi

mvn -pl desktop -am package
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --map-ids map004 --iterations "${RL_WARMUP_ITERATIONS:-40}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --map-ids map001 --iterations "${map001_iterations}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --map-ids map001,map003,map006 --iterations "${hard_iterations}"
"${python_bin}" tools/rl/train_rllib.py "${common_args[@]}" --resume --iterations "${RL_MIXED_ITERATIONS:-160}"
"${python_bin}" tools/rl/export_policy.py \
  --checkpoint-dir "${checkpoint_dir}" \
  --objective "${export_objective}"
mvn -pl desktop -am package
