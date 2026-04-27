# EKS 마이그레이션 참고 자료 (머지 X)

> 이 폴더는 **참고용 뼈대**입니다. main 인프라에 직접 적용하지 마세요.

## 무엇인가

@epicodix가 Phase 3 EKS 마이그레이션을 위해 미리 만들어둔 Terraform 모듈 + Kubernetes manifest 뼈대.

- main의 `infrastructure/`는 **ECS Fargate** 기반 (Lee-nahyung님 모듈화 작업)
- 이 뼈대는 **EKS** 기반 (Phase 3 목표 아키텍처)

`docs/` 하위에 둔 이유 — 이건 active code가 아니라 참고 자료임을 명시. 잘못 머지돼도 빌드·배포에 영향 0.

## 어떻게 쓰면 안 되는가

- 이 폴더 채로 main에 머지
- 이 폴더 안에서 `terraform apply` 실행
- `terraform.tfvars.example` 그대로 사용

## 어떻게 활용하면 좋은가

1. EKS 마이그레이션 시 **새 폴더**(예: `infrastructure-eks/`)에 골라서 복사
2. 모듈별 코드 참고 (vpc / eks / karpenter / irsa / alb / rds / elasticache / security)
3. `INSTALL.md` 단계 따라 부분 참고

## 활용 시 체크리스트

- [ ] `k8s/argocd/apps/root-app.yaml`의 `repoURL`을 실제 사용 레포로 변경
- [ ] `k8s/apps/*/serviceaccount.yaml`의 `ACCOUNT_ID` placeholder 치환
- [ ] `terraform.tfvars.example` → 실제 `terraform.tfvars` 작성 (Git 커밋 금지)
- [ ] `db_password`는 환경변수로 주입 (`TF_VAR_db_password=...`)
- [ ] main의 `buildspec-backend.yml`을 EKS 패턴으로 재작성 (build → ECR push까지만, deploy는 ArgoCD가 가져감)
- [ ] main의 CloudFront + S3 frontend 모듈 별도 통합
- [ ] feat/pg-verification의 `remote-state/` (S3+DynamoDB backend) 통합

## 의도적 제외

- **payment-service** — PG는 프론트엔드 위젯, 백엔드는 order-service 내부 PaymentClient로 검증만. 별도 서비스 불필요
- **CodePipeline / buildspec** — main에 살아있음, 통합 시 발췌
- **CloudFront / S3 frontend** — main에 살아있음, 통합 시 발췌
- **SSM Parameter Store** — EKS는 ESO + Secrets Manager로 대체
- **Remote backend 설정** — feat/pg-verification에 있음, 통합 시 발췌
