#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

if [[ "$#" -gt 0 ]]; then
  profiles=("$@")
else
  profiles=(
    strategy01 strategy02 strategy03 strategy04 strategy05 strategy06
    strategy07 strategy08 strategy09 strategy10 strategy11 strategy12
  )
fi

mvn -pl desktop -am -DskipTests package
for profile_id in "${profiles[@]}"; do
  echo "card_strategy_batch_start profile=${profile_id}"
  CARD_STRATEGY_BUILD_BEFORE_TRAINING=0 \
    tools/rl/train_card_strategy.sh "${profile_id}"
  echo "card_strategy_batch_complete profile=${profile_id}"
done
