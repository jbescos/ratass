#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python_bin="${PYTHON_BIN:-.venv-rl/bin/python}"

set_default() {
  local name="$1"
  local value="$2"
  if [[ -z "${!name:-}" ]]; then
    export "${name}=${value}"
  fi
}

usage() {
  cat <<'EOF'
usage: bash tools/rl/train_forever.sh [preset]

presets:
  target-easy       larger circle, easier maps, one learner
  target-hard       normal circle, hole-heavy maps, one learner
  target-2          normal circle, all maps, two learner cars
  target-4          normal circle, all maps, four learner cars
  target-8          normal circle, all maps, eight learner cars
  target-16         normal circle, all maps, sixteen learner cars
  target-32         normal circle, all maps, thirty-two learner cars
  target-many       alias for target-4
  target-crowd      normal circle, all maps, fifty learner cars
  target-50         alias for target-crowd
  curriculum        staged run: 1, hard 1, 2, 4, 8, 16, 32, then 50 cars forever
EOF
}

set_target_cars_defaults() {
  local car_count="$1"
  set_default RL_CONTROLLED_AGENTS "${car_count}"
  set_default RL_FIELD_SIZE "${car_count}"
  set_default RL_TARGET_RADIUS "1.65"
  set_default RL_TARGET_HOLD_SECONDS "0.85"
  set_default RL_MAX_GOALS "6"
  set_default RL_MAX_ACTION_STEPS "1350"
  set_default RL_CHECKPOINT_DIR "rl-checkpoints-target-circle-cars-1024x2-v1"
}

run_curriculum_phase() {
  local phase="$1"
  local iterations="$2"
  local max_cycles="$3"
  local checkpoint_dir="$4"
  local init_policy="$5"
  local best_eval_state="${checkpoint_dir}/best-eval/${phase}/best_policy.json"
  echo "curriculum_phase=${phase} iterations=${iterations} max_cycles=${max_cycles} checkpoint_dir=${checkpoint_dir} best_eval_state=${best_eval_state} init_policy=${init_policy:-none}"
  env \
    RL_CHECKPOINT_DIR="${checkpoint_dir}" \
    RL_FOREVER_ITERATIONS="${iterations}" \
    RL_MAX_CYCLES="${max_cycles}" \
    RL_INIT_POLICY="${init_policy}" \
    RL_BEST_EVAL_STATE="${best_eval_state}" \
    bash "${script_dir}/train_forever.sh" "${phase}"
}

run_curriculum() {
  local checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-target-cars-1024x2-gradual-v1}"
  local target_4_iterations="${RL_CURRICULUM_TARGET_4_ITERATIONS:-${RL_CURRICULUM_TARGET_MANY_ITERATIONS:-400}}"
  local target_4_max_cycles="${RL_CURRICULUM_TARGET_4_MAX_CYCLES:-${RL_CURRICULUM_TARGET_MANY_MAX_CYCLES:-1}}"

  run_curriculum_phase "target-easy" "${RL_CURRICULUM_TARGET_EASY_ITERATIONS:-400}" 1 "${checkpoint_dir}" "${RL_INIT_POLICY:-}"
  run_curriculum_phase "target-hard" "${RL_CURRICULUM_TARGET_HARD_ITERATIONS:-400}" 1 "${checkpoint_dir}" ""
  run_curriculum_phase "target-2" "${RL_CURRICULUM_TARGET_2_ITERATIONS:-400}" "${RL_CURRICULUM_TARGET_2_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_curriculum_phase "target-4" "${target_4_iterations}" "${target_4_max_cycles}" "${checkpoint_dir}" ""
  run_curriculum_phase "target-8" "${RL_CURRICULUM_TARGET_8_ITERATIONS:-500}" "${RL_CURRICULUM_TARGET_8_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_curriculum_phase "target-16" "${RL_CURRICULUM_TARGET_16_ITERATIONS:-600}" "${RL_CURRICULUM_TARGET_16_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_curriculum_phase "target-32" "${RL_CURRICULUM_TARGET_32_ITERATIONS:-700}" "${RL_CURRICULUM_TARGET_32_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_curriculum_phase "target-crowd" "${RL_CURRICULUM_TARGET_CROWD_ITERATIONS:-800}" "${RL_CURRICULUM_TARGET_CROWD_MAX_CYCLES:-0}" "${checkpoint_dir}" ""
}

preset="${RL_CURRICULUM_PRESET:-${RL_PRESET:-}}"
if [[ $# -gt 0 ]]; then
  preset="$1"
  shift
fi

if [[ "${preset}" == "-h" || "${preset}" == "--help" ]]; then
  usage
  exit 0
fi

case "${preset}" in
  "")
    ;;
  "curriculum")
    run_curriculum
    exit 0
    ;;
  "target"|"target-easy")
    set_default RL_CONTROLLED_AGENTS "1"
    set_default RL_FIELD_SIZE "1"
    set_default RL_MAP_IDS "map000,map002,map004,map005,map020"
    set_default RL_TARGET_RADIUS "2.35"
    set_default RL_TARGET_HOLD_SECONDS "0.55"
    set_default RL_MAX_GOALS "4"
    set_default RL_MAX_ACTION_STEPS "1000"
    set_default RL_CHECKPOINT_DIR "rl-checkpoints-target-circle-cars-1024-v1"
    ;;
  "target-hard")
    set_default RL_CONTROLLED_AGENTS "1"
    set_default RL_FIELD_SIZE "1"
    set_default RL_MAP_IDS "map001,map003,map006,map007,map012,map017,map019"
    set_default RL_TARGET_RADIUS "1.85"
    set_default RL_TARGET_HOLD_SECONDS "0.75"
    set_default RL_MAX_GOALS "5"
    set_default RL_MAX_ACTION_STEPS "1250"
    set_default RL_CHECKPOINT_DIR "rl-checkpoints-target-circle-cars-1024-v1"
    ;;
  "target-2")
    set_target_cars_defaults "2"
    ;;
  "target-4"|"target-many")
    set_target_cars_defaults "4"
    ;;
  "target-8")
    set_target_cars_defaults "8"
    ;;
  "target-16")
    set_target_cars_defaults "16"
    ;;
  "target-32")
    set_target_cars_defaults "32"
    ;;
  "target-crowd"|"target-50")
    set_target_cars_defaults "50"
    ;;
  *)
    usage >&2
    echo "unknown_preset=${preset}" >&2
    exit 2
    ;;
esac

objective="target"
default_controlled_agents=1
default_field_size=1
default_max_action_steps=1350
default_checkpoint_dir="rl-checkpoints-target-circle-cars-1024-v1"
export_objective="target-circle-v1"
no_reward_summary="${RL_NO_REWARD_SUMMARY:-1}"
if [[ -n "${RL_WORKERS:-}" ]]; then
  workers="${RL_WORKERS}"
elif [[ "${no_reward_summary}" == "1" || "${no_reward_summary}" == "true" ]]; then
  workers=4
else
  workers=0
fi
controlled_agents="${RL_CONTROLLED_AGENTS:-${default_controlled_agents}}"
field_size="${RL_FIELD_SIZE:-${default_field_size}}"
action_repeat="${RL_ACTION_REPEAT:-4}"
max_action_steps="${RL_MAX_ACTION_STEPS:-${default_max_action_steps}}"
max_goals="${RL_MAX_GOALS:-6}"
target_radius="${RL_TARGET_RADIUS:-1.65}"
target_hold_seconds="${RL_TARGET_HOLD_SECONDS:-0.85}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
lr="${RL_LR:-3e-4}"
hidden_size="${RL_HIDDEN_SIZE:-1024}"
hidden_layers="${RL_HIDDEN_LAYERS:-2}"
hidden_activation="${RL_HIDDEN_ACTIVATION:-tanh}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-20}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-${default_checkpoint_dir}}"
init_policy="${RL_INIT_POLICY:-}"
iterations_per_cycle="${RL_FOREVER_ITERATIONS:-100}"
max_cycles="${RL_MAX_CYCLES:-0}"
package_every_cycles="${RL_PACKAGE_EVERY_CYCLES:-1}"
num_gpus="${RL_NUM_GPUS:-0}"
map_ids="${RL_MAP_IDS:-}"
ray_num_cpus="${RL_RAY_NUM_CPUS:-0}"
ray_temp_dir="${RL_RAY_TEMP_DIR:-}"
build_before_training="${RL_BUILD_BEFORE_TRAINING:-1}"
desktop_jar="${RL_JAR:-desktop/target/ratass-desktop-1.0.jar}"
best_export="${RL_BEST_EXPORT:-1}"
best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
best_eval_episodes_per_map="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
best_eval_episodes="${RL_BEST_EVAL_EPISODES:-0}"
best_eval_field_size="${RL_BEST_EVAL_FIELD_SIZE:-${field_size}}"
best_eval_steps="${RL_BEST_EVAL_STEPS:-0}"
best_eval_map_ids="${RL_BEST_EVAL_MAP_IDS-${map_ids}}"
best_eval_state="${RL_BEST_EVAL_STATE:-}"

checkpoint_file="${checkpoint_dir}/rllib_checkpoint.json"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --action-repeat "${action_repeat}"
  --max-action-steps "${max_action_steps}"
  --max-goals "${max_goals}"
  --target-radius "${target_radius}"
  --target-hold-seconds "${target_hold_seconds}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --lr "${lr}"
  --hidden-size "${hidden_size}"
  --hidden-layers "${hidden_layers}"
  --hidden-activation "${hidden_activation}"
  --checkpoint-every "${checkpoint_every}"
  --num-gpus "${num_gpus}"
  --objective "${objective}"
)

if [[ -n "${map_ids}" ]]; then
  common_args+=(--map-ids "${map_ids}")
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
if [[ "${best_export}" == "1" || "${best_export}" == "true" ]]; then
  common_args+=(
    --best-export-output "${best_output}"
    --best-export-objective "${export_objective}"
    --best-eval-episodes-per-map "${best_eval_episodes_per_map}"
    --best-eval-episodes "${best_eval_episodes}"
    --best-eval-field-size "${best_eval_field_size}"
    --best-eval-steps "${best_eval_steps}"
  )
  if [[ -n "${best_eval_map_ids}" ]]; then
    common_args+=(--best-eval-map-ids "${best_eval_map_ids}")
  fi
  if [[ -n "${best_eval_state}" ]]; then
    common_args+=(--best-eval-state "${best_eval_state}")
  fi
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
  if [[ "${best_export}" == "1" || "${best_export}" == "true" ]]; then
    echo "best_export_mode=enabled latest_checkpoint_export=skipped"
  else
    export_policy
  fi
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
  elif [[ -n "${init_policy}" ]]; then
    cycle_args+=(--init-policy "${init_policy}")
  fi

  echo "cycle=${cycle} iterations=${iterations_per_cycle} resume=${resume} init_policy=${init_policy:-none}"
  "${python_bin}" tools/rl/train_rllib.py "${cycle_args[@]}" --iterations "${iterations_per_cycle}"

  if [[ "${best_export}" == "1" || "${best_export}" == "true" ]]; then
    echo "best_export_mode=enabled latest_checkpoint_export=skipped"
  else
    export_policy
  fi
  if should_package_cycle "${cycle}"; then
    package_game
  fi
  if [[ "${max_cycles}" != "0" && "${cycle}" -ge "${max_cycles}" ]]; then
    echo "max_cycles_reached=${max_cycles}"
    exit 0
  fi
done
