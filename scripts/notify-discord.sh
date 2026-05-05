#!/usr/bin/env bash
set -euo pipefail

WEBHOOK_URL="${DISCORD_WEBHOOK_URL:-}"
SSM_PARAM="${DISCORD_WEBHOOK_SSM_PARAM:-/pposiraegi/discord/webhook-url}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
MESSAGE="${1:-}"

if [[ -z "${WEBHOOK_URL}" ]] && command -v aws >/dev/null 2>&1; then
  WEBHOOK_URL="$(
    aws ssm get-parameter \
      --region "${AWS_REGION}" \
      --name "${SSM_PARAM}" \
      --with-decryption \
      --query Parameter.Value \
      --output text 2>/dev/null || true
  )"
fi

if [[ -z "${WEBHOOK_URL}" ]]; then
  echo "[WARN] DISCORD_WEBHOOK_URL is empty; notification skipped" >&2
  exit 0
fi

if [[ -z "${MESSAGE}" ]]; then
  echo "usage: DISCORD_WEBHOOK_URL=... $0 'message'" >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[WARN] curl not found; notification skipped" >&2
  exit 0
fi

escaped="$(
  MESSAGE="${MESSAGE}" ruby -rjson -e 'print JSON.generate({content: ENV.fetch("MESSAGE")})'
)"
curl -fsS \
  -H "Content-Type: application/json" \
  -d "${escaped}" \
  "${WEBHOOK_URL}" >/dev/null
