#!/usr/bin/env bash
# check-residue.sh
# Read-only residue checker for repeated EKS destroy/apply practice.
#
# This script does not delete or mutate any AWS/Kubernetes resources.
# It only prints resources that commonly remain outside Terraform state
# when Kubernetes controllers create AWS resources.

set -euo pipefail

CLUSTER_NAME="${CLUSTER_NAME:-pposiraegi-cluster}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
AWS_PROFILE="${AWS_PROFILE:-goorm}"
PROJECT_NAME="${PROJECT_NAME:-pposiraegi}"

export AWS_PROFILE AWS_REGION

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }

have() {
  command -v "$1" >/dev/null 2>&1
}

aws_read() {
  local title="$1"
  shift
  echo ""
  log "${title}"
  if ! "$@"; then
    warn "${title}: 조회 실패 또는 권한/리소스 없음"
  fi
}

kubectl_read() {
  local title="$1"
  shift
  echo ""
  log "${title}"
  if ! "$@"; then
    warn "${title}: 조회 실패 또는 클러스터 미연결"
  fi
}

print_context() {
  log "Residue check context"
  echo "  AWS_PROFILE : ${AWS_PROFILE}"
  echo "  AWS_REGION  : ${AWS_REGION}"
  echo "  PROJECT_NAME: ${PROJECT_NAME}"
  echo "  CLUSTER_NAME: ${CLUSTER_NAME}"

  aws sts get-caller-identity \
    --query '{Account:Account,Arn:Arn}' \
    --output table
}

check_kubernetes_residue() {
  if ! have kubectl; then
    warn "kubectl 미설치: Kubernetes 잔여물 조회 스킵"
    return 0
  fi

  if ! kubectl config current-context >/dev/null 2>&1; then
    warn "kubectl context 없음: Kubernetes 잔여물 조회 스킵"
    return 0
  fi

  kubectl_read "kubectl context" kubectl config current-context
  kubectl_read "Nodes" kubectl get nodes -o wide
  kubectl_read "Karpenter NodeClaims" kubectl get nodeclaims
  kubectl_read "Ingresses" kubectl get ingress -A
  kubectl_read "TargetGroupBindings" kubectl get targetgroupbindings -A
  kubectl_read "PVCs" kubectl get pvc -A
  kubectl_read "Helm releases" helm list -A
}

check_karpenter_residue() {
  aws_read "Karpenter-created EC2 instances" \
    aws ec2 describe-instances \
      --region "${AWS_REGION}" \
      --filters \
        "Name=tag:karpenter.sh/nodepool,Values=*" \
        "Name=instance-state-name,Values=pending,running,stopping,stopped" \
      --query 'Reservations[].Instances[].{InstanceId:InstanceId,State:State.Name,Type:InstanceType,PrivateIp:PrivateIpAddress,NodePool:Tags[?Key==`karpenter.sh/nodepool`]|[0].Value,Name:Tags[?Key==`Name`]|[0].Value}' \
      --output table

  aws_read "Karpenter discovery security groups" \
    aws ec2 describe-security-groups \
      --region "${AWS_REGION}" \
      --filters "Name=tag:karpenter.sh/discovery,Values=${CLUSTER_NAME}" \
      --query 'SecurityGroups[].{GroupId:GroupId,GroupName:GroupName,VpcId:VpcId,Discovery:Tags[?Key==`karpenter.sh/discovery`]|[0].Value}' \
      --output table

  aws_read "Active Spot requests matching Karpenter/cluster instances" \
    aws ec2 describe-spot-instance-requests \
      --region "${AWS_REGION}" \
      --filters \
        "Name=state,Values=open,active" \
        "Name=tag:karpenter.sh/nodepool,Values=*" \
      --query 'SpotInstanceRequests[].{RequestId:SpotInstanceRequestId,State:State,Status:Status.Code,InstanceId:InstanceId,Type:Type,NodePool:Tags[?Key==`karpenter.sh/nodepool`]|[0].Value}' \
      --output table
}

check_lbc_residue() {
  aws_read "Load balancers matching project/cluster" \
    aws elbv2 describe-load-balancers \
      --region "${AWS_REGION}" \
      --query "LoadBalancers[?contains(LoadBalancerName, \`${PROJECT_NAME}\`) || contains(LoadBalancerName, \`k8s\`)].{Name:LoadBalancerName,Type:Type,Scheme:Scheme,DNS:DNSName,State:State.Code}" \
      --output table

  aws_read "Target groups matching project/k8s" \
    aws elbv2 describe-target-groups \
      --region "${AWS_REGION}" \
      --query "TargetGroups[?contains(TargetGroupName, \`${PROJECT_NAME}\`) || contains(TargetGroupName, \`k8s\`)].{Name:TargetGroupName,Protocol:Protocol,Port:Port,TargetType:TargetType,Arn:TargetGroupArn}" \
      --output table
}

check_storage_residue() {
  aws_read "EBS volumes created for Kubernetes PVCs" \
    aws ec2 describe-volumes \
      --region "${AWS_REGION}" \
      --filters "Name=tag-key,Values=kubernetes.io/created-for/pvc/name" \
      --query 'Volumes[].{VolumeId:VolumeId,State:State,Size:Size,Az:AvailabilityZone,PVC:Tags[?Key==`kubernetes.io/created-for/pvc/name`]|[0].Value,Namespace:Tags[?Key==`kubernetes.io/created-for/pvc/namespace`]|[0].Value}' \
      --output table
}

check_eks_access_residue() {
  aws_read "EKS access entries" \
    aws eks list-access-entries \
      --cluster-name "${CLUSTER_NAME}" \
      --region "${AWS_REGION}" \
      --output table
}

check_logs_and_buckets() {
  aws_read "CloudWatch log groups matching project" \
    aws logs describe-log-groups \
      --region "${AWS_REGION}" \
      --log-group-name-prefix "/eks/${PROJECT_NAME}" \
      --query 'logGroups[].{Name:logGroupName,Retention:retentionInDays,StoredBytes:storedBytes}' \
      --output table

  aws_read "S3 buckets matching project" \
    aws s3api list-buckets \
      --query "Buckets[?contains(Name, \`${PROJECT_NAME}\`)].{Name:Name,CreationDate:CreationDate}" \
      --output table
}

main() {
  if ! have aws; then
    err "aws CLI 미설치"
    exit 1
  fi

  print_context
  check_kubernetes_residue
  check_karpenter_residue
  check_lbc_residue
  check_storage_residue
  check_eks_access_residue
  check_logs_and_buckets

  echo ""
  ok "Residue check completed. This script did not delete anything."
  echo "  반복해서 남는 항목은 infrastructure/docs/destroy-apply-residue-runbook.md에 기록하세요."
}

main "$@"
