-- =====================================================================
-- V11__add_course_polyline.sql
-- 목적: courses 테이블에 코스 경로/고도/시작-끝 지점 컬럼 추가.
--        지도 표시(PostGIS LineString) + 고도 단면(altitudes jsonb) + 마커 라벨.
-- =====================================================================

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS polyline geography(LineString, 4326);

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS altitudes jsonb;

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS start_name varchar(100);

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS end_name varchar(100);

CREATE INDEX IF NOT EXISTS idx_courses_polyline
    ON courses USING GIST (polyline);
