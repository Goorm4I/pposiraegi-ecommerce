# 🏗 AWS Terraform Infrastructure Architecture

## 📌 Architecture Overview

Users는 CloudFront를 통해 접근하며, 요청을 라우팅한다.  
정적 프론트엔드 파일은 CloudFront → S3(OAC)로 서빙되고,  
API 요청은 CloudFront → IGW → ALB → Public Subnet의 Backend EC2로 전달된다.  
Backend EC2는 RDS PostgreSQL(Private Subnet) 및 ElastiCache Redis(Private Subnet)와 통신한다.  
개발자는 AWS SSM Session Manager를 통해 EC2에 SSH 키 없이 접근한다.

---

## Architecture Diagram

<img width="1601" height="1281" alt="다이어그램v5 drawio" src="https://github.com/user-attachments/assets/59fbffa5-6c48-416a-9dc2-1bc279fb30dc" />

---

## 🏗️ 인프라 구조 (Terraform / AWS)

### 📦 리소스 목록

| 리소스 | 개수 | 설명 |
|--------|------|------|
| VPC | 1 | 10.0.0.0/16 |
| Public Subnet | 2 | ALB(x2 AZ), Backend EC2 |
| Private Subnet | 2 | RDS(x1), ElastiCache(x1) |
| Internet Gateway | 1 | Public 서브넷 인터넷 연결 |
| NAT Gateway | 0 | 현재 비활성 (주석 해제 시 활성화) |
| EC2 | 1 | Backend (Spring Boot) |
| ALB | 1 | Backend EC2 앞단 로드밸런서 |
| RDS (PostgreSQL) | 1 | Private 서브넷, db.t3.micro |
| ElastiCache (Redis) | 1 | Private 서브넷, cache.t3.micro |
| S3 | 1 | 프론트엔드 정적 파일 호스팅 |
| CloudFront | 1 | CDN, S3+ALB 통합 진입점 |
| IAM Role | 1 | EC2 SSM 접근용 Instance Profile |
| Security Group | 4 | ALB / Backend / RDS / Redis |

---

### 🔀 트래픽 흐름
```
사용자
  │
  ▼
CloudFront
  ├─ /api/*  ──────────────► IGW ──► ALB ──► Backend EC2 (Public Subnet)
  │                                                  │              │
  │                                                  ▼              ▼
  │                                          RDS PostgreSQL   ElastiCache Redis
  │                                           (Private Subnet)  (Private Subnet)
  └─ /*  ──────────────────► S3 (프론트엔드 정적 파일, OAC)

개발자
  │
  ▼
AWS SSM Session Manager ──► Backend EC2 (SSH 키 없이 접근)
```

---

### 🔐 Security Group 트래픽 흐름
```
인터넷 (0.0.0.0/0)
  │
  │ 80 (HTTP)
  ▼
[alb-sg] ALB
  │
  │ 8080 (API) - ALB에서 오는 것만
  ▼
[backend-sg] Backend EC2 ◄── 22 (SSH) - my_ip만
  │                 │
  │ 5432            │ 6379
  ▼                 ▼
[rds-sg] RDS    [redis-sg] ElastiCache Redis
```

### 🔐 Security Group 규칙

| SG 이름 | 인바운드 | 허용 출처 |
|---------|---------|----------|
| alb-sg | 80 (HTTP) | 0.0.0.0/0 |
| backend-sg | 8080 (API) | alb-sg |
| backend-sg | 22 (SSH) | my_ip |
| rds-sg | 5432 (PostgreSQL) | backend-sg |
| redis-sg | 6379 (Redis) | backend-sg |

---

### 🌐 주요 엔드포인트

| 항목 | 값 | 설명 |
|------|-----|------|
| 프론트엔드 URL | `https://<cloudfront-domain>` | CloudFront 도메인 (`terraform output cloudfront_url`) |
| 백엔드 API URL | `http://<alb-dns>/api/v1/...` | ALB DNS (`terraform output alb_dns`) |
| S3 버킷명 | `pposiraegi-frontend-xxxx` | 프론트 빌드 파일 업로드 대상 |
| RDS 엔드포인트 | Private 접근만 가능 | `terraform output rds_endpoint` |
| ElastiCache 엔드포인트 | Private 접근만 가능 | `terraform output elasticache_endpoint` |
| Backend IP | Public IP | `terraform output backend_public_ip` |

---

### 🚀 배포 방법

**1. 인프라 생성**
```bash
terraform init
terraform apply
```

**2. 프론트엔드 배포**
```bash
npm run build
aws s3 sync ./build s3://$(terraform output -raw s3_bucket_name) --profile goorm --delete
```

**3. Backend EC2**
- `terraform apply` 시 `user_data.sh` 자동 실행
- GitHub 레포 클론 → `docker-compose up -d` 자동 실행
- RDS / ElastiCache 연결 정보는 환경변수로 자동 주입
- CORS 허용 오리진은 CloudFront 도메인으로 자동 설정
- EC2 접근은 AWS SSM Session Manager 사용 (SSH 키 불필요)

**4. 인프라 삭제**
```bash
terraform destroy
```

---

### 📁 파일 구성

| 파일 | 설명 |
|------|------|
| `main.tf` | 전체 AWS 리소스 정의 |
| `variables.tf` | 변수 선언 및 기본값 |
| `outputs.tf` | 배포 후 출력값 (URL, IP 등) |
| `user_data.sh` | EC2 부팅 시 자동 실행 스크립트 |

---

### 🔄 주요 아키텍처 변경 이력

| 변경 항목 | 이전 | 현재 |
|----------|------|------|
| EC2 내부 DB | PostgreSQL 컨테이너 | RDS PostgreSQL (managed) |
| EC2 내부 캐시 | Redis 컨테이너 | ElastiCache Redis (managed) |
| 개발자 접근 방식 | Bastion Host → SSH | AWS SSM Session Manager |
| RDS 엔진 | MySQL | PostgreSQL 15 |
| EC2 위치 | Private Subnet | Public Subnet |
