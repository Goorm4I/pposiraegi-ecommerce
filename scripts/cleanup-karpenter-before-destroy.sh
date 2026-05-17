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
KEEP_MONITORING_PVC=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes) YES=true; shift ;;
    --notify) NOTIFY_ENABLED=true; shift ;;
    --keep-monitoring-pvc) KEEP_MONITORING_PVC=true; shift ;;
    --timeout)
      [[ $# -ge 2 ]] || { echo "--timeout requires seconds" >&2; exit 2; }
      TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    *) echo "usage: $0 [--yes] [--notify] [--keep-monitoring-pvc] [--timeout 300]" >&2; exit 2 ;;
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

kubectl_exists() {
  kubectl get "$@" >/dev/null 2>&1
}

scale_down_if_exists() {
  local namespace="$1"
  local resource="$2"

  kubectl_exists namespace "${namespace}" || return 0
  kubectl scale "${resource}" -n "${namespace}" --all --replicas=0 2>/dev/null || true
}

disable_argocd_automation() {
  echo "Disabling ArgoCD automated sync to prevent deleted Karpenter resources from being recreated..."
  kubectl patch application pposiraegi -n argocd --type merge \
    -p '{"spec":{"syncPolicy":{"automated":null}}}' \
    2>/dev/null || true
}

delete_blocking_pdbs() {
  echo "Deleting PDBs that can block drain during pre-destroy cleanup..."
  kubectl delete pdb -n production --all --ignore-not-found 2>/dev/null || true
  kubectl delete pdb -n monitoring --all --ignore-not-found 2>/dev/null || true
  kubectl delete pdb -n istio-system istiod --ignore-not-found 2>/dev/null || true
}

scale_down_workloads() {
  echo "Scaling down application and monitoring workloads before deleting Karpenter nodes..."
  scale_down_if_exists production deployment
  scale_down_if_exists production statefulset
  scale_down_if_exists monitoring deployment
  scale_down_if_exists monitoring statefulset
  kubectl delete gateway waypoint -n production --ignore-not-found 2>/dev/null || true
}

delete_monitoring_pvcs() {
  [[ "${KEEP_MONITORING_PVC}" == false ]] || {
    echo "Keeping monitoring PVCs because --keep-monitoring-pvc was set."
    return 0
  }

  kubectl_exists namespace monitoring || return 0

  echo "Uninstalling monitoring Helm releases before deleting monitoring PVCs..."
  helm uninstall opentelemetry-collector -n monitoring 2>/dev/null || true
  helm uninstall tempo -n monitoring 2>/dev/null || true
  helm uninstall promtail -n monitoring 2>/dev/null || true
  helm uninstall loki -n monitoring 2>/dev/null || true
  helm uninstall kube-prometheus-stack -n monitoring 2>/dev/null || true

  echo "Deleting monitoring PVCs to prevent orphan EBS volumes after cluster destroy..."
  kubectl delete pvc -n monitoring --all --ignore-not-found --wait=false 2>/dev/null || true

  local deadline=$((SECONDS + 180))
  local remaining
  while [[ "${SECONDS}" -lt "${deadline}" ]]; do
    remaining="$(kubectl get pvc -n monitoring --no-headers 2>/dev/null | count_lines)"
    [[ "${remaining}" -eq 0 ]] && return 0
    echo "Waiting for monitoring PVCs to disappear... remaining=${remaining}"
    sleep 10
  done

  remaining="$(kubectl get pvc -n monitoring --no-headers 2>/dev/null | count_lines)"
  if [[ "${remaining}" -ne 0 ]]; then
    echo "[WARN] monitoring PVC cleanup timed out, remaining=${remaining}" >&2
    echo "       Run check-residue.sh after terraform destroy and delete available EBS volumes if needed." >&2
  fi
}

delete_karpenter_resources() {
  echo "Deleting NodePools first to prevent Karpenter from provisioning replacement nodes..."
  kubectl delete nodepools --all --ignore-not-found --wait=false 2>/dev/null || true

  sleep 5

  echo "Deleting NodeClaims..."
  kubectl delete nodeclaims --all --ignore-not-found --wait=false 2>/dev/null || true

  local deadline=$((SECONDS + TIMEOUT_SECONDS))
  local remaining
  while [[ "${SECONDS}" -lt "${deadline}" ]]; do
    # If ArgoCD or a controller recreates a NodePool, delete it again before it can
    # keep replacing terminating NodeClaims.
    kubectl delete nodepools --all --ignore-not-found --wait=false >/dev/null 2>&1 || true
    kubectl delete nodeclaims --all --ignore-not-found --wait=false >/dev/null 2>&1 || true

    remaining="$(kubectl get nodeclaims --no-headers 2>/dev/null | count_lines)"
    [[ "${remaining}" -eq 0 ]] && return 0
    echo "Waiting for NodeClaims to disappear... remaining=${remaining}"
    sleep 10
  done

  remaining="$(kubectl get nodeclaims --no-headers 2>/dev/null | count_lines)"
  [[ "${remaining}" -eq 0 ]]
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
  echo "Use --keep-monitoring-pvc only when Prometheus/Grafana/Loki PVC data must survive."
  exit 0
fi

notify "pposiraegi cleanup: blocking Karpenter provisioning and deleting ${nodeclaim_count} NodeClaim(s) before terraform destroy"

disable_argocd_automation
delete_blocking_pdbs
scale_down_workloads
delete_monitoring_pvcs

if ! delete_karpenter_resources; then
  remaining="$(kubectl get nodeclaims --no-headers 2>/dev/null | count_lines)"
  notify "pposiraegi cleanup: NodeClaim cleanup timed out, remaining=${remaining}"
  echo "[WARN] NodeClaim cleanup timed out, remaining=${remaining}" >&2
  exit 1
fi

notify "pposiraegi cleanup: Karpenter NodeClaims deleted"
echo "[OK] Karpenter NodeClaims deleted."
