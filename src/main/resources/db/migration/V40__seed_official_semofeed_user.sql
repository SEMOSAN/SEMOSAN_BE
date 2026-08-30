-- =====================================================================
-- V40__seed_official_semofeed_user.sql
-- 목적: 관리자 세모피드 업로드용 공식 계정(세모산)을 시드한다.
--       관리자(Admin)는 User가 아니므로, 관리자가 올리는 세모피드의 작성자로
--       사용할 전용 공식 User 계정을 하나 만든다. (oauth_id=semosan_official)
-- 멱등: 이미 존재하면 아무 것도 하지 않는다.
-- =====================================================================

INSERT INTO users (created_at, updated_at, is_deleted, device_type, oauth_id, oauth_provider, onboarding_status, nickname)
SELECT now(), now(), false,
       'IOS'::device_type_enum,
       'semosan_official',
       'TEST'::oauth_provider_enum,
       'COMPLETE'::onboarding_status_enum,
       '세모산'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE oauth_id = 'semosan_official' AND oauth_provider = 'TEST'
);
