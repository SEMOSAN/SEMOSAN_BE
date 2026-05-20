-- =====================================================================
-- V4__fix_mountain_image_url_https.sql
-- 목적: V3 시드의 산림청 이미지 URL(http) 를 https 로 일괄 치환.
--        iOS ATS / 브라우저 mixed content 차단 회피.
-- 대상: forest.go.kr 도메인의 http URL 만 (관악·북한·도봉 9건).
--        위키미디어(청계·수락·인왕·아차) 는 이미 https 라 무영향.
-- 멱등: WHERE 절이 http 패턴만 잡으므로 재실행해도 noop.
-- =====================================================================

UPDATE mountains
SET image_urls = REPLACE(image_urls::text, 'http://www.forest.go.kr', 'https://www.forest.go.kr')::jsonb
WHERE image_urls::text LIKE '%http://www.forest.go.kr%';
