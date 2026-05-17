# Tempo Tracing Design Plan

이 문서는 Phase 3 EKS에서 Tempo를 도입할 때 무엇을 해결하려는지, 어떤 순서로 설계해야 하는지, 그리고 Prometheus/Loki와 어떻게 연결해 검증할지 정리한다.

## 핵심 질문

지금 Prometheus와 Loki로 알 수 있는 것은 다음이다.

```text
Prometheus: 느려졌다. 어느 서비스/endpoint가 느린지 숫자로 보인다.
Loki: 그 시간대에 timeout, exception, pool exhaustion 같은 사건이 보인다.
```

하지만 아직 비어 있는 질문이 있다.

```text
요청 하나가 api-gateway -> order-service -> product-service -> user-service -> RDS/Redis 중
정확히 어느 hop에서 오래 머물렀는가?
```

Tempo는 이 질문을 푼다.

## 현재 관측성 경계

| 도구 | 현재 역할 | 잘하는 것 | 못하는 것 |
|------|-----------|-----------|-----------|
| Prometheus | metrics | 지연, 에러율, CPU, memory, Hikari 상태 | 요청 1건의 이동 경로 |
| Loki | logs | 특정 시간대 사건과 예외 로그 | 서비스 간 인과관계 자동 연결 |
| Grafana | UI | metrics/logs/alerts 시각화 | 데이터 자체 생성 |
| Tempo | traces | 요청 1건의 분산 경로와 hop별 지연 | 집계 지표, 원문 로그 |

Tempo는 Prometheus/Loki를 대체하지 않는다.
Prometheus가 “어디가 이상한지”를 잡고, Loki가 “무슨 사건이 있었는지”를 확인하고, Tempo가 “요청이 어디서 느려졌는지”를 이어준다.

## 도입 전제

Tempo를 설치해도 앱이 trace를 내보내지 않으면 아무것도 보이지 않는다.
따라서 설치보다 먼저 다음 계약이 필요하다.

### 1. Trace Context

서비스 간 요청에는 W3C Trace Context가 유지되어야 한다.

```text
traceparent
tracestate
```

api-gateway에서 시작된 trace가 order/product/user 서비스까지 같은 trace id로 이어져야 한다.

### 2. App Instrumentation

Spring Boot 서비스는 OpenTelemetry 또는 Micrometer Tracing으로 span을 만들어야 한다.

필요한 방향:

```text
api-gateway(WebFlux)
  -> HTTP inbound/outbound span

user/product/order(WebMVC)
  -> HTTP inbound span
  -> gRPC client/server span
  -> JDBC span 또는 최소 Hikari metrics와 trace 연계
```

처음부터 모든 내부 호출을 완벽히 계측하려 하지 않는다.
1차 목표는 `api-gateway -> order-service`와 `order-service -> product-service` 경로가 하나의 trace로 이어지는지다.

### 3. Trace Export Path

앱은 Tempo에 직접 붙기보다 OpenTelemetry Collector로 보낸다.

```text
Spring Boot service
  -> OTLP HTTP/gRPC
  -> OpenTelemetry Collector
  -> Tempo
  -> Grafana Explore
```

Collector를 중간에 두는 이유:

- 앱은 collector endpoint만 알면 된다.
- sampling, attribute 정리, exporter 교체를 collector에서 처리할 수 있다.
- 나중에 Tempo 외 다른 backend로 바꿔도 앱 변경이 작다.

## 설계안

### Phase 1. Trace Contract 먼저

목표:

```text
요청 1건이 모든 서비스 로그/메트릭/trace에서 같은 식별자로 추적 가능해야 한다.
```

작업:

- API Gateway에서 request id 또는 trace id를 생성/전파
- 모든 서비스 로그에 `trace_id`, `span_id`, `service.name` 포함
- k6 요청에 `X-Test-Run-Id` 같은 헤더를 추가해 테스트 회차 구분
- Loki에서 `trace_id` 검색 가능하게 로그 포맷 확인

성공 기준:

```text
k6 주문 1건 실행
-> api-gateway 로그에서 trace_id 확인
-> order-service 로그에서 같은 trace_id 확인
```

### Phase 2. Collector + Tempo 설치

목표:

```text
trace를 받을 수 있는 플랫폼 경로를 만든다.
```

설치 경계:

| 리소스 | 관리 주체 | 이유 |
--------|-----------|------|
| Tempo Helm release | bootstrap | trace backend, 앱보다 먼저 필요 |
| OpenTelemetry Collector Helm release | bootstrap | trace ingest endpoint |
| Grafana Tempo datasource | Helm values | Grafana와 같이 배포 |
| 앱 instrumentation env | ArgoCD | 앱 Deployment 설정 |

실습 환경 기본값:

```text
Tempo: SingleBinary
Storage: 처음에는 PVC, 장기 보존 필요하면 S3 검토
Retention: 짧게
Replica: 1
Node placement: On-Demand 선호
```

운영형으로 키울 때:

```text
Tempo distributed mode
S3 backend
Collector replica 2+
sampling 정책 분리
Grafana datasource 자동 등록
```

### Phase 3. 앱 계측

목표:

```text
Spring Boot 서비스가 OTLP로 trace를 내보낸다.
```

예상 설정 방향:

```text
OTEL_SERVICE_NAME=order-service
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.monitoring.svc.cluster.local:4318
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=none 또는 prometheus 유지
OTEL_PROPAGATORS=tracecontext,baggage
```

Spring 의존성은 현재 버전에 맞춰 별도 검증이 필요하다.
Micrometer metrics와 tracing은 비슷해 보여도 다른 영역이다.

```text
micrometer-registry-prometheus = metrics
micrometer-tracing / opentelemetry exporter = traces
```

### Phase 4. Grafana 연결

목표:

```text
Grafana Explore에서 trace id로 요청 경로를 보고,
Prometheus/Loki와 함께 같은 시간대를 드릴다운한다.
```

Grafana datasource:

```text
Prometheus -> metrics
Loki       -> logs
Tempo      -> traces
```

나중에 할 수 있는 연결:

- Loki 로그 라인에서 trace id를 클릭해 Tempo trace 열기
- Prometheus exemplar에서 trace로 이동
- dashboard에서 p95 spike 구간 선택 후 trace 샘플 확인

## 검증 시나리오

### Smoke

```text
단일 주문 요청 1건
-> Grafana Tempo에서 trace 1개 확인
-> api-gateway, order-service span 확인
```

성공 기준:

- trace id가 생성된다.
- service.name이 올바르다.
- 최소 2개 이상의 서비스 span이 같은 trace에 묶인다.

### Slowness

```text
k6 주문 테스트
-> Prometheus에서 /api/v1/orders/submit p95 상승 확인
-> 해당 시간대 Loki에서 timeout/pool 로그 확인
-> Tempo에서 느린 trace를 열어 hop별 duration 확인
```

성공 기준:

- “order-service가 느리다”를 넘어서 “order-service 내부 DB transaction 전후가 느리다”처럼 좁혀진다.
- CPU가 낮고 Hikari pending이 높을 때, trace에서도 DB 주변 span이 길어지는지 확인한다.

### Failure

```text
product-service 일부 요청 실패
-> Grafana에서 error trace 확인
-> Loki에서 같은 trace_id 로그 확인
```

성공 기준:

- 에러 로그와 trace가 같은 trace_id로 연결된다.
- 실패한 요청과 느린 성공 요청을 구분할 수 있다.

## 운영 트레이드오프

### 비용

Tempo도 저장소와 CPU/memory를 먹는다.
Prometheus/Loki만으로도 모니터링 노드 비용이 커졌기 때문에 Tempo는 다음 기준을 만족할 때 추가한다.

```text
Prometheus/Loki로 병목 위치가 충분히 좁혀지지 않는다.
서비스 간 호출이 늘어나 원인 추적 시간이 길어진다.
면접/포트폴리오에서 "분산 추적까지 운영했다"는 증명이 필요하다.
```

### Sampling

모든 요청을 100% 저장하면 실습 환경에도 부담이 된다.

권장 방향:

```text
Smoke/부하테스트 중: 100% sampling
평상시: 낮은 sampling
에러/느린 요청: tail sampling 검토
```

처음부터 tail sampling을 넣으면 Collector 운영 난이도가 올라간다.
1차는 head sampling으로 단순하게 시작한다.

### 보안

Trace에는 URL, header, user id, order id 같은 민감한 정보가 섞일 수 있다.

주의:

- Authorization/JWT header 수집 금지
- 개인정보 payload attribute 금지
- trace attribute는 `service`, `route`, `status`, `duration`, `error` 중심
- Grafana/Tempo 외부 노출은 포트포워딩 또는 내부 접근부터 시작

### 장애 영향

Tracing backend가 죽어도 앱 트래픽은 죽으면 안 된다.

설계 원칙:

```text
OTLP export 실패는 앱 요청 실패로 전파하지 않는다.
Collector/Tempo는 best-effort 관측 경로다.
관측성 장애와 서비스 장애를 분리한다.
```

## 실제 검증에서 배운 점

Tempo는 설치 성공과 운영 성공을 반드시 나눠서 봐야 한다.

```text
Platform Ready:
  tempo-0 Running
  opentelemetry-collector Running
  Grafana Tempo datasource 등록
  /api/search에서 trace 검색 가능

Operational Trace Ready:
  비즈니스 요청 1건이 trace로 검색됨
  api-gateway -> product/order/user span이 같은 trace id로 연결됨
  Loki 로그에서도 같은 trace_id로 검색 가능
```

실제 확인 결과:

```text
Tempo search에서 확인된 것:
  api-gateway /actuator/health
  api-gateway /actuator/prometheus
  user-service security filter span
  product-service scheduler task span

아직 부족한 것:
  /api/v1/time-deals route trace 검색
  주문 요청 1건의 gateway -> service -> DB/Redis hop별 duration
  Loki 로그의 trace_id와 Tempo trace id 연결
```

이 상태의 해석:

```text
Tempo는 빈 껍데기가 아니다. trace ingest는 된다.
하지만 운영자가 원하는 trace가 아니라 platform noise와 background task가 먼저 보인다.
따라서 다음 작업은 "Tempo 설치"가 아니라 "비즈니스 요청 trace 품질 개선"이다.
```

추가로 발견한 설정 노이즈:

```text
OtlpMeterRegistry가 localhost:4318/v1/metrics로 metrics export를 시도하며 WARN 발생
```

이 프로젝트에서는 metrics는 Prometheus scrape가 담당한다.
OTLP는 traces 경로로 제한하고, metrics OTLP export는 끈다.

## 당장 하지 않을 것

- Tempo distributed mode
- S3 장기 보존
- tail sampling
- 전체 gRPC/JDBC 자동 계측 완성
- 외부 공개 endpoint
- Tempo 알림 체계

이것들은 Tempo 기본 경로가 안정화된 뒤 진행한다.

## 다음 작업 순서

1. 로그에 trace id가 찍히는지 현재 Spring 설정 확인
2. Spring Boot 버전에 맞는 tracing dependency 후보 검증
3. `tempo-values.yaml`, `otel-collector-values.yaml` 초안 작성
4. `bootstrap-platform.sh --only monitoring`에 Tempo 설치 추가
5. 앱 Deployment에 OTLP endpoint env 추가
6. 단일 주문 smoke trace 확인
7. k6 테스트에서 느린 요청 trace 샘플 확보

## 1차 구현 상태

2026-05-14 기준 1차 구현은 다음처럼 잡는다.

```text
Spring Boot 4 app
  -> spring-boot-starter-opentelemetry
  -> management.opentelemetry.tracing.export.otlp.endpoint
  -> opentelemetry-collector.monitoring.svc:4318/v1/traces
  -> OpenTelemetry Collector
  -> tempo.monitoring.svc:4317
  -> Grafana Tempo datasource
```

추가된 파일:

```text
infrastructure/kubernetes/monitoring/tempo-values.yaml
infrastructure/kubernetes/monitoring/otel-collector-values.yaml
```

앱 공통 환경변수:

```text
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT=http://opentelemetry-collector.monitoring.svc.cluster.local:4318/v1/traces
LOGGING_PATTERN_CORRELATION=trace_id=%X{traceId:-} span_id=%X{spanId:-}
```

주의:

```text
sampling 1.0은 검증용이다.
운영 상시값으로 쓰면 trace 저장량과 비용이 늘어난다.
```

## 다음 apply 후 검증 명령

```bash
AWS_PROFILE=goorm ./scripts/bootstrap-platform.sh --only monitoring --skip-argocd-sync
kubectl get pods -n monitoring | grep -E "tempo|opentelemetry|grafana|prometheus|loki"
kubectl get svc -n monitoring | grep -E "tempo|opentelemetry"
```

bootstrap이 끝나면 Discord에 1회 readiness 메시지를 보낸다.

```text
Tempo deployment ready
OpenTelemetry Collector deployment ready
Tempo/Collector Service ports
Spring Boot app -> collector -> tempo -> grafana trace path
```

이 알림은 "trace가 실제로 들어왔다"는 의미가 아니다.
앱 배포 전에는 backend가 trace를 보낼 수 없으므로, bootstrap 단계에서는 trace 수집 경로 준비 여부만 확인한다.

앱 배포 후:

```bash
kubectl logs -n production deploy/api-gateway --tail=50 | grep trace_id
kubectl logs -n production deploy/order-service --tail=50 | grep trace_id
```

Tempo API smoke:

```bash
kubectl port-forward -n monitoring svc/tempo 3100:3100
curl http://localhost:3100/ready
```

Grafana에서:

```text
Explore -> Tempo datasource -> Search
```

## 면접/멘토링 설명 문장

```text
Prometheus와 Loki만으로도 느림의 범위와 사건은 볼 수 있었지만,
MSA에서는 요청 한 건이 여러 서비스를 지나가기 때문에 hop별 지연을 설명하기 어렵습니다.
그래서 Tempo는 모니터링을 늘리기 위한 장식이 아니라,
"느린 성공" 요청이 gateway, order, product, DB 주변 중 어디서 시간을 썼는지 증명하기 위한 도구로 설계했습니다.
다만 비용과 운영 복잡도가 늘어나므로, 처음에는 Collector + Tempo SingleBinary + 짧은 retention으로 시작하고,
trace id 전파와 로그 연계를 먼저 검증한 뒤 확장하는 방식이 맞다고 봤습니다.
```
