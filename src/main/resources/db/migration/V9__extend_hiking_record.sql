-- =====================================================================
-- V9__extend_hiking_record.sql
-- 목적: 트래킹 흐름에서 생성될 HikingRecord 확장.
--        - 트래킹 세션 참조 (tracking_session_id, UNIQUE — 세션당 기록 1건 강제)
--        - 시간 정보 (started_at, ended_at, paused_seconds_total)
--        - GPS 통계 (distance, ascent, descent)
--        - 자유 기록 대응 (course_id NOT NULL → NULLABLE)
-- =====================================================================

-- 1) course_id 를 nullable 로 (자유 기록 대응)
ALTER TABLE hiking_records
    ALTER COLUMN course_id DROP NOT NULL;

-- 2) 트래킹 세션 참조
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS tracking_session_id bigint;

-- 3) 시간 정보
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS started_at timestamp(6);
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS ended_at timestamp(6);
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS paused_seconds_total integer not null default 0;

-- 4) GPS 통계
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS distance double precision;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS ascent double precision;
ALTER TABLE hiking_records
    ADD COLUMN IF NOT EXISTS descent double precision;

-- 5) FK + UNIQUE 제약 (NULL 은 표준 UNIQUE 검사에서 제외돼 수동 기록은 영향 없음)
ALTER TABLE hiking_records
    ADD CONSTRAINT fk_hiking_records_tracking_session
        FOREIGN KEY (tracking_session_id) REFERENCES tracking_sessions(id);

ALTER TABLE hiking_records
    ADD CONSTRAINT uk_hiking_records_tracking_session
        UNIQUE (tracking_session_id);
