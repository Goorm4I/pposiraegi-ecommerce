-- init.sql
-- Docker 컨테이너가 처음 생성될 때 1회만 실행됩니다.
-- 데이터베이스 및 역할은 docker-compose의 환경변수(POSTGRES_DB, POSTGRES_USER)에 의해 생성됩니다.
-- 이곳에는 기본적으로 필요한 사전 설정(예: extension 생성) 등을 넣을 수 있습니다.

-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 혹시나 필요한 시드 데이터를 여기에 넣을 수도 있으나,
-- 애플리케이션 시작 시마다 상태를 갱신해야 하는(예: 타임딜 시간 연장) 데이터는
-- Spring Boot의 data.sql을 사용하는 것이 더 유연합니다.
