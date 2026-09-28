-- =====================================================================
-- V46__add_unique_tracking_photo_milestone.sql
-- 목적: "마일스톤당 사진 1장" 정책을 DB 제약으로 보장한다.
-- 배경: TrackingPhotoService 의 exists 체크와 save 사이가 원자적이지 않아
--       동시 요청(셔터 연타, 타임아웃 후 재시도) 시 같은
--       (tracking_session_id, milestone_index) 조합이 2행 저장될 수 있다.
-- 비고: 새 유니크 제약이 기존 복합 인덱스를 대체하므로 함께 제거한다.
-- 멱등: DROP IF EXISTS → ADD 패턴 (V45 선례와 동일).
-- =====================================================================

DROP INDEX IF EXISTS idx_tracking_photos_session_milestone;

ALTER TABLE tracking_photos
    DROP CONSTRAINT IF EXISTS uk_tracking_photos_session_milestone;

ALTER TABLE tracking_photos
    ADD CONSTRAINT uk_tracking_photos_session_milestone
        UNIQUE (tracking_session_id, milestone_index);
