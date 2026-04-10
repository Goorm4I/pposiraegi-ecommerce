#!/bin/bash
set -e

# 로그 파일
exec > /var/log/user-data.log 2>&1
echo "[$(date)] EC2 초기화 시작"

# 패키지 업데이트 및 Docker 설치
dnf update -y
dnf install -y docker git

# Docker 시작
systemctl enable docker
systemctl start docker

# ec2-user를 docker 그룹에 추가
usermod -aG docker ec2-user

# Docker Compose 설치
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Docker Buildx 설치 (docker-compose build에 필요)
mkdir -p ~/.docker/cli-plugins
curl -SL "https://github.com/docker/buildx/releases/download/v0.17.1/buildx-v0.17.1.linux-amd64" \
  -o ~/.docker/cli-plugins/docker-buildx
chmod +x ~/.docker/cli-plugins/docker-buildx

echo "[$(date)] Docker 설치 완료"

# 레포 클론
cd /home/ec2-user
git clone ${github_repo} app
cd app

# EC2 환경용 docker-compose 설정 오버라이드
# - RDS/ElastiCache 연결 정보 환경변수 주입
# - 로컬용 db/redis 컨테이너 비활성화 (profiles: local)
# - docker-compose.local.yml은 Docker Compose가 자동으로 읽지 않음
#   (로컬 실행 시에만 -f 옵션으로 명시적으로 포함)
cat > /home/ec2-user/app/docker-compose.local.yml <<EOF
services:
  backend:
    depends_on: {}
    environment:
      JWT_SECRET: "${jwt_secret}"
      CORS_ALLOWED_ORIGINS: "${cors_allowed_origins}"
      SPRING_PROFILES_ACTIVE: "prod"
      SPRING_DATA_REDIS_HOST: "${redis_host}"
      SPRING_DATA_REDIS_PORT: "6379"
      SPRING_DATASOURCE_URL: "jdbc:postgresql://${db_host}:5432/ecommerce"
      SPRING_DATASOURCE_USERNAME: "${db_username}"
      SPRING_DATASOURCE_PASSWORD: "${db_password}"
      TZ: "UTC"
      JAVA_OPTS: "-Xms512m -Xmx512m -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom"
  db:
    profiles: ["local"]
  redis:
    profiles: ["local"]
EOF

chown -R ec2-user:ec2-user /home/ec2-user/app

echo "[$(date)] 레포 클론 완료, docker-compose 시작"

# docker-compose 실행
docker-compose up -d

echo "[$(date)] EC2 초기화 완료"
