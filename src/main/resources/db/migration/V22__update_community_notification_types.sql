-- =====================================================================
-- V22__update_community_notification_types.sql
-- 목적: notifications.type CHECK 제약에 커뮤니티 답글/좋아요 알림 타입을 포함시킨다.
-- 배경: Hibernate enum 컬럼 CHECK 는 새 enum 추가 시 자동 갱신되지 않는다.
-- =====================================================================

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'COMMUNITY_COMMENT',
        'COMMUNITY_REPLY',
        'COMMUNITY_POST_LIKE',
        'TRACKING_PHOTO_MILESTONE',
        'TRACKING_SUMMIT_REACHED'
    ));
