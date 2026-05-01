# Destroy / Apply Residue Runbook

이 문서는 Phase 3 EKS 실습 중 `terraform destroy` / `terraform apply`를 자주 반복할 때 남을 수 있는 잔여 리소스를 확인하고 정리하기 위한 기준이다.

핵심 원칙:

```text
처음에는 삭제 자동화 금지
반복해서 남는 리소스만 check script에 추가
반복 3회 이상 확인된 패턴만 cleanup 자동화 후보로 승격
```

## 왜 residue가 생기나

Terraform이 만든 리소스와 Kubernetes controller가 만든 리소스의 소유자가 다르기 때문이다.

```text
Terraform
  -> VPC, EKS, IAM, ALB, RDS, Redis, S3, SSM

bootstrap / Helm
  -> ArgoCD, Karpenter, LBC, Istio, Monitoring, ESO

Kubernetes controller
  -> EC2 instance, ALB/TG, EBS volume, SG rule 같은 AWS 리소스

manual CLI / console
  -> EKS access entry, 임시 SG rule, 수동 IAM binding
```

`terraform destroy`는 Terraform state에 있는 리소스만 확실하게 지운다.
Controller가 AWS에 만든 부속 리소스는 삭제 타이밍이 늦거나 state 밖에 남을 수 있다.

## 현재 운영 방식

자주 destroy/apply 하는 검증 기간에는 다음 방식으로 운용한다.

```text
1. terraform destroy 전후로 residue check 실행
2. 남은 리소스가 있으면 문서에 기록
3. 같은 패턴이 2회 반복되면 check script에 조회 추가
4. 같은 패턴이 3회 반복되면 cleanup 자동화 후보로 승격
```

## 체크 스크립트

읽기 전용:

```bash
cd /path/to/pposiraegi-ecommerce
AWS_PROFILE=goorm ./scripts/check-residue.sh
```

환경변수로 기준 변경 가능:

```bash
AWS_PROFILE=goorm \
AWS_REGION=ap-northeast-2 \
CLUSTER_NAME=pposiraegi-cluster \
PROJECT_NAME=pposiraegi \
./scripts/check-residue.sh
```

이 스크립트는 삭제하지 않는다.

## destroy 전 확인

클러스터가 살아 있을 때 확인한다.

```bash
helm list -A
kubectl get nodeclaims
kubectl get nodes
kubectl get ingress -A
kubectl get targetgroupbindings -A
kubectl get pvc -A
```

공부 포인트:

- `nodeclaims`가 남아 있으면 Karpenter가 만든 EC2가 아직 연결되어 있을 수 있다.
- `ingress`가 남아 있으면 LBC가 만든 ALB/TG가 남을 수 있다.
- `pvc`가 남아 있으면 EBS volume이 남을 수 있다.

## destroy 후 확인

AWS 잔여물을 확인한다.

```bash
./scripts/check-residue.sh
```

중점 확인:

```text
Karpenter EC2
LBC ALB / TargetGroup
EBS volume
EKS access entry
CloudWatch log group
S3 bucket
```

## 리소스별 해석

### Karpenter EC2

조회 기준:

```text
tag: karpenter.sh/nodepool
```

의미:

```text
Karpenter controller가 Pod 수요를 보고 만든 EC2.
Terraform state에는 직접 들어가지 않는다.
```

반복 발생 시:

```text
destroy 전 NodeClaim 정리 대기
Karpenter controller 로그 확인
EC2 termination 수동 확인
```

### LBC ALB / TargetGroup

조회 기준:

```text
ALB/TG 이름에 pposiraegi 또는 k8s 포함
```

의미:

```text
Ingress를 사용한 경우 AWS Load Balancer Controller가 만든 ALB/TG일 수 있다.
현재 전략은 Terraform ALB + TargetGroupBinding이므로,
LBC Ingress ALB가 반복해서 생기면 경계가 꼬인 것이다.
```

반복 발생 시:

```text
Ingress 존재 여부 확인
TargetGroupBinding과 Ingress를 혼용했는지 확인
LBC finalizer가 남았는지 확인
```

### EBS Volume

조회 기준:

```text
tag: kubernetes.io/created-for/pvc/name
```

의미:

```text
PVC가 만든 EBS volume.
Prometheus/Grafana/AlertManager PVC에서 생길 수 있다.
```

반복 발생 시:

```text
PVC reclaimPolicy 확인
EBS CSI controller 상태 확인
destroy 전 monitoring uninstall 여부 검토
```

### EKS Access Entry

의미:

```text
EKS API 접근 권한.
Terraform state 밖에서 CLI/console로 만들면 already exists/import 문제가 난다.
```

운영 원칙:

```text
EKS access entry는 Terraform만 관리한다.
수동 생성했으면 바로 import하거나 삭제한다.
```

### CloudWatch Log Group

의미:

```text
로그 그룹은 비용은 작지만 반복 apply/destroy에서 자주 남을 수 있다.
```

운영 원칙:

```text
실습 중에는 잔여 여부만 확인.
보존이 필요한 로그인지 확인 후 cleanup 후보로 분리.
```

## CloudTrail로 생성 주체 확인

누가 만들었는지 모를 때 CloudTrail을 사용한다.

EC2 생성 추적:

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=RunInstances \
  --region ap-northeast-2
```

ALB 생성 추적:

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=CreateLoadBalancer \
  --region ap-northeast-2
```

EBS volume 생성 추적:

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=CreateVolume \
  --region ap-northeast-2
```

해석 기준:

```text
assumed-role/...karpenter-controller...              -> Karpenter
assumed-role/...aws-load-balancer-controller...      -> AWS LBC
assumed-role/...ebs-csi...                           -> EBS CSI
IAMUser 또는 Terraform 실행 주체                      -> Terraform/manual
```

## 기록 템플릿

```markdown
## YYYY-MM-DD residue check

### 실행 시점
- [ ] destroy 전
- [ ] destroy 후
- [ ] apply 전
- [ ] apply 후

### 남은 리소스
- 종류:
- ID:
- 태그:
- 생성 주체 추정:
- 비용 영향:

### 원인 추정
-

### 조치
- [ ] 관찰만
- [ ] 수동 삭제
- [ ] terraform import
- [ ] check script 추가
- [ ] cleanup 자동화 후보

### 다음 반복 때 확인할 것
-
```

## cleanup 자동화 승격 기준

```text
1회 발생: 문서 기록
2회 발생: check-residue.sh에 조회 추가
3회 발생: cleanup-residue.sh 후보
```

자동 삭제 스크립트는 반드시 다음 조건을 만족할 때만 만든다.

```text
기본 dry-run
삭제 대상 tag/cluster/name 명시
삭제 전 목록 출력
사용자가 --yes를 넘길 때만 삭제
Terraform state 리소스는 삭제하지 않음
```
