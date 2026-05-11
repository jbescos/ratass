#!/usr/bin/env bash
set -euo pipefail

cd /workspace/ratass

mkdir -p rl-logs

objective="${RL_OBJECTIVE:-navigation}"
timestamp="$(date +%Y%m%d-%H%M%S)"
log_file="${RL_LOG_FILE:-rl-logs/${objective}-docker-${timestamp}.log}"

mkdir -p "$(dirname "${log_file}")"
exec > >(tee -a "${log_file}") 2>&1

export PYTHON_BIN="${PYTHON_BIN:-/opt/ratass-rl-venv/bin/python}"
export RL_OBJECTIVE="${objective}"
export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"

if [[ "${RL_OBJECTIVE}" == "navigation" ]]; then
  export RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR:-rl-checkpoints-navigation}"
  export RL_CONTROLLED_AGENTS="${RL_CONTROLLED_AGENTS:-1}"
  export RL_FIELD_SIZE="${RL_FIELD_SIZE:-1}"
  export RL_MAX_ACTION_STEPS="${RL_MAX_ACTION_STEPS:-1200}"
fi

echo "docker_training_started=$(date -Is)"
echo "repo=/workspace/ratass"
echo "log_file=${log_file}"
echo "python=${PYTHON_BIN}"
java -version
mvn -version

exec bash tools/rl/train_forever.sh "$@"
