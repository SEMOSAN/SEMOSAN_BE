-- =====================================================================
-- V45__update_transportations_type_check.sql
-- 목적: transportations.type CHECK 제약에 TRAIN 값을 포함시킨다.
-- 배경: Hibernate 가 enum 컬럼 CHECK 를 자동 생성하지만 새 enum 추가(#431: TRAIN) 시
--       자동 갱신하지 않아, TRAIN 저장 시 transportations_type_check 위반이 발생한다.
--       (notifications 의 V17 선례와 동일한 패턴)
-- 멱등: DROP IF EXISTS → ADD 패턴.
-- =====================================================================

ALTER TABLE transportations DROP CONSTRAINT IF EXISTS transportations_type_check;

ALTER TABLE transportations ADD CONSTRAINT transportations_type_check
    CHECK (type IN ('SUBWAY', 'BUS', 'PARKING', 'TRAIN'));
