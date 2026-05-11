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
action_repeat="${RL_ACTION_REPEAT:-4}"
max_action_steps="${RL_MAX_ACTION_STEPS:-${default_max_action_steps}}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-20}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-${default_checkpoint_dir}}"
iterations_per_cycle="${RL_FOREVER_ITERATIONS:-100}"
max_cycles="${RL_MAX_CYCLES:-0}"
package_every_cycles="${RL_PACKAGE_EVERY_CYCLES:-1}"
num_gpus="${RL_NUM_GPUS:-0}"
map_ids="${RL_MAP_IDS:-}"
opponent_count="${RL_OPPONENT_COUNT:-}"
no_reward_summary="${RL_NO_REWARD_SUMMARY:-0}"
ray_num_cpus="${RL_RAY_NUM_CPUS:-0}"
ray_temp_dir="${RL_RAY_TEMP_DIR:-}"
build_before_training="${RL_BUILD_BEFORE_TRAINING:-1}"
desktop_jar="${RL_JAR:-desktop/target/ratass-desktop-1.0.jar}"

checkpoint_file="${checkpoint_dir}/rllib_checkpoint.json"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --action-repeat "${action_repeat}"
  --max-action-steps "${max_action_steps}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --checkpoint-every "${checkpoint_every}"
  --num-gpus "${num_gpus}"
  --objective "${objective}"
)

if [[ -n "${map_ids}" ]]; then
  common_args+=(--map-ids "${map_ids}")
fi
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

export_policy() {
  if [[ ! -f "${checkpoint_file}" ]]; then
    echo "checkpoint_missing=${checkpoint_file}"
    return 0
  fi

  "${python_bin}" tools/rl/export_policy.py \
    --checkpoint-dir "${checkpoint_dir}" \
    --objective "${export_objective}"
}

package_game() {
  mvn -pl desktop -am package
}

should_package_cycle() {
  local cycle="$1"
  [[ "${package_every_cycles}" != "0" && $((cycle % package_every_cycles)) -eq 0 ]]
}

finish_from_latest_checkpoint() {
  export_policy
  if should_package_cycle 1; then
    package_game
  fi
}

on_interrupt() {
  echo "interrupted=1"
  finish_from_latest_checkpoint || true
  exit 130
}

trap on_interrupt INT TERM

if [[ "${build_before_training}" == "1" || ! -f "${desktop_jar}" ]]; then
  package_game
fi

cycle=0
while true; do
  cycle=$((cycle + 1))
  resume=false
  cycle_args=("${common_args[@]}")
  if [[ -f "${checkpoint_file}" ]]; then
    resume=true
    cycle_args+=(--resume)
  fi

  echo "cycle=${cycle} iterations=${iterations_per_cycle} resume=${resume}"
  "${python_bin}" tools/rl/train_rllib.py "${cycle_args[@]}" --iterations "${iterations_per_cycle}"

  export_policy
  if should_package_cycle "${cycle}"; then
    package_game
  fi
  if [[ "${max_cycles}" != "0" && "${cycle}" -ge "${max_cycles}" ]]; then
    echo "max_cycles_reached=${max_cycles}"
    exit 0
  fi
done
