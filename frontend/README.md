# timedeal-order

뽀시래기 타임딜 — 주문서 페이지 프론트엔드

기존 `timedeal-frontend`의 결제 플로우를 독립 페이지 기반으로 재설계한 레포입니다.
MSA 전환 시 order-service와 1:1 연동을 고려한 구조로 구성되어 있습니다.

---

## 결제 흐름

```
상품 상세 (/deal/:id)
    ↓ 구매하기
주문서 (/order/checkout/:id)       ← 이 레포의 핵심
  - 배송지 확인/변경
  - 결제 수단 선택
  - 최종 금액 확인
    ↓ 결제하기 → createOrder (Redis DECR, 재고 선점)
PG 결제 (PGSimulator)
  - 뽀시페이: 핀 입력 (주문서에서 이미 확인 완료)
  - 카카오/토스: 앱 연결 시뮬레이션
  - 신용카드: Mock PG 서버 실제 호출
    ↓ payOrder (결제 확정)
결과 (/order/:id)
```

## 백엔드 API 예시

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/orders` | 주문 생성 + Redis DECR (재고 선점) |
| `POST` | `/api/orders/{orderId}/pay` | 결제 확정 (백엔드 → PG 호출) |
| `GET`  | `/api/orders/{orderId}` | 주문 상태 조회 |

## Mock PG 서버

신용카드 결제는 실제 GCP Cloud Run에 배포된 Mock PG 서버와 통신합니다.

```
POST /mock-pg/pay   →   (CRA 프록시 경유)   →   GCP Mock PG
```

개발 환경에서는 `src/setupProxy.js`가 CORS를 우회합니다.

## 환경변수

```bash
# .env.example 복사 후 수정
cp .env.example .env
```

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `REACT_APP_USE_MOCK` | `true` | Mock 모드 여부 |
| `REACT_APP_API_URL` | `http://localhost:8080` | 백엔드 API 주소 |

## 실행

```bash
npm install
npm start       # 개발 서버 (localhost:3000)
npm run build   # 프로덕션 빌드
```

## 향후 계획

- 포인트 시스템 연동 (주문서 페이지 포인트 적용 UI)
- 주문 목록 조회 (마이페이지)
- MSA 전환 시 order-service 독립 배포 대응
