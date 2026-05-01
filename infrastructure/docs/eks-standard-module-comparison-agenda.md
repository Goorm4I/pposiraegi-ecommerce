# EKS Standard Module Comparison Agenda

이 문서는 현재 수제 EKS 구성이 어느 정도 안정화된 뒤, 표준 Terraform 모듈 기반 구성과 비교하기 위한 검증 아젠다다.

목적은 “모듈이 무조건 낫다”를 증명하는 것이 아니라, 우리가 직접 겪은 실패 지점을 기준으로 표준 모듈이 어떤 의존성을 흡수하고, 어떤 영역은 여전히 직접 설계해야 하는지 구분하는 것이다.

## 검증 시작 시점

다음 조건을 대략 70% 이상 만족하면 비교 실험을 시작한다.

```text
Terraform apply -> EKS/RDS/Redis/ECR/IAM 생성 성공
bootstrap-platform.sh -> platform components 설치 성공
ArgoCD sync -> production app 배포 성공
ESO -> app-secret 생성 성공
Karpenter -> NodeClaim 생성 및 노드 join 성공
Monitoring -> PVC Bound, Prometheus/Grafana/Loki Running
LBC/TGB 또는 Ingress -> 외부 경로 target healthy
Actuator probe -> readiness/liveness 안정화
Istio AuthPolicy -> metrics/health/internal 경계 검증
destroy/apply residue -> 반복 패턴 파악
```

## 핵심 질문

```text
표준 모듈을 썼다면 더 빨랐을까?
표준 모듈을 썼다면 덜 깨졌을까?
표준 모듈을 써도 직접 설계해야 하는 영역은 무엇인가?
수제 구성으로 얻은 학습/설명력은 무엇인가?
운영 전환 시 어떤 영역을 모듈로 넘기고 어떤 영역을 유지할 것인가?
```

## 비교 후보

### VPC

후보:

```text
terraform-aws-modules/vpc/aws
```

비교 지점:

```text
public/private subnet
NAT gateway
route table
subnet discovery tag
cluster tag
public/internal ELB tag
```

### EKS

후보:

```text
terraform-aws-modules/eks/aws
```

비교 지점:

```text
cluster
managed node group
cluster security group
node security group
OIDC provider
EKS access entries
EKS addons
aws-auth/access API migration
```

### Karpenter

후보:

```text
terraform-aws-modules/eks/aws karpenter support
Karpenter official Terraform pattern
```

비교 지점:

```text
controller IAM role
node IAM role
instance profile
SQS interruption queue
EventBridge rule
security group discovery tag
subnet discovery tag
NodePool/EC2NodeClass boundary
```

### Addons

후보:

```text
terraform-aws-eks-blueprints-addons
Helm bootstrap 유지
```

비교 지점:

```text
EBS CSI
AWS Load Balancer Controller
External Secrets Operator
metrics-server
kube-prometheus-stack
Istio
```

주의:

```text
모든 addon을 Terraform Helm provider로 넣는 것은 목표가 아니다.
Terraform state가 과도하게 커지고 Kubernetes runtime 상태와 꼬일 수 있다.
bootstrap/ArgoCD 경계를 유지한 채 어떤 부분만 모듈화할지 본다.
```

## 우리 수제 구성에서 터진 문제와 비교 관점

| 문제 | 수제 구성에서 드러난 원인 | 모듈이 흡수할 수 있는가 | 검증 기준 |
| --- | --- | --- | --- |
| EKS access entry import | AWS에는 있는데 Terraform state에 없음 | 일부 가능 | `access_entries`로 팀원 권한이 일관 관리되는가 |
| tfstate checksum mismatch | backend/DynamoDB 상태 불일치 | 직접 관련 없음 | backend 표준화/문서화가 더 중요한가 |
| Karpenter node join 실패 | node role, SG, discovery tag, IAM 경계 | 상당 부분 가능 | NodeClaim 생성 후 join까지 수동 보정이 줄어드는가 |
| monitoring PVC Pending | EBS CSI addon/IRSA/storageclass 의존성 | 일부 가능 | addon과 gp3 StorageClass 준비가 자동화되는가 |
| app-secret not found | ESO controller만 있고 ExternalSecret 없음 | 직접 설계 필요 | SecretStore/ExternalSecret은 서비스 계약으로 남는가 |
| `/actuator/health` probe 실패 | 앱 health와 운영 probe 계약 불일치 | 불가능 | 앱/Deployment 설계 영역으로 분리되는가 |
| `/actuator/prometheus` 보호 | Istio AuthPolicy와 scrape 경로 불일치 가능 | 불가능 | 보안 정책은 직접 설계 영역인가 |
| ALB/TGB 경계 | Terraform ALB와 Kubernetes endpoint 연결 필요 | 일부 가능 | LBC Ingress로 갈지 TGB로 갈지 소유권이 명확해지는가 |
| destroy/apply residue | controller-created AWS resource가 state 밖에 남음 | 일부만 가능 | 모듈을 써도 cleanup/runbook은 필요한가 |

## 비교 산출물

검증이 끝나면 다음 표를 완성한다.

| 영역 | 현재 수제 구성 | 표준 모듈 구성 | 모듈 우세 | 수제/커스텀 유지 | 결론 |
| --- | --- | --- | --- | --- | --- |
| VPC/Subnet |  |  |  |  |  |
| EKS Cluster |  |  |  |  |  |
| Access Entry/RBAC |  |  |  |  |  |
| OIDC/IRSA |  |  |  |  |  |
| EBS CSI |  |  |  |  |  |
| Karpenter Controller |  |  |  |  |  |
| NodePool/EC2NodeClass |  |  |  |  |  |
| LBC/Ingress/TGB |  |  |  |  |  |
| ESO/ExternalSecret |  |  |  |  |  |
| Monitoring |  |  |  |  |  |
| Istio/AuthPolicy |  |  |  |  |  |
| Probe/Actuator |  |  |  |  |  |
| Destroy/Apply Residue |  |  |  |  |  |

## 실험 방식

본 코드에 바로 반영하지 않는다.

```text
branch: experiment/eks-module-baseline
directory: infrastructure/experiments/eks-module-baseline
```

최소 실험 범위:

```text
VPC
EKS
Managed Node Group
EBS CSI addon
Karpenter enablement
Access entries
```

제외:

```text
Istio
Monitoring full stack
ArgoCD app sync
ESO app-secret
Service Deployment
```

이유:

```text
처음부터 모든 것을 모듈 실험에 넣으면 다시 의존성 지옥이 된다.
먼저 우리가 가장 많이 고생한 EKS/VPC/IAM/Karpenter 기본 배관만 비교한다.
```

## 판단 기준

### 모듈로 넘길 가능성이 높은 영역

```text
VPC/subnet/route table
EKS cluster
OIDC provider
managed node group
EKS access entries
EKS addon 기본 구성
Karpenter controller/node IAM 기본 배관
subnet/security group discovery tag
```

### 직접 유지해야 할 가능성이 높은 영역

```text
NodePool capacity/disruption 정책
EC2NodeClass 세부 선택
ALB vs LBC vs TGB 전략
ExternalSecret key mapping
Istio AuthorizationPolicy
management port/probe/actuator 계약
Prometheus scrape 정책
Grafana dashboard/alert
destroy/apply residue runbook
```

## 예상 결론 초안

```text
표준 모듈은 EKS/VPC/IAM/OIDC/Karpenter 기본 배관의 반복 실수를 크게 줄인다.
하지만 서비스 운영 정책, 보안 경계, probe, secret mapping, monitoring 의미 설계는 모듈이 대신해주지 않는다.

따라서 운영 전환 방향은 표준 모듈 + 얇은 커스텀이다.
수제 구성은 운영 산출물로는 위험하지만, 표준 모듈이 어떤 문제를 흡수하는지 이해하게 만든 학습 산출물로 가치가 있다.
```

## 보류 조건

다음 문제가 아직 불안정하면 모듈 비교를 시작하지 않는다.

```text
ArgoCD app Degraded 원인이 남아 있음
app-secret/ESO 경계 미완성
probe/readiness/liveness 불안정
Karpenter node join이 재현성 있게 성공하지 않음
monitoring PVC/Pod가 자주 Pending
외부 트래픽 경로가 target healthy까지 가지 않음
destroy/apply residue 패턴이 아직 파악되지 않음
```

