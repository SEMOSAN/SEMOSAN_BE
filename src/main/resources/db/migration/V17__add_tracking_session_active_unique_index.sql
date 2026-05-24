-- 유저당 활성 트래킹 세션(IN_PROGRESS/PAUSED)은 1개만 허용한다.
-- 애플리케이션의 exists -> save 검증만으로는 동시 생성 요청을 막을 수 없어서 DB 제약으로 보강한다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tracking_sessions_user_active
    ON tracking_sessions (user_id)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');
