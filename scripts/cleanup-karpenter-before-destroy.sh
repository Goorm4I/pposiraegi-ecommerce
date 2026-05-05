#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NOTIFY="${REPO_ROOT}/scripts/notify-discord.sh"

CLUSTER_NAME="${CLUSTER_NAME:-pposiraegi-cluster}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
AWS_PROFILE="${AWS_PROFILE:-goorm}"

YES=false
NOTIFY_ENABLED=false
TIMEOUT_SECONDS=300

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes) YES=true; shift ;;
    --notify) NOTIFY_ENABLED=true; shift ;;
    --timeout)
      [[ $# -ge 2 ]] || { echo "--timeout requires seconds" >&2; exit 2; }
      TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    *) echo "usage: $0 [--yes] [--notify] [--timeout 300]" >&2; exit 2 ;;
  esac
done

export AWS_PROFILE AWS_REGION

notify() {
  [[ "${NOTIFY_ENABLED}" == true ]] || return 0
  "${NOTIFY}" "$1" 2>/dev/null || true
}

count_lines() {
  awk 'NF { count++ } END { print count + 0 }'
}

nodepools="$(kubectl get nodepools --no-headers 2>/dev/null || true)"
nodepool_count="$(printf '%s\n' "${nodepools}" | count_lines)"
nodeclaims="$(kubectl get nodeclaims --no-headers 2>/dev/null || true)"
nodeclaim_count="$(printf '%s\n' "${nodeclaims}" | count_lines)"

echo "Karpenter NodePools: ${nodepool_count}"
if [[ "${nodepool_count}" -gt 0 ]]; then
  printf '%s\n' "${nodepools}"
fi
echo ""
echo "Karpenter NodeClaims: ${nodeclaim_count}"
if [[ "${nodeclaim_count}" -gt 0 ]]; then
  printf '%s\n' "${nodeclaims}"
fi

echo ""
echo "Karpenter EC2 instances:"
aws ec2 describe-instances \
  --region "${AWS_REGION}" \
  --filters \
    "Name=tag:karpenter.sh/nodepool,Values=*" \
    "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query 'Reservations[].Instances[].{InstanceId:InstanceId,State:State.Name,Type:InstanceType,Az:Placement.AvailabilityZone,NodePool:Tags[?Key==`karpenter.sh/nodepool`]|[0].Value,Name:Tags[?Key==`Name`]|[0].Value}' \
  --output table || true

if [[ "${YES}" != true ]]; then
  echo ""
  echo "[DRY-RUN] No resources were deleted."
  echo "Run with --yes to delete all Karpenter NodeClaims before terraform destroy."
  exit 0
fi

notify "pposiraegi cleanup: blocking Karpenter provisioning and deleting ${nodeclaim_count} NodeClaim(s) before terraform destroy"

echo "Disabling ArgoCD self-heal to prevent deleted Karpenter resources from being recreated..."
kubectl patch application pposiraegi -n argocd --type merge \
  -p '{"spec":{"syncPolicy":{"automated":{"prune":true,"selfHeal":false}}}}' \
  2>/dev/null || true

echo "Deleting PDBs that can block drain during pre-destroy cleanup..."
kubectl delete pdb -n production --all --ignore-not-found 2>/dev/null || true
kubectl delete pdb -n istio-system istiod --ignore-not-found 2>/dev/null || true

if [[ "${nodepool_count}" -gt 0 ]]; then
  echo "Deleting NodePools first to prevent Karpenter from provisioning replacement nodes..."
  kubectl delete nodepools --all --ignore-not-found --wait=false
fi

sleep 5

if [[ "${nodeclaim_count}" -gt 0 ]]; then
  kubectl delete nodeclaims --all --ignore-not-found --wait=false
fi

deadline=$((SECONDS + TIMEOUT_SECONDS))
while [[ "${SECONDS}" -lt "${deadline}" ]]; do
  remaining="$(kubectl get nodeclaims --no-headers 2>/dev/null | count_lines)"
  [[ "${remaining}" -eq 0 ]] && break
  echo "Waiting for NodeClaims to disappear... remaining=${remaining}"
  sleep 10
done

remaining="$(kubectl get nodeclaims --no-headers 2>/dev/null | count_lines)"
if [[ "${remaining}" -ne 0 ]]; then
  notify "pposiraegi cleanup: NodeClaim cleanup timed out, remaining=${remaining}"
  echo "[WARN] NodeClaim cleanup timed out, remaining=${remaining}" >&2
  exit 1
fi

notify "pposiraegi cleanup: Karpenter NodeClaims deleted"
echo "[OK] Karpenter NodeClaims deleted."
