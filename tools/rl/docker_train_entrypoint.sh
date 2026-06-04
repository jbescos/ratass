#!/usr/bin/env bash
set -euo pipefail

cd /workspace/ratass

mkdir -p rl-logs

objective="${1:-race}"
timestamp="$(date +%Y%m%d-%H%M%S)"
log_file="${RL_LOG_FILE:-rl-logs/${objective}-docker-${timestamp}.log}"

mkdir -p "$(dirname "${log_file}")"
exec > >(tee -a "${log_file}") 2>&1

export PYTHON_BIN="${PYTHON_BIN:-/opt/ratass-rl-venv/bin/python}"
export RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"

echo "docker_training_started=$(date -Is)"
echo "repo=/workspace/ratass"
echo "log_file=${log_file}"
echo "python=${PYTHON_BIN}"
java -version
mvn -version

exec env RL_DETACH=0 bash tools/rl/train.sh "$@"
