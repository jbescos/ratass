#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python_bin="${PYTHON_BIN:-.venv-rl/bin/python}"

source "${script_dir}/policy_profiles.sh"

if [[ -n "${RL_POLICY_ID:-}" ]] && ! rl_policy_batch_active; then
  normalized_policy_id="$(rl_policy_normalize_id "${RL_POLICY_ID}")"
  export RL_POLICY_ID="${normalized_policy_id}"
  rl_policy_load_properties "${RL_POLICY_ID}"
  export RL_BEST_OUTPUT="${RL_BEST_OUTPUT:-assets/ai/policies/${RL_POLICY_ID}/rl_enemy_policy.json}"
  export RL_CURRICULUM_CHECKPOINT_DIR="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints/policies/${RL_POLICY_ID}/forever}"
  export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-${RL_CURRICULUM_CHECKPOINT_DIR}}"
  export RL_TRAIN_LOG="${RL_TRAIN_LOG:-logs/rl-policy-${RL_POLICY_ID}-forever.log}"
  export RL_POLICY_EXPLICIT_SELECTION=1
  rl_policy_configure_resume
fi

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

progress_percent() {
  local current="$1"
  local total="$2"
  if [[ ! "${current}" =~ ^[0-9]+$ || ! "${total}" =~ ^[0-9]+$ || "${total}" -le 0 ]]; then
    printf '%s\n' "0%"
    return
  fi
  printf '%s%%\n' $(((current * 100 + total / 2) / total))
}

fresh_start_enabled() {
  is_true "${RL_FRESH_START:-1}"
}

is_positive_integer() {
  [[ "${1:-}" =~ ^[1-9][0-9]*$ ]]
}

is_non_positive_integer() {
  [[ "${1:-}" =~ ^-?[0-9]+$ && "${1}" -le 0 ]]
}

is_route_target_training_stage() {
  is_positive_integer "$1"
}

controlled_agents_for_preset() {
  case "${1:-race-single}" in
    ""|"race"|"race-single") printf '%s\n' "1" ;;
    "race-2") printf '%s\n' "2" ;;
    "race-4") printf '%s\n' "4" ;;
    "race-8") printf '%s\n' "8" ;;
    "race-16") printf '%s\n' "16" ;;
    "race-20"|"race-crowd") printf '%s\n' "20" ;;
    *)
      echo "unknown_race_preset=${1:-}" >&2
      return 2
      ;;
  esac
}

generate_route_spawn_seed() {
  local now
  local seed
  now="$(date +%s)"
  seed=$((((now + RANDOM * 32768 + RANDOM) % 2147483646) + 1))
  printf '%s\n' "${seed}"
}

ensure_route_spawn_seed_session() {
  local checkpoint_dir="$1"
  if [[ -n "${RL_ROUTE_SPAWN_SEED_FILE:-}" ]]; then
    return
  fi
  if [[ -z "${RL_ROUTE_SPAWN_RUN_ID:-}" ]]; then
    export RL_ROUTE_SPAWN_RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-$$-$RANDOM"
  fi
  if [[ -z "${RL_ROUTE_SPAWN_SEED_DIR:-}" ]]; then
    export RL_ROUTE_SPAWN_SEED_DIR="${checkpoint_dir}/spawn-seeds/${RL_ROUTE_SPAWN_RUN_ID}"
  fi
  echo "route_spawn_seed_session run_id=${RL_ROUTE_SPAWN_RUN_ID} dir=${RL_ROUTE_SPAWN_SEED_DIR}"
}

route_spawn_seed_path() {
  local checkpoint_dir="$1"
  local phase="$2"
  local seed_dir="${RL_ROUTE_SPAWN_SEED_DIR:-${checkpoint_dir}/spawn-seeds}"
  printf '%s/%s.properties\n' "${seed_dir}" "$(phase_state_key "${phase}")"
}

read_route_spawn_seed() {
  local seed_file="$1"
  if [[ ! -f "${seed_file}" ]]; then
    return 1
  fi
  awk -F= '$1 == "seed" { print $2; exit }' "${seed_file}"
}

read_or_create_route_spawn_seed() {
  local seed_file="$1"
  local phase="$2"
  local route_targets="$3"
  local seed=""
  seed="$(read_route_spawn_seed "${seed_file}" || true)"
  if [[ "${seed}" =~ ^[0-9]+$ && "${seed}" -gt 0 ]]; then
    echo "route_spawn_seed_reused phase=${phase} seed=${seed} file=${seed_file}" >&2
    printf '%s\n' "${seed}"
    return 0
  fi

  seed="$(generate_route_spawn_seed)"
  mkdir -p "$(dirname "${seed_file}")"
  {
    printf 'mode=route_random_spawns\n'
    printf 'run_id=%s\n' "${RL_ROUTE_SPAWN_RUN_ID:-}"
    printf 'phase=%s\n' "${phase}"
    printf 'route_targets=%s\n' "${route_targets}"
    printf 'seed=%s\n' "${seed}"
    printf 'created_at=%s\n' "$(date -Is)"
  } > "${seed_file}"
  echo "route_spawn_seed_created phase=${phase} seed=${seed} file=${seed_file}" >&2
  printf '%s\n' "${seed}"
}

record_route_spawn_seed_if_missing() {
  local seed_file="$1"
  local phase="$2"
  local route_targets="$3"
  local seed="$4"
  if [[ -f "${seed_file}" ]]; then
    return
  fi
  mkdir -p "$(dirname "${seed_file}")"
  {
    printf 'mode=route_random_spawns\n'
    printf 'run_id=%s\n' "${RL_ROUTE_SPAWN_RUN_ID:-}"
    printf 'phase=%s\n' "${phase}"
    printf 'route_targets=%s\n' "${route_targets}"
    printf 'seed=%s\n' "${seed}"
    printf 'created_at=%s\n' "$(date -Is)"
  } > "${seed_file}"
  echo "route_spawn_seed_recorded phase=${phase} seed=${seed} file=${seed_file}" >&2
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
  race-single       one learner car; staged route targets, then full lap
  race-2            two learner cars; full-lap fixed-grid training
  race-4            four learner cars; full-lap fixed-grid training
  race-8            eight learner cars; full-lap fixed-grid training
  race-16           sixteen learner cars; full-lap fixed-grid training
  race-20           twenty learner cars; full-lap fixed-grid training
  race              alias for race-single
  diagnostic        short staged race run: one, two, then four cars
  quick             alias for diagnostic
  fast              alias for diagnostic
  curriculum        staged race run using RL_STAGE_ROUTE_TARGETS,
                    RL_STAGE_ITERATIONS, and RL_STAGE_NUMBER_OF_CARS
EOF
}

set_race_cars_defaults() {
  local car_count="$1"
  set_default RL_OBJECTIVE "race"
  set_default RL_CONTROLLED_AGENTS "${car_count}"
  set_default RL_FIELD_SIZE "${car_count}"
  set_default RL_ROUTE_TARGETS "-1"
  set_default RL_MAX_ACTION_STEPS "6400"
  set_default RL_CHECKPOINT_DIR "rl-checkpoints-race-physics-v1"
  set_default RL_BEST_EVAL_STEPS "auto"
  set_default RL_NO_REWARD_SUMMARY "1"
}

run_curriculum_phase() {
  local phase="$1"
  local preset="$2"
  local iterations="$3"
  local max_cycles="$4"
  local checkpoint_dir="$5"
  local init_policy="$6"
  local route_targets="$7"
  local route_target_fraction="${8:-0}"
  local stage_index="${9:-1}"
  local stage_total="${10:-1}"
  local profile_stage_index="${11:-${stage_index}}"
  local profile_stage_total="${12:-${stage_total}}"
  local overall_stage_index="${13:-${profile_stage_index}}"
  local overall_stage_total="${14:-${profile_stage_total}}"
  local best_eval_state="${checkpoint_dir}/best-eval/${phase}/best_policy.json"
  local best_eval_min_route_targets="${RL_BEST_EVAL_MIN_ROUTE_TARGETS:-}"
  local phase_best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
  local phase_controlled_agents
  phase_controlled_agents="$(controlled_agents_for_preset "${preset}")"
  local phase_spawn_mode="fixed-grid"
  local phase_spawn_seed_file=""
  local phase_seed="${RL_SEED:-1}"
  local phase_route_targets="${route_targets}"
  local phase_route_target_fraction="${route_target_fraction:-0}"
  local phase_map_ids="${RL_MAP_IDS:-}"
  local phase_best_eval_map_ids="${RL_BEST_EVAL_MAP_IDS:-}"
  if [[ "${phase_route_targets}" == "-2" ]]; then
    phase_route_targets="-1"
    phase_route_target_fraction="0"
    phase_map_ids="${RL_LAP_EASY_MAP_IDS:-auto-lap-easy}"
    phase_best_eval_map_ids="${RL_LAP_EASY_BEST_EVAL_MAP_IDS:-${phase_map_ids}}"
  fi
  if [[ "${iterations}" -le 0 ]]; then
    echo "curriculum_phase_skip policy=${RL_POLICY_ID:-legacy} profile=${RL_POLICY_INDEX:-?}/${RL_POLICY_TOTAL:-?} phase=${phase} iterations=${iterations} route_targets=${phase_route_targets} route_target_fraction=${phase_route_target_fraction}"
    return
  fi
  if [[ -z "${best_eval_min_route_targets}" ]]; then
    if [[ "${phase_route_targets}" =~ ^[1-9][0-9]*$ ]]; then
      best_eval_min_route_targets="${phase_route_targets}"
    else
      best_eval_min_route_targets="1"
    fi
  fi
  if phase_state_completed "${phase}" && ! is_true "${RL_RETRAIN_COMPLETED:-0}"; then
    echo "curriculum_phase_skip_completed policy=${RL_POLICY_ID:-legacy} profile=${RL_POLICY_INDEX:-?}/${RL_POLICY_TOTAL:-?} phase=${phase} state=${RL_POLICY_TRAINING_STATE}"
    return
  fi
  if is_route_target_training_stage "${phase_route_targets}"; then
    if [[ "${phase_controlled_agents}" -ne 1 ]]; then
      echo "invalid_route_target_training_cars=${phase_controlled_agents} phase=${phase} route_targets=${phase_route_targets} required_cars=1 reason=route_target_training_uses_saved_random_single_car_spawns" >&2
      exit 2
    fi
    phase_spawn_mode="saved-random-route"
    phase_spawn_seed_file="${RL_ROUTE_SPAWN_SEED_FILE:-$(route_spawn_seed_path "${checkpoint_dir}" "${phase}")}"
    phase_seed="$(read_or_create_route_spawn_seed "${phase_spawn_seed_file}" "${phase}" "${phase_route_targets}")"
  elif is_non_positive_integer "${phase_route_targets}"; then
    phase_spawn_mode="fixed-grid"
  else
    echo "invalid_route_targets=${phase_route_targets} phase=${phase}" >&2
    exit 2
  fi
  if [[ -n "${RL_POLICY_STATUS_FILE:-}" ]]; then
    mkdir -p "$(dirname "${RL_POLICY_STATUS_FILE}")"
    {
      printf 'status=running\n'
      printf 'updated_at=%s\n' "$(date -Is)"
      printf 'policy_id=%s\n' "${RL_POLICY_ID:-legacy}"
      printf 'policy_index=%s\n' "${RL_POLICY_INDEX:-}"
      printf 'policy_total=%s\n' "${RL_POLICY_TOTAL:-}"
      printf 'current_phase=%s\n' "${phase}"
      printf 'preset=%s\n' "${preset}"
      printf 'phase_iterations=%s\n' "${iterations}"
      printf 'route_targets=%s\n' "${phase_route_targets}"
      printf 'route_target_fraction=%s\n' "${phase_route_target_fraction}"
      printf 'map_ids=%s\n' "${phase_map_ids:-}"
      printf 'spawn_mode=%s\n' "${phase_spawn_mode}"
      printf 'spawn_seed=%s\n' "${phase_seed}"
      printf 'spawn_seed_file=%s\n' "${phase_spawn_seed_file}"
      printf 'spawn_seed_run_id=%s\n' "${RL_ROUTE_SPAWN_RUN_ID:-}"
      printf 'spawn_seed_dir=%s\n' "${RL_ROUTE_SPAWN_SEED_DIR:-}"
      printf 'stage_index=%s\n' "${stage_index}"
      printf 'stage_total=%s\n' "${stage_total}"
      printf 'profile_stage_index=%s\n' "${profile_stage_index}"
      printf 'profile_stage_total=%s\n' "${profile_stage_total}"
      printf 'profile_progress_percent=%s\n' "$(progress_percent "${profile_stage_index}" "${profile_stage_total}")"
      printf 'overall_stage_index=%s\n' "${overall_stage_index}"
      printf 'overall_stage_total=%s\n' "${overall_stage_total}"
      printf 'overall_progress_percent=%s\n' "$(progress_percent "${overall_stage_index}" "${overall_stage_total}")"
      printf 'checkpoint_dir=%s\n' "${checkpoint_dir}"
      printf 'output=%s\n' "${phase_best_output}"
      printf 'training_state=%s\n' "${RL_POLICY_TRAINING_STATE:-}"
    } > "${RL_POLICY_STATUS_FILE}"
  fi
  append_phase_state \
    "status=running" \
    "completed_profile=0" \
    "current_phase=${phase}" \
    "preset=${preset}" \
    "phase_iterations=${iterations}" \
    "max_cycles=${max_cycles}" \
    "route_targets=${phase_route_targets}" \
    "route_target_fraction=${phase_route_target_fraction}" \
    "map_ids=${phase_map_ids:-}" \
    "spawn_mode=${phase_spawn_mode}" \
    "spawn_seed=${phase_seed}" \
    "spawn_seed_file=${phase_spawn_seed_file}" \
    "spawn_seed_run_id=${RL_ROUTE_SPAWN_RUN_ID:-}" \
    "spawn_seed_dir=${RL_ROUTE_SPAWN_SEED_DIR:-}" \
    "stage_index=${stage_index}" \
    "stage_total=${stage_total}" \
    "profile_stage_index=${profile_stage_index}" \
    "profile_stage_total=${profile_stage_total}" \
    "profile_progress_percent=$(progress_percent "${profile_stage_index}" "${profile_stage_total}")" \
    "overall_stage_index=${overall_stage_index}" \
    "overall_stage_total=${overall_stage_total}" \
    "overall_progress_percent=$(progress_percent "${overall_stage_index}" "${overall_stage_total}")"
  echo "============================================================"
  echo "CURRENT_TRAINING_STAGE_START"
  echo "CURRENT_TRAINING_DRIVER=${RL_POLICY_ID:-legacy} profile=${RL_POLICY_INDEX:-?}/${RL_POLICY_TOTAL:-?}"
  echo "CURRENT_TRAINING_STAGE=${phase} stage=${stage_index}/${stage_total} stage_progress=$(progress_percent "${stage_index}" "${stage_total}")"
  echo "CURRENT_TRAINING_PROFILE_PROGRESS=${profile_stage_index}/${profile_stage_total} $(progress_percent "${profile_stage_index}" "${profile_stage_total}")"
  echo "CURRENT_TRAINING_OVERALL_PROGRESS=${overall_stage_index}/${overall_stage_total} $(progress_percent "${overall_stage_index}" "${overall_stage_total}")"
  echo "CURRENT_TRAINING_DETAILS preset=${preset} iterations=${iterations} cars=${phase_controlled_agents} route_targets=${phase_route_targets} route_target_fraction=${phase_route_target_fraction} maps=${phase_map_ids:-all} spawn_mode=${phase_spawn_mode} seed=${phase_seed} seed_file=${phase_spawn_seed_file:-none} seed_run=${RL_ROUTE_SPAWN_RUN_ID:-none}"
  echo "============================================================"
  echo "curriculum_phase=${phase} policy=${RL_POLICY_ID:-legacy} profile=${RL_POLICY_INDEX:-?}/${RL_POLICY_TOTAL:-?} preset=${preset} iterations=${iterations} max_cycles=${max_cycles} route_targets=${phase_route_targets} route_target_fraction=${phase_route_target_fraction} maps=${phase_map_ids:-all} spawn_mode=${phase_spawn_mode} seed=${phase_seed} checkpoint_dir=${checkpoint_dir} best_eval_state=${best_eval_state} best_output=${phase_best_output} init_policy=${init_policy:-none}"
  env \
    RL_CHECKPOINT_DIR="${checkpoint_dir}" \
    RL_FOREVER_ITERATIONS="${iterations}" \
    RL_MAX_CYCLES="${max_cycles}" \
    RL_CONTROLLED_AGENTS="${phase_controlled_agents}" \
    RL_FIELD_SIZE="${phase_controlled_agents}" \
    RL_INIT_POLICY="${init_policy}" \
    RL_BEST_EVAL_STATE="${best_eval_state}" \
    RL_BEST_EVAL_MIN_ROUTE_TARGETS="${best_eval_min_route_targets}" \
    RL_BEST_OUTPUT="${phase_best_output}" \
    RL_ROUTE_TARGETS="${phase_route_targets}" \
    RL_ROUTE_TARGET_FRACTION="${phase_route_target_fraction}" \
    RL_MAP_IDS="${phase_map_ids}" \
    RL_BEST_EVAL_MAP_IDS="${phase_best_eval_map_ids}" \
    RL_SEED="${phase_seed}" \
    RL_ROUTE_SPAWN_SEED_FILE="${phase_spawn_seed_file}" \
    RL_ROUTE_PHASE_NAME="${phase}" \
    RL_ROUTE_STAGE_ACTIVE=1 \
    RL_FORCE_FRESH_START=0 \
    RL_FRESH_START=0 \
    bash "${script_dir}/train_forever.sh" "${preset}"
  local phase_key
  phase_key="$(phase_state_key "${phase}")"
  append_phase_state \
    "status=running" \
    "completed_profile=0" \
    "last_completed_phase=${phase}" \
    "completed_phase_${phase_key}=1"
}

route_stage_label() {
  local targets="$1"
  local target_fraction="${2:-0}"
  case "${targets}" in
    -2) printf '%s\n' "lap_easy" ;;
    -1) printf '%s\n' "lap" ;;
    *)
      if [[ "${targets}" =~ ^[1-9][0-9]*$ && "${target_fraction}" != "0" && "${target_fraction}" != "0.0" ]]; then
        local percent
        percent="$(LC_ALL=C awk -v targets="${targets}" -v fraction="${target_fraction}" 'BEGIN {
          if (targets <= 0 || fraction <= 0) exit 2;
          printf "%.6g", targets * fraction * 100.0;
        }')" || {
          printf 'route%s\n' "${targets}"
          return
        }
        percent="${percent//./p}"
        printf 'route%spct\n' "${percent}"
        return
      fi
      printf 'route%s\n' "${targets}"
      ;;
  esac
}

normalize_route_stage() {
  case "$1" in
    "lap_easy"|"easy_lap"|"easy-lap") printf '%s\n' "-2" ;;
    "lap"|"full"|"-1") printf '%s\n' "-1" ;;
    "")
      echo "empty_route_stage=1" >&2
      return 2
      ;;
    *%)
      local percent="${1%\%}"
      LC_ALL=C awk -v percent="${percent}" 'BEGIN {
        if (percent <= 0) exit 2;
        chunks = int(percent * 4 / 100 + 0.999999);
        if (chunks < 1) chunks = 1;
        print chunks;
      }' || {
        echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
        return 2
      }
      ;;
    0.*|1.0|1.00|1.000)
      LC_ALL=C awk -v fraction="$1" 'BEGIN {
        if (fraction <= 0) exit 2;
        chunks = int(fraction * 4 + 0.999999);
        if (chunks < 1) chunks = 1;
        print chunks;
      }' || {
        echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
        return 2
      }
      ;;
    *)
      if [[ "$1" =~ ^[0-9]+$ ]]; then
        printf '%s\n' "$1"
        return 0
      fi
      echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
      return 2
      ;;
  esac
}

route_stage_target_fraction() {
  local spec="$1"
  local chunks="$2"
  case "${spec}" in
    *%)
      local percent="${spec%\%}"
      LC_ALL=C awk -v percent="${percent}" -v chunks="${chunks}" 'BEGIN {
        if (percent <= 0 || chunks <= 0) exit 2;
        printf "%.8g\n", (percent / 100.0) / chunks;
      }' || {
        echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
        return 2
      }
      ;;
    0.*|1.0|1.00|1.000)
      LC_ALL=C awk -v fraction="${spec}" -v chunks="${chunks}" 'BEGIN {
        if (fraction <= 0 || chunks <= 0) exit 2;
        printf "%.8g\n", fraction / chunks;
      }' || {
        echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
        return 2
      }
      ;;
    *)
      printf '%s\n' "0"
      ;;
  esac
}

route_stage_label_from_spec() {
  local spec="$1"
  local normalized="$2"
  case "${spec}" in
    "lap_easy"|"easy_lap"|"easy-lap") printf '%s\n' "lap_easy" ;;
    "lap"|"full"|"-1") printf '%s\n' "lap" ;;
    *%)
      local percent="${spec%\%}"
      percent="${percent//./p}"
      printf 'route%spct\n' "${percent}"
      ;;
    0.*|1.0|1.00|1.000)
      local percent
      percent="$(LC_ALL=C awk -v fraction="${spec}" 'BEGIN {
        if (fraction <= 0) exit 2;
        printf "%.6g", fraction * 100.0;
      }')" || {
        echo "invalid_route_stage=$1 expected=positive_integer_chunk_count_percent_decimal_lap_easy_or_lap" >&2
        return 2
      }
      percent="${percent//./p}"
      printf 'route%spct\n' "${percent}"
      ;;
    *)
      route_stage_label "${normalized}"
      ;;
  esac
}

default_best_eval_steps_for_stage() {
  local route_target="$1"
  local max_steps="$2"
  if [[ ! "${max_steps}" =~ ^[0-9]+$ || "${max_steps}" -le 0 ]]; then
    max_steps=6400
  fi
  if [[ "${route_target}" =~ ^[1-9][0-9]*$ ]]; then
    local target_steps=$((800 + route_target * 800))
    if [[ "${target_steps}" -gt "${max_steps}" ]]; then
      target_steps="${max_steps}"
    fi
    printf '%s\n' "${target_steps}"
    return
  fi
  printf '%s\n' "${max_steps}"
}

split_csv_compact() {
  local raw="${1:-}"
  raw="${raw//[[:space:]]/}"
  if [[ -z "${raw}" ]]; then
    return 0
  fi
  IFS=',' read -r -a split_csv_result <<< "${raw}"
}

available_game_map_ids() {
  local paths=()
  local ids=()
  local path
  local name

  mapfile -t paths < <(find assets/maps -maxdepth 1 -type f -name '*_mask.png' | sort)
  if [[ "${#paths[@]}" -eq 0 ]]; then
    echo "no_mask_maps_found=assets/maps/*_mask.png" >&2
    return 2
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

available_training_map_ids() {
  local paths=()
  local ids=()
  local path
  local name

  mapfile -t paths < <(find tools/rl/trainingMaps -maxdepth 1 -type f -name 'train*_mask.png' | sort)
  if [[ "${#paths[@]}" -eq 0 ]]; then
    printf '%s\n' ""
    return
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

available_lap_easy_map_ids() {
  local paths=()
  local ids=()
  local path
  local name

  mapfile -t paths < <(find tools/rl/trainingMaps -maxdepth 1 -type f -name 'route*_mask.png' | sort)
  if [[ "${#paths[@]}" -eq 0 ]]; then
    printf '%s\n' ""
    return
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

join_map_id_lists() {
  local left="${1:-}"
  local right="${2:-}"
  if [[ -z "${left}" ]]; then
    printf '%s\n' "${right}"
  elif [[ -z "${right}" ]]; then
    printf '%s\n' "${left}"
  else
    printf '%s,%s\n' "${left}" "${right}"
  fi
}

resolve_map_ids_setting() {
  case "${1:-}" in
    "")
      printf '%s\n' ""
      ;;
    "auto-game"|"game"|"all-game")
      available_game_map_ids
      ;;
    "auto-training"|"training"|"all-training")
      available_training_map_ids
      ;;
    "auto-lap-easy"|"lap_easy"|"lap-easy"|"easy-lap")
      available_lap_easy_map_ids
      ;;
    "auto-all"|"all")
      join_map_id_lists "$(available_training_map_ids)" "$(available_game_map_ids)"
      ;;
    *)
      printf '%s\n' "$1"
      ;;
  esac
}

phase_state_key() {
  printf '%s' "$1" | tr -c '[:alnum:]_' '_'
}

phase_state_completed() {
  local phase="$1"
  local key
  if [[ -z "${RL_POLICY_TRAINING_STATE:-}" || ! -f "${RL_POLICY_TRAINING_STATE}" ]]; then
    return 1
  fi
  key="$(phase_state_key "${phase}")"
  grep -q "^completed_phase_${key}=1$" "${RL_POLICY_TRAINING_STATE}"
}

append_phase_state() {
  if [[ -z "${RL_POLICY_TRAINING_STATE:-}" ]]; then
    return
  fi
  mkdir -p "$(dirname "${RL_POLICY_TRAINING_STATE}")"
  {
    printf 'updated_at=%s\n' "$(date -Is)"
    printf 'policy_id=%s\n' "${RL_POLICY_ID:-legacy}"
    printf 'checkpoint_dir=%s\n' "${RL_CURRICULUM_CHECKPOINT_DIR:-${RL_CHECKPOINT_DIR:-}}"
    printf 'output=%s\n' "${RL_BEST_OUTPUT:-}"
    printf '%s\n' "$@"
  } >> "${RL_POLICY_TRAINING_STATE}"
}

preset_for_agent_count() {
  case "$1" in
    1) printf '%s\n' "race-single" ;;
    2|4|8|16|20) printf 'race-%s\n' "$1" ;;
    *)
      echo "unsupported_controlled_agent_stage=$1 supported=1,2,4,8,16,20" >&2
      return 2
      ;;
  esac
}

run_route_curriculum_for_preset() {
  local preset="$1"
  local total_iterations="$2"
  local final_max_cycles="$3"
  local checkpoint_dir="$4"
  local init_policy="$5"
  local stage_specs=()
  local stage_iterations=()
  local stage_cars=()
  local split_csv_result=()
  local index
  local preset_agents
  preset_agents="$(controlled_agents_for_preset "${preset}")"

  split_csv_compact "${RL_STAGE_ROUTE_TARGETS:-5%,10%,25%,50%,75%,lap_easy,lap}"
  stage_specs=("${split_csv_result[@]}")
  if [[ "${#stage_specs[@]}" -eq 0 ]]; then
    echo "empty_stage_schedule=1" >&2
    return 2
  fi

  if [[ -n "${RL_STAGE_ITERATIONS:-}" ]]; then
    split_csv_compact "${RL_STAGE_ITERATIONS}"
    stage_iterations=("${split_csv_result[@]}")
    if [[ "${#stage_iterations[@]}" -ne "${#stage_specs[@]}" ]]; then
      echo "stage_iterations_count_mismatch=${#stage_iterations[@]} expected=${#stage_specs[@]} action=redistribute_total_iterations" >&2
      stage_iterations=()
      local base=$((total_iterations / ${#stage_specs[@]}))
      local remainder=$((total_iterations % ${#stage_specs[@]}))
      for ((index = 0; index < ${#stage_specs[@]}; index++)); do
        local generated_iterations="${base}"
        if [[ "${index}" -lt "${remainder}" ]]; then
          generated_iterations=$((generated_iterations + 1))
        fi
        stage_iterations+=("${generated_iterations}")
      done
    fi
  else
    local base=$((total_iterations / ${#stage_specs[@]}))
    local remainder=$((total_iterations % ${#stage_specs[@]}))
    for ((index = 0; index < ${#stage_specs[@]}; index++)); do
      local generated_iterations="${base}"
      if [[ "${index}" -lt "${remainder}" ]]; then
        generated_iterations=$((generated_iterations + 1))
      fi
      stage_iterations+=("${generated_iterations}")
    done
  fi

  if [[ -n "${RL_STAGE_NUMBER_OF_CARS:-}" ]]; then
    split_csv_compact "${RL_STAGE_NUMBER_OF_CARS}"
    stage_cars=("${split_csv_result[@]}")
    if [[ "${#stage_cars[@]}" -ne "${#stage_specs[@]}" ]]; then
      echo "stage_number_of_cars_count_mismatch=${#stage_cars[@]} expected=${#stage_specs[@]}" >&2
      return 2
    fi
  else
    for ((index = 0; index < ${#stage_specs[@]}; index++)); do
      stage_cars+=("${preset_agents}")
    done
  fi

  local stage_group_index="${RL_STAGE_GROUP_INDEX:-1}"
  local stage_group_total="${RL_STAGE_GROUP_TOTAL:-1}"
  local policy_index="${RL_POLICY_INDEX:-1}"
  local policy_total="${RL_POLICY_TOTAL:-1}"
  if [[ ! "${stage_group_index}" =~ ^[0-9]+$ || "${stage_group_index}" -le 0 ]]; then
    stage_group_index=1
  fi
  if [[ ! "${stage_group_total}" =~ ^[0-9]+$ || "${stage_group_total}" -le 0 ]]; then
    stage_group_total=1
  fi
  if [[ ! "${policy_index}" =~ ^[0-9]+$ || "${policy_index}" -le 0 ]]; then
    policy_index=1
  fi
  if [[ ! "${policy_total}" =~ ^[0-9]+$ || "${policy_total}" -le 0 ]]; then
    policy_total=1
  fi

  local route_stage_total="${#stage_specs[@]}"
  local profile_stage_total=$((stage_group_total * route_stage_total))
  local overall_stage_total=$((policy_total * profile_stage_total))

  echo "route_curriculum_schedule preset=${preset} stages=${RL_STAGE_ROUTE_TARGETS:-5%,10%,25%,50%,75%,lap_easy,lap} stage_iterations=${stage_iterations[*]} stage_cars=${stage_cars[*]} total_iterations=${total_iterations} stage_group=${stage_group_index}/${stage_group_total}"
  for ((index = 0; index < ${#stage_specs[@]}; index++)); do
    local phase_iterations="${stage_iterations[index]}"
    local max_cycles=1
    if [[ "${index}" -eq $((${#stage_specs[@]} - 1)) ]]; then
      max_cycles="${final_max_cycles}"
    fi
    local route_target
    route_target="$(normalize_route_stage "${stage_specs[index]}")"
    local route_target_fraction
    route_target_fraction="$(route_stage_target_fraction "${stage_specs[index]}" "${route_target}")"
    local route_label
    route_label="$(route_stage_label_from_spec "${stage_specs[index]}" "${route_target}")"
    local stage_car_count="${stage_cars[index]}"
    if [[ ! "${stage_car_count}" =~ ^[1-9][0-9]*$ ]]; then
      echo "invalid_stage_number_of_cars=${stage_car_count} stage=${stage_specs[index]}" >&2
      return 2
    fi
    if is_route_target_training_stage "${route_target}" && [[ "${stage_car_count}" -ne 1 ]]; then
      echo "invalid_stage_number_of_cars=${stage_car_count} stage=${stage_specs[index]} route_targets=${route_target} required_cars=1 reason=route_target_training_uses_saved_random_single_car_spawns" >&2
      return 2
    fi
    local stage_preset
    stage_preset="$(preset_for_agent_count "${stage_car_count}")"
    local route_stage_index=$((index + 1))
    local profile_stage_index=$(((stage_group_index - 1) * route_stage_total + route_stage_index))
    local overall_stage_index=$(((policy_index - 1) * profile_stage_total + profile_stage_index))
    run_curriculum_phase \
      "${stage_preset}-${route_label}" \
      "${stage_preset}" \
      "${phase_iterations}" \
      "${max_cycles}" \
      "${checkpoint_dir}" \
      "${init_policy}" \
      "${route_target}" \
      "${route_target_fraction}" \
      "${route_stage_index}" \
      "${route_stage_total}" \
      "${profile_stage_index}" \
      "${profile_stage_total}" \
      "${overall_stage_index}" \
      "${overall_stage_total}"
    init_policy=""
  done
}

run_race_curriculum() {
  local checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints-curriculum-race-f1-1024x2-v1}"
  local total_iterations="${RL_CURRICULUM_ITERATIONS:-${RL_FOREVER_ITERATIONS:-400}}"
  local final_max_cycles="${RL_MAX_CYCLES:-1}"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  ensure_route_spawn_seed_session "${checkpoint_dir}"
  run_route_curriculum_for_preset \
    "race-single" \
    "${total_iterations}" \
    "${final_max_cycles}" \
    "${checkpoint_dir}" \
    "${RL_INIT_POLICY:-}"
}

run_curriculum() {
  run_race_curriculum
}

run_diagnostic() {
  local checkpoint_dir="${RL_DIAGNOSTIC_CHECKPOINT_DIR:-rl-checkpoints-diagnostic-race-f1-v1}"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  ensure_route_spawn_seed_session "${checkpoint_dir}"
  RL_STAGE_GROUP_INDEX=1 RL_STAGE_GROUP_TOTAL=3 \
  RL_STAGE_NUMBER_OF_CARS= \
    run_route_curriculum_for_preset "race-single" "${RL_DIAGNOSTIC_RACE_1_ITERATIONS:-40}" 1 "${checkpoint_dir}" "${RL_INIT_POLICY:-}"
  RL_STAGE_GROUP_INDEX=2 RL_STAGE_GROUP_TOTAL=3 \
  RL_STAGE_ROUTE_TARGETS=lap \
  RL_STAGE_ITERATIONS="${RL_DIAGNOSTIC_RACE_2_ITERATIONS:-40}" \
  RL_STAGE_NUMBER_OF_CARS=2 \
    run_route_curriculum_for_preset "race-2" "${RL_DIAGNOSTIC_RACE_2_ITERATIONS:-40}" 1 "${checkpoint_dir}" ""
  RL_STAGE_GROUP_INDEX=3 RL_STAGE_GROUP_TOTAL=3 \
  RL_STAGE_ROUTE_TARGETS=lap \
  RL_STAGE_ITERATIONS="${RL_DIAGNOSTIC_RACE_4_ITERATIONS:-40}" \
  RL_STAGE_NUMBER_OF_CARS=4 \
    run_route_curriculum_for_preset "race-4" "${RL_DIAGNOSTIC_RACE_4_ITERATIONS:-40}" 1 "${checkpoint_dir}" ""
}

run_direct_race_route_curriculum() {
  local preset="$1"
  local checkpoint_dir="${RL_CHECKPOINT_DIR:-rl-checkpoints-race-physics-v1}"
  local iterations="${RL_FOREVER_ITERATIONS:-100}"
  local max_cycles="${RL_MAX_CYCLES:-0}"
  local preset_agents
  preset_agents="$(controlled_agents_for_preset "${preset}")"

  clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"
  ensure_route_spawn_seed_session "${checkpoint_dir}"
  if [[ "${preset_agents}" -gt 1 ]]; then
    RL_STAGE_ROUTE_TARGETS=lap \
    RL_STAGE_ITERATIONS="${iterations}" \
    RL_STAGE_NUMBER_OF_CARS="${preset_agents}" \
      run_route_curriculum_for_preset \
      "${preset}" \
      "${iterations}" \
      "${max_cycles}" \
      "${checkpoint_dir}" \
      "${RL_INIT_POLICY:-}"
    return
  fi
  run_route_curriculum_for_preset \
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
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-single"
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
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-single"
      exit 0
    fi
    ;;
  "race-2")
    set_race_cars_defaults "2"
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-2"
      exit 0
    fi
    ;;
  "race-4")
    set_race_cars_defaults "4"
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-4"
      exit 0
    fi
    ;;
  "race-8")
    set_race_cars_defaults "8"
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-8"
      exit 0
    fi
    ;;
  "race-16")
    set_race_cars_defaults "16"
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-16"
      exit 0
    fi
    ;;
  "race-20"|"race-crowd")
    set_race_cars_defaults "20"
    if ! is_true "${RL_ROUTE_STAGE_ACTIVE:-0}" && is_true "${RL_ROUTE_CURRICULUM:-1}"; then
      run_direct_race_route_curriculum "race-20"
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
export_objective="race-route-progress-v1"
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
route_targets="${RL_ROUTE_TARGETS:-6}"
route_target_fraction="${RL_ROUTE_TARGET_FRACTION:-0}"
train_batch_size="${RL_TRAIN_BATCH_SIZE:-4096}"
minibatch_size="${RL_MINIBATCH_SIZE:-512}"
lr="${RL_LR:-3e-4}"
gamma="${RL_GAMMA:-0.995}"
entropy_coeff="${RL_ENTROPY_COEFF:-0.005}"
hidden_size="${RL_HIDDEN_SIZE:-1024}"
hidden_layers="${RL_HIDDEN_LAYERS:-2}"
hidden_activation="${RL_HIDDEN_ACTIVATION:-tanh}"
checkpoint_every="${RL_CHECKPOINT_EVERY:-0}"
checkpoint_dir="${RL_CHECKPOINT_DIR:-${default_checkpoint_dir}}"
init_policy="${RL_INIT_POLICY:-}"
iterations_per_cycle="${RL_FOREVER_ITERATIONS:-100}"
max_cycles="${RL_MAX_CYCLES:-0}"
package_every_cycles="${RL_PACKAGE_EVERY_CYCLES:-1}"
num_gpus="${RL_NUM_GPUS:-0}"
map_ids="${RL_MAP_IDS:-}"
ray_num_cpus="${RL_RAY_NUM_CPUS:-0}"
ray_temp_dir="${RL_RAY_TEMP_DIR:-}"
ray_node_ip="${RL_RAY_NODE_IP:-127.0.0.1}"
sample_timeout_s="${RL_SAMPLE_TIMEOUT_S:-600}"
build_before_training="${RL_BUILD_BEFORE_TRAINING:-1}"
regenerate_map_caches="${RL_REGENERATE_MAP_CACHES:-0}"
desktop_jar="${RL_JAR:-desktop/target/ratass-desktop-1.0.jar}"
best_export="${RL_BEST_EXPORT:-1}"
best_output="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
best_eval_episodes_per_map="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
best_eval_episodes="${RL_BEST_EVAL_EPISODES:-0}"
best_eval_min_route_targets="${RL_BEST_EVAL_MIN_ROUTE_TARGETS:-1}"
best_eval_controlled_agents="${RL_BEST_EVAL_CONTROLLED_AGENTS:-${controlled_agents}}"
best_eval_field_size="${RL_BEST_EVAL_FIELD_SIZE:-${field_size}}"
best_eval_steps="${RL_BEST_EVAL_STEPS:-auto}"
best_eval_map_ids="${RL_BEST_EVAL_MAP_IDS:-}"
best_eval_state="${RL_BEST_EVAL_STATE:-}"
best_eval_ignore_installed="${RL_BEST_EVAL_IGNORE_INSTALLED:-1}"
seed="${RL_SEED:-}"
reward_step_penalty="${RL_REWARD_STEP_PENALTY:-0.006}"
reward_progress="${RL_REWARD_PROGRESS:-0.25}"
reward_route_alignment="${RL_REWARD_ROUTE_ALIGNMENT:-0.0}"
reward_route_target="${RL_REWARD_ROUTE_TARGET:-30.0}"
reward_steering_penalty="${RL_REWARD_STEERING_PENALTY:-0.010}"
reward_reverse_free_epsilon="${RL_REWARD_REVERSE_FREE_EPSILON:-0.20}"
reward_reverse_penalty_per_unit="${RL_REWARD_REVERSE_PENALTY_PER_UNIT:-0.08}"
reward_reverse_max_penalty="${RL_REWARD_REVERSE_MAX_PENALTY:-0.90}"
reward_car_push_penalty="${RL_REWARD_CAR_PUSH_PENALTY:-3.0}"
reward_car_push_max_step_penalty="${RL_REWARD_CAR_PUSH_MAX_STEP_PENALTY:-8.0}"
reward_off_road_penalty="${RL_REWARD_OFF_ROAD_PENALTY:-0.80}"
reward_off_road_distance_penalty="${RL_REWARD_OFF_ROAD_DISTANCE_PENALTY:-0.22}"
reward_off_road_max_penalty="${RL_REWARD_OFF_ROAD_MAX_PENALTY:-5.0}"

map_ids="$(resolve_map_ids_setting "${map_ids}")"
best_eval_map_ids="$(resolve_map_ids_setting "${best_eval_map_ids}")"

clean_checkpoint_dir_for_fresh_start "${checkpoint_dir}"

race_spawn_mode="fixed-grid"
random_race_spawns=0
route_spawn_seed_file="${RL_ROUTE_SPAWN_SEED_FILE:-}"
route_phase_name="${RL_ROUTE_PHASE_NAME:-${preset:-race}-$(route_stage_label "${route_targets}" "${route_target_fraction}")}"
if is_route_target_training_stage "${route_targets}"; then
  ensure_route_spawn_seed_session "${checkpoint_dir}"
  if [[ "${controlled_agents}" -ne 1 ]]; then
    echo "invalid_route_target_training_cars=${controlled_agents} preset=${preset:-race} route_targets=${route_targets} required_cars=1 reason=route_target_training_uses_saved_random_single_car_spawns" >&2
    exit 2
  fi
  if [[ "${best_eval_controlled_agents}" -gt 1 ]]; then
    echo "invalid_route_target_eval_cars=${best_eval_controlled_agents} preset=${preset:-race} route_targets=${route_targets} required_cars=1" >&2
    exit 2
  fi
  race_spawn_mode="saved-random-route"
  random_race_spawns=1
  if [[ -z "${route_spawn_seed_file}" ]]; then
    route_spawn_seed_file="$(route_spawn_seed_path "${checkpoint_dir}" "${route_phase_name}")"
  fi
  if [[ -z "${seed}" ]]; then
    seed="$(read_or_create_route_spawn_seed "${route_spawn_seed_file}" "${route_phase_name}" "${route_targets}")"
  else
    record_route_spawn_seed_if_missing "${route_spawn_seed_file}" "${route_phase_name}" "${route_targets}" "${seed}"
  fi
elif is_non_positive_integer "${route_targets}"; then
  race_spawn_mode="fixed-grid"
  random_race_spawns=0
else
  echo "invalid_route_targets=${route_targets}" >&2
  exit 2
fi
if [[ -z "${seed}" ]]; then
  seed=1
fi
if [[ ! "${seed}" =~ ^[0-9]+$ || "${seed}" -le 0 ]]; then
  echo "invalid_seed=${seed} expected=positive_integer" >&2
  exit 2
fi

if [[ "${best_eval_steps}" == "auto" ]]; then
  best_eval_steps="$(default_best_eval_steps_for_stage "${route_targets}" "${max_action_steps}")"
fi

checkpoint_file="${checkpoint_dir}/rllib_checkpoint.json"

echo "training_step_start preset=${preset:-race} policy=${RL_POLICY_ID:-legacy} objective=${objective} checkpoint_dir=${checkpoint_dir} fresh_start=${RL_FRESH_START:-1} iterations_per_cycle=${iterations_per_cycle} max_cycles=${max_cycles} controlled_agents=${controlled_agents} field_size=${field_size} maps=${map_ids:-all} spawn_mode=${race_spawn_mode} random_race_spawns=${random_race_spawns} seed=${seed} spawn_seed_file=${route_spawn_seed_file:-none} route_targets=${route_targets} route_target_fraction=${route_target_fraction} max_action_steps=${max_action_steps} hidden=${hidden_size}x${hidden_layers} activation=${hidden_activation} workers=${workers} ray_cpus=${ray_num_cpus} sample_timeout_s=${sample_timeout_s} init_policy=${init_policy:-none} best_eval_controlled_agents=${best_eval_controlled_agents} best_eval_maps=${best_eval_map_ids:-all}"

common_args=(
  --checkpoint-dir "${checkpoint_dir}"
  --workers "${workers}"
  --controlled-agents "${controlled_agents}"
  --field-size "${field_size}"
  --action-repeat "${action_repeat}"
  --max-action-steps "${max_action_steps}"
  --route-targets "${route_targets}"
  --route-target-fraction "${route_target_fraction}"
  --seed "${seed}"
  --train-batch-size "${train_batch_size}"
  --minibatch-size "${minibatch_size}"
  --lr "${lr}"
  --gamma "${gamma}"
  --entropy-coeff "${entropy_coeff}"
  --hidden-size "${hidden_size}"
  --hidden-layers "${hidden_layers}"
  --hidden-activation "${hidden_activation}"
  --checkpoint-every "${checkpoint_every}"
  --num-gpus "${num_gpus}"
  --objective "${objective}"
  --sample-timeout-s "${sample_timeout_s}"
  --reward-step-penalty "${reward_step_penalty}"
  --reward-progress "${reward_progress}"
  --reward-route-alignment "${reward_route_alignment}"
  --reward-route-target "${reward_route_target}"
  --reward-steering-penalty "${reward_steering_penalty}"
  --reward-reverse-free-epsilon "${reward_reverse_free_epsilon}"
  --reward-reverse-penalty-per-unit "${reward_reverse_penalty_per_unit}"
  --reward-reverse-max-penalty "${reward_reverse_max_penalty}"
  --reward-car-push-penalty "${reward_car_push_penalty}"
  --reward-car-push-max-step-penalty "${reward_car_push_max_step_penalty}"
  --reward-off-road-penalty "${reward_off_road_penalty}"
  --reward-off-road-distance-penalty "${reward_off_road_distance_penalty}"
  --reward-off-road-max-penalty "${reward_off_road_max_penalty}"
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
if [[ -n "${ray_node_ip}" ]]; then
  common_args+=(--ray-node-ip "${ray_node_ip}")
fi
if [[ "${best_export}" == "1" || "${best_export}" == "true" ]]; then
  common_args+=(
    --best-export-output "${best_output}"
    --best-export-objective "${export_objective}"
    --best-eval-episodes-per-map "${best_eval_episodes_per_map}"
    --best-eval-episodes "${best_eval_episodes}"
    --best-eval-min-route-targets "${best_eval_min_route_targets}"
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

regenerate_map_caches() {
  if [[ "${regenerate_map_caches}" == "0" || "${regenerate_map_caches}" == "false" ]]; then
    echo "map_cache_regenerate_skipped=1"
    return 0
  fi
  echo "map_cache_regenerate_start jar=${desktop_jar}"
  "${python_bin}" tools/rl/regenerate_map_caches.py --jar "${desktop_jar}"
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
regenerate_map_caches

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
