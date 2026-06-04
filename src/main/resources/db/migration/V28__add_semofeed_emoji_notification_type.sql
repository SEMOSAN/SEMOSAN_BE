-- =====================================================================
-- V28__add_semofeed_emoji_notification_type.sql
-- 목적: notifications.type CHECK 제약에 SEMOFEED_EMOJI 타입을 포함시킨다.
-- 배경: V22 에서 COMMUNITY_REPLY/COMMUNITY_POST_LIKE 추가 시 SEMOFEED_EMOJI 가 누락됨.
-- 멱등: DROP IF EXISTS → ADD 패턴.
-- =====================================================================

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'COMMUNITY_COMMENT',
        'COMMUNITY_REPLY',
        'COMMUNITY_POST_LIKE',
        'SEMOFEED_EMOJI',
        'TRACKING_PHOTO_MILESTONE',
        'TRACKING_SUMMIT_REACHED'
    ));
