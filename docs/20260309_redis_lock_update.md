# 백엔드 동시성 제어 및 데이터 시딩 관련 변경 사항 (2026-03-09)

## 1. Redis 기반 타임딜 재고 분산 락(Redisson) 도입 준비
타임딜 특성상 순간적인 트래픽이 몰릴 때 발생하는 **초과 판매(Over-selling) 방지를 위해 Redisson 기반의 분산 락 로직을 구현**해두었습니다.

### 추가/수정된 파일
*   **`backend/build.gradle`**: 
    *   `spring-boot-starter-data-redis`, `redisson-spring-boot-starter` 의존성 추가
*   **`backend/src/main/java/cloud/pposiraegi/ecommerce/global/config/RedissonConfig.java`**: 
    *   Redis 접속을 위한 RedissonClient 빈 등록
*   **`backend/src/main/java/cloud/pposiraegi/ecommerce/domain/product/service/RedisTimeDealFacade.java`**: 
    *   **핵심 로직**: `decreaseStockWithLock()` 메서드를 통해 `time_deal_lock:{id}` 형태의 락을 획득하고, 성공 시에만 실제 DB의 재고 차감 로직(`timeDealService.decreaseStock()`)을 호출합니다. (최대 대기 시간 10초, 락 소유 시간 3초로 설정)
*   **`backend/src/test/java/cloud/pposiraegi/ecommerce/domain/product/service/RedisTimeDealConcurrencyTest.java`**:
    *   `ExecutorService`와 `CountDownLatch`를 이용해 100명의 동시 요청 상황 시뮬레이션. 
    *   기존 로직의 한계(초과판매 발생)와 분산 락을 통한 해결(정상 0개 도달)을 입증하는 테스트 코드입니다.

### ⚠️ Claude에게 남기는 작업 요청 (TODO)
현재 `RedisTimeDealFacade` 뼈대와 테스트 코드만 작성되어 있습니다. **실제 `OrderService` 등 주문/결제 승인 로직에서 재고를 차감할 때, 기존의 단순 DB 차감 로직 대신 `redisTimeDealFacade.decreaseStockWithLock(timeDealId)`를 호출하도록 파이프라인을 연결해주세요.**

---

## 2. 배포 환경 초기 데이터 시딩 스크립트 작성
서버 배포 시 (혹은 DB 초기화 시) 카테고리, 상품 10개, KST 기준 타임딜, 테스트 유저 데이터를 즉시 주입할 수 있는 자동화 스크립트를 `scripts` 디렉토리에 작성해두었습니다.

*   **위치**: `scripts/seed_initial_data.sh`
*   **기능**: EC2 원격 서버(또는 로컬 도커)의 PostgreSQL 컨테이너에 접속해 기초 데이터를 일괄 INSERT 합니다. 타임딜 시간은 KST(UTC+9)를 고려하여 현재 시간 기준으로 자동 연장되도록 구현되었습니다.
