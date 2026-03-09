# 인프라 완전 재구축 및 자동 시딩 테스트 플랜 (2026-03-09)

## 1. 테스트 목적
* `terraform destroy` 후 `terraform apply`를 통해 인프라를 완전히 새로 구축했을 때, 변경된 데이터 시딩 파이프라인(`docker-compose.yml`, `data.sql`, `ddl-auto=update` 등)이 정상적으로 동작하여 **수동 개입 없이 완벽한 테스트 환경이 구성되는지 확인**합니다.

## 2. 인프라 배포 플로우
1. **Terraform Apply**: VPC, ALB, EC2, Security Group 등 AWS 리소스 생성
2. **User Data 실행 (EC2)**:
   * Docker 및 Git 설치
   * GitHub에서 `pposiraegi-ecommerce` 레포지토리 `clone` (**최신 코드 반영 필수!**)
   * `docker-compose up -d` 실행
3. **Docker Compose**:
   * **DB (PostgreSQL)**: 컨테이너 실행 및 볼륨 마운트. 최초 생성 시 `init.sql` 마운트됨.
   * **Redis**: 세션/락 용도 캐시 서버 실행
   * **Backend**: DB와 Redis가 healthy 상태가 된 후 컨테이너 빌드 및 실행
4. **Spring Boot (Backend) 초기화**:
   * `ddl-auto=update`: 기존 데이터 유지 및 변경된 스키마 반영
   * `data.sql` 실행: `ON CONFLICT` 기반 안전한 초기 데이터(카테고리, 상품, 유저, 타임딜) 주입. 타임딜 시간은 KST 기준 넉넉하게 연장됨.

## 3. 실행 단계
### [Step 1] 최신 코드 반영 (필수)
현재 로컬에 작성된 파일들(`data.sql`, `application-dev.yaml`, `docker-compose.yml` 등)을 GitHub `main` 브랜치에 커밋 및 푸시합니다. EC2가 부팅될 때 이 브랜치의 코드를 받아오기 때문입니다.
```bash
cd ~/Goorm4I/pposiraegi-ecommerce
git add .
git commit -m "feat: Add auto data seeding and Redis concurrency control"
git push origin main
```

### [Step 2] 기존 인프라 파괴 (Destroy)
```bash
cd ~/Goorm4I/pposiraegi---terraform-infra
terraform destroy -auto-approve
```

### [Step 3] 새 인프라 배포 (Apply)
```bash
terraform apply -auto-approve
```

### [Step 4] EC2 초기화 대기 및 검증
EC2가 생성된 후 User Data가 실행되어 도커가 뜨기까지 약 3~5분이 소요됩니다.

## 4. 검증 및 트러블슈팅 가이드
문제가 발생했을 때 헤매지 않기 위한 체크리스트입니다.

### Q1. ALB 주소로 접속했는데 응답이 없는 경우 (502 Bad Gateway 등)
* **원인:** 백엔드 서버가 아직 뜨지 않았거나, 빌드/실행 중 에러가 발생하여 컨테이너가 죽은 경우.
* **대처법:**
  1. EC2에 SSH로 접속: `ssh -i <키파일> ec2-user@<새로운_EC2_IP>`
  2. 도커 상태 확인: `docker ps -a` (backend 컨테이너가 Exited 상태인지 확인)
  3. 도커 로그 확인: `docker logs pposiraegi-backend --tail 100`

### Q2. 백엔드 로그에서 SQL 에러(데이터 주입 실패)가 발생한 경우
* **원인:** `data.sql` 의 문법 오류, 외래 키(FK) 제약 조건 위반, 혹은 스키마 생성 타이밍(`defer-datasource-initialization`) 문제.
* **대처법:** 백엔드 로그 상의 `SQLException` 원인을 파악하고 `data.sql`을 수정한 뒤 다시 커밋&푸시 후 `docker restart pposiraegi-backend`를 실행합니다.

### Q3. 프론트엔드에서 여전히 "종료된 딜"이라고 뜨는 경우
* **원인:** KST/UTC 타임존 보정이 잘못되었거나 `ON CONFLICT DO UPDATE` 구문이 동작하지 않은 경우.
* **대처법:**
  1. DB에 직접 접속해 확인: `docker exec -i pposiraegi-db psql -U user -d ecommerce -c 'SELECT id, start_time, end_time, status FROM time_deals;'`
  2. 시간이 과거로 되어 있다면 `data.sql` 의 `NOW() + INTERVAL` 구문을 재조정합니다.

### Q4. User Data(초기 스크립트) 자체가 실패한 것 같은 경우 (도커 설치 안됨 등)
* **대처법:** EC2 접속 후 `/var/log/user-data.log` 또는 `/var/log/cloud-init-output.log` 를 열어 어디서 스크립트가 멈췄는지 확인합니다.
