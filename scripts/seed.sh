#!/bin/bash
# =============================================================
# 뽀시래기 타임딜 MSA - DB 시드 스크립트
# 사용법: ./scripts/seed.sh [API_BASE_URL]
# 예시:   ./scripts/seed.sh http://localhost:8080
#         ./scripts/seed.sh http://pposiraegi-alb-1045768515.ap-northeast-2.elb.amazonaws.com
# =============================================================
set -e

API_BASE="${1:-http://localhost:8080}"
echo "대상 서버: $API_BASE"
echo ""

# 날짜 계산
SOON=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(seconds=10)).strftime('%Y-%m-%dT%H:%M:%S'))")
PLUS_1H=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(hours=1)).strftime('%Y-%m-%dT%H:%M:%S'))")
PLUS_2H=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(hours=2)).strftime('%Y-%m-%dT%H:%M:%S'))")
PLUS_3H=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(hours=3)).strftime('%Y-%m-%dT%H:%M:%S'))")
PLUS_6H=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(hours=6)).strftime('%Y-%m-%dT%H:%M:%S'))")
PLUS_12H=$(python3 -c "from datetime import datetime, timedelta; print((datetime.now() + timedelta(hours=12)).strftime('%Y-%m-%dT%H:%M:%S'))")

call_api() {
  local method=$1
  local path=$2
  local body=$3
  local response
  response=$(curl -s -w "\n%{http_code}" -X "$method" "$API_BASE$path" \
    -H "Content-Type: application/json" \
    -d "$body")
  local http_code=$(echo "$response" | tail -1)
  local body_resp=$(echo "$response" | sed '$d')
  if [ "$http_code" -lt 200 ] || [ "$http_code" -ge 300 ]; then
    echo "[ERROR] $method $path → HTTP $http_code" >&2
    echo "$body_resp" >&2
    exit 1
  fi
  echo "$body_resp"
}

extract_id() {
  echo "$1" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['id'])"
}

echo "========================================"
echo "1단계: 카테고리 생성"
echo "========================================"

CAT_DOG=$(call_api POST /api/v1/categories '{"name":"반려견용품","displayOrder":1}')
CAT_DOG_ID=$(extract_id "$CAT_DOG")
echo "반려견용품 카테고리 ID: $CAT_DOG_ID"

CAT_CAT=$(call_api POST /api/v1/categories '{"name":"반려묘용품","displayOrder":2}')
CAT_CAT_ID=$(extract_id "$CAT_CAT")
echo "반려묘용품 카테고리 ID: $CAT_CAT_ID"

CAT_ETC=$(call_api POST /api/v1/categories '{"name":"공용용품","displayOrder":3}')
CAT_ETC_ID=$(extract_id "$CAT_ETC")
echo "공용용품 카테고리 ID: $CAT_ETC_ID"

echo ""
echo "========================================"
echo "2단계: 타임딜 + 상품 생성"
echo "========================================"

# 1. 강아지 유기농 사료 (진행중)
echo "[1/5] 강아지 유기농 사료..."
call_api POST /api/v1/admin/time-deals/with-product "$(cat <<EOF
{
  "product": {
    "categoryId": $CAT_DOG_ID,
    "name": "수의사가 직접 설계한 유기농 강아지 사료 2kg",
    "description": "100% 유기농 인증 원료만 사용 (USDA 인증). 수의사 10인 공동 설계로 영양 밸런스 최적화. 인공 방부제·착색제·향미료 無. 소화 흡수율 92% 이상.",
    "brandName": "뽀시래기 프리미엄",
    "originPrice": 50000,
    "salePrice": 25000,
    "status": "FOR_SALE",
    "images": [
      {"imageUrl": "https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=800&h=800&fit=crop&q=80", "imageType": "THUMBNAIL", "displayOrder": 1},
      {"imageUrl": "https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?w=800&h=800&fit=crop&q=80", "imageType": "DETAIL", "displayOrder": 2}
    ],
    "optionGroups": [],
    "skus": [
      {"skuCode": "DOG-FOOD-001", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 100, "selectedOptionValues": []}
    ]
  },
  "dealQuantity": 100,
  "startTime": "$SOON",
  "endTime": "$PLUS_1H"
}
EOF
)" > /dev/null
echo "완료"

# 2. 5단 캣타워 (진행중)
echo "[2/5] 5단 캣타워..."
call_api POST /api/v1/admin/time-deals/with-product "$(cat <<EOF
{
  "product": {
    "categoryId": $CAT_CAT_ID,
    "name": "고양이가 하루 종일 떠나지 않는 5단 캣타워",
    "description": "스크래칭·점프·낮잠 올인원. 천연 사이잘삼 스크래처 내장. 흔들림 없는 4중 안전 고정 시스템. 유럽 CE 안전 인증 획득.",
    "brandName": "캣라이프",
    "originPrice": 150000,
    "salePrice": 89000,
    "status": "FOR_SALE",
    "images": [
      {"imageUrl": "https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=800&h=800&fit=crop&q=80", "imageType": "THUMBNAIL", "displayOrder": 1}
    ],
    "optionGroups": [
      {"optionName": "색상", "optionsValues": ["베이지", "그레이"]}
    ],
    "skus": [
      {"skuCode": "CAT-TOWER-BEG", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 30, "selectedOptionValues": ["베이지"]},
      {"skuCode": "CAT-TOWER-GRY", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 20, "selectedOptionValues": ["그레이"]}
    ]
  },
  "dealQuantity": 50,
  "startTime": "$SOON",
  "endTime": "$PLUS_2H"
}
EOF
)" > /dev/null
echo "완료"

# 3. 강아지 수제 간식 (진행중)
echo "[3/5] 강아지 수제 간식..."
call_api POST /api/v1/admin/time-deals/with-product "$(cat <<EOF
{
  "product": {
    "categoryId": $CAT_DOG_ID,
    "name": "국내산 닭가슴살 100% 강아지 수제 간식 500g",
    "description": "무방부제·무첨가물 수제 간식. 국내산 닭가슴살만 사용. 저온 건조로 영양소 보존. 소형견~대형견 모두 가능.",
    "brandName": "뽀시래기 키친",
    "originPrice": 28000,
    "salePrice": 15000,
    "status": "FOR_SALE",
    "images": [
      {"imageUrl": "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=800&h=800&fit=crop&q=80", "imageType": "THUMBNAIL", "displayOrder": 1}
    ],
    "optionGroups": [],
    "skus": [
      {"skuCode": "DOG-SNACK-001", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 200, "selectedOptionValues": []}
    ]
  },
  "dealQuantity": 200,
  "startTime": "$SOON",
  "endTime": "$PLUS_3H"
}
EOF
)" > /dev/null
echo "완료"

# 4. 고양이 그레인프리 사료 (오픈 예정)
echo "[4/5] 고양이 그레인프리 사료 (오픈 예정)..."
call_api POST /api/v1/admin/time-deals/with-product "$(cat <<EOF
{
  "product": {
    "categoryId": $CAT_CAT_ID,
    "name": "수입 프리미엄 그레인프리 고양이 사료 1.5kg",
    "description": "곡물 無, 단백질 40% 이상. 뉴질랜드산 양고기·연어 원료. 비뇨기 건강에 최적화된 미네랄 밸런스. 비만묘 체중 관리 가능.",
    "brandName": "키위펫",
    "originPrice": 65000,
    "salePrice": 39000,
    "status": "FOR_SALE",
    "images": [
      {"imageUrl": "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&h=800&fit=crop&q=80", "imageType": "THUMBNAIL", "displayOrder": 1}
    ],
    "optionGroups": [],
    "skus": [
      {"skuCode": "CAT-FOOD-001", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 80, "selectedOptionValues": []}
    ]
  },
  "dealQuantity": 80,
  "startTime": "$PLUS_1H",
  "endTime": "$PLUS_6H"
}
EOF
)" > /dev/null
echo "완료"

# 5. IoT 스마트 자동급식기 (오픈 예정)
echo "[5/5] IoT 스마트 자동급식기 (오픈 예정)..."
call_api POST /api/v1/admin/time-deals/with-product "$(cat <<EOF
{
  "product": {
    "categoryId": $CAT_ETC_ID,
    "name": "앱 연동 IoT 스마트 자동급식기 | 강아지·고양이 겸용",
    "description": "스마트폰 앱으로 언제 어디서나 급식 제어. 1회~10회 급식 스케줄 설정 가능. 최대 3L 대용량 저장 탱크. 음성 녹음 기능으로 반려동물 호출 가능.",
    "brandName": "스마트펫",
    "originPrice": 120000,
    "salePrice": 72000,
    "status": "FOR_SALE",
    "images": [
      {"imageUrl": "https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&h=800&fit=crop&q=80", "imageType": "THUMBNAIL", "displayOrder": 1}
    ],
    "optionGroups": [
      {"optionName": "용량", "optionsValues": ["1.5L", "3L"]}
    ],
    "skus": [
      {"skuCode": "FEEDER-1.5L", "status": "AVAILABLE", "additionalPrice": 0, "stockQuantity": 30, "selectedOptionValues": ["1.5L"]},
      {"skuCode": "FEEDER-3L", "status": "AVAILABLE", "additionalPrice": 20000, "stockQuantity": 20, "selectedOptionValues": ["3L"]}
    ]
  },
  "dealQuantity": 50,
  "startTime": "$PLUS_2H",
  "endTime": "$PLUS_12H"
}
EOF
)" > /dev/null
echo "완료"

echo ""
echo "========================================"
echo "시드 완료! 총 5개 타임딜 등록됨"
echo "  - ACTIVE (진행중): 3개"
echo "  - UPCOMING (예정): 2개"
echo ""
echo "확인: $API_BASE/api/v1/time-deals"
echo "========================================"
