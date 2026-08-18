#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

properties="${RL_OVERTAKING_PROPERTIES:-${repo_root}/tools/rl/overtaking.properties}"
declare -A overrides=()
while IFS= read -r name; do
  overrides["${name}"]="${!name}"
done < <(compgen -v RL_)

if [[ ! -f "${properties}" ]]; then
  echo "Overtaking properties file was not found: ${properties}" >&2
  exit 2
fi
set -a
# shellcheck source=/dev/null
source "${properties}"
set +a
for name in "${!overrides[@]}"; do
  export "${name}=${overrides[${name}]}"
done

python_bin="${RL_PYTHON:-${repo_root}/.venv-rl/bin/python}"
jar="${RL_JAR:-${repo_root}/desktop/target/ratass-desktop-1.0.jar}"
checkpoint_root="${RL_OVERTAKING_CHECKPOINT_DIR:-${repo_root}/rl-checkpoints/overtaking}"
output="${RL_OVERTAKING_OUTPUT:-${repo_root}/assets/ai/overtaking/rl_overtaking_policy.json}"
IFS=',' read -r -a stages <<< "${RL_OVERTAKING_STAGES:-closing,straight,single,pack,mixed}"
IFS=',' read -r -a iterations <<< "${RL_OVERTAKING_STAGE_ITERATIONS:-80,60,120,180,240}"
IFS=',' read -r -a stage_names <<< "${RL_OVERTAKING_STAGE_NAMES:-}"
IFS=',' read -r -a stage_speed_scales \
  <<< "${RL_OVERTAKING_STAGE_OPPONENT_SPEED_SCALES:-}"
IFS=',' read -r -a stage_min_success_rates \
  <<< "${RL_OVERTAKING_STAGE_MIN_SUCCESS_RATES:-}"
IFS=';' read -r -a stage_map_groups \
  <<< "${RL_OVERTAKING_STAGE_MAP_GROUPS:-}"

if [[ "${#stages[@]}" -ne "${#iterations[@]}" ]]; then
  echo "Overtaking stage/iteration count mismatch" >&2
  exit 2
fi
if [[ "${#stage_names[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_names[@]}" ]]; then
  echo "Overtaking stage/name count mismatch" >&2
  exit 2
fi
if [[ "${#stage_speed_scales[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_speed_scales[@]}" ]]; then
  echo "Overtaking stage/opponent-speed count mismatch" >&2
  exit 2
fi
if [[ "${#stage_min_success_rates[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_min_success_rates[@]}" ]]; then
  echo "Overtaking stage/gate count mismatch" >&2
  exit 2
fi
if [[ "${#stage_map_groups[@]}" -gt 0
    && "${#stages[@]}" -ne "${#stage_map_groups[@]}" ]]; then
  echo "Overtaking stage/map-group count mismatch" >&2
  exit 2
fi
if [[ "${RL_BUILD_BEFORE_TRAINING:-1}" == "1" ]]; then
  mvn -pl desktop -am -DskipTests package
fi

mkdir -p "${checkpoint_root}" "$(dirname "${output}")"
init_policy="${RL_OVERTAKING_INIT_POLICY:-}"
for ((index = 0; index < ${#stages[@]}; index++)); do
  stage="${stages[index]}"
  stage_name="${stage}"
  if [[ "${#stage_names[@]}" -gt 0 ]]; then
    stage_name="${stage_names[index]}"
  fi
  stage_speed_scale="${RL_OVERTAKING_OPPONENT_THROTTLE_SCALE:-0.90}"
  if [[ "${#stage_speed_scales[@]}" -gt 0 ]]; then
    stage_speed_scale="${stage_speed_scales[index]}"
  fi
  stage_min_success_rate="${RL_OVERTAKING_MIN_SUCCESS_RATE:-0.75}"
  if [[ "${#stage_min_success_rates[@]}" -gt 0 ]]; then
    stage_min_success_rate="${stage_min_success_rates[index]}"
  fi
  stage_map_ids="${RL_OVERTAKING_MAP_IDS:-}"
  if [[ "${#stage_map_groups[@]}" -gt 0 ]]; then
    stage_map_ids="${stage_map_groups[index]}"
  elif [[ "${stage}" == "closing" && -n "${RL_OVERTAKING_CLOSING_MAP_IDS:-}" ]]; then
    stage_map_ids="${RL_OVERTAKING_CLOSING_MAP_IDS}"
  elif [[ "${stage}" == "straight" && -n "${RL_OVERTAKING_STRAIGHT_MAP_IDS:-}" ]]; then
    stage_map_ids="${RL_OVERTAKING_STRAIGHT_MAP_IDS}"
  elif [[ "${stage}" == "single" && -n "${RL_OVERTAKING_SINGLE_MAP_IDS:-}" ]]; then
    stage_map_ids="${RL_OVERTAKING_SINGLE_MAP_IDS}"
  fi
  stage_dir="${checkpoint_root}/${stage_name}"
  stage_output="${stage_dir}/best_policy.json"
  if [[ "${index}" -eq $((${#stages[@]} - 1)) ]]; then
    stage_output="${output}"
  fi
  command=(
    "${python_bin}" tools/rl/train_rllib.py
    --jar "${jar}"
    --objective overtaking
    --overtaking-scenario "${stage}"
    --overtaking-base-policy "${RL_OVERTAKING_BASE_POLICY:-profile07}"
    --overtaking-opponent-policy "${RL_OVERTAKING_OPPONENT_POLICY:-profile07}"
    --overtaking-opponents "${RL_OVERTAKING_OPPONENTS:-3}"
    --overtaking-opponent-throttle-scale "${stage_speed_scale}"
    --iterations "${iterations[index]}"
    --controlled-agents 1
    --field-size "$(( ${RL_OVERTAKING_OPPONENTS:-3} + 1 ))"
    --fixed-race-spawns
    --route-targets 1
    --action-repeat "${RL_OVERTAKING_ACTION_REPEAT:-4}"
    --max-action-steps "${RL_OVERTAKING_MAX_ACTION_STEPS:-450}"
    --no-progress-max-action-steps 0
    --off-road-failure-max-action-steps 0
    --seed "${RL_OVERTAKING_SEED:-20260818}"
    --workers "${RL_WORKERS:-4}"
    --ray-num-cpus "${RL_RAY_NUM_CPUS:-0}"
    --ray-node-ip "${RL_RAY_NODE_IP:-127.0.0.2}"
    --train-batch-size "${RL_OVERTAKING_TRAIN_BATCH_SIZE:-8192}"
    --minibatch-size "${RL_OVERTAKING_MINIBATCH_SIZE:-512}"
    --hidden-size "${RL_OVERTAKING_HIDDEN_SIZE:-128}"
    --hidden-layers "${RL_OVERTAKING_HIDDEN_LAYERS:-3}"
    --hidden-activation "${RL_OVERTAKING_HIDDEN_ACTIVATION:-tanh}"
    --lr "${RL_OVERTAKING_LR:-1e-4}"
    --gamma "${RL_OVERTAKING_GAMMA:-0.995}"
    --entropy-coeff "${RL_OVERTAKING_ENTROPY_COEFF:-0.003}"
    --free-log-std
    --initial-log-std "${RL_OVERTAKING_INITIAL_LOG_STD:--1.5}"
    --num-epochs "${RL_OVERTAKING_NUM_EPOCHS:-8}"
    --grad-clip "${RL_OVERTAKING_GRAD_CLIP:-1.0}"
    --vf-clip-param "${RL_OVERTAKING_VF_CLIP_PARAM:-2000.0}"
    --checkpoint-dir "${stage_dir}/checkpoint"
    --checkpoint-every "${RL_OVERTAKING_CHECKPOINT_EVERY:-10}"
    --checkpoint-selection reward
    --evaluate-all-checkpoint-candidates
    --best-export-output "${stage_output}"
    --best-export-objective shared-overtaking-v1
    --best-eval-overtaking-scenario "${RL_OVERTAKING_BEST_EVAL_SCENARIO:-${stage}}"
    --best-eval-episodes-per-map "${RL_OVERTAKING_EVAL_EPISODES_PER_MAP:-5}"
    --best-eval-min-route-targets "${stage_min_success_rate}"
    --best-eval-steps "${RL_OVERTAKING_EVAL_STEPS:-450}"
    --best-eval-ignore-installed
    --reward-step-penalty "${RL_OVERTAKING_REWARD_STEP_PENALTY:-0.05}"
    --reward-car-push-penalty "${RL_OVERTAKING_COLLISION_PENALTY:-50.0}"
    --reward-car-push-max-step-penalty "${RL_OVERTAKING_COLLISION_MAX_PENALTY:-50.0}"
    --reward-off-road-penalty "${RL_OVERTAKING_OFF_ROAD_PENALTY:-2.0}"
    --reward-off-road-distance-penalty "${RL_OVERTAKING_OFF_ROAD_DISTANCE_PENALTY:-1.0}"
    --reward-off-road-max-penalty "${RL_OVERTAKING_OFF_ROAD_MAX_PENALTY:-10.0}"
    --overtaking-reward-gap "${RL_OVERTAKING_REWARD_GAP:-4.0}"
    --overtaking-reward-safety "${RL_OVERTAKING_REWARD_SAFETY:-300.0}"
    --overtaking-reward-lane "${RL_OVERTAKING_REWARD_LANE:-100.0}"
    --overtaking-reward-position "${RL_OVERTAKING_REWARD_POSITION:-120.0}"
    --overtaking-reward-hold "${RL_OVERTAKING_REWARD_HOLD:-2.0}"
    --overtaking-reward-success "${RL_OVERTAKING_REWARD_SUCCESS:-800.0}"
    --overtaking-failure-penalty "${RL_OVERTAKING_FAILURE_PENALTY:-600.0}"
    --overtaking-residual-penalty "${RL_OVERTAKING_RESIDUAL_PENALTY:-0.03}"
  )
  if [[ -n "${RL_RAY_TEMP_DIR:-}" ]]; then
    command+=(--ray-temp-dir "${RL_RAY_TEMP_DIR}")
  fi
  if [[ -n "${stage_map_ids}" ]]; then
    command+=(--map-ids "${stage_map_ids}")
    command+=(--best-eval-map-ids "${stage_map_ids}")
  fi
  if [[ -n "${init_policy}" && -f "${init_policy}" ]]; then
    command+=(--init-policy "${init_policy}")
  fi
  if [[ "${RL_FORCE_FRESH_START:-0}" != "1"
      && -f "${stage_dir}/checkpoint/rllib_checkpoint.json" ]]; then
    command+=(--resume)
  fi

  echo "overtaking_stage=$((index + 1))/${#stages[@]} name=${stage_name} "\
"scenario=${stage} opponent_speed_scale=${stage_speed_scale} "\
"maps=${stage_map_ids:-all} iterations=${iterations[index]} "\
"min_success_rate=${stage_min_success_rate}"
  "${command[@]}"
  if [[ ! -f "${stage_output}" ]]; then
    echo "Overtaking stage did not produce an eligible policy: ${stage}" >&2
    exit 1
  fi
  init_policy="${stage_output}"
done

echo "overtaking_policy=${output}"
