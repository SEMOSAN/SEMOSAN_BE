-- 유저당 활성 트래킹 세션(IN_PROGRESS/PAUSED)은 1개만 허용한다.
-- 애플리케이션의 exists -> save 검증만으로는 동시 생성 요청을 막을 수 없어서 DB 제약으로 보강한다.
-- 기존 중복 데이터가 있으면 사용자별 최신 활성 세션 1건만 유지하고 나머지는 ABANDONED 처리한다.
WITH ranked_active_sessions AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY started_at DESC, id DESC
        ) AS row_number
    FROM tracking_sessions
    WHERE status IN ('IN_PROGRESS', 'PAUSED')
)
UPDATE tracking_sessions ts
SET status = 'ABANDONED',
    ended_at = COALESCE(ts.ended_at, now()),
    updated_at = now()
FROM ranked_active_sessions ras
WHERE ts.id = ras.id
  AND ras.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tracking_sessions_user_active
    ON tracking_sessions (user_id)
    WHERE status IN ('IN_PROGRESS', 'PAUSED');
