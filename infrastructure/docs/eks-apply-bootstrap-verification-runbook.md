# EKS Apply / Bootstrap / Runtime Verification Runbook

이 문서는 Phase 3 EKS 환경을 `terraform apply` 이후 실제로 동작 가능한 상태까지 검증하기 위한 런북이다.

목표는 단순히 명령어를 나열하는 것이 아니라, 각 단계에서 **어떤 계약이 성립해야 다음 단계로 넘어갈 수 있는지**를 확인하는 것이다.

## 핵심 흐름

```text
Terraform apply
  -> kubeconfig / RBAC 확인
  -> platform bootstrap
  -> Karpenter / Storage / Monitoring 확인
  -> ArgoCD sync
  -> app runtime 확인
  -> Prometheus / Istio / LBC 확인
```

짧게 말하면:

```text
Terraform = AWS 기반 계약 생성
bootstrap = 플랫폼 controller 설치
ArgoCD = 앱/쿠버네티스 리소스 지속 적용
runtime verification = 실제 계약이 지켜지는지 확인
```

## 0. 실행 전 기준

### 알아야 하는 전제

- Terraform은 VPC, EKS, IAM, IRSA, EBS CSI, LBC/ESO/Loki IAM Role 같은 AWS 기반 리소스를 만든다.
- bootstrap script는 ArgoCD, Karpenter controller, AWS LBC, Istio Ambient, monitoring stack, ESO 같은 플랫폼 컴포넌트를 Helm으로 설치한다.
- ArgoCD는 `infrastructure/kubernetes` 아래의 앱 리소스와 일부 CR을 지속 관리한다.
- 앱이 정상적으로 떠도 Prometheus, Istio, LBC 검증은 별개다.

### 로컬 전제

```bash
cd infrastructure
export AWS_PROFILE=goorm
export AWS_REGION=ap-northeast-2
```

확인:

```bash
aws sts get-caller-identity
terraform version
kubectl version --client
helm version
```

성공 기준:

- AWS 계정 ID가 팀 계정으로 나온다.
- `terraform`, `kubectl`, `helm`이 로컬에서 실행된다.

## 1. Terraform Plan / Apply

### 목적

AWS 쪽 기반 계약을 만든다.

확인하는 것:

- EKS cluster
- managed node group
- VPC / subnet / security group
- IAM roles
- IRSA roles
- EBS CSI addon
- Karpenter interruption queue
- Loki S3 bucket
- ALB / target group

### 명령

```bash
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

### 공부 포인트

Terraform 단계는 Kubernetes YAML이 아니라 **AWS API로 존재해야 하는 기반 리소스**를 만드는 단계다.

예를 들어 Karpenter YAML이 맞아도 다음이 없으면 노드는 Ready가 되지 않는다.

- Karpenter controller IAM Role
- node role / instance profile
- subnet discovery tag
- security group discovery tag
- interruption queue 권한

### 성공 기준

```bash
terraform output
aws eks describe-cluster \
  --name pposiraegi-cluster \
  --region ap-northeast-2 \
  --query 'cluster.status'
```

기대:

```text
"ACTIVE"
```

## 2. kubeconfig / RBAC 확인

### 목적

Terraform이 만든 EKS에 현재 사용자가 접근 가능한지 확인한다.

### 명령

```bash
aws eks update-kubeconfig \
  --name pposiraegi-cluster \
  --region ap-northeast-2

kubectl config current-context
kubectl get nodes
kubectl auth can-i get pods -A
kubectl auth can-i apply -f infrastructure/kubernetes/base/namespace.yaml
```

### 공부 포인트

여기서 막히면 Kubernetes 문제가 아니라 **EKS access entry / RBAC / kubeconfig 문제**다.

급하면 CLI로 직접 권한을 넣을 수 있지만, 팀 재현성을 위해서는 Terraform에서 관리하는 것이 원칙이다.

### 성공 기준

- `kubectl get nodes`가 노드를 보여준다.
- `kubectl auth can-i get pods -A`가 `yes`를 반환한다.

## 3. Bootstrap 실행

### 목적

클러스터 위에 플랫폼 controller를 설치한다.

설치 순서:

```text
metrics-server
-> ArgoCD
-> Karpenter
-> AWS LBC
-> Istio Ambient
-> gp3 StorageClass
-> kube-prometheus-stack + Loki
-> ESO
-> ArgoCD sync
```

### 명령

처음 전체 실행:

```bash
cd ..
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --skip-argocd-sync
```

중간 실패 후 재개:

```bash
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --from karpenter --skip-argocd-sync
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --from monitoring --skip-argocd-sync
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --only monitoring --skip-argocd-sync
```

### 공부 포인트

bootstrap은 앱 배포가 아니라 **플랫폼 controller 설치**다.

여기서 중요한 구분:

- controller 설치 성공
- CRD 생성 성공
- CR 적용 성공
- controller가 실제 AWS API 호출 성공

이 네 개는 서로 다르다.

예를 들어 Karpenter controller가 Running이어도 IAM 권한이 부족하면 NodeClaim은 실패할 수 있다.

## 4. Karpenter 검증

### 목적

Pod Pending 상황에서 Karpenter가 실제 노드를 만들 수 있는지 확인한다.

### 명령

```bash
kubectl get pods -n karpenter
kubectl get nodepool
kubectl get ec2nodeclass
kubectl get nodeclaims
kubectl get nodes -o wide
```

문제 발생 시:

```bash
kubectl logs -n karpenter deployment/karpenter
kubectl describe nodeclaim <NODECLAIM_NAME>
kubectl describe node <NODE_NAME>
```

AWS 쪽 확인:

```bash
aws ec2 describe-instances \
  --region ap-northeast-2 \
  --filters "Name=tag:karpenter.sh/nodepool,Values=default" \
  --query 'Reservations[].Instances[].{State:State.Name,Type:InstanceType,PrivateIp:PrivateIpAddress,Subnet:SubnetId}'
```

### 공부 포인트

Karpenter는 단순히 EC2를 띄우는 도구가 아니다.

다음 경로가 모두 맞아야 한다.

```text
Pending Pod
  -> NodePool match
  -> EC2NodeClass discovery
  -> controller IRSA
  -> EC2 instance / instance profile
  -> subnet / security group
  -> kubelet bootstrap
  -> Node Ready
  -> Pod scheduled
```

### 성공 기준

- `karpenter` pod Running
- NodePool / EC2NodeClass Ready
- NodeClaim Ready
- 새 노드가 `Ready`

## 5. Storage / EBS CSI / gp3 확인

### 목적

Monitoring stack이 PVC를 만들 수 있는지 확인한다.

### 명령

```bash
kubectl get pods -n kube-system | grep ebs
kubectl get storageclass
kubectl get pvc -A
```

문제 발생 시:

```bash
kubectl describe pvc -n monitoring <PVC_NAME>
kubectl logs -n kube-system deployment/ebs-csi-controller
```

### 공부 포인트

Prometheus/Grafana/AlertManager는 단순 stateless Pod가 아니다.

PVC가 필요하므로 다음이 맞아야 한다.

- EBS CSI addon
- EBS CSI IRSA
- gp3 StorageClass
- AZ와 노드 placement
- 충분한 노드 용량

### 성공 기준

- `gp3` StorageClass 존재
- monitoring PVC가 `Bound`

## 6. Monitoring Stack 검증

### 목적

kube-prometheus-stack, Grafana, AlertManager, Loki가 설치되어 관측성 기반이 준비됐는지 확인한다.

### 명령

```bash
kubectl get pods -n monitoring
kubectl get svc -n monitoring
kubectl get pvc -n monitoring
helm list -n monitoring
```

Prometheus target 확인용 port-forward:

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090
```

Grafana 확인용 port-forward:

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80
```

Loki 확인:

```bash
kubectl logs -n monitoring -l app.kubernetes.io/name=loki --tail=100
kubectl logs -n monitoring -l app.kubernetes.io/name=promtail --tail=100
```

### 공부 포인트

Monitoring stack은 Prometheus 단품이 아니다.

```text
Prometheus = metrics 수집/저장
Grafana = metrics/logs 시각화
Loki = logs 저장
AlertManager = 알림 라우팅
PVC/EBS CSI = 상태 저장 기반
```

따라서 문제를 이렇게 나눠 봐야 한다.

- Pod/PVC Pending = storage/node 문제
- scrape 404 = 앱 actuator 설정 문제
- scrape 403 = Istio AuthorizationPolicy 문제
- Grafana 비어 있음 = datasource/dashboard 문제
- Loki 로그 없음 = promtail/label/S3/IRSA 문제

### 성공 기준

- monitoring namespace의 주요 Pod Running
- PVC Bound
- Prometheus target 화면에서 서비스 target 확인 가능
- Grafana datasource 정상

## 7. ArgoCD Sync / 앱 배포

### 목적

앱 Deployment, Service, HPA, PDB, ServiceAccount, TargetGroupBinding 같은 Kubernetes 리소스를 GitOps로 적용한다.

### 명령

```bash
cd /path/to/pposiraegi-ecommerce
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --only argocd-sync
```

상태 확인:

```bash
kubectl get application -n argocd
kubectl get pods -n production
kubectl get svc -n production
kubectl get endpoints -n production
kubectl get pdb -n production
kubectl get hpa -n production
```

### 공부 포인트

ArgoCD는 “앱을 띄우는 버튼”이 아니라 **Git에 선언된 상태를 클러스터에 맞추는 reconciler**다.

따라서 다음을 구분해야 한다.

- Git에 리소스가 있음
- ArgoCD가 sync함
- Kubernetes가 리소스를 받음
- Pod가 실제 Running
- Service endpoint가 생김
- 외부 트래픽이 들어옴

### 성공 기준

- ArgoCD Application `Synced`
- production Pod Running
- Service endpoint 존재

## 8. App Runtime / Actuator 확인

### 목적

앱이 Kubernetes probe와 Prometheus scrape 계약을 지키는지 확인한다.

### 명령

Pod 내부에서 확인:

```bash
kubectl exec -n production deploy/api-gateway -- \
  wget -qO- http://localhost:8080/actuator/health

kubectl exec -n production deploy/api-gateway -- \
  wget -qO- http://localhost:8080/actuator/prometheus | head
```

Service 경유 확인:

```bash
kubectl run curl-test -n production --rm -it --image=curlimages/curl -- \
  curl -i http://api-gateway:8080/actuator/health

kubectl run curl-test -n production --rm -it --image=curlimages/curl -- \
  curl -i http://api-gateway:8080/actuator/prometheus
```

### 공부 포인트

이번에 4개 서비스에 추가한 설정은 이 계약을 위한 것이다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

이 설정이 없으면 Prometheus annotation이 있어도 `/actuator/prometheus`가 404일 수 있다.

### 성공 기준

- `/actuator/health` 200
- `/actuator/prometheus` 200
- Prometheus target UP

## 9. Istio AuthorizationPolicy 확인

### 목적

민감 metrics endpoint는 Prometheus만 접근 가능하고, 일반 Pod는 접근 불가한지 확인한다.

### 명령

정책 확인:

```bash
kubectl get authorizationpolicy -n production
kubectl describe authorizationpolicy -n production deny-prometheus-non-prometheus
kubectl describe authorizationpolicy -n production allow-prometheus-scrape
```

일반 Pod 접근 테스트:

```bash
kubectl run curl-deny-test -n production --rm -it --image=curlimages/curl -- \
  curl -i http://api-gateway:8080/actuator/prometheus
```

Prometheus 접근은 Prometheus target 화면 또는 Prometheus pod에서 확인한다.

### 공부 포인트

Istio AuthorizationPolicy는 ALLOW 정책이 여러 개 있으면 합산된다.

그래서 `/actuator/prometheus`는 다음 구조가 필요하다.

```text
DENY: Prometheus SA가 아닌 호출자는 /actuator/prometheus 차단
ALLOW: Prometheus SA는 /actuator/prometheus 허용
ALLOW: health check는 별도로 허용
```

이 구분이 없으면 내부 production Pod가 metrics endpoint를 볼 수 있다.

### 성공 기준

- Prometheus scrape 성공
- 일반 production Pod에서 `/actuator/prometheus` 접근 실패
- `/actuator/health`는 정상

## 10. LBC / TargetGroupBinding / ALB 확인

### 목적

Terraform ALB Target Group이 Kubernetes Service endpoint와 연결되는지 확인한다.

### 명령

```bash
kubectl get targetgroupbinding -n production
kubectl describe targetgroupbinding api-gateway-tgb -n production
kubectl get svc api-gateway -n production
kubectl get endpoints api-gateway -n production
```

AWS target health:

```bash
aws elbv2 describe-target-groups \
  --region ap-northeast-2 \
  --names pposiraegi-tg

aws elbv2 describe-target-health \
  --region ap-northeast-2 \
  --target-group-arn <TARGET_GROUP_ARN>
```

### 공부 포인트

현재 전략은 LBC Ingress로 ALB를 새로 만들지 않고, Terraform이 만든 ALB/TG를 유지한다.

```text
Terraform = ALB / Target Group 소유
AWS LBC = TargetGroupBinding으로 Service endpoint 등록
ArgoCD = TargetGroupBinding manifest 적용
```

이 구조는 ALB가 두 개 생기는 혼란을 피하고, 나중에 Ingress가 필요할 때 점진 전환하기 위한 중간 단계다.

### 성공 기준

- TargetGroupBinding 생성
- api-gateway endpoint 존재
- ALB target health `healthy`
- ALB/CloudFront 경유 요청 성공

## 11. 실패했을 때 우선순위

### Terraform 단계에서 실패

Kubernetes를 보지 않는다.

확인:

```bash
terraform plan
terraform state list
aws eks describe-cluster --name pposiraegi-cluster --region ap-northeast-2
```

### bootstrap 단계에서 실패

해당 controller의 namespace와 Helm release를 본다.

```bash
helm list -A
kubectl get pods -A | grep -v Running
kubectl describe pod -n <NAMESPACE> <POD_NAME>
kubectl logs -n <NAMESPACE> <POD_NAME>
```

### 앱 단계에서 실패

Deployment, Pod, Service, Endpoint 순서로 본다.

```bash
kubectl get deploy,pod,svc,endpoints -n production
kubectl describe pod -n production <POD_NAME>
kubectl logs -n production <POD_NAME>
```

### 관측성 단계에서 실패

증상별로 나눈다.

```text
PVC Pending -> EBS CSI / gp3 / AZ / node
Pod Pending -> Karpenter / resource request / taint
scrape 404 -> actuator exposure
scrape 403 -> Istio AuthorizationPolicy
target down -> Service annotation / endpoint / port
Grafana empty -> datasource / dashboard
Loki empty -> promtail / label / S3 / IRSA
```

## 12. 학습용 해석 문장

EKS Phase 3의 복잡도는 도구가 많아서가 아니라, 도구마다 확인하는 경계가 다르기 때문에 생긴다.

```text
보안팀: 누구 ServiceAccount로 왔나요?
네트워크팀: 어느 Service로 들어오나요?
관측팀: /actuator/prometheus 열었나요?
Istio팀: 그 경로는 Prometheus만 접근 가능한가요?
AWS팀: S3/LB/EC2 API 권한은 IRSA로 받았나요?
스토리지팀: PVC는 gp3이고 AZ가 맞나요?
스케줄링팀: 그 Pod는 Spot에 가도 되나요?
운영팀: 죽으면 누가 알림 받나요?
```

이 체크가 귀찮아진 대신, 운영자는 각 경계를 명시적으로 통제할 수 있다.

따라서 이번 프로젝트의 핵심 학습 목표는 다음이다.

```text
도구 설치가 아니라,
서비스가 운영 환경에 들어올 때 필요한 계약을 정의하고 검증하는 것.
```

## 13. 검증 기록 템플릿

```markdown
## YYYY-MM-DD 검증 기록

### 실행한 명령
- terraform apply:
- bootstrap:
- ArgoCD sync:

### 성공한 계약
- [ ] EKS ACTIVE
- [ ] kubectl 접근 가능
- [ ] Karpenter NodeClaim Ready
- [ ] gp3 PVC Bound
- [ ] monitoring pods Running
- [ ] production pods Running
- [ ] /actuator/health 200
- [ ] /actuator/prometheus 200
- [ ] Prometheus target UP
- [ ] 일반 Pod metrics 접근 차단
- [ ] TGB target healthy

### 막힌 지점
- 증상:
- 발생 단계:
- 관련 레이어:
- 확인한 명령:
- 다음 조치:

### 배운 점
-
```
