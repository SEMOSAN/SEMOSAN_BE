-- =====================================================================
-- V18__update_user_onboarding_hiking_level_check.sql
-- 목적: user_onboardings.hiking_level CHECK 제약을 현재 HikingLevel enum과 맞춘다.
-- 배경: 기존 DB 제약은 BEGINNER, INTERMEDIATE, EXPERT만 허용해
--       신규 enum 값(EXPERIENCED, HOBBY) 저장 시 INSERT가 실패함.
--       기존 INTERMEDIATE 데이터는 현재 enum의 EXPERIENCED로 변환한다.
-- 멱등: UPDATE 후 DROP IF EXISTS -> ADD 패턴.
-- =====================================================================

UPDATE user_onboardings
SET hiking_level = 'EXPERIENCED'
WHERE hiking_level = 'INTERMEDIATE';

ALTER TABLE user_onboardings
    DROP CONSTRAINT IF EXISTS user_onboardings_hiking_level_check;

ALTER TABLE user_onboardings
    ADD CONSTRAINT user_onboardings_hiking_level_check
        CHECK (hiking_level IN (
            'BEGINNER',
            'EXPERIENCED',
            'HOBBY',
            'EXPERT'
        ));
