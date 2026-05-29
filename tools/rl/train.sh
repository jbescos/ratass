#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

source "${script_dir}/policy_profiles.sh"

log_file="${RL_TRAIN_LOG:-logs/rl-profile-training.log}"

resolve_log_path() {
  if [[ "${log_file}" == /* ]]; then
    printf '%s\n' "${log_file}"
  else
    printf '%s/%s\n' "${repo_root}" "${log_file}"
  fi
}

if [[ "${1:-}" == "--detach" ]]; then
  log_path="$(resolve_log_path)"
  mkdir -p "$(dirname "${log_path}")"
  (
    cd "${repo_root}"
    setsid nohup env RL_DETACH=0 bash "${BASH_SOURCE[0]}" "${@:2}" >> "${log_path}" 2>&1 < /dev/null &
    printf '%s\n' "$!" > "${log_path}.pid"
  )
  printf 'pid=%s log=%s\n' "$(cat "${log_path}.pid")" "${log_path}"
  exit 0
fi

if [[ "${1:-}" == "--foreground" ]]; then
  shift
fi

if ! rl_policy_batch_active; then
  rl_policy_run_batch "${BASH_SOURCE[0]}" "profile" "$@"
  exit 0
fi

preset="${1:-${RL_PRESET:-curriculum}}"
exec bash "${script_dir}/train_forever.sh" "${preset}"
