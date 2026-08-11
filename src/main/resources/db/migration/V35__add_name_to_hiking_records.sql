-- =====================================================================
-- V35__add_name_to_hiking_records.sql
-- 목적: 자유기록에 사용자가 정한 이름을 저장할 컬럼 추가.
--        코스 기록은 courses.name 으로 표시되지만 자유기록은 course_id 가 null 이라
--        목록에서 이름 자리가 비어 보였다.
--
--        코스 기록에서는 null 로 남는다. 기존 자유기록도 null 이며,
--        조회 시 courseName ?? recordName 으로 처리한다.
-- =====================================================================

ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS name varchar(100);
