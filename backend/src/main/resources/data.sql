-- 카테고리 (depth, display_order 포함)
INSERT INTO categories (id, name, display_order, depth, created_at) VALUES
(818676099583051051, '반려견용품', 1, 1, NOW()),
(818676099583051052, '반려묘용품', 2, 1, NOW()),
(818676099583051053, '건강/영양', 3, 1, NOW())
ON CONFLICT (id) DO NOTHING;

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
(818676112581201481, 818676099583051052, '고양이 원형 스크래쳐', '라운지형 스크래쳐', '펫하우스', 18000, 12000, 'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=400', 'FOR_SALE', 0.0, 0)
ON CONFLICT (id) DO NOTHING;

-- 타임딜 10개
-- 앱이 구동될 때마다 시간이 부족하면 연장되도록 할 수도 있지만, 
-- 일단 ON CONFLICT (id) DO NOTHING 을 쓰면 기존 레코드가 있으면 무시됨.
-- 그러나 타임딜의 '상태'나 '시간'을 갱신하려면 DO UPDATE SET 구문을 사용하는 것이 실무에서 더 유용함.
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
(818676112606367122, 818676112581201481, NOW() + INTERVAL '8 HOURS 30 MINUTES', NOW() + INTERVAL '14 HOURS', 250, 240, 'ACTIVE', NOW())
ON CONFLICT (id) DO UPDATE SET
  start_time = EXCLUDED.start_time,
  end_time = EXCLUDED.end_time,
  status = EXCLUDED.status;
-- 타임딜은 서버가 뜰 때마다 만료되지 않게 시간 연장을 덮어씌움 (기존 남은 수량은 보존됨)

-- 테스트 유저 2명 (비밀번호: test1234)
INSERT INTO users (id, email, password_hash, nickname, name, phone_number, status, created_at) VALUES
(818690914036744266, 'test@pposiraegi.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBftN0lMhJKr1P7a.A8.OQdLXe', '테스터', '김테스트', '010-1234-5678', 'ACTIVE', NOW()),
(818690914036744267, 'admin@pposiraegi.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBftN0lMhJKr1P7a.A8.OQdLXe', '관리자', '이관리', '010-9876-5432', 'ACTIVE', NOW())
ON CONFLICT (email) DO NOTHING;
