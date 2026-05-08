#!/usr/bin/env bash
set -euo pipefail

python_bin="${PYTHON_BIN:-.venv-rl/bin/python}"
workers="${RL_WORKERS:-0}"
controlled_agents="${RL_CONTROLLED_AGENTS:-6}"
field_size="${RL_FIELD_SIZE:-12}"
action_repeat="${RL_ACTION_REPEAT:-4}"
max_action_steps="${RL_MAX_ACTION_STEPS:-900}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-20}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-rl-checkpoints-circle}"
iterations_per_cycle="${RL_FOREVER_ITERATIONS:-100}"
package_every_cycles="${RL_PACKAGE_EVERY_CYCLES:-0}"
num_gpus="${RL_NUM_GPUS:-0}"
map_ids="${RL_MAP_IDS:-}"
build_before_training="${RL_BUILD_BEFORE_TRAINING:-auto}"
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
)

if [[ -n "${map_ids}" ]]; then
  common_args+=(--map-ids "${map_ids}")
fi

export_policy() {
  if [[ ! -f "${checkpoint_file}" ]]; then
    echo "checkpoint_missing=${checkpoint_file}"
    return 0
  fi

  "${python_bin}" tools/rl/export_policy.py --checkpoint-dir "${checkpoint_dir}"
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
done
