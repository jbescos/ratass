#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  tools/rl/commit_performance_sweep.sh [options] <start-commit> <profile>

Walks from <start-commit> to HEAD, testing each commit in a disposable local
clone under /tmp by default. For every commit it runs:

  RL_FORCE_FRESH_START=1 tools/rl/train.sh <profile>
  tools/rl/evaluate_lap_times_all.sh <profile>

Each successful commit gets one raw <commit>.txt file containing only the
output of tools/rl/evaluate_lap_times_all.sh.

Options:
  --end <commit>          Last commit to test. Defaults to HEAD.
  --output-dir <dir>      Output directory. Defaults under logs/rl-commit-performance.
  --clone-dir <dir>       Temporary git clone directory. Defaults under /tmp.
  --worktree-dir <dir>    Deprecated alias for --clone-dir.
  --keep-clone            Do not remove the temporary clone on exit.
  --keep-worktree         Deprecated alias for --keep-clone.
  --no-ray-cleanup        Do not run "ray stop --force" between commits.
  --workers <n>           Override RL_WORKERS. Defaults to 2 for sweep stability.
  --ray-cpus <n>          Override RL_RAY_NUM_CPUS. Defaults to 4 for sweep stability.
  --preserve-rl-env       Forward existing RL_* environment overrides.
  -h, --help              Show this help.

By default, inherited RL_* variables are cleared so each commit is tested with
the profile config checked into that commit. The script still sets per-commit
checkpoint/status paths and RL_FORCE_FRESH_START=1.
EOF
}

die() {
  echo "error: $*" >&2
  exit 2
}

resolve_path() {
  local path="$1"
  if [[ "${path}" == /* ]]; then
    printf '%s\n' "${path}"
  else
    printf '%s/%s\n' "${repo_root}" "${path}"
  fi
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

end_ref="HEAD"
output_dir=""
clone_dir=""
keep_clone=0
preserve_rl_env=0
ray_cleanup=1
workers_override="2"
ray_cpus_override="4"

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --end)
      [[ "$#" -ge 2 ]] || die "--end requires a commit"
      end_ref="$2"
      shift 2
      ;;
    --output-dir)
      [[ "$#" -ge 2 ]] || die "--output-dir requires a path"
      output_dir="$2"
      shift 2
      ;;
    --clone-dir|--worktree-dir)
      [[ "$#" -ge 2 ]] || die "$1 requires a path"
      clone_dir="$2"
      shift 2
      ;;
    --keep-clone|--keep-worktree)
      keep_clone=1
      shift
      ;;
    --no-ray-cleanup)
      ray_cleanup=0
      shift
      ;;
    --workers)
      [[ "$#" -ge 2 ]] || die "--workers requires a value"
      workers_override="$2"
      shift 2
      ;;
    --ray-cpus)
      [[ "$#" -ge 2 ]] || die "--ray-cpus requires a value"
      ray_cpus_override="$2"
      shift 2
      ;;
    --preserve-rl-env)
      preserve_rl_env=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      break
      ;;
    -*)
      die "unknown option: $1"
      ;;
    *)
      break
      ;;
  esac
done

[[ "$#" -eq 2 ]] || {
  usage >&2
  exit 2
}

start_ref="$1"
profile="$2"

start_commit="$(git -C "${repo_root}" rev-parse --verify "${start_ref}^{commit}")"
end_commit="$(git -C "${repo_root}" rev-parse --verify "${end_ref}^{commit}")"
start_short="$(git -C "${repo_root}" rev-parse --short=12 "${start_commit}")"
end_short="$(git -C "${repo_root}" rev-parse --short=12 "${end_commit}")"

if ! git -C "${repo_root}" merge-base --is-ancestor "${start_commit}" "${end_commit}"; then
  die "${start_short} is not an ancestor of ${end_short}"
fi

if [[ -z "${output_dir}" ]]; then
  timestamp="$(date +%Y%m%dT%H%M%S)"
  output_dir="logs/rl-commit-performance/${profile}-${start_short}-to-${end_short}-${timestamp}"
fi
output_dir="$(resolve_path "${output_dir}")"
mkdir -p "${output_dir}"

if [[ -z "${clone_dir}" ]]; then
  clone_dir="/tmp/ratass-commit-performance-${profile}-${start_short}-to-${end_short}"
else
  clone_dir="$(resolve_path "${clone_dir}")"
fi
runtime_dir="/tmp/ratass-commit-performance-runtime-${profile}-${start_short}-to-${end_short}-$$"

if [[ "${clone_dir}" == "${repo_root}" ]]; then
  die "--clone-dir must not be the current repository"
fi

cleanup() {
  if declare -F stop_ray_runtime >/dev/null 2>&1; then
    stop_ray_runtime
  fi
  if [[ "${keep_clone}" == "0" && -d "${clone_dir}/.git" ]]; then
    rm -rf "${clone_dir}" >/dev/null 2>&1 || true
  fi
  rm -rf "${runtime_dir}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [[ -e "${clone_dir}" ]]; then
  die "clone directory already exists: ${clone_dir}"
fi

if git -C "${repo_root}" rev-parse --verify -q "${start_commit}^" >/dev/null; then
  commit_range="${start_commit}^..${end_commit}"
else
  commit_range="${end_commit}"
fi

mapfile -t commits < <(
  git -C "${repo_root}" rev-list --reverse --ancestry-path "${commit_range}"
)

[[ "${#commits[@]}" -gt 0 ]] || die "no commits to test"

git clone --no-checkout "${repo_root}" "${clone_dir}"
git -C "${clone_dir}" checkout --detach "${commits[0]}" >/dev/null
mkdir -p "${runtime_dir}"

prepare_clone_runtime() {
  if [[ -d "${repo_root}/.venv-rl" && ! -e "${clone_dir}/.venv-rl" ]]; then
    ln -s "${repo_root}/.venv-rl" "${clone_dir}/.venv-rl"
  fi
}

clear_rl_environment() {
  [[ "${preserve_rl_env}" == "1" ]] && return
  local name
  while IFS='=' read -r name _; do
    if [[ "${name}" == RL_* ]]; then
      unset "${name}"
    fi
  done < <(env)
}

stop_ray_runtime() {
  [[ "${ray_cleanup}" == "1" ]] || return
  [[ -d "${clone_dir}" ]] || return
  (
    cd "${clone_dir}"
    clear_rl_environment
    if [[ -x ".venv-rl/bin/ray" ]]; then
      .venv-rl/bin/ray stop --force >/dev/null 2>&1 || true
    elif command -v ray >/dev/null 2>&1; then
      ray stop --force >/dev/null 2>&1 || true
    fi
  )
}

run_to_file() {
  local log_file="$1"
  shift
  set +e
  "$@" > "${log_file}" 2>&1
  local status="$?"
  set -e
  return "${status}"
}

run_to_file_and_stdout() {
  local log_file="$1"
  shift
  set +e
  "$@" 2>&1 | tee "${log_file}"
  local status="${PIPESTATUS[0]}"
  set -e
  return "${status}"
}

for commit in "${commits[@]}"; do
  short="$(git -C "${repo_root}" rev-parse --short=12 "${commit}")"
  subject="$(git -C "${repo_root}" log -1 --format=%s "${commit}")"
  commit_dir="${runtime_dir}/${short}"
  train_log="${commit_dir}/train.log"
  eval_log="${output_dir}/${commit}.txt"
  wrapper_log="${commit_dir}/train-wrapper.log"
  status_file="${commit_dir}/status.txt"
  checkpoint_dir="${commit_dir}/checkpoint"
  ray_tmp="/tmp/ratass-ray-${short}"

  mkdir -p "${commit_dir}" "${checkpoint_dir}" "$(dirname "${status_file}")"

  echo "commit_start=${short} subject=${subject}"
  git -C "${clone_dir}" reset --hard "${commit}" >/dev/null
  prepare_clone_runtime
  stop_ray_runtime

  run_training_for_commit() {
    (
      cd "${clone_dir}"
      clear_rl_environment
      export RL_FORCE_FRESH_START=1
      export RL_CURRICULUM_CHECKPOINT_DIR="${checkpoint_dir}"
      export RL_CHECKPOINT_DIR="${checkpoint_dir}"
      export RL_POLICY_TRAINING_STATE="${commit_dir}/training-state.properties"
      export RL_POLICY_STATUS_FILE="${status_file}"
      export RL_TRAIN_LOG="${wrapper_log}"
      export RL_RAY_TEMP_DIR="${ray_tmp}"
      if [[ -n "${workers_override}" ]]; then
        export RL_WORKERS="${workers_override}"
      fi
      if [[ -n "${ray_cpus_override}" ]]; then
        export RL_RAY_NUM_CPUS="${ray_cpus_override}"
      fi
      bash tools/rl/train.sh "${profile}"
    )
  }

  run_evaluation_for_commit() {
    (
      cd "${clone_dir}"
      clear_rl_environment
      bash tools/rl/evaluate_lap_times_all.sh "${profile}"
    )
  }

  if run_to_file_and_stdout "${train_log}" run_training_for_commit; then
    train_status=0
  else
    train_status="$?"
  fi

  if [[ "${train_status}" == "0" ]]; then
    if run_to_file "${eval_log}" run_evaluation_for_commit; then
      eval_status=0
    else
      eval_status="$?"
    fi
    echo "commit_done=${short} eval=${eval_log} eval_status=${eval_status}"
  else
    eval_status=skipped
    rm -f "${eval_log}"
    echo "commit_failed=${short} train_status=${train_status}"
  fi
  stop_ray_runtime
done

echo "sweep_done output_dir=${output_dir}"
