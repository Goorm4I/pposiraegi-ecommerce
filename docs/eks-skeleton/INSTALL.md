# Phase 3 EKS 배포 가이드

## 사전 준비
```bash
aws configure  # irixoen@gmail.com, ap-northeast-2
export TF_VAR_db_password="your-secure-password"
```

## 1단계: Terraform (인프라)
```bash
cd terraform/environments/dev
terraform init
terraform plan
terraform apply
```

주요 output 저장:
```bash
terraform output cluster_name         # pposiraegi-eks
terraform output karpenter_controller_role_arn
terraform output lbc_role_arn
terraform output alb_security_group_id
```

## 2단계: kubeconfig 설정
```bash
aws eks update-kubeconfig --region ap-northeast-2 --name pposiraegi-eks
kubectl get nodes
```

## 3단계: 플랫폼 도구 설치 (순서 중요)

### 3-1. Karpenter
```bash
helm install karpenter oci://public.ecr.aws/karpenter/karpenter \
  --version "1.0.0" \
  --namespace karpenter --create-namespace \
  --set settings.clusterName=pposiraegi-eks \
  --set settings.interruptionQueue=pposiraegi-eks-karpenter-interruption \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<KARPENTER_ROLE_ARN>

kubectl apply -f k8s/karpenter/ec2nodeclass.yaml
kubectl apply -f k8s/karpenter/nodepool.yaml
```

### 3-2. AWS Load Balancer Controller
```bash
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=pposiraegi-eks \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<LBC_ROLE_ARN>
```

### 3-3. Istio
```bash
istioctl install --set profile=default -y
kubectl label namespace default istio-injection=enabled
kubectl apply -f k8s/istio/peer-authentication.yaml
```

### 3-4. ArgoCD + Argo Rollouts
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
```

### 3-5. External Secrets Operator
```bash
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace \
  --set serviceAccount.annotations."eks\.amazonaws\.com/role-arn"=<ESO_ROLE_ARN>
```

### 3-6. Monitoring Stack
```bash
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace \
  -f k8s/monitoring/values/prometheus-values.yaml

helm install loki grafana/loki-stack \
  -n monitoring \
  -f k8s/monitoring/values/loki-values.yaml
```

## 4단계: 앱 배포 (App of Apps)
```bash
# serviceaccount.yaml의 ACCOUNT_ID를 실제 계정 ID로 교체
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
for svc in order product user; do
  sed -i "s/ACCOUNT_ID/$AWS_ACCOUNT_ID/g" k8s/apps/${svc}-service/serviceaccount.yaml
done

# ArgoCD App of Apps 등록
kubectl apply -f k8s/argocd/apps/root-app.yaml
```

## 확인
```bash
kubectl get nodes                           # Karpenter Spot 노드 확인
kubectl get rollout -n default              # Argo Rollouts 상태
kubectl get pods -n monitoring              # Prometheus/Grafana/Loki
kubectl get peerauthentication -A           # Istio mTLS
```
