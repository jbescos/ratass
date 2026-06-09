#!/usr/bin/env bash

rl_policy_asset_root="${RL_POLICY_ASSET_ROOT:-assets/ai/policies}"
rl_policy_config_root="${RL_POLICY_CONFIG_ROOT:-tools/rl/policies}"
rl_policy_file_name="${RL_POLICY_FILE_NAME:-rl_enemy_policy.json}"

rl_policy_is_true() {
  [[ "${1:-}" == "1" || "${1:-}" == "true" || "${1:-}" == "yes" ]]
}

rl_policy_batch_active() {
  rl_policy_is_true "${RL_POLICY_BATCH_ACTIVE:-0}"
}

rl_policy_normalize_id() {
  local raw="$1"
  local policy_id
  policy_id="$(printf '%s' "${raw}" | tr '[:upper:]' '[:lower:]')"

  if [[ ! "${policy_id}" =~ ^[a-z][a-z0-9_-]*$ ]]; then
    echo "invalid_policy_id=${raw}" >&2
    return 2
  fi

  if [[ ! -d "${rl_policy_config_root}/${policy_id}" ]]; then
    echo "unknown_policy_id=${raw} available=$(rl_policy_list_all | paste -sd, -)" >&2
    return 2
  fi
  printf '%s\n' "${policy_id}"
}

rl_policy_list_all() {
  if [[ -d "${rl_policy_config_root}" ]]; then
    find "${rl_policy_config_root}" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' \
      | LC_ALL=C sort
    return
  fi
}

rl_policy_selected_ids() {
  local raw_ids=()
  local raw
  if [[ "$#" -gt 0 ]]; then
    raw_ids=("$@")
  elif [[ -n "${RL_POLICY_ID:-}" ]]; then
    IFS=',' read -r -a raw_ids <<< "${RL_POLICY_ID}"
  else
    rl_policy_list_all
    return
  fi

  for raw in "${raw_ids[@]}"; do
    rl_policy_normalize_id "${raw}"
  done
}

rl_policy_capture_env_overrides() {
  rl_policy_override_names=()
  if ! rl_policy_is_true "${RL_PROFILE_ENV_OVERRIDES:-1}"; then
    return
  fi
  local name
  while IFS= read -r name; do
    if [[ -z "${!name:-}" ]]; then
      continue
    fi
    rl_policy_override_names+=("${name}")
    printf -v "__rl_policy_override_${name}" '%s' "${!name}"
  done < <(compgen -v RL_)
}

rl_policy_restore_env_overrides() {
  if ! rl_policy_is_true "${RL_PROFILE_ENV_OVERRIDES:-1}"; then
    return
  fi
  local name
  local value_name
  for name in "${rl_policy_override_names[@]}"; do
    value_name="__rl_policy_override_${name}"
    export "${name}=${!value_name}"
  done
}

rl_policy_load_properties() {
  local policy_id="$1"
  local default_profile="${rl_policy_config_root}/default.properties"
  local policy_profile="${rl_policy_config_root}/${policy_id}/profile.properties"
  local policy_rewards="${rl_policy_config_root}/${policy_id}/reward.properties"
  local rl_policy_override_names=()

  rl_policy_capture_env_overrides

  if [[ -f "${default_profile}" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "${default_profile}"
    set +a
  fi
  if [[ -f "${policy_rewards}" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "${policy_rewards}"
    set +a
  fi
  if [[ -f "${policy_profile}" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "${policy_profile}"
    set +a
  fi

  rl_policy_restore_env_overrides
}

rl_policy_state_completed() {
  local state_file="$1"
  [[ -f "${state_file}" ]] \
    && awk -F= '$1 == "completed_profile" { value = $2 } END { exit !(value == "1") }' "${state_file}"
}

rl_policy_configure_resume() {
  local checkpoint_dir="${RL_CURRICULUM_CHECKPOINT_DIR:-${RL_CHECKPOINT_DIR:-}}"
  local model_path="${RL_BEST_OUTPUT:-}"
  local checkpoint_file="${checkpoint_dir}/rllib_checkpoint.json"

  if [[ -n "${checkpoint_dir}" ]]; then
    export RL_POLICY_TRAINING_STATE="${RL_POLICY_TRAINING_STATE:-${checkpoint_dir}/training-state.properties}"
  fi

  if rl_policy_is_true "${RL_FORCE_FRESH_START:-0}"; then
    export RL_FRESH_START=1
    echo "policy_resume_source=fresh_forced policy=${RL_POLICY_ID:-unknown} checkpoint_dir=${checkpoint_dir:-none} model=${model_path:-none}"
    return
  fi

  if [[ -n "${RL_FORCE_FRESH_START+x}" ]]; then
    if [[ -n "${model_path}" && -f "${model_path}" ]]; then
      export RL_FRESH_START=1
      if [[ -z "${RL_INIT_POLICY:-}" ]]; then
        export RL_INIT_POLICY="${model_path}"
      fi
      echo "policy_resume_source=model_forced policy=${RL_POLICY_ID:-unknown} model=${model_path} init_policy=${RL_INIT_POLICY:-none}"
      return
    fi
    export RL_FRESH_START=1
    echo "policy_resume_source=scratch_forced policy=${RL_POLICY_ID:-unknown} checkpoint_dir=${checkpoint_dir:-none} model=${model_path:-none}"
    return
  fi

  if [[ -n "${checkpoint_dir}" && -f "${checkpoint_file}" ]]; then
    export RL_FRESH_START=0
    echo "policy_resume_source=checkpoint policy=${RL_POLICY_ID:-unknown} checkpoint_dir=${checkpoint_dir}"
  elif [[ -n "${model_path}" && -f "${model_path}" ]]; then
    export RL_FRESH_START=0
    if [[ -z "${RL_INIT_POLICY:-}" ]]; then
      export RL_INIT_POLICY="${model_path}"
    fi
    echo "policy_resume_source=model policy=${RL_POLICY_ID:-unknown} model=${model_path} init_policy=${RL_INIT_POLICY:-none}"
  elif [[ -n "${RL_POLICY_TRAINING_STATE:-}" && -f "${RL_POLICY_TRAINING_STATE}" ]]; then
    export RL_FRESH_START=0
    echo "policy_resume_source=state policy=${RL_POLICY_ID:-unknown} state=${RL_POLICY_TRAINING_STATE}"
  else
    export RL_FRESH_START="${RL_FRESH_START:-1}"
    echo "policy_resume_source=scratch policy=${RL_POLICY_ID:-unknown} checkpoint_dir=${checkpoint_dir:-none} model=${model_path:-none}"
  fi
}

rl_policy_run_batch() {
  local script_path="$1"
  local script_key="$2"
  shift 2
  local policy_args=()
  local script_args=()
  while [[ "$#" -gt 0 ]]; do
    if [[ "$1" == "--" ]]; then
      shift
      script_args=("$@")
      break
    fi
    policy_args+=("$1")
    shift
  done

  local policy_ids=()
  mapfile -t policy_ids < <(rl_policy_selected_ids "${policy_args[@]}")
  if [[ "${#policy_ids[@]}" -eq 0 ]]; then
    echo "no_policy_profiles_selected=1" >&2
    return 2
  fi

  local policy_id
  local policy_index=0
  local policy_total="${#policy_ids[@]}"
  local explicit_selection=0
  if [[ "${#policy_args[@]}" -gt 0 || -n "${RL_POLICY_ID:-}" ]]; then
    explicit_selection=1
  fi
  local failed_count=0
  local failed_ids=()
  for policy_id in "${policy_ids[@]}"; do
    policy_index=$((policy_index + 1))
    if (
      rl_policy_load_properties "${policy_id}"
      export RL_POLICY_BATCH_ACTIVE=1
      export RL_POLICY_ID="${policy_id}"
      export RL_POLICY_INDEX="${policy_index}"
      export RL_POLICY_TOTAL="${policy_total}"
      export RL_POLICY_CONFIG_FILE="${rl_policy_config_root}/${policy_id}/profile.properties"
      export RL_BEST_OUTPUT="${RL_BEST_OUTPUT:-${rl_policy_asset_root}/${policy_id}/${rl_policy_file_name}}"
      export RL_CURRICULUM_CHECKPOINT_DIR="${RL_CURRICULUM_CHECKPOINT_DIR:-rl-checkpoints/policies/${policy_id}/${script_key}}"
      export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-${RL_CURRICULUM_CHECKPOINT_DIR}}"
      export RL_TRAIN_LOG="${RL_TRAIN_LOG:-logs/rl-policy-${policy_id}-${script_key}.log}"
      export RL_POLICY_STATUS_FILE="${RL_POLICY_STATUS_FILE:-logs/rl-current-training-policy.txt}"
      export RL_FORCE_EXPORT_ON_FINISH="${RL_FORCE_EXPORT_ON_FINISH:-1}"
      export RL_POLICY_EXPLICIT_SELECTION="${explicit_selection}"
      rl_policy_configure_resume
      if [[ "${explicit_selection}" == "0" ]] \
        && [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]] \
        && rl_policy_state_completed "${RL_POLICY_TRAINING_STATE}" \
        && ! rl_policy_is_true "${RL_RETRAIN_COMPLETED:-0}"; then
        echo "policy_training_skip_complete id=${policy_id} index=${policy_index}/${policy_total} state=${RL_POLICY_TRAINING_STATE} output=${RL_BEST_OUTPUT}"
        exit 0
      fi
      if [[ "${explicit_selection}" == "1" ]] \
        && [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]] \
        && rl_policy_state_completed "${RL_POLICY_TRAINING_STATE}" \
        && [[ -z "${RL_RETRAIN_COMPLETED:-}" ]]; then
        export RL_RETRAIN_COMPLETED=1
      fi
      mkdir -p "$(dirname "${RL_BEST_OUTPUT}")" "$(dirname "${RL_TRAIN_LOG}")" "$(dirname "${RL_POLICY_STATUS_FILE}")"
      if rl_policy_is_true "${RL_RETRAIN_COMPLETED:-0}" && [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]]; then
        mkdir -p "$(dirname "${RL_POLICY_TRAINING_STATE}")"
        : > "${RL_POLICY_TRAINING_STATE}"
      fi
      {
        printf 'status=running\n'
        printf 'started_at=%s\n' "$(date -Is)"
        printf 'completed_profile=0\n'
        printf 'policy_id=%s\n' "${policy_id}"
        printf 'policy_index=%s\n' "${policy_index}"
        printf 'policy_total=%s\n' "${policy_total}"
        printf 'script=%s\n' "${script_key}"
        printf 'config=%s\n' "${RL_POLICY_CONFIG_FILE}"
        printf 'checkpoint_dir=%s\n' "${RL_CURRICULUM_CHECKPOINT_DIR}"
        printf 'output=%s\n' "${RL_BEST_OUTPUT}"
        printf 'training_state=%s\n' "${RL_POLICY_TRAINING_STATE:-}"
        printf 'fresh_start=%s\n' "${RL_FRESH_START:-}"
        printf 'init_policy=%s\n' "${RL_INIT_POLICY:-}"
      } > "${RL_POLICY_STATUS_FILE}"
      if [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]]; then
        mkdir -p "$(dirname "${RL_POLICY_TRAINING_STATE}")"
        {
          printf 'status=running\n'
          printf 'completed_profile=0\n'
          printf 'started_at=%s\n' "$(date -Is)"
          printf 'policy_id=%s\n' "${policy_id}"
          printf 'policy_index=%s\n' "${policy_index}"
          printf 'policy_total=%s\n' "${policy_total}"
          printf 'script=%s\n' "${script_key}"
          printf 'config=%s\n' "${RL_POLICY_CONFIG_FILE}"
          printf 'checkpoint_dir=%s\n' "${RL_CURRICULUM_CHECKPOINT_DIR}"
          printf 'output=%s\n' "${RL_BEST_OUTPUT}"
          printf 'fresh_start=%s\n' "${RL_FRESH_START:-}"
          printf 'init_policy=%s\n' "${RL_INIT_POLICY:-}"
        } >> "${RL_POLICY_TRAINING_STATE}"
      fi
      echo "############################################################"
      echo "PROFILE_TRAINING_START id=${policy_id} profile=${policy_index}/${policy_total} script=${script_key}"
      echo "CURRENT_TRAINING_PROFILE=${policy_id}"
      echo "CURRENT_TRAINING_DRIVER=${policy_id} profile=${policy_index}/${policy_total} script=${script_key}"
      echo "CURRENT_TRAINING_OUTPUT=${RL_BEST_OUTPUT}"
      echo "CURRENT_TRAINING_STATUS_FILE=${RL_POLICY_STATUS_FILE}"
      echo "CURRENT_TRAINING_STATE_FILE=${RL_POLICY_TRAINING_STATE:-none}"
      echo "############################################################"
      echo "policy_training_start id=${policy_id} index=${policy_index}/${policy_total} script=${script_key} output=${RL_BEST_OUTPUT} checkpoint_dir=${RL_CURRICULUM_CHECKPOINT_DIR} config=${RL_POLICY_CONFIG_FILE}"
      local policy_exit_code=0
      if bash "${script_path}" "${script_args[@]}"; then
        {
          printf 'status=done\n'
          printf 'completed_profile=1\n'
          printf 'finished_at=%s\n' "$(date -Is)"
          printf 'policy_id=%s\n' "${policy_id}"
          printf 'policy_index=%s\n' "${policy_index}"
          printf 'policy_total=%s\n' "${policy_total}"
          printf 'script=%s\n' "${script_key}"
          printf 'output=%s\n' "${RL_BEST_OUTPUT}"
        } > "${RL_POLICY_STATUS_FILE}"
        if [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]]; then
          mkdir -p "$(dirname "${RL_POLICY_TRAINING_STATE}")"
          {
            printf 'status=done\n'
            printf 'completed_profile=1\n'
            printf 'finished_at=%s\n' "$(date -Is)"
            printf 'policy_id=%s\n' "${policy_id}"
            printf 'script=%s\n' "${script_key}"
            printf 'output=%s\n' "${RL_BEST_OUTPUT}"
          } >> "${RL_POLICY_TRAINING_STATE}"
        fi
        echo "PROFILE_TRAINING_FINISHED id=${policy_id} profile=${policy_index}/${policy_total} output=${RL_BEST_OUTPUT}"
        echo "policy_training_done id=${policy_id} index=${policy_index}/${policy_total} output=${RL_BEST_OUTPUT}"
      else
        policy_exit_code="$?"
        {
          printf 'status=failed_early\n'
          printf 'completed_profile=0\n'
          printf 'failed_at=%s\n' "$(date -Is)"
          printf 'exit_code=%s\n' "${policy_exit_code}"
          printf 'policy_id=%s\n' "${policy_id}"
          printf 'policy_index=%s\n' "${policy_index}"
          printf 'policy_total=%s\n' "${policy_total}"
          printf 'script=%s\n' "${script_key}"
          printf 'output=%s\n' "${RL_BEST_OUTPUT}"
        } > "${RL_POLICY_STATUS_FILE}"
        if [[ -n "${RL_POLICY_TRAINING_STATE:-}" ]]; then
          mkdir -p "$(dirname "${RL_POLICY_TRAINING_STATE}")"
          {
            printf 'status=failed_early\n'
            printf 'completed_profile=0\n'
            printf 'failed_at=%s\n' "$(date -Is)"
            printf 'exit_code=%s\n' "${policy_exit_code}"
            printf 'policy_id=%s\n' "${policy_id}"
            printf 'script=%s\n' "${script_key}"
            printf 'output=%s\n' "${RL_BEST_OUTPUT}"
          } >> "${RL_POLICY_TRAINING_STATE}"
        fi
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
        echo "PROFILE_TRAINING_TERMINATED_EARLY id=${policy_id} profile=${policy_index}/${policy_total} exit_code=${policy_exit_code}" >&2
        echo "PROFILE_TRAINING_NEXT_PROFILE_WILL_CONTINUE remaining=$((policy_total - policy_index))" >&2
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
        exit "${policy_exit_code}"
      fi
    ); then
      :
    else
      failed_count=$((failed_count + 1))
      failed_ids+=("${policy_id}")
      echo "############################################################" >&2
      echo "PROFILE_TRAINING_FAILED_BATCH_CONTINUES id=${policy_id} profile=${policy_index}/${policy_total}" >&2
      echo "policy_training_continue_after_failure failed_id=${policy_id} failed_count=${failed_count} next_index=$((policy_index + 1))/${policy_total}" >&2
      echo "############################################################" >&2
    fi
  done
  if [[ "${failed_count}" -gt 0 ]]; then
    echo "policy_training_batch_finished_with_failures failed_count=${failed_count} failed_ids=$(IFS=,; printf '%s' "${failed_ids[*]}")" >&2
    return 1
  fi
}
