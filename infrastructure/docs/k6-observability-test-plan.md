# k6 Observability Test Plan

이 문서는 EKS Phase 3에서 `k6 -> ALB -> api-gateway -> MSA -> RDS/Redis` 흐름을 검증하고, Prometheus/Loki로 운영 신호를 읽기 위한 기준이다.

Tempo 도입 설계는 `infrastructure/docs/tempo-tracing-design-plan.md`에 별도로 둔다.
이 문서는 현재 가능한 metrics/logs 검증을 기준으로 하고, Tempo가 들어오면 느린 요청 1건의 hop별 지연을 확인하는 단계가 추가된다.

## 현재 전제

- Prometheus는 임시 `PodMonitor/monitoring/pposiraegi-apps-temp`로 앱 Pod의 `8081/actuator/prometheus`를 직접 scrape한다.
- Loki는 임시 `promtail` Helm release로 노드 로그를 수집한다.
- GitHub/ArgoCD 권한 문제가 해결되기 전까지 위 리소스는 검증용 임시 상태다.
- 테스트 전 seed 데이터가 필요하다.

```bash
API_BASE=http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com
./scripts/seed.sh "$API_BASE"
```

## 1차 실행

```bash
BASE_URL=http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com \
k6 run load-test/order-flow-smoke.js
```

스크립트는 매 iteration마다 다음 흐름을 탄다.

```text
회원가입 -> 로그인/JWT -> 주소 등록 -> ACTIVE 타임딜 조회 -> 상세 조회 -> 주문서 생성 -> 주문 제출
```

## k6 성공 기준

- `http_req_failed < 5%`
- `http_req_duration p95 < 1500ms`
- `order_flow_duration p95 < 3000ms`
- `order_submit_success > 0`

초기 smoke 테스트에서는 성공/실패를 세밀하게 보기보다, 전체 주문 경로가 관측 가능한지 먼저 본다.

## 비즈니스 중요도 기반 검증 순서

타임딜 검증은 기술 구성요소가 아니라 장애가 비즈니스에 미치는 영향 순서로 본다.

1. **재고 정합성**
   - 성공 주문 수가 재고를 초과하지 않아야 한다.
   - Redis 차감, DB 동기화, 타임딜 잔여 수량이 서로 어긋나는지 확인한다.
   - 실패가 발생하더라도 `재고 부족`으로 제어된 실패면 장애가 아니다.

2. **주문 성공률과 사용자 지연**
   - 재고가 충분한 상황에서는 대부분의 주문이 성공해야 한다.
   - `/api/v1/orders`, `/api/v1/orders/submit` p95가 SLO를 넘으면 사용자는 장애처럼 느낀다.
   - 이 단계는 `stock > attempts`로 둬야 한다. 재고가 적으면 Redis가 빠르게 실패시켜 downstream 병목이 드러나지 않는다.

3. **병목 위치**
   - `order-service` endpoint 지연, HikariCP, RDS, Redis/gRPC 로그를 함께 본다.
   - CPU가 낮아도 지연이 높으면 CPU 기반 HPA만으로는 부족하다.

4. **오토스케일과 비용**
   - HPA/Karpenter가 반응했는지보다, 피크가 지나가기 전에 사용자 지연을 줄였는지를 본다.
   - 짧은 타임딜 spike에서는 scale-out이 늦게 도착할 수 있다.

## 주문 처리 SLO 초안

| 항목 | 목표 | 실패 해석 |
|------|------|-----------|
| 초과판매 | 0건 | Redis/DB 정합성 또는 idempotency 문제 |
| 주문 성공률 | 99% 이상 | 주문 경로 장애 또는 외부 의존성 병목 |
| `/api/v1/orders` p95 | 2초 이하 | checkout/주문서 생성 병목 |
| `/api/v1/orders/submit` p95 | 2초 이하 | 재고 차감, 주문 확정, DB transaction 병목 |
| Hikari pending | 0 유지 | 커넥션 풀 대기 발생 |
| Hikari timeout | 0 유지 | 실제 DB connection 장애 |
| Redis -> DB sync | 1분 내 반영 | 재고 표시/정산 지연 위험 |

실습 환경에서는 p95 2초 기준을 바로 만족하지 못할 수 있다. 중요한 것은 기준을 세우고, 같은 조건에서 변경 전후를 비교하는 것이다.

## Prometheus 확인 쿼리

앱 Pod scrape 상태:

```promql
up{job="monitoring/pposiraegi-apps-temp"}
```

서비스별 요청량:

```promql
sum by (pod) (
  rate({namespace="production",__name__=~"http_server_requests_seconds_count|http_server_requests_seconds_seconds_count|http_server_request_duration_seconds_count"}[1m])
)
```

HTTP p95 latency:

```promql
histogram_quantile(
  0.95,
  sum by (le, pod) (
    rate({namespace="production",__name__=~"http_server_requests_seconds_bucket|http_server_requests_seconds_seconds_bucket|http_server_request_duration_seconds_bucket"}[1m])
  )
)
```

JVM memory:

```promql
sum by (pod) (
  jvm_memory_used_bytes{namespace="production"}
)
```

DB connection pool:

```promql
sum by (pod) (
  hikaricp_connections_active{namespace="production"}
)
```

HikariCP 포화 확인:

```promql
max_over_time(hikaricp_connections_active{namespace="production", pod=~"order-service.*"}[10m])
```

```promql
max_over_time(hikaricp_connections_pending{namespace="production", pod=~"order-service.*"}[10m])
```

```promql
increase(hikaricp_connections_timeout_total{namespace="production", pod=~"order-service.*"}[10m])
```

endpoint별 평균 지연:

```promql
topk(
  12,
  sum by (pod, uri, method, status) (
    rate(http_server_requests_seconds_sum{namespace="production"}[10m])
  )
  /
  sum by (pod, uri, method, status) (
    rate(http_server_requests_seconds_count{namespace="production"}[10m])
  )
)
```

Pod CPU:

```promql
sum by (pod) (
  rate(container_cpu_usage_seconds_total{namespace="production",container!="POD",container!=""}[1m])
)
```

Pod memory:

```promql
sum by (pod) (
  container_memory_working_set_bytes{namespace="production",container!="POD",container!=""}
)
```

## Loki 확인 쿼리

전체 production 에러:

```logql
{namespace="production"} |= "ERROR"
```

주문 서비스 로그:

```logql
{namespace="production", app="order-service"}
```

Gateway 인증 실패:

```logql
{namespace="production", app="api-gateway"} |= "권한 에러"
```

DB/Redis/gRPC 의심 로그:

```logql
{namespace="production"} |~ "timeout|Exception|ERROR|UNAVAILABLE|DEADLINE_EXCEEDED|connection"
```

## Tempo 도입 후 확인 흐름

Tempo가 설치되고 앱 trace export가 연결되면 느림 진단 순서는 다음처럼 확장한다.

```text
1. Prometheus: 어떤 endpoint의 p95가 튀었는지 확인
2. Loki: 같은 시간대 timeout/error/pool exhaustion 로그 확인
3. Tempo: 느린 요청 trace를 열어 gateway/order/product/user hop별 duration 확인
```

검증 기준:

```text
단일 주문 smoke에서 api-gateway와 order-service span이 같은 trace id로 묶인다.
k6 테스트에서 느린 /api/v1/orders/submit 요청의 trace를 Grafana Explore에서 찾을 수 있다.
Loki 로그의 trace_id와 Tempo trace id가 연결된다.
```

## 운영 해석 포인트

### k6에서 Long ID가 깨진다

JavaScript `Number`는 64-bit Long ID를 안전하게 표현하지 못한다.
`838324653347833248` 같은 `skuId`, `timeDealId`, `checkoutId`를 `Number()`로 변환하면 값이 미세하게 바뀌고, 백엔드는 존재하지 않는 SKU로 판단할 수 있다.

증상:

```text
order-service: io.grpc.StatusRuntimeException: UNKNOWN
product-service: 해당 상품의 옵션(SKU) 정보를 찾을 수 없습니다.
```

대응:

```javascript
timeDealId: String(detail.timeDealId)
skuId: String(sku.skuId)
checkoutId: String(checkoutId)
```

Spring/Jackson이 문자열 숫자를 `Long`으로 변환하게 두는 편이 안전하다.

### k6 시간 포맷이 UTC로 들어간다

`Date.toISOString()`은 UTC 문자열을 만든다.
백엔드가 timezone 없는 `LocalDateTime`으로 받으면 KST 기준 9시간 과거로 해석될 수 있다.

증상:

```text
T008 시작 시간은 현재 시간보다 과거일 수 없습니다.
```

대응:

- k6에서 로컬 시간 기준 `yyyy-MM-ddTHH:mm:ss` 문자열을 직접 만든다.
- 타임딜 상태 전환은 스케줄러 주기에 의존하므로 `setupTimeout`을 넉넉히 둔다.

### API는 성공하는데 p95가 튄다

- `api-gateway`만 느린지, `order-service`도 같이 느린지 본다.
- `hikaricp_connections_active`가 같이 오르면 DB connection 병목 가능성이 있다.
- Loki에서 timeout/gRPC/DB 로그를 같이 본다.

### order-service는 느린데 product/user는 조용하다

- 주문서 생성과 주문 제출 구간을 나눠 봐야 한다.
- 주문 제출은 Redis lock, idempotency, DB transaction, product gRPC 재고 차감을 함께 탄다.

### Prometheus에는 보이는데 Loki에는 안 보인다

- 앱은 동작하지만 로그 수집기가 해당 노드에 없을 수 있다.
- `promtail` DaemonSet Pending 여부와 node affinity/taint를 확인한다.

### Loki에는 보이는데 Prometheus에는 안 보인다

- 앱 로그는 stdout으로 나오지만 actuator scrape가 끊긴 상태다.
- `PodMonitor`, `management` port, `/actuator/prometheus`, scrape target health를 확인한다.

## 다음 단계

1. 1 VU smoke test로 happy path 확인
2. 5~10 VU로 관측 지표가 움직이는지 확인
3. 고정 재고 상품을 대상으로 sold-out 동시성 테스트 작성
4. HPA/Karpenter가 반응할 정도로 부하를 올리고 scale event, latency, error rate를 함께 본다.
5. GitHub/ArgoCD 권한 복구 후 임시 `PodMonitor`와 `promtail`을 정식 매니페스트 또는 bootstrap 단계로 승격할지 결정한다.

## Sold-out 동시성 테스트

전용 상품/타임딜을 생성하고 `재고 < 주문 시도` 조건으로 동시에 주문을 넣는다.

```bash
BASE_URL=http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com \
STOCK=20 \
ATTEMPTS=50 \
VUS=50 \
k6 run load-test/timedeal-soldout-eks.js
```

검증 기준:

- `soldout_order_success <= STOCK`
- `oversell_detected == 0`
- 실패는 장애가 아니라 품절/재고 부족 응답일 수 있다.
- 테스트 후 `timeDeal.remainingQuantity`, `sku.stockQuantity`, 성공 주문 수를 함께 비교한다.

해석 포인트:

- 성공 수가 재고보다 많으면 초과판매다.
- 성공 수는 재고 이하인데 `skuStock`과 `remainingQuantity`가 다르면 DB 재고와 타임딜 재고의 차감 기준을 점검해야 한다.
- `checkout`은 성공하고 `submit`에서 실패하는 비율이 높으면 주문서 생성 시점과 실제 재고 차감 시점 사이의 경쟁을 보고 있는 것이다.

## 성공 주문 처리량 테스트

재고 방어가 아니라 주문 처리 병목을 보려면 `stock > attempts`로 둔다.

이 테스트의 목적은 "주문이 성공하는가"만 보는 것이 아니다. 타임딜 피크에서 사용자가 실제로 기다리는 시간이 어느 계층에서 발생하는지 재현 가능하게 찾는 것이다.

재현 전제:

- production namespace의 앱 4종이 모두 Ready 상태여야 한다.
- Prometheus가 앱의 `8081/actuator/prometheus`를 scrape 중이어야 한다.
- Loki/promtail이 production Pod 로그를 수집 중이어야 한다.
- 이전 테스트의 scale-out 여파가 남을 수 있으므로, 비교 실험 전에는 Deployment replica와 NodeClaim 상태를 기록한다.
- 같은 조건을 비교할 때는 `STOCK`, `ATTEMPTS`, `VUS`, `USER_COUNT`를 고정한다.

```bash
BASE_URL=http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com \
STOCK=2000 \
ATTEMPTS=1000 \
VUS=300 \
USER_COUNT=300 \
k6 run load-test/timedeal-soldout-eks.js
```

`USER_COUNT`는 테스트 데이터 준비용 사용자 수다. `ATTEMPTS`와 분리해야 회원가입/주소 생성 부하와 주문 부하가 섞이지 않는다.

재현성을 위해 실행 전후에 같이 저장할 상태:

```bash
kubectl get deploy,hpa,pods -n production -o wide
kubectl get nodeclaims
```

테스트 직후 Prometheus에서 함께 확인할 쿼리:

```promql
max_over_time(hikaricp_connections_active{namespace="production", pod=~"order-service.*"}[10m])
```

```promql
max_over_time(hikaricp_connections_pending{namespace="production", pod=~"order-service.*"}[10m])
```

```promql
increase(hikaricp_connections_timeout_total{namespace="production", pod=~"order-service.*"}[10m])
```

```promql
topk(
  12,
  sum by (pod, uri, method, status) (
    rate(http_server_requests_seconds_sum{namespace="production"}[10m])
  )
  /
  sum by (pod, uri, method, status) (
    rate(http_server_requests_seconds_count{namespace="production"}[10m])
  )
)
```

스크립트가 따로 기록하는 주문 지표:

- `order_checkout_duration`: `/api/v1/orders` 소요 시간
- `order_submit_duration`: `/api/v1/orders/submit` 소요 시간
- `soldout_order_success`: 성공 주문 수
- `soldout_order_failure`: 실패 주문 수
- `oversell_detected`: 초과판매 감지율

### 2026-05-02 기준 관측 결과

| 조건 | 결과 | 판정 |
|------|------|------|
| `STOCK=5`, `ATTEMPTS=10`, `VUS=10` | 성공 5, 실패 5, oversell 0 | 재고 정합성 통과 |
| `STOCK=1000`, `ATTEMPTS=500`, `VUS=100` | 성공 500, p95 2.75s | 성공 경로 통과, 지연 기준 실패 |
| `STOCK=2000`, `ATTEMPTS=1000`, `VUS=300`, `USER_COUNT=300` | 성공 999, 실패 1, p95 9.91s | 정합성 통과, 사용자 지연 실패 |
| `STOCK=2000`, `ATTEMPTS=1000`, `VUS=300`, `USER_COUNT=300` | 성공 1000, `checkout p95=12.4s`, `submit p95=13.1s` | order-service 주문 경로 포화 재현 |
| 위 조건 + `order-service` 사전 4 replica | 성공 996, `checkout p95=8.8s`, `submit p95=10.7s` | 개선은 있으나 SLO 실패 |

### 핵심 인사이트

`VU=300` 성공 주문 테스트는 "서비스가 죽는가"보다 더 중요한 문제를 보여줬다.

- 주문은 거의 모두 성공한다.
- 초과판매도 발생하지 않는다.
- 하지만 사용자는 checkout/submit에서 10초 이상 기다릴 수 있다.
- 즉, 장애는 `5xx`나 `timeout`이 아니라 `느린 성공`으로 나타난다.

타임딜에서는 이 상태도 위험하다. 사용자는 주문 버튼을 여러 번 누르거나, 품절로 오해하거나, 결제 단계에서 이탈할 수 있다.

`VU=300` 테스트에서 확인한 병목 신호:

- `order-service /api/v1/orders/submit` 평균 지연 최대 약 8초
- `order-service /api/v1/orders` 평균 지연 최대 약 7초
- `order-service` Hikari active connection이 pod별 10까지 상승
- 재실행 시 한 `order-service` pod에서 `/api/v1/orders` 평균 약 15초, `/api/v1/orders/submit` 평균 약 10초까지 상승
- Hikari active connection은 pod별 10까지 상승했고, pending connection은 최대 59/98까지 쌓임
- Hikari timeout은 0
- HPA는 테스트 후 `order-service 192%/70%`, `product-service 159%/70%`를 봤다.
- Karpenter는 추가 nodeclaim을 생성했지만, 짧은 spike에서는 scale-out이 사용자 지연을 줄이기 전에 피크가 지나갔다.

현재 가설:

1. 초과판매 방어는 Redis가 정상적으로 막고 있다.
2. 성공 주문이 몰리면 `order-service` 주문 처리 경로가 먼저 느려진다.
3. `active=max`, `pending>0`, `timeout=0`이므로 커넥션 풀 대기가 실제 사용자 지연으로 나타난다. 아직 timeout은 없어서 장애보다는 포화/대기 상태다.
4. CPU 기반 HPA는 짧은 spike에 늦게 반응한다. 타임딜 특성상 사전 스케일링 또는 큐 기반/KEDA 계열 지표를 별도 검토해야 한다.

### 재현 가능한 다음 실험

다음 실험은 한 번에 하나만 바꾼다. 테스트 조건은 기본적으로 아래를 유지한다.

```text
STOCK=2000
ATTEMPTS=1000
VUS=300
USER_COUNT=300
```

#### 실험 A: order-service 사전 스케일링

가설:

- 짧은 spike에서는 HPA/Karpenter가 늦게 반응하므로, 타임딜 시작 전 `order-service` replica를 미리 늘리면 p95가 줄어든다.

변경:

- `order-service` replica를 2 -> 4로 사전 조정한다.
- 다른 서비스와 Hikari 설정은 그대로 둔다.

성공 기준:

- `order_checkout_duration p95`, `order_submit_duration p95`가 기준선 대비 유의미하게 감소한다.
- Hikari pending이 기준선 `59/98`보다 낮아진다.

해석:

- p95가 줄면 사전 스케일링이 효과적이다.
- p95가 비슷하면 replica보다 DB/RDS/트랜잭션 경로가 더 강한 병목이다.

실행 결과:

- ArgoCD self-heal이 켜진 상태에서는 `kubectl patch hpa ... minReplicas=4`가 곧바로 Git 기준 `minReplicas=2`로 되돌아갔다.
- 임시 실험을 위해 ArgoCD Application의 `selfHeal=false`를 잠깐 적용하고, 실험 후 다시 `selfHeal=true`로 원복했다.
- `order-service`를 사전 4 replica로 올린 뒤 같은 조건으로 재실행했다.
- 결과: 성공 996, 실패 4, oversell 0
- `checkout p95`: 12.4s -> 8.8s
- `submit p95`: 13.1s -> 10.7s
- 전체 HTTP p95: 11.59s -> 8.63s
- Hikari pending은 여전히 일부 order-service pod에서 최대 85/77까지 쌓였다.

판정:

- 사전 스케일링은 효과가 있었다. 하지만 SLO를 만족할 정도는 아니다.
- `order-service` replica만 늘려도 커넥션 대기와 지연이 줄어드는 것은 확인했다.
- 다만 일부 pod에 부하가 치우치고, Hikari pending이 계속 쌓이므로 다음 실험은 Hikari/RDS/트랜잭션 경로 쪽으로 좁혀야 한다.

GitOps 운영 포인트:

- ArgoCD self-heal이 켜진 리소스는 임시 운영 변경이 자동 원복된다.
- 실무에서는 긴급 조치가 필요할 때도 "Git으로 변경할 것인지", "self-heal을 일시 중지하고 조치할 것인지", "조치 후 언제 원복할 것인지"를 런북으로 명확히 해야 한다.
- 이번 실험 후 `order-service-hpa minReplicas=2`, `Application selfHeal=true`로 원복했다.

#### 실험 B: Hikari pool size 조정

가설:

- `order-service` pod별 active connection이 10까지 차고 pending이 쌓였으므로, pool size가 현재 부하에 부족하다.

변경:

- `order-service` Hikari max pool size만 조정한다.
- replica 수는 기준선과 동일하게 유지한다.

성공 기준:

- pending connection이 줄어든다.
- timeout은 계속 0이어야 한다.
- RDS CPU/connection이 위험 수준으로 오르지 않아야 한다.

해석:

- p95가 줄면 커넥션 풀 크기가 직접 병목이었다.
- RDS 지표가 악화되면 애플리케이션 pool만 키운 것이 DB 병목을 뒤로 미룬 것이다.

#### 실험 C: order-service 코드 경로 분해

가설:

- checkout과 submit이 모두 느린 것은 단일 endpoint 문제가 아니라 주문 처리 공통 경로의 DB transaction, product gRPC, Redis 차감, idempotency 처리 비용이 누적된 결과다.

변경:

- 코드 변경 전에는 로그/메트릭으로 checkout과 submit 내부 단계를 더 분리한다.
- 가능하면 order-service에 단계별 timer metric을 추가한다.

성공 기준:

- 주문 처리 시간 중 DB 저장, gRPC 호출, Redis 차감, idempotency 중 어느 구간이 큰지 구분된다.

해석:

- 여기까지 가야 "replica를 늘릴 문제인지", "pool을 키울 문제인지", "트랜잭션을 줄일 문제인지" 판단할 수 있다.

코드 레벨 병목 후보:

1. checkout 경로 `/api/v1/orders`
   - `OrderService.createOrderSheet()`가 `@Transactional(readOnly = true)`로 감싸져 있다.
   - 이 안에서 `productGrpcClient.getSkuInfos()`, `productGrpcClient.getSkuPurchaseLimit()`, Redis 구매 제한 조회, Redis checkout session 저장, `userGrpcClient.getLastUsedAddress()`가 이어진다.
   - readOnly 트랜잭션이라도 DB connection을 잡을 수 있으므로, 외부 gRPC/Redis 호출이 길어지면 Hikari pool 점유 시간이 늘어날 수 있다.

2. submit 경로 `/api/v1/orders/submit`
   - `OrderService.createOrder()`에서 Redis lock을 잡고 idempotency record를 조회한 뒤 `OrderTransactionProcessor.executeCreateOrder()`로 들어간다.
   - `executeCreateOrder()`는 `@Transactional(timeout = 10)`이다.
   - 트랜잭션 내부에서 idempotency record 저장, checkout session 조회, order 저장, order item 저장, product-service gRPC 재고 차감, 주문 상태 변경, idempotency response 저장까지 수행한다.
   - 특히 `productGrpcClient.decreaseStocks()`가 DB transaction 안에 있으므로 product-service/Redis 응답 지연이 order-service DB connection 점유 시간으로 이어질 수 있다.

3. Hikari 설정
   - `application-prod.yaml`에는 explicit `spring.datasource.hikari.maximum-pool-size`가 없다.
   - 기본값은 pod당 10으로 동작했고, 테스트에서 order-service pod별 active connection이 10까지 찼다.
   - pending이 쌓인 것은 pool size 부족이거나 connection 점유 시간이 긴 구조라는 신호다.

코드 분석 후 우선순위:

1. checkout readOnly transaction에서 외부 gRPC/Redis/user address 조회를 분리할 수 있는지 확인한다.
2. submit transaction 내부의 product gRPC 재고 차감을 transaction 밖으로 이동할 수 있는지 검토한다.
3. idempotency record 저장/응답 저장을 주문 transaction과 같은 경계로 둘 필요가 있는지 검토한다.
4. Hikari pool size를 키우는 실험은 하되, RDS connection/CPU/lock 지표와 함께 본다.
5. 최종적으로는 주문 접수와 주문 확정을 큐로 분리하는 구조를 검토한다.

### 면접/회고용 문장

처음에는 Redis 재고 차감이 초과판매를 막는지만 확인했다. 하지만 재고를 충분히 늘리고 성공 주문을 대량으로 흘려보내자, 초과판매는 없는데 주문 p95가 10초 이상 튀는 `느린 성공` 문제가 드러났다. Prometheus에서 order-service Hikari active가 max까지 차고 pending이 최대 98까지 쌓이는 것을 확인했고, 이 병목은 HPA/Karpenter가 사후에 반응해도 타임딜처럼 짧은 spike에서는 늦게 도착한다는 결론을 얻었다. 그래서 다음 검증은 order-service 사전 스케일링, Hikari pool 조정, 주문 transaction 경로 분해를 각각 하나씩 비교하는 방식으로 진행한다.

사전 스케일링 실험에서는 order-service를 2개에서 4개로 미리 늘렸을 때 checkout/submit p95가 줄어드는 것을 확인했다. 그러나 여전히 p95가 8~10초대였고 Hikari pending이 남아 있어, 단순 replica 증설만으로는 SLO를 만족시키기 어렵다는 결론을 얻었다. 이 과정에서 ArgoCD self-heal이 임시 HPA 변경을 되돌리는 것도 확인해, GitOps 환경에서 운영 실험을 할 때는 변경 주체와 원복 절차까지 런북에 포함해야 한다는 교훈을 얻었다.

주의:

- `USER_COUNT=300`이어도 setup에서 회원가입/로그인/주소 등록을 순차 수행하므로 준비 단계가 2~3분 걸린다.
- 더 실무적인 방식은 seed 단계와 load 단계 분리다. 사용자/token/address/deal을 사전에 생성하고, k6 본 실행은 주문 요청만 집중적으로 발생시켜야 한다.
