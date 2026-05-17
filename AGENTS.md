# pposiraegi-ecommerce — AI Agent 컨텍스트

## 프로젝트 개요
- AWS EKS 기반 타임딜 이커머스 MSA
- 서비스: api-gateway, user-service, product-service, order-service
- 언어/프레임워크: Java 21 / Spring Boot 4.x (WebFlux: api-gateway, WebMVC: 나머지)
- 인프라: EKS + Karpenter + Istio Ambient + ArgoCD
- 현재 작업 브랜치: `feat/phase3-ops-hardening`

---

## 팀 역할 — 영역 침범 금지

| 영역 | 담당 | 수정 가능 여부 |
|------|------|----------------|
| `infrastructure/terraform/` | 나형님 | 절대 수정 금지 |
| `infrastructure/kubernetes/` | 나 (본인) | 수정 가능 |
| `.github/workflows/` | 나 (본인) | 수정 가능 |
| `backend/src/` | 팀 공유 | 신중하게 |
| `backend/*/build.gradle` | 나 (본인) | 수정 가능 |

---

## 확정된 아키텍처 결정 — 변경 제안 금지

아래 결정들은 팀 합의 또는 비용/기술 분석 후 확정된 것들이다.
이 결정을 번복하는 제안은 하지 말 것.

### Karpenter 인스턴스 전략
- `.large` 계열만 허용: m5.large, m5a.large, m6i.large, m6a.large, c5.large, c6i.large
- `xlarge` 이상 금지 — 비용 최적화 합의 완료
- `t3.*` 금지 — burstable 제외
- Spot + On-Demand 혼합, consolidateAfter: 30s

### Istio Ambient 모드
- sidecar 없는 Ambient 모드 선택 (전통적 sidecar 모드 아님)
- ztunnel: L4 mTLS DaemonSet
- Waypoint: L7 정책용, `infrastructure.replicas: 2` + PDB(minAvailable:1)
- **Waypoint에 HPA를 붙이지 말 것** — Gateway 관리 Deployment와 충돌 위험

### ArgoCD
- `argocd-app.yaml`의 `targetRevision: feat/eks-migration` 유지
- 나형님이 해당 브랜치 기준으로 테스트 중 — 절대 변경 금지

### PDB (PodDisruptionBudget)
- 모든 서비스: `minAvailable: 1`
- Waypoint: `minAvailable: 1` (waypoint-pdb)

### 모니터링
- Prometheus scrape: `/actuator/prometheus` (management 포트 8081)
- 앱 트래픽 8080 / actuator 8081 분리 — probe, scrape, SecurityFilter 충돌을 줄이기 위한 현재 구현
- Istio AuthorizationPolicy는 `/actuator/prometheus` 접근을 Prometheus SA 중심으로 제한
- Tempo tracing: OpenTelemetry Collector(`opentelemetry-collector.monitoring.svc:4318`) 경유, Tempo single binary 저장

---

## AI에게 요청하는 작업 방식

### 코드 리뷰 모드 (기본값)
파일을 수정하지 말고, 아래 형식으로 발견 사항만 출력할 것:

```
| 파일 | 라인 | 카테고리 | 내용 | 심각도(high/mid/low) |
```

카테고리:
- `yaml-error`: YAML 문법/논리 오류
- `security`: 하드코딩 secret, 과도한 권한
- `missing`: 청사진 대비 누락 항목
- `best-practice`: k8s/Istio/Spring 권장 패턴 위반
- `dependency`: 서비스 간 의존성 누락/불일치

### 수정 모드
사용자가 명시적으로 "수정해줘"라고 요청할 때만 파일을 변경할 것.
수정 전 반드시 변경 계획을 먼저 설명하고 확인받을 것.

---

## 현재 구현 상태 (feat/phase3-ops-hardening 기준)

### 완료
- PDB: api-gateway, user-service, product-service, order-service
- IRSA ServiceAccount: 서비스 4개 (role ARN은 `${AWS_ACCOUNT_ID}` 변수화)
- Deployment: serviceAccountName 연결, prometheus scrape annotation 추가
- Deployment: management port 8081 기반 readiness/liveness/prometheus scrape
- Karpenter: NodePool + EC2NodeClass (인스턴스 .large만, AZ a+c)
- Monitoring: kube-prometheus-stack + Loki Helm values
- Tracing: Tempo + OpenTelemetry Collector Helm values, Spring Boot OpenTelemetry starter
- Istio Ambient: istiod/cni/ztunnel values, waypoint(replicas:2+PDB), authorization-policy
- CI: deploy-all.yml ECS 잔재 제거, kubectl rollout restart로 교체
- Gradle: micrometer-registry-prometheus 추가 (common-web-mvc, api-gateway)

### 미완료 (다음 작업 예정)
- Argo Rollouts 카나리 매니페스트 (Istio 설치 후 작업)
- External Secrets Operator 매니페스트
- AWS Load Balancer Controller + Ingress
- README Ansible 항목 제거
- EKS 1.35 버전업 (나형님 Terraform)
- Istio Ambient 실제 설치 (EKS 1.35 이후)

---

## 주요 파일 위치

```
infrastructure/
  kubernetes/
    base/           # namespace, configmap
    karpenter/      # NodePool, EC2NodeClass
    services/       # 서비스별 deployment/service/hpa/pdb/serviceaccount
    monitoring/     # kube-prometheus-stack, loki helm values
    istio/          # istiod/cni/ztunnel values, waypoint, authorization-policy
  argocd-app.yaml

backend/
  common/common-web-mvc/build.gradle  # 공통 의존성 (actuator, micrometer)
  api-gateway/build.gradle            # WebFlux 전용 의존성

.github/workflows/deploy-all.yml      # CI/CD
```
