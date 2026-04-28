# pposiraegi-ecommerce-msa
뽀시래기 타임딜 MSA 프로젝트

# 🐾 Pposiraegi - Time-Deal E-Commerce Platform

> **IaaS Cloud Operations Automation** | Phase 3 Project  
> Team Po4i | 2026.04.20 ~ 2026.05.20

---

## 📖 Project Background

뽀시래기는 반려동물 용품을 대상으로 한 타임딜 이커머스 서비스입니다.  
3차 프로젝트에서는 기존 ECS Fargate 기반 MSA 아키텍처를 **EKS 기반 클라우드 네이티브 환경**으로 전환하고,  
IaC, GitOps, 서비스 메시, 모니터링 등 운영 자동화 전반을 구축하였습니다.

---

## 👥 Team

| 이름 | 역할 | 담당 업무 |
|------|------|-----------|
| 이나형 | 팀장 / AWS 인프라 | Terraform 모듈화, EKS 구축, 모니터링, 백업 |
| 박지훈 | 인프라 / 프론트엔드 | GitHub Actions CI, Argo CD CD, Kubernetes 매니페스트, Ansible |
| 서주원 | Git 관리 / 백엔드 | 브랜치 전략, 백엔드 EKS 이전, Dockerfile 최적화 |

---

## 🏗️ AWS Architecture

<img width="7124" height="4724" alt="image" src="https://github.com/user-attachments/assets/4acfa654-8237-427f-86d5-e9634055f0bf" />



---

## 🛠️ Tech Stack

| Category | Technology | Role |
|----------|------------|------|
| **IaC** | Terraform | AWS 인프라 전체 코드화 (모듈화) |
| **Config Management** | Ansible | 프로비저닝 후 설치/설정 자동화 |
| **Orchestration** | Kubernetes (EKS 1.32) | 컨테이너 오케스트레이션, HPA |
| **CI** | GitHub Actions | 빌드, 테스트, ECR 이미지 푸시 |
| **CD** | Argo CD | GitOps 기반 자동 배포 |
| **Service Mesh** | Istio | mTLS, gRPC 로드밸런싱, 트래픽 모니터링 |
| **Monitoring** | Prometheus + Grafana | 메트릭 수집 및 시각화 |
| **Backup** | AWS Backup | RDS, S3 자동 백업 |
| **Container Registry** | AWS ECR | Docker 이미지 저장 |
| **Database** | RDS PostgreSQL 15 | 서비스별 데이터베이스 |
| **Cache** | ElastiCache Redis 7 | 타임딜 재고 관리, JWT 토큰 |
| **CDN** | CloudFront + S3 | 프론트엔드 서빙 |

---

## 📁 Repository Structure

```
pposiraegi-ecommerce/
├── infrastructure/
│   ├── modules/
│   │   ├── networking/      # VPC, Subnet, NAT, Route Table
│   │   ├── security/        # Security Groups
│   │   ├── storage/         # RDS, ElastiCache, S3, SSM
│   │   └── eks/             # EKS Cluster, Node Group, IAM
│   ├── kubernetes/
│   │   ├── base/            # Namespace, ConfigMap
│   │   ├── services/        # Deployment, Service, HPA per service
│   │   └── monitoring/      # Prometheus, Grafana
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── pipeline.tf
├── backend/
│   ├── api-gateway/
│   ├── user-service/
│   ├── order-service/
│   └── product-service/
└── frontend/
```

---

## 🔄 CI/CD Pipeline

```
Code Push → GitHub
    └─► GitHub Actions (CI)
            └─► Docker Build → ECR Push
                    └─► Argo CD (CD)
                            └─► Watch kubernetes/ folder
                                    └─► Auto Deploy to EKS
```

---

## ☸️ Kubernetes Configuration

### Services

| Service | Min Replicas | Max Replicas | Scale Trigger |
|---------|-------------|-------------|---------------|
| api-gateway | 2 | 10 | CPU 70% |
| user-service | 2 | 10 | CPU 70% |
| order-service | 2 | 10 | CPU 70% |
| product-service | 2 | 10 | CPU 70% |

### Namespace

```
production
├── api-gateway
├── user-service
├── order-service
├── product-service
├── Argo CD
├── Prometheus
├── Grafana
└── Istio (Service Mesh)
```

---

## 🌐 Network Configuration

| Component | CIDR | Purpose |
|-----------|------|---------|
| VPC | 10.0.0.0/16 | 전체 네트워크 |
| Public Subnet A | 10.0.1.0/24 | ALB (AZ-a) |
| Public Subnet B | 10.0.2.0/24 | ALB (AZ-b) |
| Private Subnet A | 10.0.11.0/24 | EKS Nodes, RDS, Redis (AZ-a) |
| Private Subnet B | 10.0.12.0/24 | EKS Nodes, RDS, Redis (AZ-b) |

---

## 📊 Monitoring

- **Prometheus**: EKS Pod/Node 메트릭, 서비스별 RPS/응답시간/에러율, gRPC, Redis, RDS
- **Grafana**: 실시간 대시보드 시각화, Slack 알림 연동
- **CloudWatch**: 기존 `pposiraegi-production-health` 대시보드 유지

---

## 🚀 Getting Started

### Prerequisites

```bash
# Required tools
terraform >= 1.5.0
kubectl >= 1.32
aws-cli >= 2.0
helm >= 3.0
```

### Infrastructure Setup

```bash
# 1. Clone repository
git clone https://github.com/Goorm4I/pposiraegi-ecommerce.git
cd pposiraegi-ecommerce/infrastructure

# 2. Initialize Terraform
terraform init

# 3. Review plan
terraform plan -var-file="terraform.tfvars"

# 4. Apply
terraform apply -var-file="terraform.tfvars"
```

### Connect to EKS

```bash
aws eks update-kubeconfig \
  --region ap-northeast-2 \
  --name pposiraegi-cluster \
  --profile goorm
```

### Deploy with Argo CD

```bash
# Install Argo CD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Apply Argo CD Application
kubectl apply -f infrastructure/argocd-app.yaml

# Access Argo CD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

---

## 💡 Key Features

### Time-Deal Inventory Control
Redis Lua Script를 활용한 원자적 재고 차감으로 동시 주문 처리 시 데이터 정합성 보장

### Auto Scaling
HPA + Cluster Autoscaler로 타임딜 오픈 시 트래픽 폭증 자동 대응

### GitOps
Argo CD가 `kubernetes/` 폴더를 감시하여 Git 변경 사항을 EKS에 자동 반영

### Service Mesh
Istio를 통한 mTLS 암호화, gRPC L7 로드밸런싱, 서비스 간 트래픽 모니터링

---

## 📋 Phase History

| Phase | Period | Focus |
|-------|--------|-------|
| Phase 1 | 2026.02 | EC2 + RDS + ALB 기반 단일 서버 |
| Phase 2 | 2026.03 | ECS Fargate MSA + CodePipeline CI/CD |
| **Phase 3** | **2026.04~05** | **EKS + Terraform IaC + GitOps 운영 자동화** |

---

## 📄 License

This project is for educational purposes as part of the Goorm Cloud Native Engineering Course.
