// src/mocks/timedeals.js
// 모든 이미지: Unsplash 무료 라이선스 (https://unsplash.com/license)
// 상업적 이용 가능, 저작권 무료

const now = new Date();
const getTime = (minutesFromNow) => new Date(now.getTime() + minutesFromNow * 60 * 1000).toISOString();

// discountRate 자동 계산 헬퍼
const calcRate = (original, discount) => Math.round((1 - discount / original) * 100);

// Unsplash CDN 헬퍼
const u = (slug, w = 800) => `https://images.unsplash.com/${slug}?w=${w}&h=${w}&fit=crop&q=80`;

export const mockTimeDeals = [
  {
    id: 1,
    productName: "수의사가 직접 설계한 유기농 강아지 사료 2kg | 우리 아이 첫 번째 건강 선물",
    productImage: u("photo-1589924691995-400dc9ecc119"),
    images: [
      u("photo-1589924691995-400dc9ecc119"),
      u("photo-1568640347023-a616a30bc3bd"),
      u("photo-1450778869180-41d0601e046e"),
      u("photo-1587300003388-59208cc962cb"),
    ],
    features: [
      "• 100% 유기농 인증 원료만 사용 (USDA 인증)",
      "• 수의사 10인 공동 설계 — 영양 밸런스 최적화",
      "• 인공 방부제·착색제·향미료 無",
      "• 소화 흡수율 92% 이상 (일반 사료 대비 +30%)",
      "• 알러지 유발 주요 성분 7종 완전 배제",
    ],
    originalPrice: 50000, discountPrice: 25000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 51, totalStock: 100,
    startTime: getTime(-30), endTime: getTime(30), status: "ACTIVE",
  },
  {
    id: 2,
    productName: "고양이가 하루 종일 떠나지 않는 5단 캣타워 | 스크래칭·점프·낮잠 올인원",
    productImage: u("photo-1519052537078-e6302a4968d4"),
    images: [
      u("photo-1519052537078-e6302a4968d4"),
      u("photo-1543852786-1cf6624b9987"),
      u("photo-1514888286974-6c03e2ca1dba"),
    ],
    features: [
      "• 5단 구조로 점프·등반·휴식 모두 해결",
      "• 천연 사이잘 마 스크래처 내장 — 발톱 건강 케어",
      "• 흔들림 방지 넓은 베이스 플레이트",
      "• 세탁 가능한 탈부착 쿠션 포함",
      "• 조립 시간 10분 이내, 공구 불필요",
    ],
    originalPrice: 35000, discountPrice: 14000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 2, totalStock: 50,
    startTime: getTime(-60), endTime: getTime(30), status: "ACTIVE",
  },
  {
    id: 3,
    productName: "고양이 본능을 깨우는 천연 사이잘 스크래쳐 | 소파 파괴 방지 보장",
    productImage: u("photo-1601758228041-f3b2795255f1"),
    images: [
      u("photo-1601758228041-f3b2795255f1"),
      u("photo-1548802673-380ab8ebc7b7"),
      u("photo-1526336024174-e58f5cdd8e13"),
    ],
    features: [
      "• 100% 천연 사이잘 마 소재 — 발톱 관리 + 놀이 동시에",
      "• 수평·수직 겸용 디자인",
      "• 미끄럼 방지 고무 바닥 처리",
      "• 고양이 선호도 테스트 98% 통과",
      "• 환경 친화적 천연 소재, 냄새 無",
    ],
    originalPrice: 28000, discountPrice: 11200,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 0, totalStock: 30,
    startTime: getTime(-120), endTime: getTime(-60), status: "SOLDOUT",
  },
  {
    id: 4,
    productName: "외출해도 걱정 없는 스마트 자동 급식기 | 앱으로 밥 주는 시대",
    productImage: u("photo-1574158622682-e40e69881006"),
    images: [
      u("photo-1574158622682-e40e69881006"),
      u("photo-1559715745-e1b33a271c8f"),
      u("photo-1573865526739-10659fec78a5"),
    ],
    features: [
      "• 스마트폰 앱으로 원격 급여 시간·양 설정",
      "• 최대 6회/일 예약 급여 가능",
      "• 2.5L 대용량 탱크 — 최대 7일치 사료 보관",
      "• 사료 잔량 알림 기능 내장",
      "• 정전 시 배터리 자동 전환 (AA 건전지 3개)",
    ],
    originalPrice: 89000, discountPrice: 44500,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 12, totalStock: 30,
    startTime: getTime(-60), endTime: getTime(60), status: "ACTIVE",
  },
  {
    id: 5,
    productName: "항공사 승인 완료! 어디든 함께하는 반려동물 이동가방 | 통기성·안전성 모두 잡았어요",
    productImage: u("photo-1583511655857-d19b40a7a54e"),
    images: [
      u("photo-1583511655857-d19b40a7a54e"),
      u("photo-1583511655826-05700d52f4d9"),
      u("photo-1548199973-03cce0bbc87b"),
    ],
    features: [
      "• 국제선 기내 반입 승인 규격 (IATA 기준 충족)",
      "• 6면 메쉬 통기 구조 — 산소 공급 원활",
      "• 탈출 방지 이중 잠금 지퍼",
      "• 세탁기 세탁 가능한 내부 패드 포함",
      "• 최대 7kg 반려동물 수용 가능",
    ],
    originalPrice: 65000, discountPrice: 32500,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 8, totalStock: 20,
    startTime: getTime(-45), endTime: getTime(15), status: "ACTIVE",
  },
  {
    id: 6,
    productName: "살롱보다 집에서! 수의사 추천 펫 스파 세트 | 목욕이 즐거워지는 마법",
    productImage: u("photo-1581888227599-779811939961"),
    images: [
      u("photo-1581888227599-779811939961"),
      u("photo-1516734212186-a967f81ad0d7"),
      u("photo-1591946614720-90a587da4a36"),
    ],
    features: [
      "• 저자극 약산성 샴푸 — 피부 pH 균형 유지",
      "• 실리콘 마사지 브러쉬 동봉 — 혈액순환 촉진",
      "• 향균·탈취 컨디셔너 포함",
      "• 수의사 피부 안전성 임상 테스트 완료",
      "• 모든 견종·묘종 사용 가능",
    ],
    originalPrice: 42000, discountPrice: 21000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 25, totalStock: 40,
    startTime: getTime(-20), endTime: getTime(70), status: "ACTIVE",
  },
  {
    id: 7,
    productName: "노견·소형견을 위한 4단 강아지 계단 | 소파·침대 오르내림이 이렇게 편할 줄이야",
    productImage: u("photo-1537151608828-ea2b11777ee8"),
    images: [
      u("photo-1537151608828-ea2b11777ee8"),
      u("photo-1544568100-847a948585b9"),
      u("photo-1587300003388-59208cc962cb"),
    ],
    features: [
      "• 관절 부담 최소화 완만한 경사 설계",
      "• 미끄럼 방지 카펫 소재 — 발바닥 보호",
      "• 최대 15kg 하중 지지",
      "• 접이식 구조 — 보관·이동 간편",
      "• 노견·슬개골 주의견 수의사 추천 제품",
    ],
    originalPrice: 38000, discountPrice: 19000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 30, totalStock: 50,
    startTime: getTime(-15), endTime: getTime(90), status: "ACTIVE",
  },
  {
    id: 8,
    productName: "양치가 두렵지 않아요 | 강아지도 좋아하는 닭고기향 실리콘 칫솔 세트",
    productImage: u("photo-1558788353-f76d92427f16"),
    images: [
      u("photo-1558788353-f76d92427f16"),
      u("photo-1601758124510-52d02ddb7cbd"),
      u("photo-1587300003388-59208cc962cb"),
    ],
    features: [
      "• 식용 등급 실리콘 소재 — 삼켜도 안전",
      "• 천연 닭고기향 치약 포함 — 거부감 90% 감소",
      "• 손가락 끼움형 + 긴 손잡이형 2종 구성",
      "• 치석·구취 케어에 특화된 브러쉬 형태",
      "• 수의사·반려인 1,200명 실사용 후기 4.8점",
    ],
    originalPrice: 22000, discountPrice: 9900,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 60, totalStock: 80,
    startTime: getTime(-10), endTime: getTime(110), status: "ACTIVE",
  },
  {
    id: 9,
    productName: "고양이가 직접 고른 듯한 코르크 스크래쳐 하우스 | 숨숨집+스크래쳐 2in1",
    productImage: u("photo-1561948955-570b270e7c36"),
    images: [
      u("photo-1561948955-570b270e7c36"),
      u("photo-1526336024174-e58f5cdd8e13"),
      u("photo-1573865526739-10659fec78a5"),
    ],
    features: [
      "• 천연 코르크 소재 — 발톱 관리 + 놀이 동시에",
      "• 숨숨집 구조로 스트레스 해소 공간 제공",
      "• 독성 없는 친환경 접착제 사용",
      "• 고양이 행동학 전문가 설계",
      "• 분리형 구조로 부분 교체 가능",
    ],
    originalPrice: 45000, discountPrice: 18000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 15, totalStock: 25,
    startTime: getTime(5), endTime: getTime(150), status: "UPCOMING",
  },
  {
    id: 10,
    productName: "산책이 여행이 되는 순간 | 대형견도 OK! 접이식 프리미엄 개모차",
    productImage: u("photo-1477884213360-7e9d7dcc1e48"),
    images: [
      u("photo-1477884213360-7e9d7dcc1e48"),
      u("photo-1501820488136-72669149e0d4"),
      u("photo-1444212477490-ca407925329e"),
    ],
    features: [
      "• 최대 25kg 대형견까지 수용 가능",
      "• 원터치 접이식 — 트렁크 적재 5초",
      "• 360도 회전 앞바퀴 + 잠금장치",
      "• UV 차단 썬루프 & 방수 원단",
      "• 하부 수납공간 + 컵홀더 포함",
    ],
    originalPrice: 120000, discountPrice: 72000,
    get discountRate() { return calcRate(this.originalPrice, this.discountPrice); },
    stock: 10, totalStock: 15,
    startTime: getTime(60), endTime: getTime(180), status: "UPCOMING",
  },
];

export default mockTimeDeals;
