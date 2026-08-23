#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

profile_id="${1:-strategy00}"
default_properties="${repo_root}/tools/rl/card-strategies/default.properties"
profile_properties="${repo_root}/tools/rl/card-strategies/${profile_id}.properties"
if [[ ! -f "${profile_properties}" ]]; then
  echo "Unknown card strategy profile: ${profile_id}" >&2
  exit 2
fi

declare -A overrides=()
while IFS= read -r name; do
  overrides["${name}"]="${!name}"
done < <(compgen -v CARD_STRATEGY_)
set -a
# shellcheck source=/dev/null
source "${default_properties}"
# shellcheck source=/dev/null
source "${profile_properties}"
set +a
for name in "${!overrides[@]}"; do
  export "${name}=${overrides[${name}]}"
done

if [[ "${CARD_STRATEGY_BUILD_BEFORE_TRAINING:-1}" == "1" ]]; then
  mvn -pl desktop -am -DskipTests package
fi

python_bin="${CARD_STRATEGY_PYTHON:-${repo_root}/.venv-rl/bin/python}"
output="${CARD_STRATEGY_OUTPUT:-${repo_root}/assets/ai/card-strategies/${profile_id}/rl_card_strategy_policy.json}"
checkpoint="${CARD_STRATEGY_CHECKPOINT:-${repo_root}/rl-checkpoints/card-strategies/${profile_id}/model.pt}"
resume_args=()
if [[ "${CARD_STRATEGY_RESUME:-0}" == "1" ]]; then
  resume_args+=(--resume)
fi
if [[ "${CARD_STRATEGY_REFRESH_IMITATION:-0}" == "1" ]]; then
  resume_args+=(--refresh-imitation)
fi
if [[ "${CARD_STRATEGY_EVALUATE_ONLY:-0}" == "1" ]]; then
  resume_args+=(--evaluate-only)
fi
if [[ "${CARD_STRATEGY_FORCE_EXPORT:-0}" == "1" ]]; then
  resume_args+=(--force-export)
fi
init_profile="${CARD_STRATEGY_INIT_PROFILE:-}"
if [[ ! -f "${checkpoint}" && -n "${init_profile}" && "${init_profile}" != "${profile_id}" ]]; then
  init_checkpoint="${repo_root}/rl-checkpoints/card-strategies/${init_profile}/model.pt"
  if [[ -f "${init_checkpoint}" ]]; then
    resume_args+=(--init-checkpoint "${init_checkpoint}")
  fi
fi

"${python_bin}" -u tools/rl/train_card_strategy.py \
  --profile-id "${profile_id}" \
  --strategy-type "${CARD_STRATEGY_TYPE:-${profile_id}}" \
  --jar "${CARD_STRATEGY_JAR:-${repo_root}/desktop/target/ratass-desktop-1.0.jar}" \
  --policy-root "${CARD_STRATEGY_DRIVER_POLICY_ROOT:-${repo_root}/assets/ai/policies}" \
  --output "${output}" \
  --checkpoint "${checkpoint}" \
  "${resume_args[@]}" \
  --evaluate-mode "${CARD_STRATEGY_EVALUATE_MODE}" \
  --episodes "${CARD_STRATEGY_EPISODES}" \
  --imitation-decisions "${CARD_STRATEGY_IMITATION_DECISIONS}" \
  --batch-episodes "${CARD_STRATEGY_BATCH_EPISODES}" \
  --eval-episodes "${CARD_STRATEGY_EVAL_EPISODES}" \
  --selection-eval-episodes "${CARD_STRATEGY_SELECTION_EVAL_EPISODES}" \
  --validation-every "${CARD_STRATEGY_VALIDATION_EVERY}" \
  --early-stop-patience "${CARD_STRATEGY_EARLY_STOP_PATIENCE}" \
  --hidden-size "${CARD_STRATEGY_HIDDEN_SIZE}" \
  --hidden-layers "${CARD_STRATEGY_HIDDEN_LAYERS}" \
  --lr "${CARD_STRATEGY_LR}" \
  --imitation-lr "${CARD_STRATEGY_IMITATION_LR}" \
  --personality-teacher-weight "${CARD_STRATEGY_PERSONALITY_TEACHER_WEIGHT}" \
  --teacher-rollout-ratio "${CARD_STRATEGY_TEACHER_ROLLOUT_RATIO}" \
  --teacher-rollout-final-ratio "${CARD_STRATEGY_TEACHER_ROLLOUT_FINAL_RATIO}" \
  --gamma "${CARD_STRATEGY_GAMMA}" \
  --entropy "${CARD_STRATEGY_ENTROPY}" \
  --ppo-clip "${CARD_STRATEGY_PPO_CLIP}" \
  --ppo-epochs "${CARD_STRATEGY_PPO_EPOCHS}" \
  --elite-fraction "${CARD_STRATEGY_ELITE_FRACTION}" \
  --value-coefficient "${CARD_STRATEGY_VALUE_COEFFICIENT}" \
  --grad-clip "${CARD_STRATEGY_GRAD_CLIP}" \
  --self-play-ratio "${CARD_STRATEGY_SELF_PLAY_RATIO}" \
  --mixed-training-ratio "${CARD_STRATEGY_MIXED_TRAINING_RATIO}" \
  --self-play-snapshot-every "${CARD_STRATEGY_SELF_PLAY_SNAPSHOT_EVERY}" \
  --selection-opponents "${CARD_STRATEGY_SELECTION_OPPONENTS}" \
  --selection-mode "${CARD_STRATEGY_SELECTION_MODE}" \
  --max-win-rate-regression "${CARD_STRATEGY_MAX_WIN_RATE_REGRESSION}" \
  --minimum-unique-cards "${CARD_STRATEGY_MINIMUM_UNIQUE_CARDS}" \
  --minimum-stat-synergies-per-episode "${CARD_STRATEGY_MINIMUM_STAT_SYNERGIES_PER_EPISODE}" \
  --mixed-opponent-policies "${CARD_STRATEGY_MIXED_OPPONENT_POLICIES}" \
  --field-size "${CARD_STRATEGY_FIELD_SIZE}" \
  --circuits "${CARD_STRATEGY_CIRCUITS}" \
  --laps "${CARD_STRATEGY_LAPS}" \
  --seed "${CARD_STRATEGY_SEED}" \
  --reward-championship-win "${CARD_STRATEGY_REWARD_CHAMPIONSHIP_WIN}" \
  --reward-final-position "${CARD_STRATEGY_REWARD_FINAL_POSITION}" \
  --reward-race-position "${CARD_STRATEGY_REWARD_RACE_POSITION}" \
  --reward-level "${CARD_STRATEGY_REWARD_LEVEL}" \
  --reward-xp "${CARD_STRATEGY_REWARD_XP}" \
  --reward-novelty "${CARD_STRATEGY_REWARD_NOVELTY}" \
  --reward-skip-penalty "${CARD_STRATEGY_REWARD_SKIP_PENALTY}" \
  --reward-driver "${CARD_STRATEGY_REWARD_DRIVER}" \
  --reward-tuning "${CARD_STRATEGY_REWARD_TUNING}" \
  --reward-technique "${CARD_STRATEGY_REWARD_TECHNIQUE}" \
  --reward-powerup "${CARD_STRATEGY_REWARD_POWERUP}" \
  --reward-revenge "${CARD_STRATEGY_REWARD_REVENGE}" \
  --reward-technique-amplifier "${CARD_STRATEGY_REWARD_TECHNIQUE_AMPLIFIER}" \
  --reward-powerup-amplifier "${CARD_STRATEGY_REWARD_POWERUP_AMPLIFIER}" \
  --reward-revenge-amplifier "${CARD_STRATEGY_REWARD_REVENGE_AMPLIFIER}" \
  --reward-amplifier-link "${CARD_STRATEGY_REWARD_AMPLIFIER_LINK}" \
  --reward-random-powerup "${CARD_STRATEGY_REWARD_RANDOM_POWERUP}" \
  --reward-random-revenge "${CARD_STRATEGY_REWARD_RANDOM_REVENGE}" \
  --reward-preferred-cards "${CARD_STRATEGY_REWARD_PREFERRED_CARDS}" \
  --reward-preferred-card "${CARD_STRATEGY_REWARD_PREFERRED_CARD}" \
  --reward-discouraged-cards "${CARD_STRATEGY_REWARD_DISCOURAGED_CARDS}" \
  --reward-discouraged-card-penalty "${CARD_STRATEGY_REWARD_DISCOURAGED_CARD_PENALTY}" \
  --reward-lap-win "${CARD_STRATEGY_REWARD_LAP_WIN}" \
  --reward-card-selection "${CARD_STRATEGY_REWARD_CARD_SELECTION}" \
  --reward-tuning-technique-synergy "${CARD_STRATEGY_REWARD_TUNING_TECHNIQUE_SYNERGY}" \
  --reward-card-type-rotation "${CARD_STRATEGY_REWARD_CARD_TYPE_ROTATION}"
