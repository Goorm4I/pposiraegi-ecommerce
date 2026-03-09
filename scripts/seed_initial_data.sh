#!/bin/bash
# =========================================================================
# 초기 데이터 시딩 스크립트 (seed_initial_data.sh)
# 용도: EC2 서버 배포 후 초기 카테고리, 상품 10개, 타임딜, 유저를 생성합니다.
# 주의: 기존 데이터(products, time_deals, categories)가 삭제됩니다!
# =========================================================================

# 대상 서버 정보 (필요시 수정)
EC2_IP="54.206.87.196"
DB_CONTAINER="pposiraegi-db"
DB_USER="user"
DB_NAME="ecommerce"
TEMP_KEY="/tmp/ec2-temp-key-seed"

echo "=========================================="
echo "🌱 PPosiraegi 초기 데이터 시딩 스크립트"
echo "대상 서버: $EC2_IP"
echo "=========================================="

echo "[1/4] 임시 SSH 키 생성 중..."
rm -f $TEMP_KEY $TEMP_KEY.pub
ssh-keygen -t rsa -f $TEMP_KEY -N "" -q

echo "[2/4] AWS EC2 Instance Connect를 통한 키 등록..."
aws ec2-instance-connect send-ssh-public-key \
  --instance-id i-0c5486f6b3e201db2 \
  --instance-os-user ec2-user \
  --ssh-public-key file://$TEMP_KEY.pub \
  --profile goorm --region ap-southeast-2 > /dev/null

echo "[3/4] 시딩용 SQL 쿼리 생성 중..."
cat << 'EOF' > /tmp/seed_temp.sql
-- 기존 데이터 정리 (자식 레코드부터 삭제)
DELETE FROM time_deals;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users WHERE email IN ('test@pposiraegi.com', 'admin@pposiraegi.com');

-- 카테고리 (depth, display_order 포함)
INSERT INTO categories (id, name, display_order, depth, created_at) VALUES
(818676099583051051, '반려견용품', 1, 1, NOW()),
(818676099583051052, '반려묘용품', 2, 1, NOW()),
(818676099583051053, '건강/영양', 3, 1, NOW());

-- 상품 10개 (average_rating, review_count 포함)
INSERT INTO products (id, category_id, name, description, brand_name, origin_price, sale_price, thumbnail_url, status, average_rating, review_count) VALUES
(818676112581201472, 818676099583051051, '프리미엄 강아지 사료 2kg', '수의사 추천', '뽀시래기', 45000, 31500, 'https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201473, 818676099583051052, '고양이 캣타워', '3단 원목', '펫하우스', 89000, 62300, 'https://images.unsplash.com/photo-1545249390-6bdfa286032f?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201474, 818676099583051053, '반려동물 영양제', '관절 건강', '펫케어', 35000, 24500, 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201475, 818676099583051051, '강아지 덴탈껌 50p', '치석 제거에 탁월한 덴탈껌', '뽀시래기', 25000, 15000, 'https://images.unsplash.com/photo-1601758174114-e711c0cbaa69?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201476, 818676099583051052, '고양이 두부모래 10L', '먼지 없는 친환경 모래', '캣츠존', 22000, 16500, 'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201477, 818676099583051051, '강아지 친환경 풉백 10롤', '자연분해 배변봉투', '에코독', 12000, 8900, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201478, 818676099583051052, '고양이 깃털 낚시대', '스트레스 해소용 장난감', '플레이캣', 8500, 5000, 'https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201479, 818676099583051053, '반려동물 종합 유산균 30포', '장 건강 지킴이', '펫케어', 40000, 28000, 'https://images.unsplash.com/photo-1623366302587-b25fcccb1cbd?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201480, 818676099583051051, '강아지 편안한 하네스 L', '기도 압박 없는 디자인', '도그워크', 32000, 24000, 'https://images.unsplash.com/photo-1605891780491-bdf02d7335ce?w=400', 'FOR_SALE', 0.0, 0),
(818676112581201481, 818676099583051052, '고양이 원형 스크래쳐', '라운지형 스크래쳐', '펫하우스', 18000, 12000, 'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=400', 'FOR_SALE', 0.0, 0);

-- 타임딜 10개 (한국 시간 KST +9시간을 고려하여 DB에는 UTC 기준으로 넉넉히 주입)
-- start_time: 프론트(KST)에서 무조건 시작된 것으로 보이게 +8.5시간
-- end_time: 프론트(KST)에서 무조건 5시간 뒤로 보이게 +14시간
INSERT INTO time_deals (id, product_id, start_time, end_time, total_quantity, remain_quantity, status, created_at) VALUES
(818676112606367113, 818676112581201472, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 100, 87, 'ACTIVE', NOW()),
(818676112606367114, 818676112581201473, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 50, 42, 'ACTIVE', NOW()),
(818676112606367115, 818676112581201474, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 200, 200, 'ACTIVE', NOW()),
(818676112606367116, 818676112581201475, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 300, 150, 'ACTIVE', NOW()),
(818676112606367117, 818676112581201476, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 80, 75, 'ACTIVE', NOW()),
(818676112606367118, 818676112581201477, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 500, 490, 'ACTIVE', NOW()),
(818676112606367119, 818676112581201478, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 150, 20, 'ACTIVE', NOW()),
(818676112606367120, 818676112581201479, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 120, 120, 'ACTIVE', NOW()),
(818676112606367121, 818676112581201480, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 60, 45, 'ACTIVE', NOW()),
(818676112606367122, 818676112581201481, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 250, 240, 'ACTIVE', NOW());

-- 테스트 유저 2명 (비밀번호: test1234)
INSERT INTO users (id, email, password_hash, nickname, name, phone_number, status, created_at) VALUES
(818690914036744266, 'test@pposiraegi.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBftN0lMhJKr1P7a.A8.OQdLXe', '테스터', '김테스트', '010-1234-5678', 'ACTIVE', NOW()),
(818690914036744267, 'admin@pposiraegi.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBftN0lMhJKr1P7a.A8.OQdLXe', '관리자', '이관리', '010-9876-5432', 'ACTIVE', NOW());

EOF

echo "[4/4] SQL 스크립트 전송 및 원격 실행 중..."
scp -o StrictHostKeyChecking=no -i $TEMP_KEY /tmp/seed_temp.sql ec2-user@$EC2_IP:/tmp/seed_initial_data.sql > /dev/null
ssh -o StrictHostKeyChecking=no -i $TEMP_KEY ec2-user@$EC2_IP "docker exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < /tmp/seed_initial_data.sql" > /dev/null

echo "=========================================="
echo "🎉 시딩 완료! 프론트엔드에서 정상적으로 10개의 상품과 타임딜이 조회되어야 합니다."
echo "=========================================="
rm -f /tmp/seed_temp.sql $TEMP_KEY $TEMP_KEY.pub
