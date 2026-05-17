#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NOTIFY="${REPO_ROOT}/scripts/notify-discord.sh"

NOTIFY_ENABLED=false
for arg in "$@"; do
  case "${arg}" in
    --notify) NOTIFY_ENABLED=true ;;
    *) echo "usage: $0 [--notify]" >&2; exit 2 ;;
  esac
done

count_lines() {
  awk 'NF { count++ } END { print count + 0 }'
}

safe() {
  "$@" 2>/dev/null || true
}

nodes="$(safe kubectl get nodes --no-headers)"
node_total="$(printf '%s\n' "${nodes}" | count_lines)"
node_ready="$(printf '%s\n' "${nodes}" | awk '$2 == "Ready" { count++ } END { print count + 0 }')"

node_labels="$(safe kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.metadata.labels.eks\.amazonaws\.com/capacityType}{"|"}{.metadata.labels.karpenter\.sh/capacity-type}{"\n"}{end}')"
eks_on_demand="$(printf '%s\n' "${node_labels}" | awk -F'|' '$2 == "ON_DEMAND" { count++ } END { print count + 0 }')"
eks_spot="$(printf '%s\n' "${node_labels}" | awk -F'|' '$2 == "SPOT" { count++ } END { print count + 0 }')"
karpenter_on_demand="$(printf '%s\n' "${node_labels}" | awk -F'|' '$3 == "on-demand" { count++ } END { print count + 0 }')"
karpenter_spot="$(printf '%s\n' "${node_labels}" | awk -F'|' '$3 == "spot" { count++ } END { print count + 0 }')"

pod_summary() {
  local ns="$1"
  local pods
  pods="$(safe kubectl get pods -n "${ns}" --no-headers)"
  local total running ready
  total="$(printf '%s\n' "${pods}" | count_lines)"
  running="$(printf '%s\n' "${pods}" | awk '$3 == "Running" || $3 == "Completed" { count++ } END { print count + 0 }')"
  ready="$(printf '%s\n' "${pods}" | awk -F'[ /]+' '$2 == $3 { count++ } END { print count + 0 }')"
  printf '%s/%s ready, %s/%s running' "${ready}" "${total}" "${running}" "${total}"
}

production_pods="$(pod_summary production)"
monitoring_pods="$(pod_summary monitoring)"
kube_system_pods="$(pod_summary kube-system)"

nodeclaims="$(safe kubectl get nodeclaims --no-headers)"
nodeclaim_total="$(printf '%s\n' "${nodeclaims}" | count_lines)"

nodepool_status="$(safe kubectl get nodepool default -o jsonpath='{range .status.conditions[*]}{.type}={.status}{" "}{end}')"
ec2nodeclass_status="$(safe kubectl get ec2nodeclass default -o jsonpath='{range .status.conditions[*]}{.type}={.status}{" "}{end}')"

argocd_status="$(safe kubectl get application pposiraegi -n argocd -o jsonpath='sync={.status.sync.status} health={.status.health.status}')"
[[ -z "${argocd_status}" ]] && argocd_status="unavailable"

hpa_status="$(safe kubectl get hpa -n production --no-headers | awk '{ print $1 ":" $6 "/" $7 }' | paste -sd ', ' -)"
[[ -z "${hpa_status}" ]] && hpa_status="unavailable"

tempo_status="$(safe kubectl get statefulset tempo -n monitoring -o jsonpath='{.status.readyReplicas}/{.status.replicas}')"
[[ -z "${tempo_status}" ]] && tempo_status="unavailable"
otel_status="$(safe kubectl get deployment opentelemetry-collector -n monitoring -o jsonpath='{.status.readyReplicas}/{.status.replicas}')"
[[ -z "${otel_status}" ]] && otel_status="unavailable"

timestamp="$(date '+%Y-%m-%d %H:%M:%S %Z')"
message="$(cat <<EOF
pposiraegi cluster summary (${timestamp})

Nodes: ${node_ready}/${node_total} Ready
- EKS ON_DEMAND: ${eks_on_demand}
- EKS SPOT: ${eks_spot}
- Karpenter on-demand: ${karpenter_on_demand}
- Karpenter spot: ${karpenter_spot}

Pods:
- production: ${production_pods}
- monitoring: ${monitoring_pods}
- kube-system: ${kube_system_pods}

Karpenter:
- NodeClaims: ${nodeclaim_total}
- NodePool: ${nodepool_status:-unavailable}
- EC2NodeClass: ${ec2nodeclass_status:-unavailable}

GitOps:
- ArgoCD pposiraegi: ${argocd_status}
- HPA: ${hpa_status}

Tracing:
- Tempo: ${tempo_status}
- OpenTelemetry Collector: ${otel_status}
EOF
)"

echo "${message}"

if [[ "${NOTIFY_ENABLED}" == true ]]; then
  "${NOTIFY}" "${message}"
fi
