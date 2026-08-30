-- =====================================================================
-- V43__create_admin_login_lockouts.sql
-- 목적: 관리자 로그인 브루트포스 잠금(#353)을 위한 원자적 실패 카운터.
--       INSERT ... ON CONFLICT DO UPDATE 로 락 대기 없이 증가+판정을
--       단일 문장에서 처리한다 (advisory lock은 커넥션 풀 고갈 위험 있어 폐기).
-- =====================================================================

CREATE TABLE admin_login_lockouts (
    username           VARCHAR(50) PRIMARY KEY,
    fail_count         INTEGER     NOT NULL DEFAULT 0,
    window_started_at  TIMESTAMP   NOT NULL DEFAULT now()
);
