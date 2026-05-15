#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
image="${RL_DOCKER_IMAGE:-ratass-rl:latest}"
shm_size="${RL_DOCKER_SHM_SIZE:-12g}"

cd "${repo_root}"

mkdir -p .docker-home/.m2 rl-logs

if [[ "${RL_DOCKER_BUILD:-1}" != "0" ]]; then
  docker build -f tools/rl/Dockerfile -t "${image}" .
fi

docker_args=(--rm --shm-size="${shm_size}")
if [[ -t 0 && -t 1 ]]; then
  docker_args+=(-it)
fi
if [[ "${RL_DOCKER_USER:-1}" != "0" ]]; then
  docker_args+=(--user "$(id -u):$(id -g)")
fi
if [[ "${RL_DOCKER_GPU:-0}" == "1" ]]; then
  docker_args+=(--gpus all)
fi

docker_args+=(
  -v "${repo_root}:/workspace/ratass"
  -v "${repo_root}/.docker-home:/home/ratass"
  -e HOME=/home/ratass
  -e MAVEN_CONFIG=/home/ratass/.m2
  -e RL_FOREVER_ITERATIONS="${RL_FOREVER_ITERATIONS:-100}"
  -e RL_MAX_CYCLES="${RL_MAX_CYCLES:-0}"
  -e RL_CHECKPOINT_EVERY="${RL_CHECKPOINT_EVERY:-20}"
  -e RL_WORKERS="${RL_WORKERS:-4}"
  -e RL_LR="${RL_LR:-3e-4}"
  -e RL_MAX_GOALS="${RL_MAX_GOALS:-6}"
  -e RL_TARGET_RADIUS="${RL_TARGET_RADIUS:-1.65}"
  -e RL_TARGET_HOLD_SECONDS="${RL_TARGET_HOLD_SECONDS:-0.85}"
  -e RL_NUM_GPUS="${RL_NUM_GPUS:-0}"
  -e RL_NO_REWARD_SUMMARY="${RL_NO_REWARD_SUMMARY:-1}"
  -e RL_PACKAGE_EVERY_CYCLES="${RL_PACKAGE_EVERY_CYCLES:-1}"
  -e RL_BUILD_BEFORE_TRAINING="${RL_BUILD_BEFORE_TRAINING:-1}"
  -e RL_RAY_TEMP_DIR="${RL_RAY_TEMP_DIR:-rl-logs/ray}"
  -e RL_BEST_EXPORT="${RL_BEST_EXPORT:-1}"
  -e RL_BEST_OUTPUT="${RL_BEST_OUTPUT:-assets/ai/rl_enemy_policy.json}"
  -e RL_BEST_EVAL_EPISODES_PER_MAP="${RL_BEST_EVAL_EPISODES_PER_MAP:-1}"
  -e RL_BEST_EVAL_EPISODES="${RL_BEST_EVAL_EPISODES:-0}"
  -e RL_BEST_EVAL_FIELD_SIZE="${RL_BEST_EVAL_FIELD_SIZE:-}"
  -e RL_BEST_EVAL_STEPS="${RL_BEST_EVAL_STEPS:-0}"
)

if [[ -n "${RL_CHECKPOINT_DIR:-}" ]]; then
  docker_args+=(-e RL_CHECKPOINT_DIR="${RL_CHECKPOINT_DIR}")
fi
if [[ -n "${RL_CONTROLLED_AGENTS:-}" ]]; then
  docker_args+=(-e RL_CONTROLLED_AGENTS="${RL_CONTROLLED_AGENTS}")
fi
if [[ -n "${RL_FIELD_SIZE:-}" ]]; then
  docker_args+=(-e RL_FIELD_SIZE="${RL_FIELD_SIZE}")
fi
if [[ -n "${RL_MAX_ACTION_STEPS:-}" ]]; then
  docker_args+=(-e RL_MAX_ACTION_STEPS="${RL_MAX_ACTION_STEPS}")
fi
if [[ -n "${RL_INIT_POLICY:-}" ]]; then
  docker_args+=(-e RL_INIT_POLICY="${RL_INIT_POLICY}")
fi
if [[ -n "${RL_MAP_IDS:-}" ]]; then
  docker_args+=(-e RL_MAP_IDS="${RL_MAP_IDS}")
fi
if [[ "${RL_BEST_EVAL_MAP_IDS+x}" == "x" ]]; then
  docker_args+=(-e RL_BEST_EVAL_MAP_IDS="${RL_BEST_EVAL_MAP_IDS}")
fi
if [[ -n "${RL_BEST_EVAL_STATE:-}" ]]; then
  docker_args+=(-e RL_BEST_EVAL_STATE="${RL_BEST_EVAL_STATE}")
fi
if [[ -n "${RL_CURRICULUM_PRESET:-}" ]]; then
  docker_args+=(-e RL_CURRICULUM_PRESET="${RL_CURRICULUM_PRESET}")
fi
if [[ -n "${RL_DOCKER_EXTRA_ARGS:-}" ]]; then
  # shellcheck disable=SC2206
  extra_args=(${RL_DOCKER_EXTRA_ARGS})
  docker_args+=("${extra_args[@]}")
fi

exec docker run "${docker_args[@]}" "${image}" bash tools/rl/docker_train_entrypoint.sh "$@"
