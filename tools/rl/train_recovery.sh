#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

recovery_properties="${RL_RECOVERY_PROPERTIES:-${repo_root}/tools/rl/recovery.properties}"
declare -A recovery_env_overrides=()
while IFS= read -r name; do
  recovery_env_overrides["${name}"]="${!name}"
done < <(compgen -v RL_)

if [[ ! -f "${recovery_properties}" ]]; then
  echo "Recovery properties file was not found: ${recovery_properties}" >&2
  exit 2
fi
set -a
# shellcheck source=/dev/null
source "${recovery_properties}"
set +a

# Explicit environment values take precedence over the properties file.
for name in "${!recovery_env_overrides[@]}"; do
  export "${name}=${recovery_env_overrides[${name}]}"
done

python_bin="${RL_PYTHON:-${repo_root}/.venv-rl/bin/python}"
jar="${RL_JAR:-${repo_root}/desktop/target/ratass-desktop-1.0.jar}"
checkpoint_root="${RL_RECOVERY_CHECKPOINT_DIR:-${repo_root}/rl-checkpoints/recovery}"
output="${RL_RECOVERY_OUTPUT:-${repo_root}/assets/ai/recovery/rl_recovery_policy.json}"
stages_csv="${RL_RECOVERY_STAGES:-offroad_near,offroad_shallow,offroad_angled,mixed}"
iterations_csv="${RL_RECOVERY_STAGE_ITERATIONS:-40,40,60,180}"
min_success_rates_csv="${RL_RECOVERY_STAGE_MIN_SUCCESS_RATES:-}"
eval_episodes_per_map_csv="${RL_RECOVERY_STAGE_EVAL_EPISODES_PER_MAP:-}"

if [[ "${RL_BUILD_BEFORE_TRAINING:-1}" == "1" ]]; then
  mvn -pl desktop -am -DskipTests package
fi
if [[ ! -x "${python_bin}" ]]; then
  echo "Recovery trainer Python was not found: ${python_bin}" >&2
  exit 2
fi
if [[ ! -f "${jar}" ]]; then
  echo "Desktop jar was not found: ${jar}" >&2
  exit 2
fi

IFS=',' read -r -a stages <<< "${stages_csv}"
IFS=',' read -r -a stage_iterations <<< "${iterations_csv}"
stage_min_success_rates=()
stage_eval_episode_counts=()
if [[ -n "${min_success_rates_csv}" ]]; then
  IFS=',' read -r -a stage_min_success_rates <<< "${min_success_rates_csv}"
fi
if [[ -n "${eval_episodes_per_map_csv}" ]]; then
  IFS=',' read -r -a stage_eval_episode_counts <<< "${eval_episodes_per_map_csv}"
fi
if [[ "${#stages[@]}" -ne "${#stage_iterations[@]}" ]]; then
  echo "Recovery stage/iteration count mismatch: ${#stages[@]} != ${#stage_iterations[@]}" >&2
  exit 2
fi
if [[ "${#stage_eval_episode_counts[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_eval_episode_counts[@]}" ]]; then
  echo "Recovery stage/evaluation coverage count mismatch: ${#stages[@]} != ${#stage_eval_episode_counts[@]}" >&2
  exit 2
fi
if [[ "${#stage_min_success_rates[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_min_success_rates[@]}" ]]; then
  echo "Recovery stage/gate count mismatch: ${#stages[@]} != ${#stage_min_success_rates[@]}" >&2
  exit 2
fi

mkdir -p "${checkpoint_root}" "$(dirname "${output}")"
init_policy="${RL_RECOVERY_INIT_POLICY:-}"
for ((index = 0; index < ${#stages[@]}; index++)); do
  stage="${stages[index]}"
  iterations="${stage_iterations[index]}"
  stage_dir="${checkpoint_root}/${stage}"
  stage_output="${stage_dir}/best_policy.json"
  final_mixed_stage=0
  stage_min_success_rate="${RL_RECOVERY_MIN_SUCCESS_RATE:-1.00}"
  if [[ "${#stage_min_success_rates[@]}" -gt 0 ]]; then
    stage_min_success_rate="${stage_min_success_rates[index]}"
  fi
  if [[ "${stage}" == "mixed" && "${index}" -eq $((${#stages[@]} - 1)) ]]; then
    stage_output="${output}"
    final_mixed_stage=1
  fi
  stage_map_ids="${RL_MAP_IDS:-}"
  stage_best_eval_map_ids="${RL_BEST_EVAL_MAP_IDS:-}"
  stage_eval_episodes_per_map="${RL_RECOVERY_EVAL_EPISODES_PER_MAP:-9}"
  stage_seed=$(( ${RL_RECOVERY_SEED:-20260506} + index * 100003 ))
  if [[ "${#stage_eval_episode_counts[@]}" -gt 0 ]]; then
    stage_eval_episodes_per_map="${stage_eval_episode_counts[index]}"
  fi
  if [[ "${stage}" == "map014_inflection" ]]; then
    stage_map_ids="map014"
    stage_best_eval_map_ids="map014"
    stage_eval_episodes_per_map="${RL_RECOVERY_MAP014_EVAL_EPISODES_PER_MAP:-64}"
  fi

  command=(
    "${python_bin}" tools/rl/train_rllib.py
    --jar "${jar}"
    --objective recovery
    --recovery-scenario "${stage}"
    --iterations "${iterations}"
    --controlled-agents 1
    --field-size 1
    --fixed-race-spawns
    --route-targets 1
    --action-repeat "${RL_RECOVERY_ACTION_REPEAT:-4}"
    --max-action-steps "${RL_RECOVERY_MAX_ACTION_STEPS:-300}"
    --no-progress-max-action-steps 0
    --off-road-failure-max-action-steps 0
    --seed "${stage_seed}"
    --workers "${RL_WORKERS:-4}"
    --ray-num-cpus "${RL_RAY_NUM_CPUS:-0}"
    --train-batch-size "${RL_RECOVERY_TRAIN_BATCH_SIZE:-4096}"
    --minibatch-size "${RL_RECOVERY_MINIBATCH_SIZE:-256}"
    --hidden-size "${RL_RECOVERY_HIDDEN_SIZE:-96}"
    --hidden-layers "${RL_RECOVERY_HIDDEN_LAYERS:-2}"
    --hidden-activation "${RL_RECOVERY_HIDDEN_ACTIVATION:-tanh}"
    --lr "${RL_RECOVERY_LR:-7.5e-5}"
    --gamma "${RL_RECOVERY_GAMMA:-0.995}"
    --gae-lambda "${RL_RECOVERY_GAE_LAMBDA:-0.95}"
    --entropy-coeff "${RL_RECOVERY_ENTROPY_COEFF:-0.001}"
    --clip-param "${RL_RECOVERY_CLIP_PARAM:-0.2}"
    --kl-coeff "${RL_RECOVERY_KL_COEFF:-0.2}"
    --kl-target "${RL_RECOVERY_KL_TARGET:-0.01}"
    --num-epochs "${RL_RECOVERY_NUM_EPOCHS:-5}"
    --grad-clip "${RL_RECOVERY_GRAD_CLIP:-1.0}"
    --vf-clip-param "${RL_RECOVERY_VF_CLIP_PARAM:-100000000.0}"
    --vf-loss-coeff "${RL_RECOVERY_VF_LOSS_COEFF:-0.001}"
    --checkpoint-dir "${stage_dir}/checkpoint"
    --checkpoint-every "${RL_RECOVERY_CHECKPOINT_EVERY:-20}"
    --checkpoint-selection "${RL_RECOVERY_CHECKPOINT_SELECTION:-latest}"
    --best-export-output "${stage_output}"
    --best-export-objective shared-recovery-v1
    --best-eval-episodes-per-map "${stage_eval_episodes_per_map}"
    --best-eval-min-route-targets "${stage_min_success_rate}"
    --best-eval-steps "${RL_RECOVERY_EVAL_STEPS:-300}"
    --best-eval-seed "${RL_RECOVERY_EVAL_SEED:-${RL_RECOVERY_SEED:-20260506}}"
    --reward-step-penalty "${RL_RECOVERY_REWARD_STEP_PENALTY:-0.50}"
    --reward-steering-penalty "${RL_RECOVERY_REWARD_STEERING_PENALTY:-5.0}"
    --reward-off-road-penalty "${RL_RECOVERY_REWARD_OFF_ROAD_PENALTY:-0.25}"
    --reward-off-road-distance-penalty "${RL_RECOVERY_REWARD_OFF_ROAD_DISTANCE_PENALTY:-0.50}"
    --reward-off-road-max-penalty "${RL_RECOVERY_REWARD_OFF_ROAD_MAX_PENALTY:-5.0}"
    --reward-off-road-recovery "${RL_RECOVERY_REWARD_OFF_ROAD_RECOVERY:-6.0}"
    --reward-off-road-failure-penalty "${RL_RECOVERY_REWARD_FAILURE_PENALTY:-5000.0}"
    --recovery-reward-distance "${RL_RECOVERY_REWARD_DISTANCE:-4.0}"
    --recovery-reward-alignment "${RL_RECOVERY_REWARD_ALIGNMENT:-8.0}"
    --recovery-reward-target-alignment "${RL_RECOVERY_REWARD_TARGET_ALIGNMENT:-6.0}"
    --recovery-reward-motion "${RL_RECOVERY_REWARD_MOTION:-0.75}"
    --recovery-reward-launch-throttle "${RL_RECOVERY_REWARD_LAUNCH_THROTTLE:-5.0}"
    --recovery-reward-steering "${RL_RECOVERY_REWARD_STEERING:-5.0}"
    --recovery-penalty-stationary "${RL_RECOVERY_PENALTY_STATIONARY:-1.0}"
    --recovery-reward-success "${RL_RECOVERY_REWARD_SUCCESS:-3000.0}"
  )
  if [[ "${final_mixed_stage}" != "1" ]]; then
    command+=(--best-eval-ignore-installed)
  fi
  if [[ "${RL_RECOVERY_SKIP_TRAINING_IF_BASELINE_ELIGIBLE:-1}" == "1" ]]; then
    command+=(--skip-training-if-baseline-eligible)
  fi
  stage_evaluate_all_candidates="${RL_RECOVERY_EVALUATE_ALL_CANDIDATES:-0}"
  if [[ "${final_mixed_stage}" == "1" ]]; then
    stage_evaluate_all_candidates="${RL_RECOVERY_FINAL_EVALUATE_ALL_CANDIDATES:-0}"
    if [[ "${RL_RECOVERY_FINAL_PROMOTE_IF_BETTER_THAN_INSTALLED:-1}" == "1" ]]; then
      command+=(--best-eval-promote-if-better-than-installed)
    fi
  fi
  if [[ "${stage_evaluate_all_candidates}" == "1" ]]; then
    command+=(--evaluate-all-checkpoint-candidates)
  fi
  if [[ -n "${RL_RECOVERY_EVAL_SCENARIO:-}" ]]; then
    command+=(--best-eval-recovery-scenario "${RL_RECOVERY_EVAL_SCENARIO}")
  fi
  if [[ -n "${stage_map_ids}" ]]; then
    command+=(--map-ids "${stage_map_ids}")
  fi
  if [[ -n "${stage_best_eval_map_ids}" ]]; then
    command+=(--best-eval-map-ids "${stage_best_eval_map_ids}")
  fi
  if [[ -n "${RL_RAY_TEMP_DIR:-}" ]]; then
    command+=(--ray-temp-dir "${RL_RAY_TEMP_DIR}")
  fi
  if [[ -n "${init_policy}" && -f "${init_policy}" ]]; then
    command+=(--init-policy "${init_policy}")
  fi
  if [[ "${RL_RECOVERY_RESUME:-1}" == "1"
      && "${RL_FORCE_FRESH_START:-0}" != "1"
      && -f "${stage_dir}/checkpoint/rllib_checkpoint.json" ]]; then
    command+=(--resume)
  fi

  echo "recovery_stage=$((index + 1))/${#stages[@]} scenario=${stage} iterations=${iterations} min_success_rate=${stage_min_success_rate} seed=${stage_seed}"
  "${command[@]}"
  if [[ ! -f "${stage_output}" ]]; then
    echo "Recovery stage did not produce an eligible policy: ${stage}" >&2
    exit 1
  fi
  init_policy="${stage_output}"
done

echo "recovery_policy=${output}"
