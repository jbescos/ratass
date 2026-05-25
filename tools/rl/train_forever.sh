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

is_true() {
  [[ "${1:-}" == "1" || "${1:-}" == "true" || "${1:-}" == "yes" ]]
}

fresh_start_enabled() {
  is_true "${RL_FRESH_START:-1}"
}

clean_checkpoint_dir_for_fresh_start() {
  local checkpoint_dir="$1"
  if ! fresh_start_enabled; then
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

usage() {
  cat <<'EOF'
usage: bash tools/rl/train_forever.sh [preset]

presets:
  race-single       one learner car; staged 1, 2, 3 checkpoints, then full lap
  race-2            two learner cars; staged 1, 2, 3 checkpoints, then full lap
  race-4            four learner cars; staged 1, 2, 3 checkpoints, then full lap
  race-8            eight learner cars; staged 1, 2, 3 checkpoints, then full lap
  race-16           sixteen learner cars; staged 1, 2, 3 checkpoints, then full lap
  race-20           twenty learner cars; staged 1, 2, 3 checkpoints, then full lap
  race              alias for race-single
  diagnostic        short staged race run: one, two, then four cars
  quick             alias for diagnostic
  fast              alias for diagnostic
  curriculum        staged race run ending with 20 cars forever
EOF
}

set_race_cars_defaults() {
  local car_count="$1"
  set_default RL_OBJECTIVE "race"
  set_default RL_CONTROLLED_AGENTS "${car_count}"
  set_default RL_FIELD_SIZE "${car_count}"
  set_default RL_CHECKPOINT_RADIUS "3.00"
  set_default RL_CHECKPOINT_DEADLINE_SECONDS "60"
  set_default RL_MAX_CHECKPOINTS "-1"
  set_default RL_MAX_ACTION_STEPS "6400"
  set_default RL_CHECKPOINT_DIR "rl-checkpoints-race-physics-v1"
  set_default RL_BEST_EVAL_STEPS "6400"
  set_default RL_NO_REWARD_SUMMARY "0"
}

run_curriculum_phase() {
  local phase="$1"
  local preset="$2"
  local iterations="$3"
  local max_cycles="$4"
  local checkpoint_dir="$5"
  local init_policy="$6"
  local max_checkpoints="$7"
  local best_eval_state="${checkpoint_dir}/best-eval/${phase}/best_policy.json"
  local best_eval_min_checkpoints="${RL_BEST_EVAL_MIN_CHECKPOINTS:-}"
  local phase_best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
  if [[ "${iterations}" -le 0 ]]; then
    echo "curriculum_phase_skip=${phase} iterations=${iterations} max_checkpoints=${max_checkpoints}"
    return
  fi
  if [[ -z "${best_eval_min_checkpoints}" ]]; then
    if [[ "${max_checkpoints}" =~ ^[1-9][0-9]*$ ]]; then
      best_eval_min_checkpoints="${max_checkpoints}"
    else
      best_eval_min_checkpoints="1"
    fi
  fi
  echo "curriculum_phase=${phase} preset=${preset} iterations=${iterations} max_cycles=${max_cycles} max_checkpoints=${max_checkpoints} checkpoint_dir=${checkpoint_dir} best_eval_state=${best_eval_state} best_output=${phase_best_output} init_policy=${init_policy:-none}"
  env \
    RL_CHECKPOINT_DIR="${checkpoint_dir}" \
    RL_FOREVER_ITERATIONS="${iterations}" \
    RL_MAX_CYCLES="${max_cycles}" \
    RL_INIT_POLICY="${init_policy}" \
    RL_BEST_EVAL_STATE="${best_eval_state}" \
    RL_BEST_EVAL_MIN_CHECKPOINTS="${best_eval_min_checkpoints}" \
    RL_BEST_OUTPUT="${phase_best_output}" \
    RL_MAX_CHECKPOINTS="${max_checkpoints}" \
    RL_CHECKPOINT_STAGE_ACTIVE=1 \
    RL_FRESH_START=0 \
    bash "${script_dir}/train_forever.sh" "${preset}"
}

checkpoint_stage_label() {
  case "$1" in
    -1) printf '%s\n' "lap" ;;
    *) printf 'cp%s\n' "$1" ;;
  esac
}

run_checkpoint_curriculum_for_preset() {
  local preset="$1"
  local total_iterations="$2"
  local final_max_cycles="$3"
  local checkpoint_dir="$4"
  local init_policy="$5"
  local base=$((total_iterations / 4))
  local remainder=$((total_iterations % 4))
  local checkpoints=(1 2 3 -1)
  local index

  for index in 0 1 2 3; do
    local phase_iterations="${base}"
    if [[ "${index}" -lt "${remainder}" ]]; then
      phase_iterations=$((phase_iterations + 1))
    fi
    local max_cycles=1
    if [[ "${index}" -eq 3 ]]; then
      max_cycles="${final_max_cycles}"
    fi
    local checkpoint_target="${checkpoints[index]}"
    local checkpoint_label
    checkpoint_label="$(checkpoint_stage_label "${checkpoint_target}")"
    run_curriculum_phase \
      "${preset}-${checkpoint_label}" \
      "${preset}" \
      "${phase_iterations}" \
      "${max_cycles}" \
      "${checkpoint_dir}" \
      "${init_policy}" \
      "${checkpoint_target}"
    init_policy=""
  done
}

run_race_curriculum() {
  local checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-race-f1-1024x2-v1}"
  local race_1_iterations="${RL_CURRICULUM_RACE_1_ITERATIONS:-400}"
  local race_2_iterations="${RL_CURRICULUM_RACE_2_ITERATIONS:-400}"
  local race_4_iterations="${RL_CURRICULUM_RACE_4_ITERATIONS:-400}"
  local race_8_iterations="${RL_CURRICULUM_RACE_8_ITERATIONS:-500}"
  local race_16_iterations="${RL_CURRICULUM_RACE_16_ITERATIONS:-600}"
  local race_20_iterations="${RL_CURRICULUM_RACE_20_ITERATIONS:-800}"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  run_checkpoint_curriculum_for_preset "race-single" "${race_1_iterations}" "${RL_CURRICULUM_RACE_1_MAX_CYCLES:-1}" "${checkpoint_dir}" "${RL_INIT_POLICY:-}"
  run_checkpoint_curriculum_for_preset "race-2" "${race_2_iterations}" "${RL_CURRICULUM_RACE_2_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_checkpoint_curriculum_for_preset "race-4" "${race_4_iterations}" "${RL_CURRICULUM_RACE_4_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_checkpoint_curriculum_for_preset "race-8" "${race_8_iterations}" "${RL_CURRICULUM_RACE_8_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_checkpoint_curriculum_for_preset "race-16" "${race_16_iterations}" "${RL_CURRICULUM_RACE_16_MAX_CYCLES:-1}" "${checkpoint_dir}" ""
  run_checkpoint_curriculum_for_preset "race-20" "${race_20_iterations}" "${RL_CURRICULUM_RACE_20_MAX_CYCLES:-0}" "${checkpoint_dir}" ""
}

run_curriculum() {
  run_race_curriculum
}

run_diagnostic() {
  local checkpoint_dir="${RL_DIAGNOSTIC_CHECKPOINT_DIR:-rl-checkpoints-diagnostic-race-f1-v1}"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  run_checkpoint_curriculum_for_preset "race-single" "${RL_DIAGNOSTIC_RACE_1_ITERATIONS:-40}" 1 "${checkpoint_dir}" "${RL_INIT_POLICY:-}"
  run_checkpoint_curriculum_for_preset "race-2" "${RL_DIAGNOSTIC_RACE_2_ITERATIONS:-40}" 1 "${checkpoint_dir}" ""
  run_checkpoint_curriculum_for_preset "race-4" "${RL_DIAGNOSTIC_RACE_4_ITERATIONS:-40}" 1 "${checkpoint_dir}" ""
}

run_direct_race_checkpoint_curriculum() {
  local preset="$1"
  local checkpoint_dir="${RL_CHECKPOINT_DIR:-rl-checkpoints-race-physics-v1}"
  local iterations="${RL_FOREVER_ITERATIONS:-100}"
  local max_cycles="${RL_MAX_CYCLES:-0}"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  run_checkpoint_curriculum_for_preset \
    "${preset}" \
    "${iterations}" \
    "${max_cycles}" \
    "${checkpoint_dir}" \
    "${RL_INIT_POLICY:-}"
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
    set_race_cars_defaults "1"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-single"
      exit 0
    fi
    ;;
  "curriculum")
    run_curriculum
    exit 0
    ;;
  "diagnostic"|"quick"|"fast")
    run_diagnostic
    exit 0
    ;;
  "race"|"race-single")
    set_race_cars_defaults "1"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-single"
      exit 0
    fi
    ;;
  "race-2")
    set_race_cars_defaults "2"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-2"
      exit 0
    fi
    ;;
  "race-4")
    set_race_cars_defaults "4"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-4"
      exit 0
    fi
    ;;
  "race-8")
    set_race_cars_defaults "8"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-8"
      exit 0
    fi
    ;;
  "race-16")
    set_race_cars_defaults "16"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-16"
      exit 0
    fi
    ;;
  "race-20"|"race-crowd")
    set_race_cars_defaults "20"
    if ! is_true "${RL_CHECKPOINT_STAGE_ACTIVE:-0}" && is_true "${RL_CHECKPOINT_CURRICULUM:-1}"; then
      run_direct_race_checkpoint_curriculum "race-20"
      exit 0
    fi
    ;;
  *)
    usage >&2
    echo "unknown_preset=${preset}" >&2
    exit 2
    ;;
esac

objective="race"
default_controlled_agents=1
default_field_size=1
default_max_action_steps=6400
default_checkpoint_dir="rl-checkpoints-race-physics-v1"
export_objective="race-checkpoints-v1"
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
max_checkpoints="${RL_MAX_CHECKPOINTS:-6}"
checkpoint_radius="${RL_CHECKPOINT_RADIUS:-3.00}"
checkpoint_deadline_seconds="${RL_CHECKPOINT_DEADLINE_SECONDS:-0}"
random_race_spawns="${RL_RANDOM_RACE_SPAWNS:-1}"
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
sample_timeout_s="${RL_SAMPLE_TIMEOUT_S:-600}"
build_before_training="${RL_BUILD_BEFORE_TRAINING:-1}"
desktop_jar="${RL_JAR:-desktop/target/ratass-desktop-1.0.jar}"
best_export="${RL_BEST_EXPORT:-1}"
best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
best_eval_episodes_per_map="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
best_eval_episodes="${RL_BEST_EVAL_EPISODES:-0}"
best_eval_min_checkpoints="${RL_BEST_EVAL_MIN_CHECKPOINTS:-1}"
best_eval_controlled_agents="${RL_BEST_EVAL_CONTROLLED_AGENTS:-${controlled_agents}}"
best_eval_field_size="${RL_BEST_EVAL_FIELD_SIZE:-${field_size}}"
best_eval_steps="${RL_BEST_EVAL_STEPS:-0}"
best_eval_map_ids="${RL_BEST_EVAL_MAP_IDS-${map_ids}}"
best_eval_state="${RL_BEST_EVAL_STATE:-}"
best_eval_ignore_installed="${RL_BEST_EVAL_IGNORE_INSTALLED:-1}"

clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
checkpoint_file="${checkpoint_dir}/rllib_checkpoint.json"

echo "training_step_start preset=${preset:-race} objective=${objective} checkpoint_dir=${checkpoint_dir} fresh_start=${RL_FRESH_START:-1} iterations_per_cycle=${iterations_per_cycle} max_cycles=${max_cycles} controlled_agents=${controlled_agents} field_size=${field_size} maps=${map_ids:-all} random_race_spawns=${random_race_spawns} checkpoint_radius=${checkpoint_radius} checkpoint_deadline_seconds=${checkpoint_deadline_seconds} max_checkpoints=${max_checkpoints} max_action_steps=${max_action_steps} hidden=${hidden_size}x${hidden_layers} activation=${hidden_activation} workers=${workers} ray_cpus=${ray_num_cpus} sample_timeout_s=${sample_timeout_s} init_policy=${init_policy:-none} best_eval_controlled_agents=${best_eval_controlled_agents} best_eval_maps=${best_eval_map_ids:-all}"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --action-repeat "${action_repeat}"
  --max-action-steps "${max_action_steps}"
  --max-checkpoints "${max_checkpoints}"
  --checkpoint-radius "${checkpoint_radius}"
  --checkpoint-deadline-seconds "${checkpoint_deadline_seconds}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --lr "${lr}"
  --hidden-size "${hidden_size}"
  --hidden-layers "${hidden_layers}"
  --hidden-activation "${hidden_activation}"
  --checkpoint-every "${checkpoint_every}"
  --num-gpus "${num_gpus}"
  --objective "${objective}"
  --sample-timeout-s "${sample_timeout_s}"
)

if [[ -n "${map_ids}" ]]; then
  common_args+=(--map-ids "${map_ids}")
fi
if is_true "${random_race_spawns}"; then
  common_args+=(--random-race-spawns)
else
  common_args+=(--fixed-race-spawns)
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
    --best-eval-min-checkpoints "${best_eval_min_checkpoints}"
    --best-eval-controlled-agents "${best_eval_controlled_agents}"
    --best-eval-field-size "${best_eval_field_size}"
    --best-eval-steps "${best_eval_steps}"
  )
  if [[ -n "${best_eval_map_ids}" ]]; then
    common_args+=(--best-eval-map-ids "${best_eval_map_ids}")
  fi
  if [[ -n "${best_eval_state}" ]]; then
    common_args+=(--best-eval-state "${best_eval_state}")
  fi
  if is_true "${best_eval_ignore_installed}"; then
    common_args+=(--best-eval-ignore-installed)
  fi
fi

export_policy() {
  local best_eval_dir="${checkpoint_dir}/best-eval"
  local state_file="${best_eval_dir}/best_policy.json"
  if [[ -n "${best_eval_state}" ]]; then
    state_file="${best_eval_state}"
    best_eval_dir="$(dirname "${best_eval_state}")"
  fi
  local best_archive="${best_eval_dir}/best_policy_export.json"
  if [[ "${best_export}" == "1" || "${best_export}" == "true" ]] && [[ -f "${best_archive}" ]]; then
    mkdir -p "$(dirname "${best_output}")"
    cp "${best_archive}" "${best_output}"
    echo "export_best_policy archive=${best_archive} output=${best_output}"
    return 0
  fi
  if [[ "${best_export}" == "1" || "${best_export}" == "true" ]] \
    && ! is_true "${RL_FORCE_EXPORT_ON_FINISH:-0}"; then
    if [[ -f "${state_file}" && -f "${best_output}" ]]; then
      echo "best_policy_kept state=${state_file} output=${best_output}"
      return 0
    fi
    echo "best_export_managed_by_train_rllib=1"
    return 0
  fi

  if [[ ! -f "${checkpoint_file}" ]]; then
    echo "checkpoint_missing=${checkpoint_file}"
    return 0
  fi

  echo "export_latest_policy checkpoint_dir=${checkpoint_dir} output=${best_output}"
  "${python_bin}" tools/rl/export_policy.py \
    --checkpoint-dir "${checkpoint_dir}" \
    --output "${best_output}" \
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
  elif [[ -n "${init_policy}" ]]; then
    cycle_args+=(--init-policy "${init_policy}")
  fi

  echo "training_step_cycle preset=${preset:-race} cycle=${cycle} iterations=${iterations_per_cycle} resume=${resume} init_policy=${init_policy:-none} checkpoint_dir=${checkpoint_dir}"
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
