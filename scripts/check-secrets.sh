#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

patterns=(
  'discord\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+'
  'DISCORD_WEBHOOK_URL=.*https://'
  'aws_secret_access_key[[:space:]]*='
  'aws_access_key_id[[:space:]]*='
  'db_password[[:space:]]*=[[:space:]]*"[^"]+"'
  'jwt_secret[[:space:]]*=[[:space:]]*"[^"]+"'
)

exclude_args=(
  --glob '!.git/**'
  --glob '!node_modules/**'
  --glob '!frontend/node_modules/**'
  --glob '!backend/**/build/**'
  --glob '!**/.terraform/**'
  --glob '!scripts/check-secrets.sh'
)

found=0
for pattern in "${patterns[@]}"; do
  if rg -n --hidden "${exclude_args[@]}" "${pattern}" "${ROOT}"; then
    found=1
  fi
done

if [[ "${found}" -eq 1 ]]; then
  echo "[ERROR] Secret-like value found. Remove it before pushing." >&2
  exit 1
fi

echo "[OK] No obvious secrets found."
