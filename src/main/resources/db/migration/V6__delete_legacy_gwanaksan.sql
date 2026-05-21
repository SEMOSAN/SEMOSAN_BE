-- =====================================================================
-- V6__delete_legacy_gwanaksan.sql
-- 목적: V3 시드 적용 전 운영 DB 에 hand-seed 되어있던 옛 관악산 row 정리.
--        해당 row 좌표(37.4331, 126.9634) 와 V3 시드 관악산(37.445044, 126.964223) 이
--        다른 row 로 공존해 지도 API 응답에 관악산이 2개로 나오는 문제 발생.
-- 안전성: 좌표 기준으로 명시적 식별 — V3 시드 row 는 좌표가 달라 영향받지 않음.
-- 멱등: 해당 좌표 row 가 없는 환경에선 noop (예: 로컬, 신규 환경).
-- =====================================================================
DO $$
DECLARE v_mountain_id bigint;
BEGIN
    SELECT id INTO v_mountain_id
    FROM mountains
    WHERE name = '관악산'
      AND latitude = 37.4331
      AND longitude = 126.9634;

    IF v_mountain_id IS NULL THEN
        RETURN;
    END IF;

    DELETE FROM reviews         WHERE mountain_id = v_mountain_id;
    DELETE FROM mountain_likes  WHERE mountain_id = v_mountain_id;
    DELETE FROM amenities       WHERE mountain_id = v_mountain_id;
    DELETE FROM transportations WHERE mountain_id = v_mountain_id;
    DELETE FROM restaurants     WHERE section_id IN (SELECT id FROM restaurant_sections WHERE mountain_id = v_mountain_id);
    DELETE FROM restaurant_sections WHERE mountain_id = v_mountain_id;
    DELETE FROM hiking_records  WHERE course_id IN (SELECT id FROM courses WHERE mountain_id = v_mountain_id);
    DELETE FROM courses         WHERE mountain_id = v_mountain_id;
    DELETE FROM mountains       WHERE id = v_mountain_id;
END $$;
