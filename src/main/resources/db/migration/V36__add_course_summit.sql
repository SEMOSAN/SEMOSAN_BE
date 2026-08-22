-- =====================================================================
-- V36__add_course_summit.sql
-- 목적: 관리자가 코스 waypoint 중에서 선택해 반영하는 코스별 정상 좌표 저장.
--        미반영 코스는 모두 null.
-- =====================================================================

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS summit_lat double precision;
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS summit_lng double precision;
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS summit_ele double precision;
