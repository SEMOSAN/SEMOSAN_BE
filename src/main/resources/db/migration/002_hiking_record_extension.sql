-- =====================================================================
-- 002_hiking_record_extension.sql
-- 목적: 트래킹 흐름에서 생성될 HikingRecord 가 가져야 할 필드들 추가.
--        - 트래킹 세션 참조 (tracking_session_id)
--        - 시간 정보 (started_at, ended_at, paused_seconds_total)
--        - GPS 통계 (distance, ascent, descent)
--        - 자유 기록 대응 (course_id NOT NULL → NULLABLE)
-- 적용 시점: PR #20 머지 직후. 1회성.
-- idempotent: ADD COLUMN IF NOT EXISTS / DROP NOT NULL 사용.
-- =====================================================================

-- 1) course_id 를 nullable 로 (자유 기록 대응)
ALTER TABLE hiking_records
    ALTER COLUMN course_id DROP NOT NULL;

-- 2) 트래킹 세션 참조
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS tracking_session_id BIGINT;

-- 3) 시간 정보
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS paused_seconds_total INTEGER NOT NULL DEFAULT 0;

-- 4) GPS 통계
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS distance DOUBLE PRECISION;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS ascent DOUBLE PRECISION;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS descent DOUBLE PRECISION;

-- 5) FK 제약 (tracking_sessions 테이블이 먼저 존재해야 함 — 본 마이그레이션 이전에 tracking_sessions DDL 적용 필요)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'hiking_records'
          AND constraint_name = 'fk_hiking_records_tracking_session'
    ) THEN
        ALTER TABLE hiking_records
            ADD CONSTRAINT fk_hiking_records_tracking_session
            FOREIGN KEY (tracking_session_id) REFERENCES tracking_sessions(id);
    END IF;
END$$;

-- 6) 조회 인덱스 (HikingRecord 로 트래킹 세션 역방향 찾을 일은 거의 없으나, 디버그/분석용)
CREATE INDEX IF NOT EXISTS idx_hiking_records_tracking_session
    ON hiking_records (tracking_session_id);
