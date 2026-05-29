-- =====================================================================
-- V25__update_user_onboarding_exercise_type_check.sql
-- 목적: user_onboardings.exercise_type CHECK 제약을 현재 ExerciseType enum과 맞춘다.
-- 배경: 기존 DB 제약은 CARDIO, PILATES, YOGA, WALKING_HIKING를 허용하지만
--       현재 enum은 WALKING, RUNNING, PILATES_YOGA, HIKING를 사용한다.
--       신규 enum 값 저장 시 UPDATE/INSERT가 실패하지 않도록 기존 데이터를 변환한다.
-- 멱등: DROP IF EXISTS -> UPDATE -> ADD 패턴.
-- =====================================================================

ALTER TABLE user_onboardings
    DROP CONSTRAINT IF EXISTS user_onboardings_exercise_type_check;

UPDATE user_onboardings
SET exercise_type = 'WALKING'
WHERE exercise_type = 'CARDIO';

UPDATE user_onboardings
SET exercise_type = 'PILATES_YOGA'
WHERE exercise_type IN ('PILATES', 'YOGA');

UPDATE user_onboardings
SET exercise_type = 'HIKING'
WHERE exercise_type = 'WALKING_HIKING';

ALTER TABLE user_onboardings
    ADD CONSTRAINT user_onboardings_exercise_type_check
        CHECK (exercise_type IN (
            'GYM',
            'HOME_TRAINING',
            'PILATES_YOGA',
            'WALKING',
            'RUNNING',
            'HIKING',
            'SPORTS',
            'CROSSFIT',
            'SWIMMING',
            'NONE'
        ));
