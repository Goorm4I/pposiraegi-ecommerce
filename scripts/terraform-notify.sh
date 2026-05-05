#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA_DIR="${REPO_ROOT}/infrastructure"
NOTIFY="${REPO_ROOT}/scripts/notify-discord.sh"

if [[ $# -lt 1 ]]; then
  cat >&2 <<'USAGE'
usage:
  DISCORD_WEBHOOK_URL=... AWS_PROFILE=goorm scripts/terraform-notify.sh plan -out=tfplan
  DISCORD_WEBHOOK_URL=... AWS_PROFILE=goorm scripts/terraform-notify.sh apply tfplan
  DISCORD_WEBHOOK_URL=... AWS_PROFILE=goorm scripts/terraform-notify.sh destroy
  DISCORD_WEBHOOK_SSM_PARAM=/pposiraegi/discord/webhook-url AWS_PROFILE=goorm scripts/terraform-notify.sh apply tfplan

This wrapper sends start/success/failure notifications around Terraform. It can read the
Discord webhook from DISCORD_WEBHOOK_URL or SSM SecureString.
USAGE
  exit 2
fi

cmd="$1"
shift

cluster="${CLUSTER_NAME:-pposiraegi-cluster}"
region="${AWS_REGION:-ap-northeast-2}"
profile="${AWS_PROFILE:-default}"
started_at="$(date '+%Y-%m-%d %H:%M:%S %Z')"

"${NOTIFY}" "terraform ${cmd} started: ${cluster}/${region} profile=${profile} at ${started_at}"

set +e
(
  cd "${INFRA_DIR}"
  terraform "${cmd}" "$@"
)
status=$?
set -e

finished_at="$(date '+%Y-%m-%d %H:%M:%S %Z')"
if [[ ${status} -eq 0 ]]; then
  "${NOTIFY}" "terraform ${cmd} succeeded: ${cluster}/${region} at ${finished_at}"
else
  "${NOTIFY}" "terraform ${cmd} failed(${status}): ${cluster}/${region} at ${finished_at}"
fi

exit "${status}"
