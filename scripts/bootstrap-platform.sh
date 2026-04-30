#!/usr/bin/env bash
# bootstrap-platform.sh
# pposiraegi EKS 플랫폼 컴포넌트 일괄 설치
# 순서: metrics-server → argocd → karpenter → lbc → istio → monitoring → eso → argocd-sync
#
# 사전 요건:
#   - kubectl, helm, aws CLI 설치 및 kubeconfig 설정 완료
#   - EKS 클러스터 pposiraegi-cluster 가동 중
#   - AWS_PROFILE 또는 aws configure로 goorm 계정(779846782353) 인증
#
# 사용법:
#   ./scripts/bootstrap-platform.sh                       # 전체 실행
#   ./scripts/bootstrap-platform.sh --skip-argocd-sync   # argocd-sync 제외
#   ./scripts/bootstrap-platform.sh --from lbc            # lbc 단계부터 재개
#   ./scripts/bootstrap-platform.sh --from=lbc            # 동일 (= 형태도 지원)
#   ./scripts/bootstrap-platform.sh --only monitoring     # monitoring만 실행
#   ./scripts/bootstrap-platform.sh --only=monitoring     # 동일
#
# 단계 이름: metrics-server | argocd | karpenter | lbc | istio | storage | monitoring | eso | argocd-sync

set -euo pipefail

###############################################################
# 설정값
###############################################################
CLUSTER_NAME="pposiraegi-cluster"
AWS_REGION="ap-northeast-2"
# 환경변수 우선, 없으면 STS로 동적 조회 (계정 ID 하드코딩 방지)
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K8S_DIR="${REPO_ROOT}/infrastructure/kubernetes"

# Helm 차트 버전 고정 (재현성)
METRICS_SERVER_VERSION="3.12.1"
ARGOCD_VERSION="7.7.3"
KARPENTER_VERSION="1.2.1"
LBC_VERSION="1.11.0"
ISTIO_VERSION="1.25.1"
PROM_STACK_VERSION="67.9.0"
LOKI_VERSION="6.29.0"
ESO_VERSION="0.14.0"

# IRSA ARN — 계정 ID를 동적으로 참조
KARPENTER_ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/pposiraegi-karpenter-controller"
LBC_ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/pposiraegi-aws-load-balancer-controller"
ESO_ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/pposiraegi-external-secrets-operator"
LOKI_ROLE_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:role/pposiraegi-loki-role"

###############################################################
# 공통 유틸
###############################################################
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

log()  { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
die()  { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

wait_rollout() {
  local ns="$1" deploy="$2"
  log "rollout 대기: ${ns}/${deploy}"
  kubectl rollout status deployment/"${deploy}" -n "${ns}" --timeout=300s
}

wait_daemonset() {
  local ns="$1" ds="$2"
  log "DaemonSet 대기: ${ns}/${ds}"
  kubectl rollout status daemonset/"${ds}" -n "${ns}" --timeout=300s
}

wait_crd() {
  local crd="$1"
  log "CRD 대기: ${crd}"
  for i in $(seq 1 30); do
    kubectl get crd "${crd}" &>/dev/null && return 0
    sleep 5
  done
  die "CRD ${crd} 타임아웃"
}

helm_repo_add() {
  local name="$1" url="$2"
  helm repo list 2>/dev/null | grep -q "^${name}\s" || helm repo add "${name}" "${url}"
}

###############################################################
# 0. 사전 검증
###############################################################
preflight() {
  log "=== 0. 사전 검증 ==="

  for cmd in kubectl helm aws; do
    command -v "${cmd}" &>/dev/null || die "${cmd} 미설치"
  done

  local ctx
  ctx="$(kubectl config current-context)"
  log "현재 컨텍스트: ${ctx}"

  log "AWS 계정 ID: ${AWS_ACCOUNT_ID}"

  ok "사전 검증 완료"
}

###############################################################
# 1. metrics-server
###############################################################
install_metrics_server() {
  log "=== 1. metrics-server 설치 ==="
  helm_repo_add metrics-server https://kubernetes-sigs.github.io/metrics-server/
  helm repo update metrics-server

  helm upgrade --install metrics-server metrics-server/metrics-server \
    --namespace kube-system \
    --version "${METRICS_SERVER_VERSION}" \
    --set args="{--kubelet-insecure-tls}" \
    --wait --timeout 3m
    # EKS kubelet은 자체 서명 인증서를 사용하므로 TLS 검증 비활성화 필요
    # 외부 노출 없이 클러스터 내부 통신만 하므로 실습 환경에서 허용

  ok "metrics-server 완료"
}

###############################################################
# 2. ArgoCD
###############################################################
install_argocd() {
  log "=== 2. ArgoCD 설치 ==="
  helm_repo_add argo https://argoproj.github.io/argo-helm
  helm repo update argo

  kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

  helm upgrade --install argocd argo/argo-cd \
    --namespace argocd \
    --version "${ARGOCD_VERSION}" \
    --set server.service.type=ClusterIP \
    --set applicationSet.enabled=false \
    --set configs.params."server\.insecure"=true \
    --wait --timeout 5m
    # server.insecure=true: TLS 종료를 ALB/Ingress에 위임하는 구조 (내부 ClusterIP 전용)
    # 외부 직접 노출 시 반드시 false로 변경

  wait_rollout argocd argocd-server
  ok "ArgoCD 완료"

  # 비밀번호를 로그에 직접 출력하지 않음 — CI/CD 로그 노출 방지
  warn "ArgoCD 초기 비밀번호 확인 명령:"
  warn "  kubectl -n argocd get secret argocd-initial-admin-secret \\"
  warn "    -o jsonpath='{.data.password}' | base64 -d && echo"
}

###############################################################
# 3. Karpenter
###############################################################
install_karpenter() {
  log "=== 3. Karpenter 설치 ==="
  # OCI 레지스트리는 helm repo add/update 불필요

  kubectl create namespace karpenter --dry-run=client -o yaml | kubectl apply -f -

  local queue_name="pposiraegi-karpenter-interruption"

  helm upgrade --install karpenter oci://public.ecr.aws/karpenter/karpenter \
    --namespace karpenter \
    --version "${KARPENTER_VERSION}" \
    --set "settings.clusterName=${CLUSTER_NAME}" \
    --set "settings.interruptionQueue=${queue_name}" \
    --set "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=${KARPENTER_ROLE_ARN}" \
    --set controller.resources.requests.cpu=100m \
    --set controller.resources.requests.memory=256Mi \
    --set controller.resources.limits.cpu=1 \
    --set controller.resources.limits.memory=1Gi \
    --wait --timeout 5m

  wait_rollout karpenter karpenter

  log "EC2NodeClass / NodePool 적용"
  kubectl apply -f "${K8S_DIR}/karpenter/ec2nodeclass.yaml"
  kubectl apply -f "${K8S_DIR}/karpenter/nodepool.yaml"

  ok "Karpenter 완료"
}

###############################################################
# 4. AWS Load Balancer Controller
###############################################################
install_lbc() {
  log "=== 4. AWS Load Balancer Controller 설치 ==="
  helm_repo_add eks https://aws.github.io/eks-charts
  helm repo update eks

  local vpc_id
  vpc_id="$(aws eks describe-cluster --name "${CLUSTER_NAME}" --region "${AWS_REGION}" \
    --query 'cluster.resourcesVpcConfig.vpcId' --output text)"

  helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
    --namespace kube-system \
    --version "${LBC_VERSION}" \
    --set clusterName="${CLUSTER_NAME}" \
    --set region="${AWS_REGION}" \
    --set vpcId="${vpc_id}" \
    --set "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=${LBC_ROLE_ARN}" \
    --set replicaCount=2 \
    --wait --timeout 5m

  wait_rollout kube-system aws-load-balancer-controller
  ok "AWS LBC 완료"
}

###############################################################
# 5. Istio Ambient
###############################################################
install_istio() {
  log "=== 5. Istio Ambient 설치 ==="
  helm_repo_add istio https://istio-release.storage.googleapis.com/charts
  helm repo update istio

  kubectl create namespace istio-system --dry-run=client -o yaml | kubectl apply -f -

  log "  5-1. istio-base (CRD)"
  helm upgrade --install istio-base istio/base \
    --namespace istio-system \
    --version "${ISTIO_VERSION}" \
    --wait --timeout 3m

  wait_crd "virtualservices.networking.istio.io"

  log "  5-2. istiod"
  helm upgrade --install istiod istio/istiod \
    --namespace istio-system \
    --version "${ISTIO_VERSION}" \
    -f "${K8S_DIR}/istio/istiod-values.yaml" \
    --wait --timeout 5m

  wait_rollout istio-system istiod

  log "  5-3. istio-cni"
  helm upgrade --install istio-cni istio/cni \
    --namespace kube-system \
    --version "${ISTIO_VERSION}" \
    -f "${K8S_DIR}/istio/cni-values.yaml" \
    --wait --timeout 3m

  wait_daemonset kube-system istio-cni-node

  log "  5-4. ztunnel"
  helm upgrade --install ztunnel istio/ztunnel \
    --namespace istio-system \
    --version "${ISTIO_VERSION}" \
    -f "${K8S_DIR}/istio/ztunnel-values.yaml" \
    --wait --timeout 3m

  wait_daemonset istio-system ztunnel

  log "  5-5. Kubernetes Gateway API CRD"
  # Waypoint는 gateway.networking.k8s.io/v1 Gateway 리소스를 사용 — Istio보다 먼저 설치 필요
  kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml
  wait_crd "gateways.gateway.networking.k8s.io"

  log "  5-6. production 네임스페이스"
  kubectl apply -f "${K8S_DIR}/base/namespace.yaml"

  log "  5-7. Waypoint / AuthorizationPolicy"
  kubectl apply -f "${K8S_DIR}/istio/waypoint.yaml"
  kubectl apply -f "${K8S_DIR}/istio/authorization-policy.yaml"

  ok "Istio Ambient 완료"
}

###############################################################
# 6. Storage — gp3 StorageClass (EBS CSI Driver 전제)
###############################################################
install_storage() {
  log "=== 6. gp3 StorageClass 적용 ==="

  # EBS CSI 드라이버 준비 대기 (Terraform addon이 배포되기까지 최대 2분)
  log "  ebs-csi-controller 준비 대기"
  kubectl rollout status deployment/ebs-csi-controller -n kube-system --timeout=120s 2>/dev/null || \
    warn "ebs-csi-controller rollout 확인 불가 — 계속 진행"

  kubectl apply -f "${K8S_DIR}/base/storageclass-gp3.yaml"

  # 기존 gp2 default 해제 (gp3가 default가 되므로)
  kubectl patch storageclass gp2 \
    -p '{"metadata":{"annotations":{"storageclass.kubernetes.io/is-default-class":"false"}}}' \
    2>/dev/null || true

  ok "gp3 StorageClass 완료"
}

###############################################################
# 7. kube-prometheus-stack + Loki
###############################################################
install_monitoring() {
  log "=== 7. kube-prometheus-stack + Loki 설치 ==="
  helm_repo_add prometheus-community https://prometheus-community.github.io/helm-charts
  helm_repo_add grafana https://grafana.github.io/helm-charts
  helm repo update prometheus-community grafana

  kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

  log "  6-1. kube-prometheus-stack"
  helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --version "${PROM_STACK_VERSION}" \
    -f "${K8S_DIR}/monitoring/kube-prometheus-stack-values.yaml" \
    --wait --timeout 10m

  log "  6-2. Loki (S3 IRSA annotation 포함)"
  # loki-values.yaml의 ${AWS_ACCOUNT_ID} 플레이스홀더를 실제 값으로 치환
  local loki_values
  loki_values="$(sed "s|\${AWS_ACCOUNT_ID}|${AWS_ACCOUNT_ID}|g" \
    "${K8S_DIR}/monitoring/loki-values.yaml")"

  helm upgrade --install loki grafana/loki \
    --namespace monitoring \
    --version "${LOKI_VERSION}" \
    --values <(echo "${loki_values}") \
    --set "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=${LOKI_ROLE_ARN}" \
    --wait --timeout 10m

  ok "모니터링 스택 완료"
}

###############################################################
# 7. External Secrets Operator
###############################################################
install_eso() {
  log "=== 8. External Secrets Operator 설치 ==="
  helm_repo_add external-secrets https://charts.external-secrets.io
  helm repo update external-secrets

  kubectl create namespace external-secrets --dry-run=client -o yaml | kubectl apply -f -

  helm upgrade --install external-secrets external-secrets/external-secrets \
    --namespace external-secrets \
    --version "${ESO_VERSION}" \
    --set installCRDs=true \
    --set "serviceAccount.annotations.eks\.amazonaws\.com/role-arn=${ESO_ROLE_ARN}" \
    --wait --timeout 5m

  wait_crd "externalsecrets.external-secrets.io"
  wait_rollout external-secrets external-secrets

  ok "ESO 완료"
}

###############################################################
# 8. ArgoCD sync (pposiraegi Application 등록)
###############################################################
sync_argocd() {
  log "=== 9. ArgoCD Application sync ==="

  local current_branch
  current_branch="$(git -C "${REPO_ROOT}" rev-parse --abbrev-ref HEAD)"
  log "현재 브랜치: ${current_branch}"

  kubectl create namespace production --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || true
  kubectl apply -f "${REPO_ROOT}/infrastructure/argocd-app.yaml"

  log "ArgoCD sync 대기 (최대 3분)"
  if command -v argocd &>/dev/null; then
    argocd app wait pposiraegi --sync --health --timeout 180 \
      --server localhost:8080 --plaintext 2>/dev/null || \
      warn "argocd CLI wait 실패 — 대시보드에서 수동 확인"
  else
    warn "argocd CLI 미설치 — kubectl로 폴링"
    for i in $(seq 1 18); do
      local phase
      phase="$(kubectl get application pposiraegi -n argocd \
        -o jsonpath='{.status.sync.status}' 2>/dev/null || echo unknown)"
      [[ "${phase}" == "Synced" ]] && { ok "ArgoCD Synced"; return 0; }
      log "  대기 중... (${phase}) [${i}/18]"
      sleep 10
    done
    warn "3분 내 Synced 미확인 — ArgoCD UI 확인 필요"
  fi
}

###############################################################
# 완료 메시지
###############################################################
print_summary() {
  echo ""
  echo -e "${GREEN}============================================${NC}"
  echo -e "${GREEN}  pposiraegi 플랫폼 부트스트랩 완료${NC}"
  echo -e "${GREEN}============================================${NC}"
  echo ""
  echo "  클러스터 : ${CLUSTER_NAME}"
  echo "  리전     : ${AWS_REGION}"
  echo "  계정 ID  : ${AWS_ACCOUNT_ID}"
  if [[ -n "${ONLY_STEP}" ]]; then
    echo "  실행 모드 : --only ${ONLY_STEP}"
  elif [[ -n "${FROM_STEP}" ]]; then
    echo "  실행 모드 : --from ${FROM_STEP}"
  else
    echo "  실행 모드 : 전체"
  fi
  echo ""
  echo "  설치된 컴포넌트:"
  printf "    %-30s %s\n" "metrics-server"        "${METRICS_SERVER_VERSION}"
  printf "    %-30s %s\n" "ArgoCD"                "${ARGOCD_VERSION}"
  printf "    %-30s %s\n" "Karpenter"             "${KARPENTER_VERSION}"
  printf "    %-30s %s\n" "AWS LBC"               "${LBC_VERSION}"
  printf "    %-30s %s\n" "Istio Ambient"         "${ISTIO_VERSION}"
  printf "    %-30s %s\n" "kube-prometheus-stack" "${PROM_STACK_VERSION}"
  printf "    %-30s %s\n" "Loki"                  "${LOKI_VERSION}"
  printf "    %-30s %s\n" "ESO"                   "${ESO_VERSION}"
  echo ""
  echo "  다음 단계:"
  echo "    Grafana:  kubectl port-forward svc/kube-prometheus-stack-grafana 3000:80 -n monitoring"
  echo "    ArgoCD:   kubectl port-forward svc/argocd-server 8080:80 -n argocd"
  echo "    Karpenter: kubectl get nodepools,ec2nodeclasses"
  echo ""
}

###############################################################
# 메인
###############################################################
SKIP_ARGOCD_SYNC=false
FROM_STEP=""
ONLY_STEP=""

# --from lbc / --from=lbc 둘 다 지원
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-argocd-sync) SKIP_ARGOCD_SYNC=true; shift ;;
    --from=*)  FROM_STEP="${1#--from=}"; shift ;;
    --from)    [[ $# -ge 2 ]] || die "--from 뒤에 단계 이름 필요"; FROM_STEP="$2"; shift 2 ;;
    --only=*)  ONLY_STEP="${1#--only=}"; shift ;;
    --only)    [[ $# -ge 2 ]] || die "--only 뒤에 단계 이름 필요"; ONLY_STEP="$2"; shift 2 ;;
    *) die "알 수 없는 옵션: $1  (--from, --only, --skip-argocd-sync)" ;;
  esac
done

# 단계 이름 유효성 검증
STEPS=(metrics-server argocd karpenter lbc istio storage monitoring eso argocd-sync)

validate_step() {
  local name="$1" s
  for s in "${STEPS[@]}"; do [[ "${s}" == "${name}" ]] && return 0; done
  die "알 수 없는 단계: '${name}'  가능한 값: ${STEPS[*]}"
}
[[ -n "${FROM_STEP}" ]] && validate_step "${FROM_STEP}"
[[ -n "${ONLY_STEP}" ]] && validate_step "${ONLY_STEP}"

# 특정 단계를 실행해야 하는지 판단
should_run() {
  local target="$1"

  if [[ -n "${ONLY_STEP}" ]]; then
    [[ "${target}" == "${ONLY_STEP}" ]] && return 0 || return 1
  fi

  if [[ -n "${FROM_STEP}" ]]; then
    local from_idx=0 target_idx=0 i=0
    for s in "${STEPS[@]}"; do
      [[ "${s}" == "${FROM_STEP}" ]] && from_idx=$i
      [[ "${s}" == "${target}" ]]    && target_idx=$i
      ((i++))
    done
    [[ $target_idx -ge $from_idx ]] && return 0 || return 1
  fi

  return 0
}

preflight

should_run metrics-server && install_metrics_server
should_run argocd         && install_argocd
should_run karpenter      && install_karpenter
should_run lbc            && install_lbc
should_run istio          && install_istio
should_run storage        && install_storage
should_run monitoring     && install_monitoring
should_run eso            && install_eso
should_run argocd-sync    && [[ "${SKIP_ARGOCD_SYNC}" == false ]] && sync_argocd

print_summary
