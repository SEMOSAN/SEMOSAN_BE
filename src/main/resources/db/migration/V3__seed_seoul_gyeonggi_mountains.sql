-- =====================================================================
-- V3__seed_seoul_gyeonggi_mountains.sql
-- 목적: 서울 친숙도가 높은 산 7개 초기 시드.
--        BBox/추천/지도 API 의 기본 테스트 데이터.
-- 출처:
--   - 관악산/북한산/도봉산: 산림청 100대 명산 데이터 (mountain_master/_all.json)
--     image_urls: 산림청 공식 이미지(forest.go.kr).
--   - 청계산/수락산/인왕산/아차산: 100대 명산 외.
--     좌표/고도: OSM peak 데이터(peaks_kr.json) 서울권 정상부.
--     주소/소요시간/난이도: 네이버 등산 코스 데이터 평균/최빈값.
--     image_urls: 위키미디어 커먼즈 (Wikipedia 한국어 인포박스/본문 이미지).
-- difficulty 산정: 코스 난이도(1/2/3) 분포의 가중평균 → 1=EASY, 2=NORMAL, 3=HARD.
-- duration: 코스별 소요시간(분) 평균.
-- =====================================================================

INSERT INTO mountains (created_at, updated_at, name, address, altitude, difficulty, duration, latitude, longitude, image_urls) VALUES
  (now(), now(), '관악산', '서울특별시 관악구, 경기도 안양시, 과천시', 629.0, 'NORMAL', 119, 37.445044, 126.964223, '["http://www.forest.go.kr/images/data/down/mountain/20000059_3.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000059_1.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000059_2.jpg"]'::jsonb),
  (now(), now(), '북한산', '서울특별시 강북구ㆍ성북구ㆍ종로구ㆍ은평구, 경기도 고양시ㆍ양주시', 837.0, 'NORMAL', 106, 37.658657, 126.978056, '["http://www.forest.go.kr/images/data/down/mountain/20000317_3.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000317_1.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000317_2.jpg"]'::jsonb),
  (now(), now(), '도봉산', '서울특별시 도봉구, 경기도 의정부시 호원동ㆍ양주시 장흥면', 740.0, 'NORMAL', 107, 37.69883, 127.01547, '["http://www.forest.go.kr/images/data/down/mountain/20000155_3.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000155_2.jpg", "http://www.forest.go.kr/images/data/down/mountain/20000155_1.jpg"]'::jsonb),
  (now(), now(), '청계산', '서울특별시 서초구, 경기도 과천시ㆍ성남시', 618.0, 'NORMAL', 143, 37.421899, 127.0432171, '["https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Maebawi_of_Cheong_gye_Mt_Seoul.JPG/330px-Maebawi_of_Cheong_gye_Mt_Seoul.JPG", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/%EC%B2%AD%EA%B3%84%EC%82%B0%28AMJ%29.jpg/250px-%EC%B2%AD%EA%B3%84%EC%82%B0%28AMJ%29.jpg"]'::jsonb),
  (now(), now(), '수락산', '서울특별시 노원구, 경기도 의정부시', 638.0, 'NORMAL', 91, 37.6992644, 127.0813401, '["https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Suraksan.JPG/330px-Suraksan.JPG"]'::jsonb),
  (now(), now(), '인왕산', '서울특별시 종로구ㆍ서대문구', 339.0, 'NORMAL', 49, 37.5849526, 126.957877, '["https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/Mount_Inwang.jpg/330px-Mount_Inwang.jpg", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Korea-Seoul-Inwangsan-12.jpg/120px-Korea-Seoul-Inwangsan-12.jpg", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Korea-Seoul-Inwangsan-28.jpg/120px-Korea-Seoul-Inwangsan-28.jpg"]'::jsonb),
  (now(), now(), '아차산', '서울특별시 광진구', 295.0, 'NORMAL', 46, 37.5711235, 127.1043503, '["https://upload.wikimedia.org/wikipedia/commons/thumb/8/81/AchasanPost.jpg/330px-AchasanPost.jpg", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/AchasanRuins.jpg/250px-AchasanRuins.jpg", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Achasan_GeneralOndal.jpg/250px-Achasan_GeneralOndal.jpg"]'::jsonb);
