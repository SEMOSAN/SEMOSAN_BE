-- =====================================================================
-- V17__update_notifications_type_check.sql
-- 목적: notifications.type CHECK 제약에 TRACKING_SUMMIT_REACHED 값을 포함시킨다.
-- 배경: Hibernate 가 enum 컬럼 CHECK 를 자동 생성하지만 새 enum 추가 시 자동 갱신하지 않음.
--       기존 enum 값(COMMUNITY_COMMENT, TRACKING_PHOTO_MILESTONE) + 신규(TRACKING_SUMMIT_REACHED)
--       으로 제약을 재정의해 INSERT 실패를 막는다.
-- 멱등: DROP IF EXISTS → ADD 패턴.
-- =====================================================================

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'COMMUNITY_COMMENT',
        'TRACKING_PHOTO_MILESTONE',
        'TRACKING_SUMMIT_REACHED'
    ));
