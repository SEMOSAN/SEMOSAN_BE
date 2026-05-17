-- =====================================================================
-- 003_tracking_photos.sql
-- 목적: 트래킹 마일스톤 사진 메타 저장 테이블 생성.
--        실제 이미지 바이너리는 MinIO 에 보관되고 본 테이블엔 URL 만 저장.
-- 적용 시점: PR #46 머지 직후. 1회성.
-- =====================================================================

CREATE TABLE IF NOT EXISTS tracking_photos (
    id                    BIGSERIAL PRIMARY KEY,
    tracking_session_id   BIGINT       NOT NULL REFERENCES tracking_sessions(id),
    milestone_index       INTEGER      NOT NULL,
    milestone_distance_m  DOUBLE PRECISION NOT NULL,
    image_url             VARCHAR(500) NOT NULL,
    captured_at           TIMESTAMP    NOT NULL,
    lat                   DOUBLE PRECISION NOT NULL,
    lng                   DOUBLE PRECISION NOT NULL,
    altitude              DOUBLE PRECISION,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tracking_photos_session
    ON tracking_photos (tracking_session_id);

CREATE INDEX IF NOT EXISTS idx_tracking_photos_session_milestone
    ON tracking_photos (tracking_session_id, milestone_index);

-- =====================================================================
-- notifications.type CHECK 제약 갱신
-- Hibernate 가 enum 컬럼 생성 시점에 그 시점 값만으로 CHECK 를 만들고
-- 새 enum 값 추가 시 자동 갱신하지 않으므로 수동 갱신 필요.
-- TRACKING_PHOTO_MILESTONE 추가 — 추후 enum 값 추가 시 동일 패턴으로 갱신.
-- =====================================================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('COMMUNITY_COMMENT', 'TRACKING_PHOTO_MILESTONE'));
